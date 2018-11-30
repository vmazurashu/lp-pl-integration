/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
 * Copyright (C) 2010-2011 Igalia, S.L.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.libreplan.web.planner.allocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.libreplan.business.common.Flagged;
import org.libreplan.business.common.IAdHocTransactionService;
import org.libreplan.business.common.IOnTransaction;
import org.libreplan.business.common.ProportionalDistributor;
import org.libreplan.business.orders.entities.AggregatedHoursGroup;
import org.libreplan.business.planner.entities.DayAssignment;
import org.libreplan.business.planner.entities.DerivedAllocation;
import org.libreplan.business.planner.entities.DerivedAllocationGenerator.IWorkerFinder;
import org.libreplan.business.planner.entities.ResourceAllocation;
import org.libreplan.business.planner.entities.Task;
import org.libreplan.business.planner.entities.TaskElement;
import org.libreplan.business.resources.daos.IResourceDAO;
import org.libreplan.business.resources.daos.IResourcesSearcher;
import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.resources.entities.Machine;
import org.libreplan.business.resources.entities.MachineWorkersConfigurationUnit;
import org.libreplan.business.resources.entities.Resource;
import org.libreplan.business.resources.entities.ResourceEnum;
import org.libreplan.business.resources.entities.Worker;
import org.libreplan.web.planner.allocation.AllocationRowsHandler.Warnings;
import org.libreplan.web.planner.order.PlanningStateCreator.PlanningState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zkoss.ganttz.extensions.IContextWithPlannerTask;

/**
 * Model for UI operations related to {@link Task}.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 * @author Diego Pino García <dpino@igalia.com>
 * @author Javier Moran Rua <jmoran@igalia.com>
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ResourceAllocationModel implements IResourceAllocationModel {

    @Autowired
    private IResourceDAO resourceDAO;

    @Autowired
    private IResourcesSearcher searchModel;

    private Task task;

    private PlanningState planningState;

    private AllocationRowsHandler allocationRowsHandler;

    private IContextWithPlannerTask<TaskElement> context;

    @Autowired
    private IAdHocTransactionService transactionService;

    private Date currentStartDate;

    @Override
    @Transactional(readOnly = true)
    public void addSpecific(Collection<? extends Resource> resources) {
        reassociateResourcesWithSession();
        allocationRowsHandler.addSpecificResourceAllocationFor(reloadResources(resources));
    }

    @SuppressWarnings("unchecked")
    private <T extends Resource> List<T> reloadResources(Collection<? extends T> resources) {
        List<T> result = new ArrayList<>();
        for (T each : resources) {
            Resource reloaded = resourceDAO.findExistingEntity(each.getId());
            reattachResource(reloaded);
            result.add((T) reloaded);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ProportionalDistributor addDefaultAllocations() {
        reassociateResourcesWithSession();
        List<AggregatedHoursGroup> hoursGroups = task.getAggregatedByCriterions();

        int hours[] = new int[hoursGroups.size()];
        int i = 0;
        for (AggregatedHoursGroup each : hoursGroups) {
            hours[i++] = each.getHours();
            List<? extends Resource> resourcesFound = searchModel.searchBy(each.getResourceType())
                    .byCriteria(each.getCriterions()).execute();

            boolean added = allocationRowsHandler.addGeneric(each.getResourceType(),
                    each.getCriterions(), reloadResources(resourcesFound),
                    each.getHours());

            if (!added) {
                return null;
            }
        }

        return ProportionalDistributor.create(hours);
    }

    @Override
    @Transactional(readOnly = true)
    public void addGeneric(ResourceEnum resourceType,
                           Collection<? extends Criterion> criteria,
                           Collection<? extends Resource> resourcesMatched) {

        reassociateResourcesWithSession();
        List<Resource> reloadResources = reloadResources(resourcesMatched);
        allocationRowsHandler.addGeneric(resourceType, criteria, reloadResources);
    }

    @Override
    public void cancel() {
        if (currentStartDate != null) {
            task.setStartDate(currentStartDate);
        }
        allocationRowsHandler = null;
        currentStartDate = null;
    }

    @Override
    public Flagged<AllocationResult, Warnings> accept() {
        if (context != null) {
            return applyDateChangesNotificationIfNoFlags(() -> {
                stepsBeforeDoingAllocation();
                Flagged<AllocationResult, Warnings> allocationResult = allocationRowsHandler.doAllocation();

                if (!allocationResult.isFlagged()) {
                    allocationResult.getValue().applyTo(
                            planningState.getCurrentScenario(), task);
                }

                return allocationResult;
            });
        }

        return null;
    }

    @Override
    public void accept(final AllocationResult modifiedAllocationResult) {
        if (context != null) {
            applyDateChangesNotificationIfNoFlags((IOnTransaction<Flagged<Void, Void>>) () -> {
                stepsBeforeDoingAllocation();
                modifiedAllocationResult.applyTo(planningState.getCurrentScenario(), task);

                return Flagged.justValue(null);
            });
        }
    }

    private <V, T extends Flagged<V, ?>> T applyDateChangesNotificationIfNoFlags(IOnTransaction<T> allocationDoer) {
        org.zkoss.ganttz.data.Task ganttTask = context.getTask();
        T result = transactionService.runOnReadOnlyTransaction(allocationDoer);
        if (!result.isFlagged()) {
            ganttTask.enforceDependenciesDueToPositionPotentiallyModified();
        }

        return result;
    }

    private void stepsBeforeDoingAllocation() {
        ensureResourcesAreReadyForDoingAllocation();
        removeDeletedAllocations();
    }

    @Override
    @Transactional(readOnly = true)
    public <T> T onAllocationContext(
            IResourceAllocationContext<T> resourceAllocationContext) {
        ensureResourcesAreReadyForDoingAllocation();
        return resourceAllocationContext.doInsideTransaction();
    }

    private void ensureResourcesAreReadyForDoingAllocation() {
        Set<Resource> resources = allocationRowsHandler.getAllocationResources();

        for (Resource each : resources) {
            reattachResource(each);
        }
    }

    private void reassociateResourcesWithSession() {
        planningState.reassociateResourcesWithSession();
    }

    private void removeDeletedAllocations() {
        Set<ResourceAllocation<?>> allocationsRequestedToRemove = allocationRowsHandler
                .getAllocationsRequestedToRemove();
        for (ResourceAllocation<?> resourceAllocation : allocationsRequestedToRemove) {
            task.removeResourceAllocation(resourceAllocation);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AllocationRowsHandler initAllocationsFor(Task task,
                                                    IContextWithPlannerTask<TaskElement> context,
                                                    PlanningState planningState) {
        this.context = context;
        this.task = task;
        this.currentStartDate = task.getStartDate();
        this.planningState = planningState;
        planningState.reassociateResourcesWithSession();
        loadDerivedAllocations(this.task.getSatisfiedResourceAllocations());
        List<AllocationRow> initialRows = AllocationRow.toRows(task.getNonLimitingResourceAllocations(), searchModel);
        allocationRowsHandler = AllocationRowsHandler.create(task, initialRows, createWorkerFinder());

        return allocationRowsHandler;
    }

    private IWorkerFinder createWorkerFinder() {
        return requiredCriteria -> {
            reassociateResourcesWithSession();
            List<Worker> workers = new ArrayList<>();

            if (!requiredCriteria.isEmpty()) {
                workers = searchModel.searchWorkers().byCriteria(requiredCriteria).execute();
            }

            return reloadResources(workers);
        };
    }

    private void loadMachine(Machine eachMachine) {
        for (MachineWorkersConfigurationUnit eachUnit : eachMachine.getConfigurationUnits()) {
            Hibernate.initialize(eachUnit);
        }
    }

    private void loadDerivedAllocations(Set<ResourceAllocation<?>> resourceAllocations) {
        for (ResourceAllocation<?> each : resourceAllocations) {
            for (DerivedAllocation eachDerived : each.getDerivedAllocations()) {
                Hibernate.initialize(eachDerived);
                eachDerived.getAssignments();
                eachDerived.getAlpha();
                eachDerived.getName();
            }
        }
    }

    private void reattachResource(Resource resource) {
        resourceDAO.reattach(resource);

        for (DayAssignment dayAssignment : resource.getAssignments()) {
            Hibernate.initialize(dayAssignment);
        }

        if (resource instanceof Machine) {
            loadMachine((Machine) resource);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AggregatedHoursGroup> getHoursAggregatedByCriterions() {
        List<AggregatedHoursGroup> result = task.getTaskSource().getAggregatedByCriterions();
        ensuringAccesedPropertiesAreLoaded(result);

        return result;
    }

    private void ensuringAccesedPropertiesAreLoaded(List<AggregatedHoursGroup> result) {
        for (AggregatedHoursGroup each : result) {
            each.getCriterionsJoinedByComma();
            each.getHours();
        }
    }

    @Override
    public Integer getOrderHours() {
        if (task == null) {
            return 0;
        }

        return AggregatedHoursGroup.sum(task.getAggregatedByCriterions());
    }

    @Override
    public Date getTaskEnd() {
        if (task == null) {
            return null;
        }

        return task.getEndDate();
    }

    @Override
    public Date getTaskStart() {
        Date result;

        if (task == null) {
            result = null;
        } else {
            result = task.getStartDate();
        }

        return result;
    }

}

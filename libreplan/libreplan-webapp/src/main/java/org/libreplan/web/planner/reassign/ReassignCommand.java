/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
 * Copyright (C) 2010-2012 Igalia, S.L.
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
package org.libreplan.web.planner.reassign;

import static org.libreplan.web.I18nHelper._;
import static org.zkoss.ganttz.util.LongOperationFeedback.and;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.Validate;
import org.libreplan.business.common.IAdHocTransactionService;
import org.libreplan.business.common.IOnTransaction;
import org.libreplan.business.planner.daos.ITaskElementDAO;
import org.libreplan.business.planner.entities.GenericResourceAllocation;
import org.libreplan.business.planner.entities.ResourceAllocation;
import org.libreplan.business.planner.entities.TaskElement;
import org.libreplan.business.resources.daos.ICriterionTypeDAO;
import org.libreplan.business.resources.daos.IResourcesSearcher;
import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.resources.entities.CriterionType;
import org.libreplan.web.planner.order.PlanningStateCreator.PlanningState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.zkoss.ganttz.adapters.IDomainAndBeansMapper;
import org.zkoss.ganttz.data.Dependency;
import org.zkoss.ganttz.data.GanttDate;
import org.zkoss.ganttz.data.GanttDiagramGraph;
import org.zkoss.ganttz.data.Task;
import org.zkoss.ganttz.extensions.IContext;
import org.zkoss.ganttz.util.IAction;
import org.zkoss.ganttz.util.LongOperationFeedback;
import org.zkoss.ganttz.util.LongOperationFeedback.IBackGroundOperation;
import org.zkoss.ganttz.util.LongOperationFeedback.IDesktopUpdate;
import org.zkoss.ganttz.util.LongOperationFeedback.IDesktopUpdatesEmitter;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Messagebox;

/**
 * Handles reassign functionality.
 * There is a green button on Project Scheduling page ( toolbar section ).
 *
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 * @author Manuel Rego Casasnovas <rego@igalia.com>
 * @author Vova Perebykivskyi <vova@libreplan-enterprise.com>
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ReassignCommand implements IReassignCommand {

    private PlanningState planningState;

    @Autowired
    private IAdHocTransactionService transactionService;

    @Autowired
    private IResourcesSearcher resourcesSearcher;

    @Autowired
    private ITaskElementDAO taskElementDAO;

    @Autowired
    private ICriterionTypeDAO criterionTypeDAO;

    public interface IConfigurationResult {
        void result(ReassignConfiguration configuration);
    }

    @Override
    public void setState(PlanningState planningState) {
        Validate.notNull(planningState);
        this.planningState = planningState;
    }

    @Override
    public void doAction(final IContext<TaskElement> context) {
        ReassignController.openOn(context.getRelativeTo(), configuration ->  {
            final List<WithAssociatedEntity> reassignations = getReassignations(context, configuration);
            LongOperationFeedback.progressive(getDesktop(context), reassignations(context, reassignations));
        });
    }

    private IBackGroundOperation<IDesktopUpdate> reassignations(
            final IContext<TaskElement> context, final List<WithAssociatedEntity> reassignations) {

        return updater -> {
            updater.doUpdate(busyStart(reassignations.size()));
            GanttDiagramGraph<Task, Dependency>.DeferedNotifier notifications = null;
            try {
                GanttDiagramGraph<Task, Dependency> ganttDiagramGraph = context.getGanttDiagramGraph();

                notifications = ganttDiagramGraph.manualNotificationOn(
                        doReassignations(ganttDiagramGraph, reassignations, updater));
            } finally {
                if (notifications != null) {

                    // null if error
                    updater.doUpdate(and(
                            doNotifications(notifications),
                            reloadCharts(context),
                            busyEnd(),
                            tellUserOnEnd(context, () -> _("{0} reassignations finished", reassignations.size()))));
                } else {
                    updater.doUpdate(and(
                            busyEnd(),
                            tellUserOnEnd(context, () -> _("Assignments could not be completed"))));
                }
            }
        };
    }

    private IAction doReassignations(final GanttDiagramGraph<Task, Dependency> diagramGraph,
                                     final List<WithAssociatedEntity> reassignations,
                                     final IDesktopUpdatesEmitter<IDesktopUpdate> updater) {
        return () -> {
            int i = 1;
            final int total = reassignations.size();

            for (final WithAssociatedEntity each : reassignations) {
                Task ganttTask = each.ganntTask;
                GanttDate previousStart = ganttTask.getBeginDate();
                GanttDate previousEnd = ganttTask.getEndDate();

                transactionService.runOnReadOnlyTransaction(reassignmentTransaction(each));
                diagramGraph.enforceRestrictions(each.ganntTask);
                ganttTask.enforceDependenciesDueToPositionPotentiallyModified();
                ganttTask.updateSizeDueToDateChanges(previousStart, previousEnd);

                updater.doUpdate(showCompleted(i, total));
                i++;
            }
        };
    }

    private IDesktopUpdate busyStart(final int total) {
        return () -> Clients.showBusy(_("Doing {0} reassignations", total));
    }

    private IDesktopUpdate showCompleted(final int number, final int total) {
        return () -> Clients.showBusy(_("Done {0} of {1}", number, total));
    }

    private IDesktopUpdate reloadCharts(final IContext<?> context) {
        return () -> context.reloadCharts();
    }

    private IDesktopUpdate doNotifications(final GanttDiagramGraph<Task, Dependency>.DeferedNotifier notifier) {
        return () -> notifier.doNotifications();
    }

    /**
     * After migration from ZK5 to ZK8 API has changed for {@link Clients#clearBusy()}.
     * My investigation:
     * http://forum.zkoss.org/question/101181/infinite-clientsshowbusy/?answer=101256#post-id-101256
     */
    private IDesktopUpdate busyEnd() {
        return () -> Clients.clearBusy();
    }

    private IDesktopUpdate tellUserOnEnd(final IContext<TaskElement> context,
                                         final Callable<String> message) {

        // Using callable so the message is built inside a zk execution and the locale is correctly retrieved

        return new IDesktopUpdate() {

            @Override
            public void doUpdate() {
                final org.zkoss.zk.ui.Component relativeTo = context.getRelativeTo();
                final String eventName = "onLater";

                Events.echoEvent(eventName, relativeTo, null);

                relativeTo.addEventListener(eventName, new EventListener() {

                    @Override
                    public void onEvent(Event event) {
                        relativeTo.removeEventListener(eventName, this);
                        try {
                            Messagebox.show(
                                    resolve(message),
                                    _("Reassignation"),
                                    Messagebox.OK, Messagebox.INFORMATION);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        };
    }

    private <T> T resolve(Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class WithAssociatedEntity {

        private TaskElement domainEntity;

        private Task ganntTask;

        WithAssociatedEntity(TaskElement domainEntity, Task ganntTask) {
            Validate.notNull(domainEntity);
            Validate.notNull(ganntTask);
            this.domainEntity = domainEntity;
            this.ganntTask = ganntTask;
        }

        static WithAssociatedEntity create(IDomainAndBeansMapper<TaskElement> mapper, Task each) {
            return new WithAssociatedEntity(mapper.findAssociatedDomainObject(each), each);
        }
    }

    private List<WithAssociatedEntity> getReassignations(
            IContext<TaskElement> context, ReassignConfiguration configuration) {

        Validate.notNull(configuration);
        List<Task> taskToReassign = configuration.filterForReassignment(context.getTasksOrderedByStartDate());

        return withEntities(context.getMapper(), taskToReassign);
    }

    private List<WithAssociatedEntity> withEntities(
            IDomainAndBeansMapper<TaskElement> mapper,
            List<Task> forReassignment) {

        List<WithAssociatedEntity> result = new ArrayList<>();
        for (Task each : forReassignment) {
            result.add(WithAssociatedEntity.create(mapper, each));
        }

        return result;
    }

    private IOnTransaction<Void> reassignmentTransaction(final WithAssociatedEntity withAssociatedEntity) {
        return () -> {
            reattach(withAssociatedEntity);
            reassign(withAssociatedEntity.domainEntity);

            return null;
        };
    }

    private void reattach(WithAssociatedEntity each) {
        planningState.reassociateResourcesWithSession();
        Set<Long> idsOfTypesAlreadyAttached = new HashSet<>();
        taskElementDAO.reattach(each.domainEntity);
        Set<ResourceAllocation<?>> resourceAllocations = each.domainEntity.getSatisfiedResourceAllocations();

        List<GenericResourceAllocation> generic =
                ResourceAllocation.getOfType(GenericResourceAllocation.class, resourceAllocations);

        reattachCriterionTypesToAvoidLazyInitializationExceptionOnType(idsOfTypesAlreadyAttached, generic);
    }

    private void reattachCriterionTypesToAvoidLazyInitializationExceptionOnType(
            Set<Long> idsOfTypesAlreadyAttached,
            List<GenericResourceAllocation> generic) {

        for (GenericResourceAllocation eachGenericAllocation : generic) {
            Set<Criterion> criterions = eachGenericAllocation.getCriterions();

            for (Criterion eachCriterion : criterions) {
                CriterionType type = eachCriterion.getType();

                if (!idsOfTypesAlreadyAttached.contains(type.getId())) {
                    idsOfTypesAlreadyAttached.add(type.getId());
                    criterionTypeDAO.reattachUnmodifiedEntity(type);
                }
            }
        }
    }

    private void reassign(TaskElement taskElement) {
        org.libreplan.business.planner.entities.Task t = (org.libreplan.business.planner.entities.Task) taskElement;
        t.reassignAllocationsWithNewResources(planningState.getCurrentScenario(), resourcesSearcher);
    }

    @Override
    public String getName() {
        return _("Reassign");
    }

    @Override
    public String getImage() {
        return "/common/img/ico_reassign.png";
    }

    private Desktop getDesktop(final IContext<TaskElement> context) {
        return context.getRelativeTo().getDesktop();
    }

    @Override
    public boolean isDisabled() {
        return false;
    }

    @Override
    public boolean isPlannerCommand() {
        return true;
    }

}

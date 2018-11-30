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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.joda.time.LocalDate;
import org.libreplan.business.planner.entities.AggregateOfResourceAllocations;
import org.libreplan.business.planner.entities.CalculatedValue;
import org.libreplan.business.planner.entities.GenericResourceAllocation;
import org.libreplan.business.planner.entities.ResourceAllocation;
import org.libreplan.business.planner.entities.ResourceAllocation.Direction;
import org.libreplan.business.planner.entities.SpecificResourceAllocation;
import org.libreplan.business.planner.entities.Task;
import org.libreplan.business.planner.entities.Task.ModifiedAllocation;
import org.libreplan.business.scenarios.entities.Scenario;
import org.libreplan.business.workingday.IntraDayDate;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 *
 */
public class AllocationResult {

    public static AllocationResult create(Task task,
            CalculatedValue calculatedValue, List<AllocationRow> rows,
            Integer newWorkableDays) {
        List<ResourceAllocation<?>> newAllocations = AllocationRow
                .getNewFrom(rows);
        List<ModifiedAllocation> modified = AllocationRow.getModificationsDone(rows);
        return new AllocationResult(task, newWorkableDays, calculatedValue,
                createAggregate(newAllocations, modified), newAllocations,
                modified);
    }

    private static AggregateOfResourceAllocations createAggregate(
            List<ResourceAllocation<?>> newAllocations,
            List<ModifiedAllocation> modified) {
        List<ResourceAllocation<?>> all = new ArrayList<ResourceAllocation<?>>();
        all.addAll(newAllocations);
        all.addAll(ModifiedAllocation.modified(modified));
        return AggregateOfResourceAllocations.createFromSatisfied(all);
    }

    public static AllocationResult createCurrent(Scenario scenario, Task task) {
        Set<ResourceAllocation<?>> resourceAllocations = task
                .getSatisfiedResourceAllocations();
        List<ModifiedAllocation> modifiedAllocations = ModifiedAllocation.copy(
                scenario, resourceAllocations);
        AggregateOfResourceAllocations aggregate = AggregateOfResourceAllocations
                .createFromSatisfied(ModifiedAllocation
                        .modified(modifiedAllocations));
        return new AllocationResult(task, task.getSpecifiedWorkableDays(),
                task.getCalculatedValue(), aggregate,
                Collections.<ResourceAllocation<?>> emptyList(),
                modifiedAllocations);

    }

    private final AggregateOfResourceAllocations aggregate;

    private final CalculatedValue calculatedValue;

    private final Task task;

    private final List<ResourceAllocation<?>> newAllocations;

    private final List<Task.ModifiedAllocation> modified;

    /**
     * The number of workable days with wich the allocation has been done. Can
     * be <code>null</code>
     */
    private final Integer newWorkableDays;

    private AllocationResult(Task task, Integer newWorkableDays,
            CalculatedValue calculatedValue,
            AggregateOfResourceAllocations aggregate,
            List<ResourceAllocation<?>> newAllocations,
            List<Task.ModifiedAllocation> modified) {
        Validate.notNull(aggregate);
        Validate.notNull(calculatedValue);
        Validate.notNull(task);
        this.task = task;
        this.newWorkableDays = newWorkableDays;
        this.calculatedValue = calculatedValue;
        this.aggregate = aggregate;
        this.newAllocations = newAllocations;
        this.modified = modified;
    }

    public AggregateOfResourceAllocations getAggregate() {
        return aggregate;
    }

    private List<ResourceAllocation<?>> getNew() {
        return newAllocations;
    }

    private List<Task.ModifiedAllocation> getModified() {
        return modified;
    }

    public CalculatedValue getCalculatedValue() {
        return calculatedValue;
    }

    public void applyTo(Scenario scenario, Task task) {
        List<ModifiedAllocation> modified = getModified();
        if (aggregate.isEmpty()) {
            return;
        }
        task.mergeAllocation(scenario, getIntraDayStart(), getIntraDayEnd(),
                newWorkableDays,
                getCalculatedValue(), getNew(), modified,
                getNotModified(originals(modified)));
    }

    private List<ResourceAllocation<?>> originals(
            List<ModifiedAllocation> modified) {
        List<ResourceAllocation<?>> result = new ArrayList<ResourceAllocation<?>>();
        for (ModifiedAllocation modifiedAllocation : modified) {
            result.add(modifiedAllocation.getOriginal());
        }
        return result;
    }

    private Set<ResourceAllocation<?>> getNotModified(
            List<ResourceAllocation<?>> modified) {
        Set<ResourceAllocation<?>> all = new HashSet<ResourceAllocation<?>>(
                task.getSatisfiedResourceAllocations());
        all.removeAll(modified);
        return all;
    }

    public List<ResourceAllocation<?>> getAllSortedByStartDate() {
        return aggregate.getAllocationsSortedByStartDate();
    }

    public Task getTask() {
        return task;
    }

    public List<GenericResourceAllocation> getGenericAllocations() {
        return onlyGeneric(getAllSortedByStartDate());
    }

    private List<GenericResourceAllocation> onlyGeneric(
            List<ResourceAllocation<?>> allocations) {
        List<GenericResourceAllocation> result = new ArrayList<GenericResourceAllocation>();
        for (ResourceAllocation<?> resourceAllocation : allocations) {
            if (resourceAllocation instanceof GenericResourceAllocation) {
                result.add((GenericResourceAllocation) resourceAllocation);
            }
        }
        return result;
    }

    public List<SpecificResourceAllocation> getSpecificAllocations() {
        return onlySpecific(getAllSortedByStartDate());
    }

    private List<SpecificResourceAllocation> onlySpecific(
            List<ResourceAllocation<?>> allocations) {
        List<SpecificResourceAllocation> result = new ArrayList<SpecificResourceAllocation>();
        for (ResourceAllocation<?> r : allocations) {
            if (r instanceof SpecificResourceAllocation) {
                result.add((SpecificResourceAllocation) r);
            }
        }
        return result;
    }

    public LocalDate getStart() {
        return task.getStartAsLocalDate();
    }

    private boolean isForwardsScheduled() {
        return Direction.FORWARD.equals(task.getAllocationDirection());
    }

    public IntraDayDate getIntraDayStart() {
        if (isForwardsScheduled() || aggregate.isEmpty()) {
            return task.getIntraDayStartDate();
        } else {
            return aggregate.getStart();
        }
    }

    public IntraDayDate getIntraDayEnd() {
        if (!isForwardsScheduled() || aggregate.isEmpty()) {
            return task.getIntraDayEndDate();
        } else {
            return aggregate.getEnd();
        }
    }

}

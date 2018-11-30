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

package org.libreplan.business.planner.entities;

import static org.libreplan.business.workingday.EffortDuration.hours;
import static org.libreplan.business.workingday.EffortDuration.zero;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import java.util.Objects;
import org.apache.commons.lang3.Validate;
import javax.validation.constraints.NotNull;
import org.joda.time.LocalDate;
import org.libreplan.business.calendars.entities.AvailabilityTimeLine;
import org.libreplan.business.calendars.entities.BaseCalendar;
import org.libreplan.business.calendars.entities.Capacity;
import org.libreplan.business.calendars.entities.CombinedWorkHours;
import org.libreplan.business.calendars.entities.ICalendar;
import org.libreplan.business.calendars.entities.SameWorkHoursEveryDay;
import org.libreplan.business.calendars.entities.ThereAreHoursOnWorkHoursCalculator;
import org.libreplan.business.calendars.entities.ThereAreHoursOnWorkHoursCalculator.CapacityResult;
import org.libreplan.business.common.BaseEntity;
import org.libreplan.business.common.Registry;
import org.libreplan.business.planner.entities.AssignedEffortForResource.IAssignedEffortForResource;
import org.libreplan.business.planner.entities.DerivedAllocationGenerator.IWorkerFinder;
import org.libreplan.business.planner.entities.allocationalgorithms.AllocationModification;
import org.libreplan.business.planner.entities.allocationalgorithms.AllocatorForTaskDurationAndSpecifiedResourcesPerDay;
import org.libreplan.business.planner.entities.allocationalgorithms.Distributor;
import org.libreplan.business.planner.entities.allocationalgorithms.EffortModification;
import org.libreplan.business.planner.entities.allocationalgorithms.ResourcesPerDayModification;
import org.libreplan.business.planner.entities.allocationalgorithms.UntilFillingHoursAllocator;
import org.libreplan.business.planner.entities.consolidations.Consolidation;
import org.libreplan.business.planner.limiting.entities.LimitingResourceQueueElement;
import org.libreplan.business.resources.daos.IResourcesSearcher;
import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.resources.entities.Machine;
import org.libreplan.business.resources.entities.MachineWorkersConfigurationUnit;
import org.libreplan.business.resources.entities.Resource;
import org.libreplan.business.scenarios.IScenarioManager;
import org.libreplan.business.scenarios.entities.Scenario;
import org.libreplan.business.util.deepcopy.OnCopy;
import org.libreplan.business.util.deepcopy.Strategy;
import org.libreplan.business.workingday.EffortDuration;
import org.libreplan.business.workingday.EffortDuration.IEffortFrom;
import org.libreplan.business.workingday.IntraDayDate;
import org.libreplan.business.workingday.IntraDayDate.PartialDay;
import org.libreplan.business.workingday.ResourcesPerDay;

/**
 * Resources are allocated to planner tasks.
 *
 * @author Diego Pino García <dpino@igalia.com>
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public abstract class ResourceAllocation<T extends DayAssignment>
        extends BaseEntity
        implements IAssignedEffortForResource {

    private Task task;

    private AssignmentFunction assignmentFunction;

    @OnCopy(Strategy.SHARE)
    private ResourcesPerDay resourcesPerDay;

    @OnCopy(Strategy.SHARE)
    private ResourcesPerDay intendedResourcesPerDay;

    private Integer intendedTotalHours;

    private Set<DerivedAllocation> derivedAllocations = new HashSet<>();

    @OnCopy(Strategy.SHARE_COLLECTION_ELEMENTS)
    private Set<LimitingResourceQueueElement> limitingResourceQueueElements = new HashSet<>();

    @OnCopy(Strategy.SHARE)
    private EffortDuration intendedTotalAssignment = zero();

    @OnCopy(Strategy.SHARE)
    private EffortDuration intendedNonConsolidatedEffort = zero();

    @OnCopy(Strategy.IGNORE)
    private DayAssignmentsState assignmentsState;

    private IOnDayAssignmentRemoval dayAssignmenteRemoval = new DoNothing();

    /**
     * Constructor for hibernate. Do not use!
     */
    public ResourceAllocation() {
        this.assignmentsState = buildFromDBState();
    }

    public static <T extends ResourceAllocation<?>> List<T> getSatisfied(Collection<T> resourceAllocations) {
        Validate.notNull(resourceAllocations);
        Validate.noNullElements(resourceAllocations);
        List<T> result = new ArrayList<>();

        for (T each : resourceAllocations) {
            if ( each.isSatisfied() ) {
                result.add(each);
            }
        }
        return result;
    }

    public static <T extends ResourceAllocation<?>> List<T> getOfType(
            Class<T> type, Collection<? extends ResourceAllocation<?>> resourceAllocations) {

        List<T> result = new ArrayList<>();
        for (ResourceAllocation<?> allocation : resourceAllocations) {
            if ( type.isInstance(allocation) ) {
                result.add(type.cast(allocation));
            }
        }
        return result;
    }

    public static <R extends ResourceAllocation<?>> Map<Resource, List<R>> byResource(
            Collection<? extends R> allocations) {

        Map<Resource, List<R>> result = new HashMap<>();
        for (R resourceAllocation : allocations) {

            for (Resource resource : resourceAllocation.getAssociatedResources()) {
                if ( !result.containsKey(resource) ) {
                    result.put(resource, new ArrayList<>());
                }
                result.get(resource).add(resourceAllocation);
            }
        }
        return result;
    }

    public static <R extends ResourceAllocation<?>> List<R> sortedByStartDate(Collection<? extends R> allocations) {
        List<R> result = new ArrayList<>(allocations);
        Collections.sort(result, byStartDateComparator());

        return result;
    }

    public static <R extends ResourceAllocation<?>> Map<Task, List<R>> byTask(List<? extends R> allocations) {
        Map<Task, List<R>> result = new LinkedHashMap<>();

        for (R resourceAllocation : allocations) {
            if ( resourceAllocation.getTask() != null ) {
                Task task = resourceAllocation.getTask();
                initializeIfNeeded(result, task);
                result.get(task).add(resourceAllocation);
            }
        }
        return result;
    }

    private static <E extends ResourceAllocation<?>> void initializeIfNeeded(Map<Task, List<E>> result, Task task) {
        if ( !result.containsKey(task) ) {
            result.put(task, new ArrayList<>());
        }
    }

    private static Comparator<ResourceAllocation<?>> byStartDateComparator() {
        return (o1, o2) -> {
            if ( o1.getIntraDayStartDate() == null ) {
                return -1;
            }

            if ( o2.getIntraDayStartDate() == null ) {
                return 1;
            }

            return o1.getIntraDayStartDate().compareTo(o2.getIntraDayStartDate());
        };
    }

    public enum Direction {
        FORWARD {
            @Override
            public IntraDayDate getDateFromWhichToAllocate(Task task) {
                return IntraDayDate.max(task.getFirstDayNotConsolidated(), task.getIntraDayStartDate());
            }

            @Override
            void limitAvailabilityOn(AvailabilityTimeLine availability, IntraDayDate dateFromWhichToAllocate) {
                availability.invalidUntil(dateFromWhichToAllocate.asExclusiveEnd());
            }
        },

        BACKWARD {
            @Override
            public IntraDayDate getDateFromWhichToAllocate(Task task) {
                return task.getIntraDayEndDate();
            }

            @Override
            void limitAvailabilityOn(AvailabilityTimeLine availability, IntraDayDate dateFromWhichToAllocate) {
                availability.invalidFrom(dateFromWhichToAllocate.getDate());
            }
        };

        public abstract IntraDayDate getDateFromWhichToAllocate(Task task);

        abstract void limitAvailabilityOn(AvailabilityTimeLine availability, IntraDayDate dateFromWhichToAllocate);

    }

    public static AllocationsSpecified allocating(List<ResourcesPerDayModification> resourceAllocations) {
        resourceAllocations = new ArrayList<>(resourceAllocations);
        sortBySpecificFirst(resourceAllocations);

        return new AllocationsSpecified(resourceAllocations);
    }

    /**
     * Specific allocations should be done first in order to generic allocations
     * selects the less charged resources if there are several allocations in the same task.
     *
     * @param resourceAllocations
     *            Sorted with specific allocations before generic ones
     */
    private static <T extends AllocationModification> void sortBySpecificFirst(List<T> resourceAllocations) {
        Collections.sort(resourceAllocations, new Comparator<AllocationModification>() {
            @Override
            public int compare(AllocationModification o1, AllocationModification o2) {
                if ( o1.isSpecific() && o2.isSpecific() ) {
                    return 0;
                }
                if ( o1.isSpecific() ) {
                    return -1;
                }
                if ( o2.isSpecific() ) {
                    return 1;
                }
                return 0;
            }
        });
    }

    private static void checkStartLessOrEqualToEnd(IntraDayDate startInclusive, IntraDayDate endExclusive) {
        Validate.isTrue(startInclusive.compareTo(endExclusive) <= 0, "the end must be equal or posterior to the start");
    }

    private static void checkStartLessOrEqualToEnd(LocalDate start, LocalDate end) {
        Validate.isTrue(start.compareTo(end) <= 0, "the end must be equal or posterior to the start");
    }

    /**
     * Needed for doing fluent interface calls:
     * <ul>
     *     <li>
     *         {@link ResourceAllocation#allocating(List)}.
     *         {@link AllocationsSpecified#untilAllocating(int) untiAllocating(int)}
     *     </li>
     *     <li>
     *         {@link ResourceAllocation#allocating(List)}.
     *         {@link AllocationsSpecified#allocateOnTaskLength() allocateOnTaskLength}
     *     </li>
     *     <li>
     *          {@link ResourceAllocation#allocating(List)}.
     *          {@link AllocationsSpecified#allocateUntil(LocalDate) allocateUntil(LocalDate)}
     *     </li>
     * </ul>
     */
    public static class AllocationsSpecified {

        private final List<ResourcesPerDayModification> allocations;

        private final Task task;

        public AllocationsSpecified(List<ResourcesPerDayModification> resourceAllocations) {
            Validate.notNull(resourceAllocations);
            Validate.notEmpty(resourceAllocations);
            Validate.noNullElements(resourceAllocations);
            checkNoOneHasNullTask(resourceAllocations);
            checkAllHaveSameTask(resourceAllocations);
            checkNoAllocationWithZeroResourcesPerDay(resourceAllocations);

            this.allocations = resourceAllocations;
            this.task = resourceAllocations.get(0).getBeingModified().getTask();
        }

        private static void checkNoAllocationWithZeroResourcesPerDay(List<ResourcesPerDayModification> allocations) {
            for (ResourcesPerDayModification r : allocations) {
                if (isZero(r.getGoal().getAmount())) {
                    throw new IllegalArgumentException("all resources per day must be no zero");
                }
            }
        }

        public static boolean isZero(BigDecimal amount) {
            return amount.movePointRight(amount.scale()).intValue() == 0;
        }

        private static void checkNoOneHasNullTask(List<ResourcesPerDayModification> allocations) {
            for (ResourcesPerDayModification resourcesPerDayModification : allocations) {
                if (resourcesPerDayModification.getBeingModified().getTask() == null) {
                    throw new IllegalArgumentException("all allocations must have task");
                }
            }
        }

        private static void checkAllHaveSameTask(List<ResourcesPerDayModification> resourceAllocations) {
            Task task = null;
            for (ResourcesPerDayModification r : resourceAllocations) {
                if (task == null) {
                    task = r.getBeingModified().getTask();
                }

                if (!task.equals(r.getBeingModified().getTask())) {
                    throw new IllegalArgumentException("all allocations must belong to the same task");
                }
            }
        }

        public interface INotFulfilledReceiver {
            void cantFulfill(ResourcesPerDayModification allocationAttempt, CapacityResult capacityResult);
        }

        public IntraDayDate untilAllocating(EffortDuration effort) {
            return untilAllocating(Direction.FORWARD, effort);
        }

        public IntraDayDate untilAllocating(Direction direction, EffortDuration effort) {
            return untilAllocating(direction, effort, doNothing());
        }

        private static INotFulfilledReceiver doNothing() {
            return (allocationAttempt, capacityResult) -> {};
        }

        public IntraDayDate untilAllocating(EffortDuration effort, final INotFulfilledReceiver receiver) {
            return untilAllocating(Direction.FORWARD, effort, receiver);
        }

        public IntraDayDate untilAllocating(
                Direction direction,
                EffortDuration toAllocate,
                final INotFulfilledReceiver receiver) {

            UntilFillingHoursAllocator allocator = new UntilFillingHoursAllocator(direction, task, allocations) {

                @Override
                protected <T extends DayAssignment> void setNewDataForAllocation(
                        ResourceAllocation<T> allocation,
                        IntraDayDate resultDate,
                        ResourcesPerDay resourcesPerDay,
                        List<T> dayAssignments) {

                    Task task = AllocationsSpecified.this.task;
                    allocation.setIntendedResourcesPerDay(resourcesPerDay);

                    if ( isForwardScheduling() ) {

                        allocation.resetAllAllocationAssignmentsTo(
                                dayAssignments, task.getIntraDayStartDate(), resultDate);

                    } else {

                        allocation.resetAllAllocationAssignmentsTo(
                                dayAssignments, resultDate, task.getIntraDayEndDate());

                    }
                    allocation.updateResourcesPerDay();
                }

                @Override
                protected CapacityResult thereAreAvailableHoursFrom(
                        IntraDayDate dateFromWhichToAllocate,
                        ResourcesPerDayModification resourcesPerDayModification,
                        EffortDuration effortToAllocate) {

                    ICalendar calendar = getCalendar(resourcesPerDayModification);
                    ResourcesPerDay resourcesPerDay = resourcesPerDayModification.getGoal();
                    AvailabilityTimeLine availability = resourcesPerDayModification.getAvailability();
                    getDirection().limitAvailabilityOn(availability, dateFromWhichToAllocate);

                    return ThereAreHoursOnWorkHoursCalculator.thereIsAvailableCapacityFor(
                            calendar,
                            availability,
                            resourcesPerDay,
                            effortToAllocate);
                }

                private CombinedWorkHours getCalendar(ResourcesPerDayModification resourcesPerDayModification) {
                    return CombinedWorkHours.minOf(
                            resourcesPerDayModification.getBeingModified().getTaskCalendar(),
                            resourcesPerDayModification.getResourcesCalendar());
                }

                @Override
                protected void markUnsatisfied(
                        ResourcesPerDayModification allocationAttempt, CapacityResult capacityResult) {

                    allocationAttempt.getBeingModified().markAsUnsatisfied();
                    receiver.cantFulfill(allocationAttempt, capacityResult);
                }

            };
            IntraDayDate result = allocator.untilAllocating(toAllocate);

            if (result == null) {
                // Allocation could not be done
                return direction == Direction.FORWARD ? task.getIntraDayEndDate() : task.getIntraDayStartDate();
            }
            return result;
        }

        public void allocateOnTaskLength() {
            AllocatorForTaskDurationAndSpecifiedResourcesPerDay allocator =
                    new AllocatorForTaskDurationAndSpecifiedResourcesPerDay(allocations);

            allocator.allocateOnTaskLength();
        }

        public void allocateUntil(IntraDayDate endExclusive) {
            AllocatorForTaskDurationAndSpecifiedResourcesPerDay allocator =
                    new AllocatorForTaskDurationAndSpecifiedResourcesPerDay(allocations);

            allocator.allocateUntil(endExclusive);
        }

        public void allocateFromEndUntil(IntraDayDate start) {
            AllocatorForTaskDurationAndSpecifiedResourcesPerDay allocator =
                    new AllocatorForTaskDurationAndSpecifiedResourcesPerDay(allocations);

            allocator.allocateFromEndUntil(start);
        }
    }

    public static HoursAllocationSpecified allocatingHours(List<EffortModification> effortsModifications) {
        effortsModifications = new ArrayList<>(effortsModifications);
        sortBySpecificFirst(effortsModifications);

        return new HoursAllocationSpecified(effortsModifications);
    }

    /**
     * Needed for doing fluent interface calls:
     * <ul>
     *     <li>
     *         {@link ResourceAllocation#allocatingHours(List)}
     *         .{@link HoursAllocationSpecified#allocateUntil(LocalDate) allocateUntil(LocalDate)}
     *     </li>
     *     <li>
     *         {@link ResourceAllocation#allocatingHours(List)}.{@link HoursAllocationSpecified#allocate() allocate()}
     *     </li>
     * </ul>
     */
    public static class HoursAllocationSpecified {

        private final List<EffortModification> hoursModifications;

        private Task task;

        public HoursAllocationSpecified(List<EffortModification> hoursModifications) {
            Validate.noNullElements(hoursModifications);
            Validate.isTrue(!hoursModifications.isEmpty());
            this.hoursModifications = hoursModifications;
            this.task = hoursModifications.get(0).getBeingModified().getTask();
            Validate.notNull(task);
        }

        public void allocate() {
            allocateUntil(task.getIntraDayEndDate());
        }

        public void allocateUntil(IntraDayDate end) {
            Validate.notNull(end);
            checkStartLessOrEqualToEnd(task.getIntraDayStartDate(), end);
            for (EffortModification each : hoursModifications) {
                each.allocateUntil(end);
            }
        }

        public void allocateFromEndUntil(IntraDayDate start) {
            Validate.notNull(start);
            checkStartLessOrEqualToEnd(start, task.getIntraDayEndDate());
            for (EffortModification each : hoursModifications) {
                each.allocateFromEndUntil(start);
            }

        }

    }

    public interface IOnDayAssignmentRemoval {

        void onRemoval(ResourceAllocation<?> allocation, DayAssignment assignment);
    }

    public static class DoNothing implements IOnDayAssignmentRemoval {
        @Override
        public void onRemoval(ResourceAllocation<?> allocation, DayAssignment assignment) {}
    }

    public static class DetachDayAssignmentOnRemoval implements IOnDayAssignmentRemoval {
        @Override
        public void onRemoval(ResourceAllocation<?> allocation, DayAssignment assignment) {
            assignment.detach();
        }
    }

    public void setOnDayAssignmentRemoval(IOnDayAssignmentRemoval dayAssignmentRemoval) {
        Validate.notNull(dayAssignmentRemoval);
        this.dayAssignmenteRemoval = dayAssignmentRemoval;
    }

    /**
     * Returns the associated resources from the day assignments of this {@link ResourceAllocation}.
     *
     * @return the associated resources with no repeated elements
     */
    public abstract List<Resource> getAssociatedResources();

    public void switchToScenario(Scenario scenario) {
        Validate.notNull(scenario);
        assignmentsState = assignmentsState.switchTo(scenario);
        switchDerivedAllocationsTo(scenario);
    }

    private void switchDerivedAllocationsTo(Scenario scenario) {
        for (DerivedAllocation each : derivedAllocations) {
            each.useScenario(scenario);
        }
    }

    protected void updateResourcesPerDay() {
        if ( !isSatisfied() ) {
            return;
        }
        ResourcesPerDay resourcesPerDay = calculateResourcesPerDayFromAssignments(getAssignments());
        assert resourcesPerDay != null;
        this.resourcesPerDay = resourcesPerDay;
    }

    protected void setResourcesPerDayToAmount(int amount) {
        this.resourcesPerDay = ResourcesPerDay.amount(amount);
        this.intendedResourcesPerDay = this.resourcesPerDay;
    }

    private void setIntendedResourcesPerDay(ResourcesPerDay resourcesPerDay) {
        Validate.notNull(resourcesPerDay);
        Validate.isTrue(!resourcesPerDay.isZero());
        this.intendedResourcesPerDay = resourcesPerDay;
    }

    /**
     * Returns the last specified resources per day.
     */
    public ResourcesPerDay getIntendedResourcesPerDay() {
        return intendedResourcesPerDay;
    }

    private ResourcesPerDay getReassignationResourcesPerDay() {
        ResourcesPerDay intended = getIntendedResourcesPerDay();

        return intended != null ? intended : getResourcesPerDay();
    }

    public boolean areIntendedResourcesPerDaySatisfied() {
        return getTask().getCalculatedValue() == CalculatedValue.RESOURCES_PER_DAY ||
                Objects.equals(getNonConsolidatedResourcePerDay(), getIntendedResourcesPerDay());
    }

    public ResourceAllocation(Task task) {
        this(task, null);
    }

    public ResourceAllocation(Task task, AssignmentFunction assignmentFunction) {
        Validate.notNull(task);
        this.task = task;
        this.assignmentFunction = assignmentFunction;
        this.assignmentsState = buildInitialTransientState();
    }

    protected ResourceAllocation(ResourcesPerDay resourcesPerDay, Task task) {
        this(task);
        Validate.notNull(resourcesPerDay);
        this.resourcesPerDay = resourcesPerDay;
    }

    @NotNull
    public Task getTask() {
        return task;
    }

    private void updateOriginalTotalAssignment() {
        if ( !isSatisfied() ) {
            return;
        }
        intendedNonConsolidatedEffort = getNonConsolidatedEffort();
        Consolidation consolidation = task.getConsolidation();
        if ( consolidation == null ) {
            intendedTotalAssignment = intendedNonConsolidatedEffort;
        } else if ( consolidation.isCompletelyConsolidated() ) {
            intendedTotalAssignment = getConsolidatedEffort();
        } else {
            intendedTotalAssignment = consolidation.getTotalFromNotConsolidated(getNonConsolidatedEffort());
        }
    }

    @NotNull
    public EffortDuration getIntendedTotalAssignment() {
        return intendedTotalAssignment;
    }

    public interface IVisitor<T> {

        T on(SpecificResourceAllocation specificAllocation);

        T on(GenericResourceAllocation genericAllocation);
    }

    public static <T> T visit(ResourceAllocation<?> allocation, IVisitor<T> visitor) {
        Validate.notNull(allocation);
        Validate.notNull(visitor);

        if (allocation instanceof GenericResourceAllocation) {
            GenericResourceAllocation generic = (GenericResourceAllocation) allocation;

            return visitor.on(generic);
        } else if (allocation instanceof SpecificResourceAllocation) {
            SpecificResourceAllocation specific = (SpecificResourceAllocation) allocation;

            return visitor.on(specific);
        }
        throw new RuntimeException("can't handle: " + allocation.getClass());
    }

    /**
     * This method is in use.
     */
    public abstract ResourcesPerDayModification withDesiredResourcesPerDay(ResourcesPerDay resourcesPerDay);

    public final ResourcesPerDayModification asResourcesPerDayModification() {
        if ( getReassignationResourcesPerDay().isZero() ) {
            return null;
        }

        return visit(this, new IVisitor<ResourcesPerDayModification>() {
            @Override
            public ResourcesPerDayModification on(SpecificResourceAllocation specificAllocation) {
                return ResourcesPerDayModification.create(specificAllocation, getReassignationResourcesPerDay());
            }

            @Override
            public ResourcesPerDayModification on(GenericResourceAllocation genericAllocation) {
                return ResourcesPerDayModification.create(
                        genericAllocation,
                        getReassignationResourcesPerDay(),
                        getAssociatedResources());
            }
        });
    }

    public final EffortModification asHoursModification() {
        return visit(this, new IVisitor<EffortModification>() {

            @Override
            public EffortModification on(GenericResourceAllocation genericAllocation) {
                return EffortModification.create(
                        genericAllocation, getEffortForReassignation(), getAssociatedResources());
            }

            @Override
            public EffortModification on(SpecificResourceAllocation specificAllocation) {
                return EffortModification.create(specificAllocation, getEffortForReassignation());
            }
        });
    }

    public abstract IAllocatable withPreviousAssociatedResources();

    public interface IEffortDistributor<T extends DayAssignment> {
        /**
         * It does not add the created assignments to the underlying allocation.
         * It just distributes them.
         */
        List<T> distributeForDay(PartialDay day, EffortDuration effort);
    }

    protected abstract class AssignmentsAllocator implements IAllocatable, IEffortDistributor<T> {

        @Override
        public final void allocate(ResourcesPerDay resourcesPerDay) {
            Task currentTask = getTask();

            AllocateResourcesPerDayOnInterval allocator = new AllocateResourcesPerDayOnInterval(
                    currentTask.getIntraDayStartDate(),
                    currentTask.getIntraDayEndDate());

            allocator.allocate(resourcesPerDay);
        }

        private List<T> createAssignments(
                ResourcesPerDay resourcesPerDay,
                IntraDayDate startInclusive,
                IntraDayDate endExclusive) {

            List<T> assignmentsCreated = new ArrayList<>();
            for (PartialDay day : getDays(startInclusive, endExclusive)) {
                EffortDuration durationForDay = calculateTotalToDistribute(day, resourcesPerDay);
                assignmentsCreated.addAll(distributeForDay(day, durationForDay));
            }
            return onlyNonZeroHours(assignmentsCreated);
        }

        @Override
        public IAllocateResourcesPerDay resourcesPerDayUntil(IntraDayDate end) {
            IntraDayDate startInclusive = getStartSpecifiedByTask();

            return new AllocateResourcesPerDayOnInterval(startInclusive, end);
        }

        @Override
        public IAllocateResourcesPerDay resourcesPerDayFromEndUntil(IntraDayDate start) {
            IntraDayDate startInclusive = IntraDayDate.max(start, getStartSpecifiedByTask());
            IntraDayDate endDate = task.getIntraDayEndDate();

            return new AllocateResourcesPerDayOnInterval(startInclusive, endDate);
        }

        private Iterable<PartialDay> getDays(IntraDayDate startInclusive, IntraDayDate endExclusive) {
            checkStartLessOrEqualToEnd(startInclusive, endExclusive);

            return startInclusive.daysUntil(endExclusive);
        }

        private final class AllocateResourcesPerDayOnInterval implements IAllocateResourcesPerDay {

            private final IntraDayDate startInclusive;

            private final IntraDayDate endExclusive;

            private AllocateResourcesPerDayOnInterval(IntraDayDate startInclusive, IntraDayDate endExclusive) {
                this.startInclusive = startInclusive;
                this.endExclusive = IntraDayDate.max(startInclusive, endExclusive);
            }

            @Override
            public void allocate(ResourcesPerDay resourcesPerDay) {
                setIntendedResourcesPerDay(resourcesPerDay);
                List<T> assignmentsCreated = createAssignments(resourcesPerDay, startInclusive, endExclusive);
                resetAllAllocationAssignmentsTo(assignmentsCreated, startInclusive, endExclusive);
                updateResourcesPerDay();
            }
        }

        @Override
        public IAllocateEffortOnInterval onIntervalWithinTask(final LocalDate start, final LocalDate end) {
            checkStartLessOrEqualToEnd(start, end);

            return new OnSubIntervalAllocator(new AllocationIntervalInsideTask(start, end));
        }

        @Override
        public IAllocateEffortOnInterval onIntervalWithinTask(IntraDayDate start, IntraDayDate end) {
            checkStartLessOrEqualToEnd(start, end);

            return new OnSubIntervalAllocator(new AllocationIntervalInsideTask(start, end));
        }

        @Override
        public IAllocateEffortOnInterval onInterval(final LocalDate startInclusive, final LocalDate endExclusive) {
            checkStartLessOrEqualToEnd(startInclusive, endExclusive);

            return new OnSubIntervalAllocator(new AllocationInterval(startInclusive, endExclusive));
        }

        @Override
        public IAllocateEffortOnInterval onInterval(IntraDayDate start, IntraDayDate end) {
            checkStartLessOrEqualToEnd(start, end);

            return new OnSubIntervalAllocator(new AllocationInterval(start, end));
        }

        private class OnSubIntervalAllocator implements IAllocateEffortOnInterval {

            private final AllocationInterval allocationInterval;

            private OnSubIntervalAllocator(AllocationInterval allocationInterval) {
                this.allocationInterval = allocationInterval;
            }

            @Override
            public void allocateHours(int hours) {
                allocate(hours(hours));
            }

            @Override
            public void allocate(EffortDuration duration) {
                List<T> assignmentsCreated = createAssignments(allocationInterval, duration);
                allocationInterval.resetAssignments(assignmentsCreated);
            }

            @Override
            public void allocate(List<EffortDuration> durationsByDay) {
                allocateDurationsByDay(allocationInterval, durationsByDay);
            }

        }

        private void allocateDurationsByDay(AllocationInterval interval, List<EffortDuration> durationsByDay) {
            List<EffortDuration> rightSlice = interval.getRightSlice(durationsByDay);
            AvailabilityTimeLine availability = getAvailability();

            List<T> assignments = createAssignments(
                    interval, availability, rightSlice.toArray(new EffortDuration[rightSlice.size()]));

            interval.resetAssignments(assignments);
        }

        @Override
        public IAllocateEffortOnInterval fromStartUntil(final IntraDayDate end) {
            return getIAllocateEffortOnInterval(new AllocationInterval(getStartSpecifiedByTask(), end));
        }

        private IAllocateEffortOnInterval getIAllocateEffortOnInterval(final AllocationInterval interval) {
            return new IAllocateEffortOnInterval() {

                @Override
                public void allocateHours(int hours) {
                    allocate(hours(hours));
                }

                @Override
                public void allocate(EffortDuration effortDuration) {
                    allocateTheWholeAllocation(interval, effortDuration);
                }

                @Override
                public void allocate(List<EffortDuration> durationsByDay) {
                    List<EffortDuration> rightSlice = interval.getRightSlice(durationsByDay);
                    AvailabilityTimeLine availability = getAvailability();
                    createAssignments(interval, availability, rightSlice.toArray(new EffortDuration[rightSlice.size()]));
                }

            };
        }

        @Override
        public IAllocateEffortOnInterval fromEndUntil(IntraDayDate start) {
            final AllocationInterval interval = new AllocationInterval(start, task.getIntraDayEndDate());

            return new IAllocateEffortOnInterval() {

                @Override
                public void allocateHours(int hours) {
                    allocate(hours(hours));
                }

                @Override
                public void allocate(EffortDuration effortDuration) {
                    allocateTheWholeAllocation(interval, effortDuration);
                }

                @Override
                public void allocate(List<EffortDuration> durationsByDay) {
                    allocateDurationsByDay(interval, durationsByDay);
                }

            };
        }

        private void allocateTheWholeAllocation(AllocationInterval interval, EffortDuration durationToAssign) {
            List<T> assignmentsCreated = createAssignments(interval, durationToAssign);
            ResourceAllocation.this.allocateTheWholeAllocation(interval, assignmentsCreated);
        }

        protected abstract AvailabilityTimeLine getResourcesAvailability();

        private List<T> createAssignments(AllocationInterval interval, EffortDuration durationToAssign) {
            AvailabilityTimeLine availability = getAvailability();

            Iterable<PartialDay> days = getDays(interval.getStartInclusive(), interval.getEndExclusive());
            EffortDuration[] durationsEachDay = secondsDistribution(availability, days, durationToAssign);
            return createAssignments(interval, availability, durationsEachDay);
        }

        private List<T> createAssignments(
                AllocationInterval interval,
                AvailabilityTimeLine availability,
                EffortDuration[] durationsEachDay) {

            List<T> result = new ArrayList<>();
            int i = 0;
            for (PartialDay day : getDays(interval.getStartInclusive(), interval.getEndExclusive())) {
                // If all days are not available, it would try to assign them anyway, preventing it with a check
                if ( availability.isValid(day.getDate()) ) {
                    result.addAll(distributeForDay(day, durationsEachDay[i]));
                }
                i++;
            }
            return onlyNonZeroHours(result);
        }

        private AvailabilityTimeLine getAvailability() {
            AvailabilityTimeLine resourcesAvailability = getResourcesAvailability();
            BaseCalendar taskCalendar = getTask().getCalendar();
            if ( taskCalendar != null ) {
                return taskCalendar.getAvailability().and(resourcesAvailability);
            } else {
                return resourcesAvailability;
            }
        }

        private List<T> onlyNonZeroHours(List<T> assignmentsCreated) {
            List<T> result = new ArrayList<>();
            for (T each : assignmentsCreated) {
                if ( !each.getDuration().isZero() ) {
                    result.add(each);
                }
            }
            return result;
        }

        private EffortDuration[] secondsDistribution(
                AvailabilityTimeLine availability,
                Iterable<PartialDay> days,
                EffortDuration duration) {

            List<Capacity> capacities = new ArrayList<>();
            for (PartialDay each : days) {
                capacities.add(getCapacity(availability, each));
            }
            Distributor distributor = Distributor.among(capacities);
            return distributor.distribute(duration).toArray(new EffortDuration[0]);
        }

        private Capacity getCapacity(AvailabilityTimeLine availability, PartialDay day) {
            return availability.isValid(day.getDate())
                    ? getCapacityAt(day)
                    : Capacity.create(zero()).notOverAssignableWithoutLimit();
        }

        protected abstract Capacity getCapacityAt(PartialDay each);
    }

    public void markAsUnsatisfied() {
        removingAssignments(getAssignments());
        assert isUnsatisfied();
    }

    public boolean isLimiting() {
        return getLimitingResourceQueueElement() != null;
    }

    public boolean isLimitingAndHasDayAssignments() {
        return isLimiting() && hasAssignments();
    }

    public boolean isSatisfied() {
        return isCompletelyConsolidated() ? hasAssignments() : !getNonConsolidatedAssignments().isEmpty();
    }

    private boolean isCompletelyConsolidated() {
        return task.getConsolidation() != null && task.getConsolidation().isCompletelyConsolidated();
    }

    public boolean isUnsatisfied() {
        return !isSatisfied();
    }

    public void copyAssignmentsFromOneScenarioToAnother(Scenario from, Scenario to){
        copyAssignments(from, to);
        for (DerivedAllocation each : derivedAllocations) {
            each.copyAssignments(from, to);
        }
    }

    protected abstract void copyAssignments(Scenario from, Scenario to);

    protected void resetAssignmentsTo(List<T> assignments) {
        resetAllAllocationAssignmentsTo(assignments, task.getIntraDayStartDate(), task.getIntraDayEndDate());
    }

    protected void allocateTheWholeAllocation(AllocationInterval interval, List<T> assignments) {
        resetAllAllocationAssignmentsTo(assignments, interval.getStartInclusive(), interval.getEndExclusive());
        updateResourcesPerDay();
    }

    protected void resetAllAllocationAssignmentsTo(
            List<T> assignments,
            IntraDayDate intraDayStart,
            IntraDayDate intraDayEnd) {

        removingAssignments(withoutConsolidated(getAssignments()));
        addingAssignments(assignments);
        updateOriginalTotalAssignment();
        getDayAssignmentsState().setIntraDayStart(intraDayStart);
        getDayAssignmentsState().setIntraDayEnd(intraDayEnd);
    }

    class AllocationInterval {

        private IntraDayDate originalStart;

        private IntraDayDate originalEnd;

        private final IntraDayDate start;

        private final IntraDayDate end;

        AllocationInterval(IntraDayDate originalStart, IntraDayDate originalEnd, IntraDayDate start, IntraDayDate end) {
            this.originalStart = originalStart;
            this.originalEnd = originalEnd;

            IntraDayDate startConsideringConsolidated =
                    task.hasConsolidations() ? IntraDayDate.max(task.getFirstDayNotConsolidated(), start) : start;

            this.start = IntraDayDate.min(startConsideringConsolidated, end);
            this.end = IntraDayDate.max(this.start, end);
        }

        AllocationInterval(IntraDayDate start, IntraDayDate end) {
            this(start, end, start, end);
        }

        AllocationInterval(LocalDate startInclusive, LocalDate endExclusive) {
            this(IntraDayDate.startOfDay(startInclusive), IntraDayDate.startOfDay(endExclusive));
        }

        public List<EffortDuration> getRightSlice(List<EffortDuration> original) {
            List<EffortDuration> result = new ArrayList<>(original);
            final int numberOfDaysToFill = originalStart.numberOfDaysUntil(originalEnd);
            for (int i = 0; i < numberOfDaysToFill - original.size(); i++) {
                result.add(zero());
            }
            return result.subList(
                    originalStart.numberOfDaysUntil(start),
                    result.size() - end.numberOfDaysUntil(originalEnd));
        }


        public void resetAssignments(List<T> assignmentsCreated) {
            resetAssignmentsFittingAllocationDatesToResultingAssignments(this, assignmentsCreated);
        }

        public IntraDayDate getStartInclusive() {
            return this.start;
        }

        public IntraDayDate getEndExclusive() {
            return this.end;
        }

        public List<DayAssignment> getAssignmentsOnInterval() {
            return getAssignments(this.start.getDate(), this.end.asExclusiveEnd());
        }

        public List<DayAssignment> getNoConsolidatedAssignmentsOnInterval() {
            return DayAssignment.withConsolidatedValue(getAssignmentsOnInterval(), false);
        }

        public List<DayAssignment> getConsolidatedAssignmentsOnInterval() {
            return DayAssignment.withConsolidatedValue(getAssignmentsOnInterval(), true);
        }
    }

    class AllocationIntervalInsideTask extends AllocationInterval {

        AllocationIntervalInsideTask(LocalDate startInclusive, LocalDate endExclusive) {
            this(IntraDayDate.startOfDay(startInclusive), IntraDayDate.startOfDay(endExclusive));
        }

        AllocationIntervalInsideTask(IntraDayDate startInclusive, IntraDayDate endExclusive) {
            super(
                    startInclusive,
                    endExclusive,
                    IntraDayDate.max(startInclusive, getTask().getFirstDayNotConsolidated()),
                    IntraDayDate.min(endExclusive, task.getIntraDayEndDate()));
        }

        @Override
        public void resetAssignments(List<T> assignmentsCreated) {
            resetAssignmentsForInterval(this, assignmentsCreated);
        }
    }

    protected void resetAssignmentsForInterval(AllocationIntervalInsideTask interval, List<T> assignmentsCreated) {
        IntraDayDate originalStart = getIntraDayStartDate();
        IntraDayDate originalEnd = getIntraDayEndDate();

        updateAssignments(interval, assignmentsCreated);

        // The resource allocation cannot grow beyond the start of the task.
        // This is guaranteed by IntervalInsideTask.
        // It also cannot shrink from the original size, this is guaranteed by originalStart.
        getDayAssignmentsState().setIntraDayStart(IntraDayDate.min(originalStart, interval.getStartInclusive()));

        // The resource allocation cannot grow beyond the end of the task.
        // This is guaranteed by IntervalInsideTask.
        // It also cannot shrink from the original size, this is guaranteed by originalEnd.
        getDayAssignmentsState().setIntraDayEnd(IntraDayDate.max(originalEnd, interval.getEndExclusive()));
    }

    private void updateAssignments(AllocationInterval interval, List<T> assignmentsCreated) {
        removingAssignments(withoutConsolidated(interval.getAssignmentsOnInterval()));
        addingAssignments(assignmentsCreated);

        updateOriginalTotalAssignment();
        updateResourcesPerDay();
    }

    void updateAssignmentsConsolidatedValues() {
        LocalDate firstNotConsolidated = task.getFirstDayNotConsolidated().getDate();
        for (T each : getAssignments()) {
            each.setConsolidated(each.getDay().isBefore(firstNotConsolidated));
        }
    }

    private void resetAssignmentsFittingAllocationDatesToResultingAssignments(
            AllocationInterval interval, List<T> assignmentsCreated) {

        updateAssignments(interval, assignmentsCreated);

        LocalDate startConsideringAssignments = getStartConsideringAssignments();
        IntraDayDate start = IntraDayDate.startOfDay(startConsideringAssignments);

        if ( interval.getStartInclusive().areSameDay(startConsideringAssignments) ) {
            start = interval.getStartInclusive();
        }

        getDayAssignmentsState().setIntraDayStart(start);

        LocalDate endConsideringAssignments = getEndDateGiven(getAssignments());
        IntraDayDate end = IntraDayDate.startOfDay(endConsideringAssignments);

        if ( interval.getEndExclusive().areSameDay(endConsideringAssignments) ) {
            end = interval.getEndExclusive();
        }

        getDayAssignmentsState().setIntraDayEnd(end);
    }

    private static <T extends DayAssignment> List<T> withoutConsolidated(List<? extends T> assignments) {
        List<T> result = new ArrayList<>();
        for (T each : assignments) {
            if ( !each.isConsolidated() ) {
                result.add(each);
            }
        }
        return result;
    }

    protected final void addingAssignments(Collection<? extends T> assignments) {
        getDayAssignmentsState().addingAssignments(withoutAlreadyPresent(assignments));
    }

    private List<? extends T> withoutAlreadyPresent(Collection<? extends T> assignments) {
        if ( assignments.isEmpty() ) {
            return Collections.emptyList();
        }

        LocalDate min = Collections.min(assignments, DayAssignment.byDayComparator()).getDay();
        LocalDate max = Collections.max(assignments, DayAssignment.byDayComparator()).getDay();
        Set<LocalDate> daysPresent = DayAssignment.byDay(getAssignments(min, max.plusDays(1))).keySet();

        List<T> result = new ArrayList<>();
        for (T each : assignments) {
            if ( !daysPresent.contains(each.getDay()) ) {
                result.add(each);
            }
        }
        return result;
    }

    public void removeLimitingDayAssignments() {
        resetAssignmentsTo(Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    public void allocateLimitingDayAssignments(
            List<? extends DayAssignment> assignments, IntraDayDate start, IntraDayDate end) {

        assert isLimiting();
        resetAllAllocationAssignmentsTo((List<T>) assignments, start, end);
    }

    private void removingAssignments(List<? extends DayAssignment> assignments) {
        getDayAssignmentsState().removingAssignments(assignments);
    }

    public final EffortDuration calculateTotalToDistribute(PartialDay day, ResourcesPerDay resourcesPerDay) {
        return getAllocationCalendar().asDurationOn(day, resourcesPerDay);
    }

    public ResourcesPerDay calculateResourcesPerDayFromAssignments() {
        return calculateResourcesPerDayFromAssignments(getAssignments());
    }

    private ResourcesPerDay calculateResourcesPerDayFromAssignments(Collection<? extends T> assignments) {
        if ( assignments.isEmpty() ) {
            return ResourcesPerDay.amount(0);
        }

        Map<LocalDate, List<T>> byDay = DayAssignment.byDay(assignments);
        LocalDate min = Collections.min(byDay.keySet());
        LocalDate max = Collections.max(byDay.keySet());
        Iterable<PartialDay> daysToIterate = startFor(min).daysUntil(endFor(max));

        EffortDuration sumTotalEffort = zero();
        EffortDuration sumWorkableEffort = zero();
        final ResourcesPerDay ONE_RESOURCE_PER_DAY = ResourcesPerDay.amount(1);

        for (PartialDay day : daysToIterate) {
            List<T> assignmentsAtDay =  avoidNull(byDay.get(day.getDate()), Collections.<T> emptyList());

            EffortDuration incrementWorkable = getAllocationCalendar().asDurationOn(day, ONE_RESOURCE_PER_DAY);
            sumWorkableEffort = sumWorkableEffort.plus(incrementWorkable);

            sumTotalEffort = sumTotalEffort.plus(sumDuration(assignmentsAtDay));
        }
        if ( sumWorkableEffort.equals(zero()) ) {
            return ResourcesPerDay.amount(0);
        }

        return ResourcesPerDay.calculateFrom(sumTotalEffort, sumWorkableEffort);
    }

    private IntraDayDate startFor(LocalDate dayDate) {
        IntraDayDate start = getIntraDayStartDate();

        return start.getDate().equals(dayDate) ? start : IntraDayDate.startOfDay(dayDate);
    }

    private IntraDayDate endFor(LocalDate assignmentDate) {
        IntraDayDate end = getIntraDayEndDate();

        return end.getDate().equals(assignmentDate) ? end : IntraDayDate.startOfDay(assignmentDate).nextDayAtStart();
    }

    private static <T> T avoidNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    public ICalendar getAllocationCalendar() {
        return getCalendarGivenTaskCalendar(getTaskCalendar());
    }

    private ICalendar getTaskCalendar() {
        return getTask().getCalendar() == null ? SameWorkHoursEveryDay.getDefaultWorkingDay() : getTask().getCalendar();
    }

    protected abstract ICalendar getCalendarGivenTaskCalendar(ICalendar taskCalendar);

    /**
     * This method is in use.
     */
    protected abstract Class<T> getDayAssignmentType();

    public ResourceAllocation<T> copy(Scenario scenario) {
        Validate.notNull(scenario);
        ResourceAllocation<T> copy = createCopy(scenario);

        copy.assignmentsState = copy.toTransientStateWithInitial(
                getUnorderedFor(scenario),
                getIntraDayStartDateFor(scenario),
                getIntraDayEndFor(scenario));

        copy.resourcesPerDay = resourcesPerDay;
        copy.intendedTotalAssignment = intendedTotalAssignment;
        copy.task = task;
        copy.assignmentFunction = assignmentFunction;
        copy.intendedResourcesPerDay = intendedResourcesPerDay;

        return copy;
    }

    private DayAssignmentsState toTransientStateWithInitial(
            Collection<? extends T> initialAssignments,
            IntraDayDate start,
            IntraDayDate end) {

        TransientState result = new TransientState(initialAssignments);
        result.setIntraDayStart(start);
        result.setIntraDayEnd(end);

        return result;
    }

    private Set<T> getUnorderedFor(Scenario scenario) {
        IDayAssignmentsContainer<T> container = retrieveContainerFor(scenario);

        return container == null ? new HashSet<>() : container.getDayAssignments();
    }

    private IntraDayDate getIntraDayStartDateFor(Scenario scenario) {
        IDayAssignmentsContainer<T> container = retrieveContainerFor(scenario);

        return container == null ? null : container.getIntraDayStart();
    }

    private IntraDayDate getIntraDayEndFor(Scenario scenario) {
        IDayAssignmentsContainer<T> container = retrieveContainerFor(scenario);

        return container == null ? null : container.getIntraDayEnd();
    }

    abstract ResourceAllocation<T> createCopy(Scenario scenario);

    public AssignmentFunction getAssignmentFunction() {
        return assignmentFunction;
    }

    /**
     * If {@link AssignmentFunction} is null, it's just set and nothing is applied.
     *
     * @param assignmentFunction
     */
    public void setAssignmentFunctionAndApplyIfNotFlat(AssignmentFunction assignmentFunction) {
        this.assignmentFunction = assignmentFunction;

        if ( this.assignmentFunction != null ) {
            this.assignmentFunction.applyTo(this);
        }
    }

    public void setAssignmentFunctionWithoutApply(AssignmentFunction assignmentFunction) {
        this.assignmentFunction = assignmentFunction;
    }

    public int getAssignedHours() {
        return getAssignedEffort().roundToHours();
    }

    public EffortDuration getAssignedEffort() {
        return DayAssignment.sum(getAssignments());
    }

    protected EffortDuration getIntendedNonConsolidatedEffort() {
        return intendedNonConsolidatedEffort;
    }

    protected DayAssignmentsState getDayAssignmentsState() {
        return assignmentsState;
    }

    private TransientState buildInitialTransientState() {
        return new TransientState(new HashSet<T>());
    }

    private DayAssignmentsState buildFromDBState() {
        return new NoExplicitlySpecifiedScenario();
    }

    abstract class DayAssignmentsState {

        private List<T> dayAssignmentsOrdered = null;

        protected List<T> getOrderedDayAssignments() {
            if ( dayAssignmentsOrdered == null ) {
                dayAssignmentsOrdered = DayAssignment.orderedByDay(getUnorderedAssignments());
            }
            return dayAssignmentsOrdered;
        }

        /**
         * It can be null.
         * It allows to mark that the allocation is started in a point within a day instead of the start of the day.
         */
        abstract IntraDayDate getIntraDayStart();

        /**
         * Set a new intraDayStart.
         *
         * @param intraDayStart
         *            it can be <code>null</code>
         * @see getIntraDayStart
         */
        public abstract void setIntraDayStart(IntraDayDate intraDayStart);


        /**
         * It can be null.
         * It allows to mark that the allocation is finished in a point within a day instead of taking the whole day.
         */
        abstract IntraDayDate getIntraDayEnd();

        /**
         * Set a new intraDayEnd.
         *
         * @param intraDayEnd
         *            it can be <code>null</code>
         * @see getIntraDayEnd
         */
        public abstract void setIntraDayEnd(IntraDayDate intraDayEnd);

        protected abstract Collection<T> getUnorderedAssignments();

        protected void addingAssignments(Collection<? extends T> assignments) {
            setParentFor(assignments);
            addAssignments(assignments);
            clearCachedData();
        }

        protected void clearCachedData() {
            dayAssignmentsOrdered = null;
        }

        private void setParentFor(Collection<? extends T> assignments) {
            for (T each : assignments) {
                setItselfAsParentFor(each);
            }
        }

        protected void removingAssignments(List<? extends DayAssignment> assignments){
            removeAssignments(assignments);
            clearCachedData();
            for (DayAssignment each : assignments) {
                dayAssignmenteRemoval.onRemoval(ResourceAllocation.this, each);
            }
        }

        protected abstract void removeAssignments(List<? extends DayAssignment> assignments);

        protected abstract void addAssignments(Collection<? extends T> assignments);

        @SuppressWarnings("unchecked")
        public void mergeAssignments(ResourceAllocation<?> modification) {
            detachAssignments();
            resetTo(((ResourceAllocation<T>) modification).getAssignments());
            clearCachedData();
        }

        protected abstract void resetTo(Collection<T> assignmentsCopied);

        void detachAssignments() {
            for (DayAssignment each : getUnorderedAssignments()) {
                each.detach();
            }
        }

        final protected DayAssignmentsState switchTo(Scenario scenario) {
            DayAssignmentsState result = explicitlySpecifiedState(scenario);
            copyTransientPropertiesIfAppropiateTo(result);
            return result;
        }

        /**
         * Override if necessary to do extra actions.
         */
        protected void copyTransientPropertiesIfAppropiateTo(DayAssignmentsState newStateForScenario) {}
    }

    protected abstract void setItselfAsParentFor(T dayAssignment);

    private class TransientState extends DayAssignmentsState {

        private final Set<T> assignments;

        private IntraDayDate intraDayStart;

        private IntraDayDate intraDayEnd;

        TransientState(Collection<? extends T> assignments) {
            this.assignments = new HashSet<>(assignments);
        }

        @Override
        final protected Collection<T> getUnorderedAssignments() {
            return assignments;
        }

        @Override
        final protected void removeAssignments(List<? extends DayAssignment> assignments) {
            this.assignments.removeAll(assignments);
        }

        @Override
        final protected void addAssignments(Collection<? extends T> assignments) {
            this.assignments.addAll(assignments);
        }

        @Override
        final protected void resetTo(Collection<T> assignments) {
            this.assignments.clear();
            this.assignments.addAll(assignments);
        }

        @Override
        public IntraDayDate getIntraDayStart() {
            return intraDayStart;
        }

        @Override
        public void setIntraDayStart(IntraDayDate intraDayStart) {
            this.intraDayStart = intraDayStart;
        }

        @Override
        final IntraDayDate getIntraDayEnd() {
            return intraDayEnd;
        }

        @Override
        public final void setIntraDayEnd(IntraDayDate intraDayEnd) {
            this.intraDayEnd = intraDayEnd;
        }

        protected void copyTransientPropertiesIfAppropiateTo(DayAssignmentsState newStateForScenario) {
            newStateForScenario.resetTo(getUnorderedAssignments());
            newStateForScenario.setIntraDayStart(getIntraDayStart());
            newStateForScenario.setIntraDayEnd(getIntraDayEnd());
        }

    }

    private DayAssignmentsState explicitlySpecifiedState(Scenario scenario) {
        return new ExplicitlySpecifiedScenarioState(retrieveOrCreateContainerFor(scenario));
    }

    protected abstract IDayAssignmentsContainer<T> retrieveContainerFor(Scenario scenario);

    protected abstract IDayAssignmentsContainer<T> retrieveOrCreateContainerFor(Scenario scenario);
    /**
     * It uses the current scenario retrieved from {@link IScenarioManager} in
     * order to return the assignments for that scenario.
     * This state doesn't allow to update the current assignments for that scenario.
     * <br />
     * Note that this implementation doesn't work well if the current scenario
     * is changed since the assignments are cached and the assignments for the
     * previous one would be returned.
     * <br />
     */
    private class NoExplicitlySpecifiedScenario extends DayAssignmentsState {

        @Override
        protected final void removeAssignments(List<? extends DayAssignment> assignments) {
            modificationsNotAllowed();
        }

        @Override
        protected final void addAssignments(Collection<? extends T> assignments) {
            modificationsNotAllowed();
        }

        @Override
        final void detachAssignments() {
            modificationsNotAllowed();
        }

        @Override
        protected final void resetTo(Collection<T> assignmentsCopied) {
            modificationsNotAllowed();
        }

        private void modificationsNotAllowed() {
            throw new IllegalStateException("modifications to assignments can't be done " +
                    "if the scenario on which to work on is not explicitly specified");
        }

        @Override
        protected Collection<T> getUnorderedAssignments() {
            return retrieveOrCreateContainerFor(currentScenario()).getDayAssignments();
        }

        private Scenario currentScenario() {
            return Registry.getScenarioManager().getCurrent();
        }

        @Override
        IntraDayDate getIntraDayStart() {
            return retrieveContainerFor(currentScenario()).getIntraDayStart();
        }

        @Override
        IntraDayDate getIntraDayEnd() {
            return retrieveOrCreateContainerFor(currentScenario()).getIntraDayEnd();
        }

        @Override
        public void setIntraDayEnd(IntraDayDate intraDayEnd) {
            modificationsNotAllowed();
        }

        @Override
        public void setIntraDayStart(IntraDayDate intraDayStart) {
            modificationsNotAllowed();
        }

    }

    private class ExplicitlySpecifiedScenarioState extends DayAssignmentsState {

        private final IDayAssignmentsContainer<T> container;

        ExplicitlySpecifiedScenarioState(IDayAssignmentsContainer<T> container) {
            Validate.notNull(container);
            this.container = container;
        }

        @Override
        protected void addAssignments(Collection<? extends T> assignments) {
            container.addAll(assignments);
        }

        @Override
        protected Collection<T> getUnorderedAssignments() {
            return container.getDayAssignments();
        }

        @Override
        protected void removeAssignments(List<? extends DayAssignment> assignments) {
            container.removeAll(assignments);
        }

        @Override
        protected void resetTo(Collection<T> assignmentsCopied) {
            container.resetTo(assignmentsCopied);
        }

        @Override
        IntraDayDate getIntraDayStart() {
            return container.getIntraDayStart();
        }

        @Override
        public void setIntraDayStart(IntraDayDate intraDayStart) {
            container.setIntraDayStart(intraDayStart);
        }

        @Override
        IntraDayDate getIntraDayEnd() {
            return container.getIntraDayEnd();
        }

        @Override
        public void setIntraDayEnd(IntraDayDate intraDayEnd) {
            container.setIntraDayEnd(intraDayEnd);
        }

    }

    public EffortDuration getConsolidatedEffort() {
        return DayAssignment.sum(getConsolidatedAssignments());
    }

    public int getNonConsolidatedHours() {
        return getNonConsolidatedEffort().roundToHours();
    }

    public EffortDuration getEffortForReassignation() {
        return isSatisfied() ? getNonConsolidatedEffort() : getIntendedNonConsolidatedEffort();
    }

    public EffortDuration getNonConsolidatedEffort() {
        return DayAssignment.sum(getNonConsolidatedAssignments());
    }

    /**
     * @return a list of {@link DayAssignment} ordered by date
     */
    public final List<T> getAssignments() {
        return getDayAssignmentsState().getOrderedDayAssignments();
    }

    public List<T> getNonConsolidatedAssignments() {
        return DayAssignment.withConsolidatedValue(getAssignments(), false);
    }

    public List<T> getConsolidatedAssignments() {
        return DayAssignment.withConsolidatedValue(getAssignments(), true);
    }

    public ResourcesPerDay getNonConsolidatedResourcePerDay() {
        return calculateResourcesPerDayFromAssignments(getNonConsolidatedAssignments());
    }

    public ResourcesPerDay getConsolidatedResourcePerDay() {
        return calculateResourcesPerDayFromAssignments(getConsolidatedAssignments());
    }

    /**
     * Just called for validation purposes.
     * It must be public, otherwise if it's a proxy the call is not intercepted.
     */
    @NotNull
    public ResourcesPerDay getRawResourcesPerDay() {
        return resourcesPerDay;
    }

    public ResourcesPerDay getResourcesPerDay() {
        return resourcesPerDay == null ? ResourcesPerDay.amount(0) : resourcesPerDay;
    }

    public void createDerived(IWorkerFinder finder) {
        final List<? extends DayAssignment> assignments = getAssignments();
        List<DerivedAllocation> result = new ArrayList<>();
        List<Machine> machines = Resource.machines(getAssociatedResources());

        for (Machine machine : machines) {
            for (MachineWorkersConfigurationUnit each : machine.getConfigurationUnits()) {
                result.add(DerivedAllocationGenerator.generate(this, finder, each, assignments));
            }
        }
        resetDerivedAllocationsTo(result);
    }

    /**
     * Resets the derived allocations.
     */
    private void resetDerivedAllocationsTo(Collection<DerivedAllocation> derivedAllocations) {
        // Avoiding error:
        // A collection with cascade="all-delete-orphan" was no longer referenced by the owning entity instance.
        this.derivedAllocations.clear();
        this.derivedAllocations.addAll(derivedAllocations);
    }

    public Set<DerivedAllocation> getDerivedAllocations() {
        return Collections.unmodifiableSet(derivedAllocations);
    }

    public LocalDate getStartConsideringAssignments() {
        List<? extends DayAssignment> assignments = getAssignments();

        return assignments.isEmpty() ? getStartDate() : assignments.get(0).getDay();
    }

    public LocalDate getStartDate() {
        IntraDayDate start = getIntraDayStartDate();

        return start != null ? start.getDate() : null;
    }

    private IntraDayDate getStartSpecifiedByTask() {
        IntraDayDate taskStart = task.getIntraDayStartDate();
        IntraDayDate firstDayNotConsolidated = getTask().getFirstDayNotConsolidated();

        return IntraDayDate.max(taskStart, firstDayNotConsolidated);
    }

    public IntraDayDate getIntraDayStartDate() {
        IntraDayDate intraDayStart = getDayAssignmentsState().getIntraDayStart();

        return intraDayStart != null ? intraDayStart : task.getIntraDayStartDate();
    }

    public LocalDate getEndDate() {
        IntraDayDate intraDayEndDate = getIntraDayEndDate();

        return intraDayEndDate != null ? intraDayEndDate.asExclusiveEnd() : null;
    }

    public IntraDayDate getIntraDayEndDate() {
        IntraDayDate intraDayEnd = getDayAssignmentsState().getIntraDayEnd();
        if (intraDayEnd != null) {
            return intraDayEnd;
        }

        LocalDate l = getEndDateGiven(getAssignments());

        return l == null ? task.getIntraDayEndDate() : IntraDayDate.startOfDay(l);
    }

    private LocalDate getEndDateGiven(List<? extends DayAssignment> assignments) {
        if (assignments.isEmpty()) {
            return null;
        }
        DayAssignment lastAssignment = assignments.get(assignments.size() - 1);

        return IntraDayDate.create(lastAssignment.getDay(), lastAssignment.getDuration()).asExclusiveEnd();
    }

    public boolean isAlreadyFinishedBy(LocalDate date) {
        return getEndDate() != null && getEndDate().compareTo(date) <= 0;
    }

    private interface PredicateOnDayAssignment {
        boolean satisfiedBy(DayAssignment dayAssignment);
    }

    public int getAssignedHours(final Resource resource, LocalDate start, LocalDate endExclusive) {
        return getAssignedEffort(
                resource, IntraDayDate.create(start, zero()), IntraDayDate.create(endExclusive, zero())).roundToHours();
    }

    public EffortDuration getAssignedEffort(final Resource resource, IntraDayDate start, IntraDayDate endExclusive) {
        return getAssignedDuration(
                getAssingments(resource, start.getDate(), endExclusive.asExclusiveEnd()), start, endExclusive);
    }

    @Override
    public EffortDuration getAssignedDurationAt(Resource resource, LocalDate day) {
        IntraDayDate start = IntraDayDate.startOfDay(day);

        return getAssignedEffort(resource, start, start.nextDayAtStart());
    }

    private List<DayAssignment> getAssingments(
            final Resource resource, LocalDate startInclusive, LocalDate endExclusive) {

        return filter(
                getAssignments(startInclusive, endExclusive), dayAssignment -> dayAssignment.isAssignedTo(resource));
    }

    public List<DayAssignment> getAssignments(IntraDayDate start, IntraDayDate endExclusive) {
        return getAssignments(start.getDate(), endExclusive.asExclusiveEnd());
    }

    public List<DayAssignment> getAssignments(LocalDate start, LocalDate endExclusive) {
        return new ArrayList<>(DayAssignment.getAtInterval(getAssignments(), start, endExclusive));
    }

    public int getAssignedHours(LocalDate start, LocalDate endExclusive) {
        return getAssignedDuration(
                IntraDayDate.create(start, zero()), IntraDayDate.create(endExclusive, zero())).roundToHours();
    }

    public abstract EffortDuration getAssignedEffort(
            Criterion criterion, IntraDayDate startInclusive, IntraDayDate endExclusive);

    private List<DayAssignment> filter(List<DayAssignment> assignments, PredicateOnDayAssignment predicate) {
        List<DayAssignment> result = new ArrayList<>();
        for (DayAssignment dayAssignment : assignments) {
            if (predicate.satisfiedBy(dayAssignment)) {
                result.add(dayAssignment);
            }
        }
        return result;
    }

    protected EffortDuration getAssignedDuration(IntraDayDate startInclusive, IntraDayDate endExclusive) {
        return getAssignedDuration(getAssignments(startInclusive, endExclusive), startInclusive, endExclusive);
    }

    private EffortDuration sumDuration(Collection<? extends DayAssignment> assignments) {
        return EffortDuration.sum(assignments, new IEffortFrom<DayAssignment>() {
            @Override
            public EffortDuration from(DayAssignment each) {
                return each.getDuration();
            }
        });
    }

    private EffortDuration getAssignedDuration(
            List<? extends DayAssignment> assignments,
            final IntraDayDate startInclusive,
            final IntraDayDate endExclusive) {

        final IntraDayDate allocationStart = getIntraDayStartDate();

        return EffortDuration.sum(assignments, new IEffortFrom<DayAssignment>() {
            @Override
            public EffortDuration from(DayAssignment value) {
                return getPartialDay(value, startInclusive, endExclusive).limitWorkingDay(value.getDuration());
            }

            private PartialDay getPartialDay(DayAssignment assignment,
                                             IntraDayDate startInclusive,
                                             IntraDayDate endExclusive) {

                LocalDate assignmentDay = assignment.getDay();
                LocalDate startDate = startInclusive.getDate();
                LocalDate endDate = endExclusive.getDate();

                PartialDay result = PartialDay.wholeDay(assignment.getDay());
                if (assignmentDay.equals(startDate)) {
                    result = new PartialDay(startInclusive, result.getEnd());
                }
                if (assignmentDay.equals(endDate)) {
                    result = new PartialDay(result.getStart(), endExclusive);
                }
                return adjustPartialDayToAllocationStart(result);
            }

            // If the start of the allocation is in the middle of a day, its work also starts later,
            // so the PartialDay must be moved to earlier so it doesn't limit the duration more that it should
            private PartialDay adjustPartialDayToAllocationStart(PartialDay day) {
                PartialDay result = day;
                if (allocationStart.areSameDay(day.getDate())) {
                    EffortDuration substractingAtStart = day.getStart().getEffortDuration();

                    EffortDuration newSubstractionAtStart = substractingAtStart.minus(
                            EffortDuration.min(substractingAtStart, allocationStart.getEffortDuration()));

                    IntraDayDate newStart = IntraDayDate.create(day.getDate(), newSubstractionAtStart);
                    result = new PartialDay(newStart, day.getEnd());
                }
                return result;
            }
        });
    }

    public void mergeAssignmentsAndResourcesPerDay(Scenario scenario, ResourceAllocation<?> modifications) {
        if (modifications == this) {
            return;
        }

        switchToScenario(scenario);
        mergeAssignments(modifications);
        this.intendedResourcesPerDay = modifications.intendedResourcesPerDay;

        if (modifications.isSatisfied()) {
            updateOriginalTotalAssignment();
            updateResourcesPerDay();
        }

        setAssignmentFunctionWithoutApply(modifications.getAssignmentFunction());
        mergeDerivedAllocations(scenario, modifications.getDerivedAllocations());
    }

    private void mergeDerivedAllocations(Scenario scenario, Set<DerivedAllocation> derivedAllocations) {
        Map<MachineWorkersConfigurationUnit, DerivedAllocation> newMap =
                DerivedAllocation.byConfigurationUnit(derivedAllocations);

        Map<MachineWorkersConfigurationUnit, DerivedAllocation> currentMap =
                DerivedAllocation.byConfigurationUnit(getDerivedAllocations());

        for (Entry<MachineWorkersConfigurationUnit, DerivedAllocation> entry : newMap.entrySet()) {
            final MachineWorkersConfigurationUnit key = entry.getKey();
            final DerivedAllocation modification = entry.getValue();
            DerivedAllocation current = currentMap.get(key);

            if (current == null) {
                DerivedAllocation derived = modification.asDerivedFrom(this);
                derived.useScenario(scenario);
                currentMap.put(key, derived);
            } else {
                current.useScenario(scenario);
                current.resetAssignmentsTo(modification.getAssignments());
            }
        }
        resetDerivedAllocationsTo(currentMap.values());
    }

    final void mergeAssignments(ResourceAllocation<?> modifications) {
        getDayAssignmentsState().mergeAssignments(modifications);
        getDayAssignmentsState().setIntraDayStart(modifications.getDayAssignmentsState().getIntraDayStart());
        getDayAssignmentsState().setIntraDayEnd(modifications.getDayAssignmentsState().getIntraDayEnd());
    }

    public void detach() {
        getDayAssignmentsState().detachAssignments();
    }

    void associateAssignmentsToResource() {
        for (DayAssignment dayAssignment : getAssignments()) {
            dayAssignment.associateToResource();
        }
    }

    public boolean hasAssignments() {
        return !getAssignments().isEmpty();
    }

    public LimitingResourceQueueElement getLimitingResourceQueueElement() {
        return (!limitingResourceQueueElements.isEmpty()) ? limitingResourceQueueElements.iterator().next() : null;
    }

    public void setLimitingResourceQueueElement(LimitingResourceQueueElement element) {
        limitingResourceQueueElements.clear();
        if (element != null) {
            element.setResourceAllocation(this);
            limitingResourceQueueElements.add(element);
        }
    }

    public Integer getIntendedTotalHours() {
        return intendedTotalHours;
    }

    public void setIntendedTotalHours(Integer intendedTotalHours) {
        this.intendedTotalHours = intendedTotalHours;
    }

    /**
     * Do a query to recover a list of resources that are suitable for this allocation.
     * For a {@link SpecificResourceAllocation} returns the current resource.
     * For a {@link GenericResourceAllocation} returns the resources that currently match this allocation criterions.
     *
     * @return a list of resources that are proper for this allocation
     */
    public abstract List<Resource> querySuitableResources(IResourcesSearcher resourceSearcher);

    public abstract void makeAssignmentsContainersDontPoseAsTransientAnyMore();

    public void removePredecessorsDayAssignmentsFor(Scenario scenario) {
        for (DerivedAllocation each : getDerivedAllocations()) {
            each.removePredecessorContainersFor(scenario);
        }
        removePredecessorContainersFor(scenario);
    }

    protected abstract void removePredecessorContainersFor(Scenario scenario);

    public void removeDayAssignmentsFor(Scenario scenario) {
        for (DerivedAllocation each : getDerivedAllocations()) {
            each.removeContainersFor(scenario);
        }
        removeContainersFor(scenario);
    }

    protected abstract void removeContainersFor(Scenario scenario);

    /**
     * Returns first non consolidated day.
     */
    public LocalDate getFirstNonConsolidatedDate() {
        List<T> nonConsolidated = getNonConsolidatedAssignments();

        return (!nonConsolidated.isEmpty()) ? nonConsolidated.get(0).getDay() : null;
    }

    public boolean isManualAssignmentFunction() {
        return assignmentFunction != null && assignmentFunction.isManual();
    }

    public void resetIntendedIntendedResourcesPerDayWithNonConsolidated() {
        intendedResourcesPerDay = getNonConsolidatedResourcePerDay();
    }

    public void removeDayAssignmentsBeyondDate(LocalDate date) {
        List<T> toRemove = new ArrayList<>();

        for (T t : getAssignments()) {
            if (t.getDay().compareTo(date) >= 0) {
                toRemove.add(t);
            }
        }

        setOnDayAssignmentRemoval(new DetachDayAssignmentOnRemoval());
        getDayAssignmentsState().removingAssignments(toRemove);
    }

}

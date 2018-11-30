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

package org.libreplan.business.test.planner.entities;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.verify;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.libreplan.business.test.planner.entities.DayAssignmentMatchers.haveHours;
import static org.libreplan.business.workingday.EffortDuration.hours;
import static org.libreplan.business.workingday.EffortDuration.zero;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.easymock.IAnswer;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.libreplan.business.calendars.entities.AvailabilityTimeLine;
import org.libreplan.business.calendars.entities.BaseCalendar;
import org.libreplan.business.calendars.entities.ThereAreHoursOnWorkHoursCalculator;
import org.libreplan.business.calendars.entities.ThereAreHoursOnWorkHoursCalculator.CapacityResult;
import org.libreplan.business.planner.entities.DayAssignment;
import org.libreplan.business.planner.entities.GenericResourceAllocation;
import org.libreplan.business.planner.entities.ResourceAllocation;
import org.libreplan.business.planner.entities.ResourceAllocation.AllocationsSpecified.INotFulfilledReceiver;
import org.libreplan.business.planner.entities.ResourceAllocation.Direction;
import org.libreplan.business.planner.entities.SpecificResourceAllocation;
import org.libreplan.business.planner.entities.Task;
import org.libreplan.business.planner.entities.allocationalgorithms.ResourcesPerDayModification;
import org.libreplan.business.resources.entities.Resource;
import org.libreplan.business.resources.entities.Worker;
import org.libreplan.business.workingday.EffortDuration;
import org.libreplan.business.workingday.IntraDayDate;
import org.libreplan.business.workingday.IntraDayDate.PartialDay;
import org.libreplan.business.workingday.ResourcesPerDay;

public class UntilFillingHoursAllocatorTest {

    private List<ResourcesPerDayModification> allocations = new ArrayList<>();

    private List<Resource> resources = new ArrayList<>();

    private Task task;

    private IntraDayDate startDate;

    private BaseCalendar taskCalendar;

    private Integer initialLengthDaysForTask;

    private IntraDayDate endDate;

    @Test(expected = IllegalArgumentException.class)
    public void allTasksOfAllocationsMustBeNotNull() {
        givenAllocationsWithoutTask();
        ResourceAllocation.allocating(allocations);
    }

    @Test(expected = IllegalArgumentException.class)
    public void allAllocationsMustBelongToTheSameTask() {
        givenAllocationsBelongingToDifferentTasks();
        ResourceAllocation.allocating(allocations);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mustReceiveAtLeastOneAllocation() {
        ResourceAllocation.allocating(new ArrayList<>());
    }

    @Test
    public void theEndDateIsTheDayAfterAllTheHoursAreAllocatedIfItIsCompletelyFilled() {
        givenSpecificAllocations(ResourcesPerDay.amount(2));
        IntraDayDate endDate = ResourceAllocation.allocating(allocations).untilAllocating(hours(32));
        assertThat(endDate.getDate(), equalTo(startDate.getDate().plusDays(2)));
        assertTrue(endDate.isStartOfDay());
    }

    @Test
    public void whenAllocatingUntilEndTheIntendedResourcesPerDayAreUpdated() {
        ResourcesPerDay specifiedAmount = ResourcesPerDay.amount(2);
        givenSpecificAllocations(specifiedAmount);

        ResourceAllocation.allocating(allocations).untilAllocating(hours(32));

        ResourcesPerDay intendedResourcesPerDay = allocations.get(0).getBeingModified().getIntendedResourcesPerDay();
        assertThat(intendedResourcesPerDay, equalTo(specifiedAmount));
    }

    @Test
    public void theEndDateIsTheSameDayIfItIsNotCompletelyFilled() {
        givenSpecificAllocations(ResourcesPerDay.amount(1));
        IntraDayDate endDate = ResourceAllocation.allocating(allocations).untilAllocating(hours(31));
        assertThat(endDate.getDate(), equalTo(startDate.getDate().plusDays(3)));
        assertThat(endDate.getEffortDuration(), equalTo(hours(7)));
    }

    @Test
    public void theResourcesPerDayUsedInfluenceTheEndHourWhenLastDayNotCompletelyFilled() {
        givenSpecificAllocations(ResourcesPerDay.amount(2));
        IntraDayDate endDate = ResourceAllocation.allocating(allocations).untilAllocating(hours(20));
        assertThat(endDate.getDate(), equalTo(startDate.getDate().plusDays(1)));
        assertThat(endDate.getEffortDuration(), equalTo(hours(2)));
    }

    @Test
    public void theEndDateIsTheDayAfterIfItIsCompletelyFilled() {
        LocalDate start = new LocalDate();
        givenStartDate(IntraDayDate.create(start, hours(1)));
        givenSpecificAllocations(ResourcesPerDay.amount(1));
        IntraDayDate endDate = ResourceAllocation.allocating(allocations).untilAllocating(hours(7));
        assertThat(endDate, equalTo(startDate.nextDayAtStart()));
        assertThat(endDate.getEffortDuration(), equalTo(hours(0)));
    }

    @Test
    public void allTheRequestedHoursAreAssignedFor() {
        givenSpecificAllocations(ResourcesPerDay.amount(2));
        ResourceAllocation.allocating(allocations).untilAllocating(hours(32));
        ResourceAllocation<?> allocation = allocations.get(0).getBeingModified();
        assertThat(allocation.getAssignments(), haveHours(16, 16));
    }

    @Test
    public void theAllocationCanBeDoneFromEnd() {
        givenStartDate(IntraDayDate.startOfDay(new LocalDate(2009, 1, 10)));

        givenTaskOfDaysLength(10); // so end is day 20

        givenSpecificAllocations(ResourcesPerDay.amount(1));
        IntraDayDate newStart = ResourceAllocation.allocating(allocations).untilAllocating(Direction.BACKWARD, hours(16));
        assertThat(newStart, equalTo(IntraDayDate.startOfDay(new LocalDate(2009, 1, 18))));
    }

    @Test
    public void theAllocationCanBeDoneFromAnEndThatIsInTheMiddleOfTheDay() {
        givenStartDate(IntraDayDate.create(new LocalDate(2009, 1, 10), hours(4)));
        givenEndDate(IntraDayDate.create(new LocalDate(2009, 1, 19), hours(2)));
        givenSpecificAllocations(ResourcesPerDay.amount(1));
        IntraDayDate newStart = ResourceAllocation.allocating(allocations).untilAllocating(Direction.BACKWARD, hours(10));
        assertThat(newStart, equalTo(IntraDayDate.startOfDay(new LocalDate(2009, 1, 18))));
    }

    @Test
    public void theAllocationCanEndOnTheSameDay() {
        givenStartDate(IntraDayDate.create(new LocalDate(2009, 1, 10), hours(4)));
        givenEndDate(IntraDayDate.create(new LocalDate(2009, 1, 19), hours(4)));

        // 4 hours left for a one resource per day assignment, 8 hours left for two resources per day
        givenSpecificAllocations(ResourcesPerDay.amount(2));
        IntraDayDate newStart = ResourceAllocation.allocating(allocations).untilAllocating(Direction.BACKWARD, hours(6));
        assertThat(newStart, equalTo(IntraDayDate.create(new LocalDate(2009, 1, 19), hours(1))));
    }

    @Test
    public void theAllocationCanBeDoneFromEndAndTheStartDateIsCorrectlyCalculatedIfTheLastDayDoesNotTakeAll() {
        givenStartDate(IntraDayDate.startOfDay(new LocalDate(2009, 1, 10)));

        givenTaskOfDaysLength(10); // so end is day 20

        givenSpecificAllocations(ResourcesPerDay.amount(1));
        IntraDayDate newStart = ResourceAllocation.allocating(allocations).untilAllocating(Direction.BACKWARD, hours(14));
        assertThat(newStart, equalTo(IntraDayDate.create(new LocalDate(2009, 1, 18), hours(2))));
    }

    @Test
    public void theAllocationCanBeDoneFromTheEndAndResourcesPerDayInfluenceTheEndHour() {
        givenStartDate(IntraDayDate.startOfDay(new LocalDate(2009, 1, 10)));

        givenTaskOfDaysLength(10); // so end is day 20

        givenSpecificAllocations(ResourcesPerDay.amount(2));
        IntraDayDate newStart = ResourceAllocation.allocating(allocations).untilAllocating(Direction.BACKWARD, hours(20));
        assertThat(newStart, equalTo(IntraDayDate.create(new LocalDate(2009, 1, 18), hours(6))));
    }

    @Test
    public void ifNoAvailableHoursTheAllocationsAreNotSatisfied() {
        AvailabilityTimeLine availability = AvailabilityTimeLine.allValid();
        availability.invalidUntil(new LocalDate(2010, 11, 13));
        availability.invalidFrom(new LocalDate(2010, 11, 15));
        givenCalendarWithAvailability(availability, hours(8));
        givenSpecificAllocations(ResourcesPerDay.amount(1));
        ResourceAllocation.allocating(allocations).untilAllocating(hours(24));

        for (ResourcesPerDayModification each : allocations) {
            assertTrue(each.getBeingModified().isUnsatisfied());
        }
    }

    @Test
    public void ifAvailableHoursTheAllocationsAreSatisfied() {
        AvailabilityTimeLine availability = AvailabilityTimeLine.allValid();
        availability.invalidUntil(new LocalDate(2010, 11, 13));
        availability.invalidFrom(new LocalDate(2010, 11, 15));
        givenCalendarWithAvailability(availability, hours(8));
        givenSpecificAllocations(ResourcesPerDay.amount(1));
        ResourceAllocation.allocating(allocations).untilAllocating(hours(16));

        for (ResourcesPerDayModification each : allocations) {
            assertTrue(each.getBeingModified().isSatisfied());
        }
    }

    @Test
    public void ifNoAllocationsCantBeDoneTheTaskEndIsReturned() {
        givenTaskOfDaysLength(10);
        AvailabilityTimeLine availability = AvailabilityTimeLine.allValid();
        availability.invalidUntil(new LocalDate(2010, 11, 13));
        availability.invalidFrom(new LocalDate(2010, 11, 15));
        givenCalendarWithAvailability(availability, hours(8));
        givenSpecificAllocations(ResourcesPerDay.amount(1));
        IntraDayDate end = ResourceAllocation.allocating(allocations).untilAllocating(hours(17));
        IntraDayDate expectedEnd = IntraDayDate.startOfDay(task.getIntraDayStartDate().getDate().plusDays(10));
        assertThat(end, equalTo(expectedEnd));
    }

    private void givenTaskOfDaysLength(int days) {
        this.initialLengthDaysForTask = days;
    }

    @Test
    public void theAllocationsThatAreNotSatisfiedAreNotified() {
        AvailabilityTimeLine availability = AvailabilityTimeLine.allValid();
        availability.invalidUntil(new LocalDate(2010, 11, 13));
        availability.invalidFrom(new LocalDate(2010, 11, 15));
        givenCalendarWithAvailability(availability, hours(8));
        givenSpecificAllocations(ResourcesPerDay.amount(1), ResourcesPerDay.amount(2));
        INotFulfilledReceiver receiver = createMock(INotFulfilledReceiver.class);
        receiver.cantFulfill(isA(ResourcesPerDayModification.class), isA(CapacityResult.class));
        expectLastCall().times(2);
        replay(receiver);
        ResourceAllocation.allocating(allocations).untilAllocating(hours(49), receiver);
        verify(receiver);
    }

    @Test
    public void theResourcesPerDayIsCalculatedCorrectlyIfTheLastDayHasFilledAllHours() {
        givenSpecificAllocations(ResourcesPerDay.amount(1));
        ResourceAllocation.allocating(allocations).untilAllocating(hours(32));
        ResourceAllocation<?> allocation = allocations.get(0).getBeingModified();
        assertThat(allocation.getResourcesPerDay(), equalTo(ResourcesPerDay.amount(1)));
    }

    @Test
    public void theResourcesPerDayIsCalculatedCorrectlyIfHasEndedInTheMiddleOfTheEnd() {
        givenSpecificAllocations(ResourcesPerDay.amount(2));
        ResourceAllocation.allocating(allocations).untilAllocating(hours(30));
        ResourceAllocation<?> allocation = allocations.get(0).getBeingModified();
        assertThat(allocation.getResourcesPerDay(), equalTo(ResourcesPerDay.amount(2)));
    }

    @Test
    public void theResourcesPerDayIsCalculatedCorrectlyIfTheStartIsInTheMiddleOfADay() {
        givenStartDate(IntraDayDate.create(new LocalDate(2009, 10, 10), EffortDuration.hours(2)));
        givenSpecificAllocations(ResourcesPerDay.amount(1));
        ResourceAllocation.allocating(allocations).untilAllocating(hours(8));
        ResourceAllocation<?> allocation = allocations.get(0).getBeingModified();
        assertThat(allocation.getResourcesPerDay(), equalTo(ResourcesPerDay.amount(1)));
    }

    @Test
    public void theEndDateIsCalculatedCorrectlyIfTheStartIsInTheMiddleOfADayAndEndsTheSameDay() {
        givenStartDate(IntraDayDate.create(new LocalDate(2009, 10, 10), EffortDuration.hours(2)));
        givenSpecificAllocations(ResourcesPerDay.amount(1));
        ResourceAllocation.allocating(allocations).untilAllocating(hours(4));
        ResourceAllocation<?> allocation = allocations.get(0).getBeingModified();

        assertThat(allocation.getIntraDayEndDate(),
                equalTo(IntraDayDate.create(new LocalDate(2009, 10, 10), EffortDuration.hours(6))));
    }

    @Test
    public void theResourcesPerDayAreKeptCorrectlyCalculatedAfterUpdatingTheEndInterval() {
        givenTaskOfDaysLength(10);
        final ResourcesPerDay oneResourcePerDay = ResourcesPerDay.amount(1);
        givenSpecificAllocations(oneResourcePerDay);
        ResourceAllocation.allocating(allocations).untilAllocating(hours(30));
        SpecificResourceAllocation allocation = (SpecificResourceAllocation) allocations.get(0).getBeingModified();

        // hours per day: 8, 8, 8, 6
        allocation.onIntervalWithinTask(startDate.getDate(), startDate.getDate().plusDays(1)).allocateHours(6);

        // hours per day: 6, 8, 8, 6
        assertTrue(allocation.getResourcesPerDay().getAmount().compareTo(oneResourcePerDay.getAmount()) < 0);

        allocation.onIntervalWithinTask(startDate.getDate().plusDays(3), startDate.getDate().plusDays(4)).allocateHours(8);

        // hours per day: 6, 8, 8, 8
        assertThat(allocation.getResourcesPerDay(), equalTo(oneResourcePerDay));

        // This last assertion is questionable.
        // A solution would be to keep a spec object at ResourceAllocation with the desired parameters from
        // the user and then the real values.
        // In the meantime doing an effort to keep the original value.

        allocation.onIntervalWithinTask(startDate.getDate().plusDays(4), startDate.getDate().plusDays(5)).allocateHours(8);

        // hours per day: 6, 8, 8, 8, 8
        assertTrue(allocation.getResourcesPerDay().getAmount().compareTo(oneResourcePerDay.getAmount()) < 0);

        // hours per day: 6, 8, 8, 8, 10
        allocation.onIntervalWithinTask(startDate.getDate().plusDays(4), startDate.getDate().plusDays(5)).allocateHours(10);
        assertThat(allocation.getResourcesPerDay(), equalTo(oneResourcePerDay));
    }

    @Test
    public void worksWellForSeveralSpecificAllocations() {
        givenSpecificAllocations(ResourcesPerDay.amount(1), ResourcesPerDay.amount(1));
        ResourceAllocation.allocating(allocations).untilAllocating(hours(32));
        ResourceAllocation<?> first = allocations.get(0).getBeingModified();
        ResourceAllocation<?> second = allocations.get(1).getBeingModified();
        assertThat(first.getAssignments(), haveHours(8, 8));
        assertThat(second.getAssignments(), haveHours(8, 8));
    }

    @Test
    public void theRemainderIsProportionallyDistributed() {
        givenSpecificAllocations(ResourcesPerDay.amount(2), ResourcesPerDay.amount(1));
        ResourceAllocation.allocating(allocations).untilAllocating(hours(60));
        ResourceAllocation<?> first = allocations.get(0).getBeingModified();
        ResourceAllocation<?> second = allocations.get(1).getBeingModified();
        assertThat(first.getAssignments(), haveHours(16, 16, 8));
        assertThat(second.getAssignments(), haveHours(8, 8, 4));
    }

    @Test
    public void withUnequalRatioWorksOk() {
        givenSpecificAllocations(ResourcesPerDay.amount(1), ResourcesPerDay.amount(new BigDecimal(0.5)));
        ResourceAllocation.allocating(allocations).untilAllocating(hours(36));
        ResourceAllocation<?> first = allocations.get(0).getBeingModified();
        ResourceAllocation<?> second = allocations.get(1).getBeingModified();
        assertThat(first.getAssignments(), haveHours(8, 8, 8));
        assertThat(second.getAssignments(), haveHours(4, 4, 4));
    }

    @Test
    public void withGenericAllocationAlsoWorks() {
        givenWorkers(1);
        givenGenericAllocation(ResourcesPerDay.amount(2));
        givenSpecificAllocations(ResourcesPerDay.amount(1), ResourcesPerDay.amount(1));
        ResourceAllocation.allocating(allocations).untilAllocating(hours(64));
        ResourceAllocation<?> generic = allocations.get(0).getBeingModified();
        ResourceAllocation<?> firstSpecific = allocations.get(1).getBeingModified();
        ResourceAllocation<?> secondSpecific = allocations.get(2).getBeingModified();
        assertThat(generic.getAssignments(), haveHours(16, 16));
        assertThat(firstSpecific.getAssignments(), haveHours(8, 8));
        assertThat(secondSpecific.getAssignments(), haveHours(8, 8));
    }

    @Test
    public void theResourcesPerDayInfuenceTheEndHourWhenUsingGenericAllocationOnOneWorkerForLastDay() {
        givenWorkers(1);
        givenGenericAllocation(ResourcesPerDay.amount(2));
        IntraDayDate endDate = ResourceAllocation.allocating(allocations).untilAllocating(hours(20));
        assertThat(endDate.getDate(), equalTo(startDate.getDate().plusDays(1)));
        assertThat(endDate.getEffortDuration(), equalTo(hours(2)));
    }

    @Test
    public void theHoursInLastDayAreDistributedConsideringResourcesPerDay() {
        givenWorkers(2);
        givenGenericAllocation(ResourcesPerDay.amount(2));
        IntraDayDate endDate = ResourceAllocation.allocating(allocations).untilAllocating(hours(26));
        assertThat(endDate.getDate(), equalTo(startDate.getDate().plusDays(1)));
        assertThat(endDate.getEffortDuration(), equalTo(hours(5)));

        Map<Resource, List<DayAssignment>> byResource =
                DayAssignment.byResourceAndOrdered(allocations.get(0).getBeingModified().getAssignments());

        for (Entry<Resource, List<DayAssignment>> each : byResource.entrySet()) {
            List<DayAssignment> assignments = each.getValue();
            assertThat(assignments.get(0).getDuration(), equalTo(hours(8)));
            assertThat(assignments.get(1).getDuration(), equalTo(hours(5)));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void withGenericAllocationWithNoResourcesPerDay() {
        givenWorkers(1);
        givenGenericAllocation(ResourcesPerDay.amount(0));
        ResourceAllocation.allocating(allocations).untilAllocating(hours(100));
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotDoAGenericAllocationWithoutWorkers() {
        givenWorkers(0);
        givenGenericAllocation(ResourcesPerDay.amount(2));
        ResourceAllocation.allocating(allocations).untilAllocating(hours(100));
    }

    @Test
    public void withoutWorkersYouCanDoSpecificAllocation() {
        givenWorkers(0);
        givenSpecificAllocations(ResourcesPerDay.amount(2));
        ResourceAllocation.allocating(allocations).untilAllocating(hours(100));
    }

    private void givenGenericAllocation(ResourcesPerDay resourcesPerDay) {
        createTaskIfNotCreatedYet();

        allocations.add(ResourcesPerDayModification.create(
                GenericResourceAllocation.create(task), resourcesPerDay, resources));
    }

    private void givenSpecificAllocations(ResourcesPerDay... specifiedResourcesPerDay) {
        createTaskIfNotCreatedYet();
        Worker worker = createWorker();

        for (ResourcesPerDay resourcesPerDay : specifiedResourcesPerDay) {
            SpecificResourceAllocation allocation = createSpecificResourceAllocationFor(task, worker);
            allocations.add(ResourcesPerDayModification.create(allocation, resourcesPerDay));
        }
    }

    private Worker createWorker() {
        Worker worker = createNiceMock(Worker.class);
        GenericResourceAllocationTest.mockZeroLoad(worker);
        replay(worker);
        return worker;
    }

    private void givenStartDate(IntraDayDate start) {
        this.startDate = start;
    }

    private void givenEndDate(IntraDayDate end) {
        this.endDate = end;
    }

    private void createTaskIfNotCreatedYet() {
        if (task != null) {
            return;
        }

        task = createNiceMock(Task.class);
        if (startDate == null) {
            startDate = IntraDayDate.startOfDay(new LocalDate(2009, 10, 10));
        }

        IntraDayDate end = null;
        if (this.endDate != null) {
            end = endDate;
        } else if (initialLengthDaysForTask != null) {
            LocalDate startPlusDays = startDate.getDate().plusDays(initialLengthDaysForTask);
            end = IntraDayDate.startOfDay(startPlusDays);
        }
        expect(task.getStartDate()).andReturn(
                startDate.toDateTimeAtStartOfDay().toDate()).anyTimes();
        if (end != null) {
            expect(task.getIntraDayEndDate()).andReturn(end).anyTimes();
        }
        expect(task.getIntraDayStartDate()).andReturn(startDate).anyTimes();
        expect(task.getCriterions()).andReturn(Collections.emptySet()).anyTimes();
        expect(task.getFirstDayNotConsolidated()).andReturn(startDate).anyTimes();
        if (taskCalendar != null) {
            expect(task.getCalendar()).andReturn(taskCalendar).anyTimes();
        }
        replay(task);
    }

    private void givenAllocationsWithoutTask() {
        allocations.add(ResourcesPerDayModification.create(
                createStubAllocationReturning(SpecificResourceAllocation.class, null),
                ResourcesPerDay.amount(2)));

        allocations.add(ResourcesPerDayModification.create(
                createStubAllocationReturning(SpecificResourceAllocation.class, null),
                ResourcesPerDay.amount(2)));
    }

    private void givenAllocationsBelongingToDifferentTasks() {
        Task task = createStubTask();

        allocations.add(ResourcesPerDayModification.create(
                createStubAllocationReturning(SpecificResourceAllocation.class, task),
                ResourcesPerDay.amount(2)));

        allocations.add(ResourcesPerDayModification.create(
                createStubAllocationReturning(SpecificResourceAllocation.class, task),
                ResourcesPerDay.amount(2)));

        Task other = createStubTask();

        allocations.add(ResourcesPerDayModification.create(
                createStubAllocationReturning(SpecificResourceAllocation.class, other),
                ResourcesPerDay.amount(2)));
    }

    private Task createStubTask() {
        Task task = createNiceMock(Task.class);
        replay(task);
        return task;
    }

    private void givenWorkers(int n) {
        for (int i = 0; i < n; i++) {
            resources.add(createWorker());
        }
    }

    private SpecificResourceAllocation createSpecificResourceAllocationFor(Task task, Resource resource) {
        SpecificResourceAllocation result = SpecificResourceAllocation.create(task);
        result.setResource(resource);
        return result;
    }

    private <T extends ResourceAllocation<?>> T createStubAllocationReturning(Class<T> allocationClass, Task task) {
        T resourceAllocation = createNiceMock(allocationClass);
        expect(resourceAllocation.getTask()).andReturn(task).anyTimes();
        replay(resourceAllocation);
        return resourceAllocation;
    }

    private BaseCalendar givenCalendarWithAvailability(final AvailabilityTimeLine availability, final EffortDuration workingDay) {
        taskCalendar = mockCalendarWithAvailability(availability, workingDay);
        return taskCalendar;
    }

    private BaseCalendar mockCalendarWithAvailability(final AvailabilityTimeLine availability, final EffortDuration workingDay) {
        final BaseCalendar result = createNiceMock(BaseCalendar.class);
        expect(result.getAvailability()).andReturn(availability).anyTimes();

        expect(result.getCapacityOn(isA(PartialDay.class)))
                .andAnswer(new IAnswer<EffortDuration>() {
                    @Override
                    public EffortDuration answer() throws Throwable {
                        PartialDay day = (PartialDay) getCurrentArguments()[0];
                        if (availability.isValid(day.getDate())) {
                            return day.limitWorkingDay(workingDay);
                        } else {
                            return zero();
                        }
                    }
                })
                .anyTimes();

        expect(result.asDurationOn(isA(PartialDay.class), isA(ResourcesPerDay.class)))
                .andAnswer(new IAnswer<EffortDuration>() {
                    @Override
                    public EffortDuration answer() throws Throwable {
                        PartialDay day = (PartialDay) getCurrentArguments()[0];
                        ResourcesPerDay resourcesPerDay = (ResourcesPerDay) getCurrentArguments()[1];
                        if (availability.isValid(day.getDate())) {
                            return day.limitWorkingDay(resourcesPerDay.asDurationGivenWorkingDayOf(workingDay));
                        } else {
                            return zero();
                        }
                    }
                })
                .anyTimes();

        expect(result.thereAreCapacityFor(isA(AvailabilityTimeLine.class), isA(ResourcesPerDay.class), isA(EffortDuration.class)))
                .andAnswer(new IAnswer<Boolean>() {
                    @Override
                    public Boolean answer() throws Throwable {
                        AvailabilityTimeLine availability = (AvailabilityTimeLine) getCurrentArguments()[0];
                        ResourcesPerDay resourcesPerDay = (ResourcesPerDay) getCurrentArguments()[1];
                        EffortDuration effortDuration = (EffortDuration) getCurrentArguments()[2];

                        return ThereAreHoursOnWorkHoursCalculator.thereIsAvailableCapacityFor(
                                result, availability, resourcesPerDay, effortDuration).thereIsCapacityAvailable();
                    }
                })
                .anyTimes();

        replay(result);
        return result;
    }

}

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
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.libreplan.business.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_FILE;
import static org.libreplan.business.test.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_TEST_FILE;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libreplan.business.IDataBootstrap;
import org.libreplan.business.orders.entities.HoursGroup;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.orders.entities.OrderLine;
import org.libreplan.business.orders.entities.OrderLineGroup;
import org.libreplan.business.orders.entities.SchedulingDataForVersion;
import org.libreplan.business.orders.entities.TaskSource;
import org.libreplan.business.planner.entities.Dependency;
import org.libreplan.business.planner.entities.Dependency.Type;
import org.libreplan.business.planner.entities.PositionConstraintType;
import org.libreplan.business.planner.entities.Task;
import org.libreplan.business.planner.entities.TaskElement;
import org.libreplan.business.planner.entities.TaskGroup;
import org.libreplan.business.planner.entities.TaskMilestone;
import org.libreplan.business.planner.entities.TaskPositionConstraint;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { BUSINESS_SPRING_CONFIG_FILE, BUSINESS_SPRING_CONFIG_TEST_FILE })
public class TaskElementTest {

    @Resource
    private IDataBootstrap defaultAdvanceTypesBootstrapListener;

    @Before
    public void loadRequiredData() {
        defaultAdvanceTypesBootstrapListener.loadRequiredData();
    }

    private TaskElement task = new Task();

    private Dependency exampleDependency;

    public TaskElementTest() {
        this.exampleDependency = Dependency.create(new Task(), new Task(), Type.END_START);
    }

    @Test
    @Transactional
    public void initiallyAssociatedDependenciesAreEmpty() {
        assertTrue(task.getDependenciesWithThisDestination().isEmpty());
        assertTrue(task.getDependenciesWithThisOrigin().isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    @Transactional
    public void dependenciesWithThisOriginCollectionCannotBeModified() {
        task.getDependenciesWithThisOrigin().add(exampleDependency);
    }

    @Test(expected = UnsupportedOperationException.class)
    @Transactional
    public void dependenciesWithThisDestinationCollectionCannotBeModified() {
        task.getDependenciesWithThisDestination().add(exampleDependency);
    }

    @Test
    @Transactional
    public void taskElementHasStartDatePropertyAndItIsRoundedToTheStartOfTheDay() {
        Date now = new Date();
        task.setStartDate(now);
        assertThat(task.getStartDate(), equalTo(toStartOfDay(now)));
        task.setEndDate(now);
        assertThat(task.getEndDate(), equalTo(toStartOfDay(now)));
    }

    private static Date toStartOfDay(Date date) {
        return LocalDate.fromDateFields(date).toDateTimeAtStartOfDay().toDate();
    }

    @Test
    @Transactional
    public void aDependencyWithThisOriginCanBeRemoved() {
        Task origin = new Task();
        Task destination = new Task();
        Type type = Type.START_END;
        Dependency.create(origin, destination, type);
        assertThat(origin.getDependenciesWithThisOrigin().size(), equalTo(1));
        assertThat(destination.getDependenciesWithThisDestination().size(), equalTo(1));
        origin.removeDependencyWithDestination(destination, type);
        assertThat(origin.getDependenciesWithThisOrigin().size(), equalTo(0));
        assertThat(destination.getDependenciesWithThisDestination().size(), equalTo(0));
    }

    private void addDependenciesForChecking(TaskElement taskBeingTransformed, TaskElement sourceDependencyTask,
                                            TaskElement destinationDependencyTask) {

        Dependency.create(sourceDependencyTask, taskBeingTransformed, Type.END_START);
        Dependency.create(taskBeingTransformed, destinationDependencyTask, Type.END_START);
    }

    public void detachRemovesDependenciesFromRelatedTasks() {
        Task taskToDetach = TaskTest.createValidTask();
        Task sourceDependencyTask = TaskTest.createValidTask();
        Task destinationDependencyTask = TaskTest.createValidTask();
        taskToDetach.setName("prueba");
        taskToDetach.setNotes("blabla");
        taskToDetach.setStartDate(new Date());
        addDependenciesForChecking(taskToDetach, sourceDependencyTask, destinationDependencyTask);
        taskToDetach.detach();
        assertThat(sourceDependencyTask.getDependenciesWithThisOrigin().size(), equalTo(0));
        assertThat(destinationDependencyTask.getDependenciesWithThisDestination().size(), equalTo(0));
    }

    @Test
    @Transactional
    public void detachRemovesTaskFromParent() {
        TaskGroup parent = TaskGroupTest.createValidTaskGroup();
        Task child = TaskTest.createValidTask();
        Task anotherChild = TaskTest.createValidTask();
        parent.addTaskElement(child);
        parent.addTaskElement(anotherChild);
        child.detach();
        assertThat(parent.getChildren().size(), equalTo(1));
    }

    @Test
    @Transactional
    public void MilestoneOrderElementIsNull() {
        TaskMilestone milestone = TaskMilestone.create(new Date());
        assertThat(milestone.getOrderElement(), nullValue());
    }

    @Test
    @Transactional
    public void theDeadlineOfTheOrderElementIsCopied() {
        OrderLine orderLine = OrderLine.create();
        addOrderTo(orderLine);
        LocalDate deadline = new LocalDate(2007, 4, 4);
        orderLine.setDeadline(asDate(deadline));
        TaskSource taskSource = asTaskSource(orderLine);
        Task task = Task.createTask(taskSource);
        assertThat(task.getDeadline(), equalTo(deadline));
    }

    private TaskSource asTaskSource(OrderLine orderLine) {
        List<HoursGroup> hoursGroups = orderLine.getHoursGroups();
        if ( hoursGroups.isEmpty() ) {
            hoursGroups = Collections.singletonList(createHoursGroup(100));
        }
        return TaskSource.create(mockSchedulingDataForVersion(orderLine), hoursGroups);
    }

    public static SchedulingDataForVersion mockSchedulingDataForVersion(OrderElement orderElement) {
        SchedulingDataForVersion result = createNiceMock(SchedulingDataForVersion.class);
        TaskSource taskSource = createNiceMock(TaskSource.class);
        expect(result.getOrderElement()).andReturn(orderElement).anyTimes();
        expect(taskSource.getOrderElement()).andReturn(orderElement).anyTimes();
        expect(result.getTaskSource()).andReturn(taskSource).anyTimes();
        replay(result, taskSource);
        return result;
    }

    private static Date asDate(LocalDate localDate) {
        return localDate.toDateTimeAtStartOfDay().toDate();
    }

    private static HoursGroup createHoursGroup(int hours) {
        HoursGroup result = new HoursGroup();
        result.setWorkingHours(hours);
        return result;
    }

    @Test
    @Transactional
    public void ifNoParentWithStartDateThePositionConstraintIsSoonAsPossible() {
        OrderLine orderLine = OrderLine.create();
        addOrderTo(orderLine);
        TaskSource taskSource = asTaskSource(orderLine);
        Task task = Task.createTask(taskSource);
        assertThat(task.getPositionConstraint(), isOfType(PositionConstraintType.AS_SOON_AS_POSSIBLE));
    }

    private void addOrderTo(OrderElement orderElement) {
        Order order = new Order();
        order.useSchedulingDataFor(TaskTest.mockOrderVersion());
        order.setInitDate(new Date());
        order.add(orderElement);
    }

    @Test
    @Transactional
    @SuppressWarnings("unchecked")
    public void ifSomeParentHasInitDateThePositionConstraintIsNotEarlierThan() {
        LocalDate initDate = new LocalDate(2005, 10, 5);
        OrderLineGroup group = OrderLineGroup.create();
        addOrderTo(group);
        group.setInitDate(asDate(initDate));
        OrderLine orderLine = OrderLine.create();
        group.add(orderLine);
        TaskSource taskSource = asTaskSource(orderLine);
        Task task = Task.createTask(taskSource);
        assertThat(task.getPositionConstraint(),
                allOf(isOfType(PositionConstraintType.START_NOT_EARLIER_THAN), hasValue(initDate)));
    }

    @Test
    @Transactional
    public void unlessTheOnlyParentWithInitDateNotNullIsTheOrder() {
        OrderLine orderLine = OrderLine.create();
        addOrderTo(orderLine);
        Order order = orderLine.getOrder();
        Date initDate = asDate(new LocalDate(2005, 10, 5));
        order.setInitDate(initDate);
        TaskSource taskSource = asTaskSource(orderLine);
        Task task = Task.createTask(taskSource);
        assertThat(task.getPositionConstraint(), isOfType(PositionConstraintType.AS_SOON_AS_POSSIBLE));
    }

    private static Matcher<TaskPositionConstraint> isOfType(final PositionConstraintType type) {
        return new BaseMatcher<TaskPositionConstraint>() {
            @Override
            public boolean matches(Object object) {
                if ( object instanceof TaskPositionConstraint ) {
                    TaskPositionConstraint startConstraint = (TaskPositionConstraint) object;
                    return startConstraint.getConstraintType() == type;
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("the start constraint must be of type " + type);
            }
        };
    }

    private static Matcher<TaskPositionConstraint> hasValue(final LocalDate value) {
        return new BaseMatcher<TaskPositionConstraint>() {

            @Override
            public boolean matches(Object object) {
                if ( object instanceof TaskPositionConstraint ) {
                    TaskPositionConstraint startConstraint = (TaskPositionConstraint) object;
                    LocalDate constraintDate = startConstraint.getConstraintDate().toDateTimeAtStartOfDay().toLocalDate();
                    boolean bothNotNull = value != null && constraintDate != null;

                    return value == constraintDate || bothNotNull && constraintDate.equals(value);
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("the start constraint must have date " + value);
            }
        };
    }

}

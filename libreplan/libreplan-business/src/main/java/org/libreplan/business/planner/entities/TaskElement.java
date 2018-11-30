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

package org.libreplan.business.planner.entities;

import static java.util.Arrays.asList;
import static org.libreplan.business.workingday.EffortDuration.zero;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.validation.constraints.NotNull;
import org.joda.time.LocalDate;
import org.libreplan.business.calendars.entities.BaseCalendar;
import org.libreplan.business.common.BaseEntity;
import org.libreplan.business.common.entities.ProgressType;
import org.libreplan.business.externalcompanies.entities.ExternalCompany;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.orders.entities.OrderStatusEnum;
import org.libreplan.business.orders.entities.TaskSource;
import org.libreplan.business.planner.entities.DayAssignment.FilterType;
import org.libreplan.business.planner.entities.Dependency.Type;
import org.libreplan.business.resources.daos.IResourcesSearcher;
import org.libreplan.business.scenarios.entities.Scenario;
import org.libreplan.business.util.TaskElementVisitor;
import org.libreplan.business.util.deepcopy.OnCopy;
import org.libreplan.business.util.deepcopy.Strategy;
import org.libreplan.business.workingday.EffortDuration;
import org.libreplan.business.workingday.IntraDayDate;
import org.libreplan.business.workingday.ResourcesPerDay;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 * @author Manuel Rego Casasnovas <rego@igalia.com>
 */
public abstract class TaskElement extends BaseEntity {

    private static final Log LOG = LogFactory.getLog(TaskElement.class);

    private static final IDatesInterceptor EMPTY_INTERCEPTOR = new IDatesInterceptor() {
        @Override
        public void setStartDate(IntraDayDate previousStart, IntraDayDate previousEnd, IntraDayDate newStart) {}

        @Override
        public void setNewEnd(IntraDayDate previousEnd, IntraDayDate newEnd) {}
    };

    @OnCopy(Strategy.SHARE)
    private IDatesInterceptor datesInterceptor = EMPTY_INTERCEPTOR;

    @OnCopy(Strategy.SHARE)
    private BaseCalendar calendar;

    private IntraDayDate startDate;

    private IntraDayDate endDate;

    private LocalDate deadline;

    private String name;

    private String notes;

    private BigDecimal advancePercentage = BigDecimal.ZERO;

    private Boolean simplifiedAssignedStatusCalculationEnabled = false;

    private Boolean updatedFromTimesheets = false;

    private EffortDuration sumOfAssignedEffort = EffortDuration.zero();

    private TaskGroup parent;

    private Set<Dependency> dependenciesWithThisOrigin = new HashSet<>();

    private Set<Dependency> dependenciesWithThisDestination = new HashSet<>();

    private TaskSource taskSource;

    public static List<Task> justTasks(Collection<? extends TaskElement> tasks) {
        List<Task> result = new ArrayList<>();
        for (TaskElement taskElement : tasks) {
            if ( taskElement instanceof Task ) {
                result.add((Task) taskElement);
            }
        }

        return result;
    }

    public interface IDatesInterceptor {

        void setStartDate(IntraDayDate previousStart, IntraDayDate previousEnd, IntraDayDate newStart);

        void setNewEnd(IntraDayDate previousEnd, IntraDayDate newEnd);
    }

    public static Comparator<TaskElement> getByStartDateComparator() {
        Comparator<TaskElement> result = new Comparator<TaskElement>() {
            @Override
            public int compare(TaskElement o1, TaskElement o2) {
                return o1.getStartDate().compareTo(o2.getStartDate());
            }
        };

        return result;
    }

    public static Comparator<? super TaskElement> getByEndAndDeadlineDateComparator() {
        return new Comparator<TaskElement>() {
            @Override
            public int compare(TaskElement o1, TaskElement o2) {
                return o1.getBiggestAmongEndOrDeadline().compareTo(o2.getBiggestAmongEndOrDeadline());
            }
        };
    }

    /**
     * @returns the biggest one among the deadline (if exists) or the end date.
     */
    @SuppressWarnings("unchecked")
    public LocalDate getBiggestAmongEndOrDeadline() {
        return this.getDeadline() != null
                ? Collections.max(asList(this.getDeadline(), this.getEndAsLocalDate()))
                : this.getEndAsLocalDate();
    }

    protected static <T extends TaskElement> T create(T taskElement, TaskSource taskSource) {
        taskElement.setTaskSource(taskSource);
        taskElement.updateDeadlineFromOrderElement();
        taskElement.setName(taskElement.getOrderElement().getName());
        taskElement.updateAdvancePercentageFromOrderElement();
        Order order = taskElement.getOrderElement().getOrder();

        if ( order.isScheduleBackwards() ) {
            taskElement.setEndDate(order.getDeadline());
        } else {
            taskElement.setStartDate(order.getInitDate());
        }
        return create(taskElement);
    }

    protected static <T extends TaskElement> T createWithoutTaskSource(T taskElement) {
        return create(taskElement);
    }

    public void initializeDatesIfNeeded() {
        if ( getIntraDayEndDate() == null || getIntraDayStartDate() == null ) {
            initializeDates();
        }
    }

    protected abstract void initializeDates();

    public void updateDeadlineFromOrderElement() {
        Date newDeadline = this.taskSource.getOrderElement().getDeadline();
        setDeadline(newDeadline == null ? null : new LocalDate(newDeadline));
    }

    public void setDatesInterceptor(IDatesInterceptor datesIntercerptor) {
        Validate.notNull(datesIntercerptor);
        this.datesInterceptor = datesIntercerptor;
    }

    public Integer getWorkHours() {
        return taskSource == null ? 0 : taskSource.getTotalHours();
    }

    protected void copyPropertiesFrom(TaskElement task) {
        this.name = task.getName();
        this.notes = task.getNotes();
        this.startDate = task.startDate;
        this.taskSource = task.getTaskSource();
    }

    public TaskSource getTaskSource() {
        return taskSource;
    }

    protected void setTaskSource(TaskSource taskSource) {
        this.taskSource = taskSource;
    }

    protected void copyDependenciesTo(TaskElement result) {
        for (Dependency dependency : getDependenciesWithThisOrigin()) {
            Dependency.create(result, dependency.getDestination(), dependency.getType());
        }

        for (Dependency dependency : getDependenciesWithThisDestination()) {
            Dependency.create(dependency.getOrigin(), result, dependency.getType());
        }
    }

    protected void copyParenTo(TaskElement result) {
        if ( this.getParent() != null ) {
            this.getParent().addTaskElement(result);
        }
    }

    public TaskGroup getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return getOrderElement().getCode();
    }

    public String getProjectCode() {
        //then get the top level project code
        return getTopMost().getOrderElement().getOrder().getCode();
    }

    public void setName(String name) {
        this.name = name;
        if ( taskSource != null && taskSource.getOrderElement() != null ) {
            taskSource.getOrderElement().setName(name);
        }
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public OrderElement getOrderElement() {
        return getTaskSource() == null ? null : getTaskSource().getOrderElement();
    }

    public Set<Dependency> getDependenciesWithThisOrigin() {
        return Collections.unmodifiableSet(dependenciesWithThisOrigin);
    }

    public Set<Dependency> getDependenciesWithThisDestination() {
        return Collections.unmodifiableSet(dependenciesWithThisDestination);
    }

    public Set<Dependency> getDependenciesWithThisDestinationAndAllParents() {
        Set<Dependency> result = new HashSet<>(getDependenciesWithThisDestination());
        if ( parent != null ) {
            result.addAll(parent.getDependenciesWithThisDestinationAndAllParents());
        }

        return result;
    }

    public Date getStartDate() {
        return startDate != null ? startDate.getDate().toDateTimeAtStartOfDay().toDate() : null;
    }

    @NotNull
    public IntraDayDate getIntraDayStartDate() {
        return startDate;
    }

    public LocalDate getStartAsLocalDate() {
        return startDate == null ? null : startDate.getDate();
    }

    public LocalDate getEndAsLocalDate() {
        return endDate == null ? null : endDate.getDate();
    }

    public void setStartDate(Date startDate) {
        setIntraDayStartDate(IntraDayDate.startOfDay(LocalDate.fromDateFields(startDate)));
    }

    public void setIntraDayStartDate(IntraDayDate startDate) {
        if ( startDate == null ) {
            LOG.error(doNotProvideNullsDiscouragingMessage());
        }

        IntraDayDate previousStart = getIntraDayStartDate();
        IntraDayDate previousEnd = getIntraDayEndDate();
        this.startDate = startDate;
        datesInterceptor.setStartDate(previousStart, previousEnd, getIntraDayStartDate());
    }

    @NotNull
    public Date getEndDate() {
        return (endDate != null) ? endDate.toDateTimeAtStartOfDay().toDate() : null;
    }

    public void setEndDate(Date endDate) {
        setIntraDayEndDate( (endDate != null)
                ? IntraDayDate.create(LocalDate.fromDateFields(endDate), EffortDuration.zero())
                : null);
    }

    public void setIntraDayEndDate(IntraDayDate endDate) {
        if ( endDate == null ) {
            LOG.error(doNotProvideNullsDiscouragingMessage());
        }

        IntraDayDate previousEnd = getIntraDayEndDate();
        this.endDate = endDate;
        datesInterceptor.setNewEnd(previousEnd, this.endDate);
    }

    private String doNotProvideNullsDiscouragingMessage() {
        return "The provided date shouldn't be null.\n" +
                "Providing null values to start or end dates is not safe.\n" +
                "In a near future an exception will be thrown if you provide a null value to a start or end date.\n" +
                "Please detect the caller and fix it";
    }

    @NotNull
    public IntraDayDate getIntraDayEndDate() {
        return endDate;
    }

    public IDatesHandler getDatesHandler(Scenario scenario, IResourcesSearcher resourcesSearcher) {
        return noNullDates(createDatesHandler(scenario, resourcesSearcher));
    }

    private IDatesHandler noNullDates(final IDatesHandler decorated) {
        return new IDatesHandler() {

            @Override
            public void resizeTo(IntraDayDate endDate) {
                Validate.notNull(endDate);
                decorated.resizeTo(endDate);
            }

            @Override
            public void moveTo(IntraDayDate newStartDate) {
                Validate.notNull(newStartDate);
                decorated.moveTo(newStartDate);
            }

            @Override
            public void moveEndTo(IntraDayDate newEnd) {
                Validate.notNull(newEnd);
                decorated.moveEndTo(newEnd);
            }
        };
    }

    protected abstract IDatesHandler createDatesHandler(Scenario scenario, IResourcesSearcher resourcesSearcher);

    public interface IDatesHandler {

        /**
         * Sets the startDate to newStartDate. It can update the endDate.
         *
         * @param newStartDate
         */
        void moveTo(IntraDayDate newStartDate);

        void moveEndTo(IntraDayDate newEnd);

        void resizeTo(IntraDayDate endDate);
    }

    protected abstract boolean canBeResized();

    /**
     * @return if this task can be resized by an explicit action
     */
    public abstract boolean canBeExplicitlyResized();

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
        if ( taskSource != null && taskSource.getOrderElement() != null ) {

            taskSource.getOrderElement().setDeadline((deadline == null)
                    ? null
                    : deadline.toDateTimeAtStartOfDay().toDate());
        }
    }

    public void add(Dependency dependency) {
        if ( this.equals(dependency.getOrigin()) ) {
            dependenciesWithThisOrigin.add(dependency);
        }

        if ( this.equals(dependency.getDestination()) ) {
            dependenciesWithThisDestination.add(dependency);
        }
    }

    private void removeDependenciesWithThisOrigin(TaskElement origin, Type type) {
        ArrayList<Dependency> toBeRemoved = new ArrayList<>();
        for (Dependency dependency : dependenciesWithThisDestination) {
            if ( dependency.getOrigin().equals(origin) && dependency.getType().equals(type) ) {
                toBeRemoved.add(dependency);
            }
        }
        dependenciesWithThisDestination.removeAll(toBeRemoved);
    }

    public void removeDependencyWithDestination(TaskElement destination, Type type) {
        ArrayList<Dependency> toBeRemoved = new ArrayList<>();
        for (Dependency dependency : dependenciesWithThisOrigin) {
            if ( dependency.getDestination().equals(destination) && dependency.getType().equals(type) ) {
                toBeRemoved.add(dependency);
            }
        }
        destination.removeDependenciesWithThisOrigin(this, type);
        dependenciesWithThisOrigin.removeAll(toBeRemoved);
    }

    public abstract boolean isLeaf();

    public abstract List<TaskElement> getChildren();

    protected void setParent(TaskGroup taskGroup) {
        this.parent = taskGroup;
    }

    public void detach() {
        detachDependencies();
        detachFromParent();
    }

    public void detachFromParent() {
        if ( parent != null ) {
            parent.remove(this);
        }
    }

    private void removeDependenciesWithOrigin(TaskElement t) {
        List<Dependency> dependenciesToRemove = getDependenciesWithOrigin(t);
        dependenciesWithThisDestination.removeAll(dependenciesToRemove);
    }

    private void removeDependenciesWithDestination(TaskElement t) {
        List<Dependency> dependenciesToRemove = getDependenciesWithDestination(t);
        dependenciesWithThisOrigin.removeAll(dependenciesToRemove);
    }

    private List<Dependency> getDependenciesWithDestination(TaskElement t) {
        ArrayList<Dependency> result = new ArrayList<>();
        for (Dependency dependency : dependenciesWithThisOrigin) {
            if ( dependency.getDestination().equals(t) ) {
                result.add(dependency);
            }
        }

        return result;
    }

    private List<Dependency> getDependenciesWithOrigin(TaskElement t) {
        ArrayList<Dependency> result = new ArrayList<>();
        for (Dependency dependency : dependenciesWithThisDestination) {
            if ( dependency.getOrigin().equals(t) ) {
                result.add(dependency);
            }
        }

        return result;
    }

    public void detachDependencies() {
        detachOutcomingDependencies();
        detachIncomingDependencies();
    }

    private void detachIncomingDependencies() {
        Set<TaskElement> tasksToNotify = new HashSet<>();
        for (Dependency dependency : dependenciesWithThisDestination) {
            TaskElement origin = dependency.getOrigin();
            if ( origin != null ) {
                tasksToNotify.add(origin);
            }
        }
        for (TaskElement taskElement : tasksToNotify) {
            this.removeDependenciesWithOrigin(taskElement);
            taskElement.removeDependenciesWithDestination(this);
        }
    }

    private void detachOutcomingDependencies() {
        Set<TaskElement> tasksToNotify = new HashSet<>();
        for (Dependency dependency : dependenciesWithThisOrigin) {
            TaskElement destination = dependency.getDestination();
            if ( destination != null ) {
                tasksToNotify.add(destination);
            }
        }
        for (TaskElement taskElement : tasksToNotify) {
            this.removeDependenciesWithDestination(taskElement);
            taskElement.removeDependenciesWithOrigin(this);
        }
    }

    public void setCalendar(BaseCalendar calendar) {
        this.calendar = calendar;
    }

    public BaseCalendar getOwnCalendar() {
        return calendar;
    }

    public BaseCalendar getCalendar() {
        if ( calendar == null ) {
            OrderElement orderElement = getOrderElement();

            return orderElement != null ? orderElement.getOrder().getCalendar() : null;
        }

        return calendar;
    }

    public abstract Set<ResourceAllocation<?>> getSatisfiedResourceAllocations();

    public abstract Set<ResourceAllocation<?>> getAllResourceAllocations();

    public SortedMap<LocalDate, EffortDuration> getDurationsAssignedByDay() {
        SortedMap<LocalDate, EffortDuration> result = new TreeMap<>();
        for (ResourceAllocation<?> resourceAllocation : getSatisfiedResourceAllocations()) {
            for (DayAssignment each : resourceAllocation.getAssignments()) {
                addToResult(result, each.getDay(), each.getDuration());
            }
        }

        return result;
    }

    private void addToResult(SortedMap<LocalDate, EffortDuration> result, LocalDate date, EffortDuration duration) {
        EffortDuration current = result.get(date) != null ? result.get(date) : zero();
        result.put(date, current.plus(duration));
    }

    public List<DayAssignment> getDayAssignments(DayAssignment.FilterType filter) {
        List<DayAssignment> dayAssignments = new ArrayList<>();
        Set<ResourceAllocation<?>> resourceAllocations = getSatisfiedResourceAllocations();

        for (ResourceAllocation<?> resourceAllocation : resourceAllocations) {
            dayAssignments.addAll(resourceAllocation.getAssignments());
            Set<DerivedAllocation> derivedAllocations = resourceAllocation.getDerivedAllocations();

            for (DerivedAllocation each : derivedAllocations) {
                dayAssignments.addAll(each.getAssignments());
            }
        }
        return DayAssignment.filter(dayAssignments, filter);
    }

    /**
     * Just Task could be subcontracted.
     */
    public boolean isSubcontracted() {
        return false;
    }

    public String getSubcontractionName() {
        return "";
    }

    /**
     * Just Task could be subcontracted.
     */
    public boolean isSubcontractedAndWasAlreadySent() {
        return false;
    }

    public boolean isLimiting() {
        return false;
    }

    public boolean isLimitingAndHasDayAssignments() {
        return false;
    }

    /**
     * Just Task could be consolidated.
     */
    public boolean hasConsolidations() {
        return false;
    }

    public TaskElement getTopMost() {
        TaskElement result = this;
        while (result.getParent() != null) {
            result = result.getParent();
        }

        return result;
    }

    public abstract boolean isMilestone();

    public Boolean isSimplifiedAssignedStatusCalculationEnabled() {
        return simplifiedAssignedStatusCalculationEnabled;
    }

    public void setSimplifiedAssignedStatusCalculationEnabled(Boolean enabled) {
        this.simplifiedAssignedStatusCalculationEnabled = enabled;
    }

    public String getAssignedStatus() {
        if (isSimplifiedAssignedStatusCalculationEnabled()) {
            // Simplified calculation has only two states:
            // 1. Unassigned, when hours allocated is zero.
            // 2. Assigned otherwise.

            return getSumOfAssignedEffort().isZero() ? "unassigned" : "assigned";
        }

        Set<ResourceAllocation<?>> resourceAllocations = getSatisfiedResourceAllocations();
        if ( resourceAllocations.isEmpty() ) {
            return "unassigned";
        }

        for (ResourceAllocation<?> resourceAllocation : resourceAllocations) {
            final ResourcesPerDay resourcesPerDay = resourceAllocation.getResourcesPerDay();
            if ( resourcesPerDay != null && resourcesPerDay.isZero() ) {
                return "partially-assigned";
            }
        }

        return "assigned";
    }

    public Boolean belongsClosedProject() {

        EnumSet<OrderStatusEnum> CLOSED =
                EnumSet.of(OrderStatusEnum.CANCELLED, OrderStatusEnum.FINISHED, OrderStatusEnum.STORED);

        return CLOSED.contains(getOrderElement().getOrder().getState());
    }

    public abstract boolean hasLimitedResourceAllocation();

    public void removePredecessorsDayAssignmentsFor(Scenario scenario) {
        for (ResourceAllocation<?> each : getAllResourceAllocations()) {
            each.removePredecessorsDayAssignmentsFor(scenario);
        }
    }

    public void removeDayAssignmentsFor(Scenario scenario) {
        for (ResourceAllocation<?> each : getAllResourceAllocations()) {
            each.removeDayAssignmentsFor(scenario);
        }
    }

    public BigDecimal getAdvancePercentage() {
        return (advancePercentage == null) ? BigDecimal.ZERO : advancePercentage;
    }

    /**
     * For common tasks it just return the spread progress.
     *
     * It's overridden in {@link TaskGroup} to return different progresses depending on parameter.
     */
    public BigDecimal getAdvancePercentage(ProgressType progressType) {
        return progressType != null && progressType.equals(ProgressType.SPREAD_PROGRESS)
                ? advancePercentage
                : BigDecimal.ZERO;
    }

    public void setAdvancePercentage(BigDecimal advancePercentage) {
        this.advancePercentage = advancePercentage;
        this.resetStatus();
    }

    public void setSumOfAssignedEffort(EffortDuration sumOfAssignedEffort) {
        this.sumOfAssignedEffort = sumOfAssignedEffort;
    }

    public EffortDuration getSumOfAssignedEffort() {
        if ( this.getParent() == null ) {
            // It's an order, we use the cached value
            return sumOfAssignedEffort;
        }
        else {
            return getSumOfAssignedEffortCalculated();
        }
    }

    private EffortDuration getSumOfAssignedEffortCalculated() {
        EffortDuration result = EffortDuration.zero();
        for (ResourceAllocation<?> allocation : getAllResourceAllocations()) {
            result = result.plus(allocation.getAssignedEffort());
        }
        return result;
    }

    public String toString() {
        return super.toString() + " :: " + getName();
    }

    public abstract boolean isTask();

    public List<TaskElement> getAllChildren() {
        List<TaskElement> children = getChildren();
        List<TaskElement> result = new ArrayList<>();
        for (TaskElement child : children) {
            result.add(child);
            result.addAll(child.getAllChildren());
        }

        return result;
    }

    public abstract EffortDuration getTheoreticalCompletedTimeUntilDate(Date date);

    public BigDecimal getTheoreticalAdvancePercentageUntilDate(Date date) {
        EffortDuration totalAllocatedTime =
                AggregateOfDayAssignments.create(this.getDayAssignments(FilterType.KEEP_ALL)).getTotalTime();

        EffortDuration totalTheoreticalCompletedTime = this.getTheoreticalCompletedTimeUntilDate(date);

        if ( totalAllocatedTime.isZero() || totalTheoreticalCompletedTime.isZero() ) {
            return BigDecimal.ZERO;
        }

        Validate.isTrue(totalTheoreticalCompletedTime.getSeconds() <= totalAllocatedTime.getSeconds());

        return totalTheoreticalCompletedTime.dividedByAndResultAsBigDecimal(totalAllocatedTime);
    }

    public abstract boolean isFinished();

    public abstract boolean isInProgress();

    public abstract void acceptVisitor(TaskElementVisitor visitor);

    public abstract void resetStatus();

    public void updateAdvancePercentageFromOrderElement() {
        setAdvancePercentage(getOrderElement().getAdvancePercentage());
    }

    public Boolean isRoot() {
        return this.getParent() == null;
    }

    public BigDecimal getBudget() {
        return (taskSource != null) && (taskSource.getOrderElement() != null)
                ? taskSource.getOrderElement().getBudget()
                : null;
    }

    public BigDecimal getResourcesBudget() {
        return (taskSource != null) && (taskSource.getOrderElement() != null)
                ? taskSource.getOrderElement().getResourcesBudget()
                : null;
    }

    public ExternalCompany getSubcontractedCompany() {
        return null;
    }

    public abstract boolean isAnyTaskWithConstraint(PositionConstraintType type);

    public TaskDeadlineViolationStatusEnum getDeadlineViolationStatus() {
        LocalDate deadline = this.getDeadline();

        if ( deadline == null ) {
            return TaskDeadlineViolationStatusEnum.NO_DEADLINE;

        } else if ( this.getEndAsLocalDate().isAfter(deadline) ) {
            return TaskDeadlineViolationStatusEnum.DEADLINE_VIOLATED;

        } else {
            return TaskDeadlineViolationStatusEnum.ON_SCHEDULE;
        }
    }

    public static IntraDayDate maxDate(Collection<? extends TaskElement> tasksToSave) {
        List<IntraDayDate> endDates = toEndDates(tasksToSave);

        return endDates.isEmpty() ? null : Collections.max(endDates);
    }

    private static List<IntraDayDate> toEndDates(Collection<? extends TaskElement> tasksToSave) {
        List<IntraDayDate> result = new ArrayList<>();

        for (TaskElement taskElement : tasksToSave) {
            IntraDayDate endDate = taskElement.getIntraDayEndDate();
            if ( endDate != null ) {
                result.add(endDate);
            } else {
                LOG.warn("the task" + taskElement + " has null end date");
            }
        }

        return result;
    }

    public static IntraDayDate minDate(Collection<? extends TaskElement> tasksToSave) {
        List<IntraDayDate> startDates = toStartDates(tasksToSave);

        return startDates.isEmpty() ? null : Collections.min(startDates);
    }

    private static List<IntraDayDate> toStartDates(Collection<? extends TaskElement> tasksToSave) {
        List<IntraDayDate> result = new ArrayList<>();
        for (TaskElement taskElement : tasksToSave) {
            IntraDayDate startDate = taskElement.getIntraDayStartDate();
            if ( startDate != null ) {
                result.add(startDate);
            } else {
                LOG.warn("the task" + taskElement + " has null start date");
            }
        }

        return result;
    }

    public Boolean isUpdatedFromTimesheets() {
        return updatedFromTimesheets;
    }

    public void setUpdatedFromTimesheets(Boolean updatedFromTimesheets) {
        this.updatedFromTimesheets = BooleanUtils.isTrue(updatedFromTimesheets);
    }

}

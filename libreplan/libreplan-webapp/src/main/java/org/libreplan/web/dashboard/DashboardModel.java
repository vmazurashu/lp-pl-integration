/*
 * This file is part of LibrePlan
 *
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

package org.libreplan.web.dashboard;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.planner.chart.ContiguousDaysLine;
import org.libreplan.business.planner.chart.ContiguousDaysLine.OnDay;
import org.libreplan.business.planner.entities.IOrderResourceLoadCalculator;
import org.libreplan.business.planner.entities.TaskDeadlineViolationStatusEnum;
import org.libreplan.business.planner.entities.TaskElement;
import org.libreplan.business.planner.entities.TaskGroup;
import org.libreplan.business.planner.entities.TaskStatusEnum;
import org.libreplan.business.planner.entities.visitors.AccumulateTasksDeadlineStatusVisitor;
import org.libreplan.business.planner.entities.visitors.AccumulateTasksStatusVisitor;
import org.libreplan.business.planner.entities.visitors.CalculateFinishedTasksEstimationDeviationVisitor;
import org.libreplan.business.planner.entities.visitors.CalculateFinishedTasksLagInCompletionVisitor;
import org.libreplan.business.planner.entities.visitors.ResetTasksStatusVisitor;
import org.libreplan.business.workingday.EffortDuration;
import org.libreplan.web.planner.order.PlanningStateCreator.PlanningState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Nacho Barrientos <nacho@igalia.com>
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 * @author Diego Pino García <dpino@igalia.com>
 * @author Manuel Rego Casasnovas <rego@igalia.com>
 *
 * Model for UI operations related to Order Dashboard View
 *
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DashboardModel implements IDashboardModel {

    @Autowired
    private IOrderResourceLoadCalculator resourceLoadCalculator;

    private Order currentOrder;

    private List<TaskElement> criticalPath;

    private Integer taskCount = null;

    private final Map<TaskStatusEnum, BigDecimal> taskStatusStats;

    private final Map<TaskDeadlineViolationStatusEnum, BigDecimal> taskDeadlineViolationStatusStats;

    private BigDecimal marginWithDeadLine;

    private Integer absoluteMarginWithDeadLine;

    public DashboardModel() {
        taskStatusStats = new EnumMap<>(TaskStatusEnum.class);
        taskDeadlineViolationStatusStats = new EnumMap<>(TaskDeadlineViolationStatusEnum.class);
    }

    @Override
    public void setCurrentOrder(PlanningState planningState, List<TaskElement> criticalPath) {
        final Order order = planningState.getOrder();

        resourceLoadCalculator.setOrder(order, planningState.getAssignmentsCalculator());
        this.currentOrder = order;
        this.criticalPath = criticalPath;
        this.taskCount = null;

        if ( tasksAvailable() ) {
            this.calculateGlobalProgress();
            this.calculateTaskStatusStatistics();
            this.calculateTaskViolationStatusStatistics();
            this.calculateAbsoluteMarginWithDeadLine();
            this.calculateMarginWithDeadLine();
        }
    }

    /* Progress KPI: "Number of tasks by status" */
    @Override
    public BigDecimal getPercentageOfFinishedTasks() {
        return taskStatusStats.get(TaskStatusEnum.FINISHED);
    }

    @Override
    public BigDecimal getPercentageOfInProgressTasks() {
        return taskStatusStats.get(TaskStatusEnum.IN_PROGRESS);
    }

    @Override
    public BigDecimal getPercentageOfReadyToStartTasks() {
        return taskStatusStats.get(TaskStatusEnum.READY_TO_START);
    }

    @Override
    public BigDecimal getPercentageOfBlockedTasks() {
        return taskStatusStats.get(TaskStatusEnum.BLOCKED);
    }

    /* Progress KPI: "Deadline violation" */
    @Override
    public BigDecimal getPercentageOfOnScheduleTasks() {
        return taskDeadlineViolationStatusStats.get(TaskDeadlineViolationStatusEnum.ON_SCHEDULE);
    }

    @Override
    public BigDecimal getPercentageOfTasksWithViolatedDeadline() {
        return taskDeadlineViolationStatusStats.get(TaskDeadlineViolationStatusEnum.DEADLINE_VIOLATED);
    }

    @Override
    public BigDecimal getPercentageOfTasksWithNoDeadline() {
        return taskDeadlineViolationStatusStats.get(TaskDeadlineViolationStatusEnum.NO_DEADLINE);
    }

    /* Progress KPI: "Global Progress of the Project" */
    private void calculateGlobalProgress() {
        TaskGroup rootTask = getRootTask();
        if ( rootTask == null ) {
            throw new RuntimeException("Root task is null");
        }
        rootTask.updateCriticalPathProgress(criticalPath);
    }

    @Override
    public BigDecimal getSpreadProgress() {
        return asPercentage(getRootTask().getAdvancePercentage());
    }

    private BigDecimal asPercentage(BigDecimal value) {
        return value != null ? value.multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getAdvancePercentageByHours() {
        return asPercentage(getRootTask().getProgressAllByNumHours());
    }

    @Override
    public BigDecimal getExpectedAdvancePercentageByHours() {
        return asPercentage(getRootTask().getTheoreticalProgressByNumHoursForAllTasksUntilNow());
    }

    @Override
    public BigDecimal getCriticalPathProgressByNumHours() {
        return asPercentage(getRootTask().getCriticalPathProgressByNumHours());
    }

    @Override
    public BigDecimal getExpectedCriticalPathProgressByNumHours() {
        return asPercentage(getRootTask().getTheoreticalProgressByNumHoursForCriticalPathUntilNow());
    }

    @Override
    public BigDecimal getCriticalPathProgressByDuration() {
        return asPercentage(getRootTask().getCriticalPathProgressByDuration());
    }

    @Override
    public BigDecimal getExpectedCriticalPathProgressByDuration() {
        return asPercentage(getRootTask().getTheoreticalProgressByDurationForCriticalPathUntilNow());
    }

    /* Time KPI: Margin with deadline */
    @Override
    public BigDecimal getMarginWithDeadLine() {
        return this.marginWithDeadLine;
    }

    private void calculateMarginWithDeadLine() {
        if ( this.getRootTask() == null ) {
            throw new RuntimeException("Root task is null");
        }

        if ( this.currentOrder.getDeadline() == null ) {
            this.marginWithDeadLine = null;
            return;
        }

        TaskGroup rootTask = getRootTask();
        LocalDate endDate = TaskElement.maxDate(rootTask.getChildren()).asExclusiveEnd();
        Days orderDuration = Days.daysBetween(TaskElement.minDate(rootTask.getChildren()).getDate(), endDate);

        LocalDate deadLineAsLocalDate = LocalDate.fromDateFields(currentOrder.getDeadline());
        Days deadlineOffset = Days.daysBetween(endDate, deadLineAsLocalDate.plusDays(1));

        BigDecimal outcome = new BigDecimal(deadlineOffset.getDays(), MathContext.DECIMAL32);

        this.marginWithDeadLine = orderDuration.getDays()!= 0
                ? outcome.divide(new BigDecimal(orderDuration.getDays()), 8, BigDecimal.ROUND_HALF_EVEN)
                : new BigDecimal(
                Days.daysBetween(rootTask.getStartAsLocalDate(), deadLineAsLocalDate.plusDays(1)).getDays());
    }

    @Override
    public Integer getAbsoluteMarginWithDeadLine() {
        return absoluteMarginWithDeadLine;
    }

    private void calculateAbsoluteMarginWithDeadLine() {
        TaskElement rootTask = getRootTask();
        Date deadline = currentOrder.getDeadline();

        if ( rootTask == null ) {
            throw new RuntimeException("Root task is null");
        }

        if ( deadline == null ) {
            this.absoluteMarginWithDeadLine = null;
            return;
        }

        absoluteMarginWithDeadLine = daysBetween(
                TaskElement.maxDate(rootTask.getChildren()).asExclusiveEnd(),
                LocalDate.fromDateFields(deadline).plusDays(1));
    }

    private int daysBetween(LocalDate start, LocalDate end) {
        return Days.daysBetween(start, end).getDays();
    }

    /**
     * Calculates the task completion deviations for the current order
     *
     * All the deviations are groups in 6 intervals of equal size. If the order
     * contains just one single task then, the upper limit will be the deviation
     * of the task +3, and the lower limit will be deviation of the task -3
     *
     * Each {@link Interval} contains the number of tasks that fit in that
     * interval
     */
    @Override
    public Map<Interval, Integer> calculateTaskCompletion() {
        List<Double> deviations = getTaskLagDeviations();

        return calculateHistogramIntervals(deviations, 6, 1);
    }

    private List<Double> getTaskLagDeviations() {
        if ( this.getRootTask() == null ) {
            throw new RuntimeException("Root task is null");
        }

        CalculateFinishedTasksLagInCompletionVisitor visitor = new CalculateFinishedTasksLagInCompletionVisitor();
        TaskElement rootTask = getRootTask();
        rootTask.acceptVisitor(visitor);

        return visitor.getDeviations();
    }

    /**
     * Calculates the estimation accuracy deviations for the current order.
     *
     * All the deviations are groups in 6 intervals of equal size (not less than
     * 10). There're some restrictions:
     * <ul>
     * <li>If the order contains just one single task then, the upper limit will
     * be the deviation of the task +30, and the lower limit will be deviation
     * of the task -30.</li>
     * <li>If the difference between values is bigger than 60, then the
     * intervals will be bigger than 10 but it'll keep generating 6 intervals.
     * For example with min -45 and max +45, we'll have 6 intervals of size 15.</li>
     * <li>In the case that we have enough distance for, it doesn't need to set
     * the min to -30. For example, with min 0 and max 60, it'll keep intervals
     * of size 10.</li>
     * <li>If the min was 10 and the max 40, it'll have to decrease the min and
     * increase the max to get a difference of 60. For example setting min to
     * -10 and max to 50. (In order to calculate this it subtracts 10 to the min
     * and check if the difference is 60 again, if not it adds 10 to the max and
     * check it again, repeating this till it has a difference of 60).</li>
     * </ul>
     *
     * Each {@link Interval} contains the number of tasks that fit in that
     * interval.
     */
    @Override
    public Map<Interval, Integer> calculateEstimationAccuracy() {
        List<Double> deviations = getEstimationAccuracyDeviations();

        return calculateHistogramIntervals(deviations, 6, 10);
    }

    private Map<Interval, Integer> calculateHistogramIntervals(List<Double> values, int intervalsNumber,
                                                               int intervalMinimumSize) {
        Map<Interval, Integer> result = new LinkedHashMap<>();

        int totalMinimumSize = intervalsNumber * intervalMinimumSize;
        int halfSize = totalMinimumSize / 2;

        double maxDouble, minDouble;
        if ( values.isEmpty() ) {
            minDouble = -halfSize;
            maxDouble = halfSize;
        } else {
            minDouble = Collections.min(values);
            maxDouble = Collections.max(values);
        }

        // If min and max are between -halfSize and +halfSize, set -halfSize as
        // min and +halfSize as max
        if ( minDouble >= -halfSize && maxDouble <= halfSize ) {
            minDouble = -halfSize;
            maxDouble = halfSize;
        }

        // If the difference between min and max is less than totalMinimumSize,
        // decrease min
        while (maxDouble - minDouble < totalMinimumSize) {
            minDouble -= intervalMinimumSize;
        }

        // Round min and max properly depending on decimal part or not
        int min;
        double minDecimalPart = minDouble - (int) minDouble;
        if ( minDouble >= 0 ) {
            min = (int) (minDouble - minDecimalPart);
        } else {
            min = (int) (minDouble - minDecimalPart);
            if ( minDecimalPart != 0 ) {
                min--;
            }
        }

        int max;
        double maxDecimalPart = maxDouble - (int) maxDouble;
        if ( maxDouble >= 0 ) {
            max = (int) (maxDouble - maxDecimalPart);
            if ( maxDecimalPart != 0 ) {
                max++;
            }
        } else {
            max = (int) (maxDouble - maxDecimalPart);
        }

        // Calculate intervals size
        double delta = (double) (max - min) / intervalsNumber;
        double deltaDecimalPart = delta - (int) delta;

        // Generate intervals
        int from = min;
        for (int i = 0; i < intervalsNumber; i++) {
            int to = from + (int) delta;
            // Fix to depending on decimal part if it's not the last interval
            if ( deltaDecimalPart == 0 && i != (intervalsNumber - 1) ) {
                to--;
            }

            result.put(new Interval(from, to), 0);

            from = to + 1;
        }

        // Construct map with number of tasks for each interval
        final Set<Interval> intervals = result.keySet();
        for (Double each : values) {
            Interval interval = Interval.containingValue(intervals, each);
            if ( interval != null ) {
                Integer value = result.get(interval);
                result.put(interval, value + 1);
            }
        }

        return result;
    }

    private List<Double> getEstimationAccuracyDeviations() {
        if ( this.getRootTask() == null ) {
            throw new RuntimeException("Root task is null");
        }

        CalculateFinishedTasksEstimationDeviationVisitor visitor = new CalculateFinishedTasksEstimationDeviationVisitor();
        TaskElement rootTask = getRootTask();
        rootTask.acceptVisitor(visitor);

        return visitor.getDeviations();
    }

    static class Interval {
        private int min;
        private int max;

        public Interval(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public static Interval containingValue(Collection<Interval> intervals, double value) {
            for (Interval each : intervals) {
                if ( each.includes(value) ) {
                    return each;
                }
            }

            return null;
        }

        private boolean includes(double value) {
            return (value >= min) && (value <= max);
        }

        @Override
        public String toString() {
            return "[" + min + ", " + max + "]";
        }

    }

    @Override
    public Map<TaskStatusEnum, Integer> calculateTaskStatus() {
        AccumulateTasksStatusVisitor visitor = new AccumulateTasksStatusVisitor();
        TaskElement rootTask = getRootTask();

        if ( this.getRootTask() == null ) {
            throw new RuntimeException("Root task is null");
        }

        resetTasksStatusInGraph();
        rootTask.acceptVisitor(visitor);

        return visitor.getTaskStatusData();
    }

    private void calculateTaskStatusStatistics() {
        AccumulateTasksStatusVisitor visitor = new AccumulateTasksStatusVisitor();
        TaskElement rootTask = getRootTask();

        if ( this.getRootTask() == null ) {
            throw new RuntimeException("Root task is null");
        }

        resetTasksStatusInGraph();
        rootTask.acceptVisitor(visitor);
        Map<TaskStatusEnum, Integer> count = visitor.getTaskStatusData();
        mapAbsoluteValuesToPercentages(count, taskStatusStats);
    }

    private void calculateTaskViolationStatusStatistics() {
        AccumulateTasksDeadlineStatusVisitor visitor = new AccumulateTasksDeadlineStatusVisitor();
        TaskElement rootTask = getRootTask();

        if ( this.getRootTask() == null ) {
            throw new RuntimeException("Root task is null");
        }

        rootTask.acceptVisitor(visitor);
        Map<TaskDeadlineViolationStatusEnum, Integer> count = visitor.getTaskDeadlineViolationStatusData();
        mapAbsoluteValuesToPercentages(count, taskDeadlineViolationStatusStats);
    }

    private <T> void mapAbsoluteValuesToPercentages(Map<T, Integer> source, Map<T, BigDecimal> dest) {
        int totalTasks = countTasksInAResultMap(source);
        for (Map.Entry<T, Integer> entry : source.entrySet()) {
            BigDecimal percentage;
            if ( totalTasks == 0 ) {
                percentage = BigDecimal.ZERO;

            } else {
                percentage = new BigDecimal(
                        100 * (entry.getValue() / (1.0 * totalTasks)),
                        MathContext.DECIMAL32);
            }
            dest.put(entry.getKey(), percentage);
        }
    }

    private TaskGroup getRootTask() {
        return currentOrder.getAssociatedTaskElement();
    }

    private void resetTasksStatusInGraph() {
        ResetTasksStatusVisitor visitor = new ResetTasksStatusVisitor();
        getRootTask().acceptVisitor(visitor);
    }

    private int countTasksInAResultMap(Map<? extends Object, Integer> map) {

         // It's only needed to count the number of tasks once each time setOrder is called.
        if ( this.taskCount != null ) {
            return this.taskCount.intValue();
        }

        int sum = 0;
        for (Object count : map.values()) {
            sum += (Integer) count;
        }
        this.taskCount = sum;

        return sum;
    }

    @Override
    public boolean tasksAvailable() {
        return getRootTask() != null;
    }

    @Override
    public BigDecimal getOvertimeRatio() {
        EffortDuration totalLoad = sumAll(resourceLoadCalculator.getAllLoad());
        EffortDuration overload = sumAll(resourceLoadCalculator.getAllOverload());

        return overload.dividedByAndResultAsBigDecimal(totalLoad).setScale(2, RoundingMode.HALF_UP);
    }

    private EffortDuration sumAll(ContiguousDaysLine<EffortDuration> contiguousDays) {
        EffortDuration result = EffortDuration.zero();
        Iterator<OnDay<EffortDuration>> iterator = contiguousDays.iterator();

        while (iterator.hasNext()) {
            OnDay<EffortDuration> value = iterator.next();
            EffortDuration effort = value.getValue();
            result = EffortDuration.sum(result, effort);
        }

        return result;
    }

    @Override
    public BigDecimal getAvailabilityRatio() {
        EffortDuration totalLoad = sumAll(resourceLoadCalculator.getAllLoad());
        EffortDuration overload = sumAll(resourceLoadCalculator.getAllOverload());
        EffortDuration load = totalLoad.minus(overload);
        EffortDuration capacity = sumAll(resourceLoadCalculator.getMaxCapacityOnResources());

        return BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP).subtract(load.dividedByAndResultAsBigDecimal(capacity));
    }

}

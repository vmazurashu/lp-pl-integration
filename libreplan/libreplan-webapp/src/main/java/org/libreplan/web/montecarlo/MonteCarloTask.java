/*
 * This file is part of LibrePlan
 *
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
package org.libreplan.web.montecarlo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.apache.commons.lang3.Validate;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.libreplan.business.calendars.entities.BaseCalendar;
import org.libreplan.business.planner.entities.Task;
import org.libreplan.business.workingday.EffortDuration;
import org.libreplan.business.workingday.IntraDayDate.PartialDay;

/**
 * @author Diego Pino Garcia<dpino@igalia.com>
 */
public class MonteCarloTask {

    private static final MathContext mathContext = new MathContext(2, RoundingMode.HALF_UP);

    private Task task;

    private BigDecimal duration;

    private BigDecimal pessimisticDuration;

    private Integer pessimisticDurationPercentage;

    private BigDecimal normalDuration;

    private Integer normalDurationPercentage;

    private BigDecimal optimisticDuration;

    private Integer optimisticDurationPercentage;

    private MonteCarloTask(Task task) {
        this.task = task;
        duration = BigDecimal.valueOf(task.getWorkableDays());
        pessimisticDuration = duration.multiply(BigDecimal.valueOf(1.50));
        pessimisticDurationPercentage = 30;
        normalDuration = duration;
        normalDurationPercentage = 50;
        optimisticDuration = duration.multiply(BigDecimal.valueOf(0.50));
        optimisticDurationPercentage = 20;
    }

    private MonteCarloTask(MonteCarloTask task) {
        this.task = task.getTask();
        this.duration = task.getDuration();
        this.pessimisticDuration = task.getPessimisticDuration();
        this.pessimisticDurationPercentage = task.getPessimisticDurationPercentage();
        this.normalDuration = task.getNormalDuration();
        this.normalDurationPercentage = task.getNormalDurationPercentage();
        this.optimisticDuration = task.getOptimisticDuration();
        this.optimisticDurationPercentage = task.getOptimisticDurationPercentage();
    }

    public static MonteCarloTask create(Task task) {
        return new MonteCarloTask(task);
    }

    public static MonteCarloTask copy(MonteCarloTask task) {
        return new MonteCarloTask(task);
    }

    public static BigDecimal calculateRealDurationFor(MonteCarloTask task, BigDecimal daysDuration) {
        LocalDate start = task.getStartDate();
        Validate.notNull(start);
        LocalDate end = calculateEndDateFor(task, daysDuration);
        Days daysBetween = Days.daysBetween(start, end);

        return BigDecimal.valueOf(daysBetween.getDays());
    }

    private static LocalDate calculateEndDateFor(MonteCarloTask task, BigDecimal daysDuration) {
        BaseCalendar calendar = task.getCalendar();
        LocalDate day = new LocalDate(task.getStartDate());

        double duration = daysDuration.doubleValue();
        for (int i = 0; i < duration;) {
            EffortDuration workableTime = calendar.getCapacityOn(PartialDay.wholeDay(day));

            if (!EffortDuration.zero().equals(workableTime)) {
                i++;
            }

            day = day.plusDays(1);
        }
        return day;
    }

    public Task getTask() {
        return task;
    }

    public LocalDate getStartDate() {
        return task.getStartAsLocalDate();
    }

    private BaseCalendar getCalendar() {
        return task.getCalendar();
    }

    public String getTaskName() {
        return task.getName();
    }

    public BigDecimal getDuration() {
        return duration;
    }

    public BigDecimal getPessimisticDuration() {
        return pessimisticDuration;
    }

    public Integer getPessimisticDurationPercentage() {
        return pessimisticDurationPercentage;
    }

    public BigDecimal getNormalDuration() {
        return normalDuration;
    }

    public Integer getNormalDurationPercentage() {
        return normalDurationPercentage;
    }

    public BigDecimal getOptimisticDuration() {
        return optimisticDuration;
    }

    public Integer getOptimisticDurationPercentage() {
        return optimisticDurationPercentage;
    }

    public void setDuration(BigDecimal duration) {
        this.duration = duration;
    }

    public void setPessimisticDuration(BigDecimal pessimisticDuration) {
        this.pessimisticDuration = pessimisticDuration;
    }

    public void setPessimisticDurationPercentage(Integer pessimisticDurationPercentage) {
        this.pessimisticDurationPercentage = pessimisticDurationPercentage;
    }

    public void setNormalDuration(BigDecimal normalDuration) {
        this.normalDuration = normalDuration;
    }

    public void setNormalDurationPercentage(Integer normalDurationPercentage) {
        this.normalDurationPercentage = normalDurationPercentage;
    }

    public void setOptimisticDuration(BigDecimal optimisticDuration) {
        this.optimisticDuration = optimisticDuration;
    }

    public void setOptimisticDurationPercentage(Integer optimisticDurationPercentage) {
        this.optimisticDurationPercentage = optimisticDurationPercentage;
    }

    @Override
    public String toString() {
        return String.format(
                "%s:%f:(%f,%d):(%f,%d):(%f,%d)", task.getName(),
                duration, pessimisticDuration,
                pessimisticDurationPercentage, normalDuration,
                normalDurationPercentage, optimisticDuration,
                optimisticDurationPercentage);
    }

    public String getOrderName() {
        return task.getOrderElement().getOrder().getName();
    }

    public BigDecimal getPessimisticDurationPercentageLowerLimit() {
        return BigDecimal.ZERO;
    }

    public BigDecimal getPessimisticDurationPercentageUpperLimit() {
        return BigDecimal.valueOf(pessimisticDurationPercentage).divide(BigDecimal.valueOf(100), mathContext);
    }

    public BigDecimal getNormalDurationPercentageLowerLimit() {
        return getPessimisticDurationPercentageUpperLimit();
    }

    public BigDecimal getNormalDurationPercentageUpperLimit() {
        return BigDecimal.valueOf(pessimisticDurationPercentage.longValue() + normalDurationPercentage)
                .divide(BigDecimal.valueOf(100), mathContext);
    }

    public BigDecimal getOptimisticDurationPercentageLowerLimit() {
        return getNormalDurationPercentageUpperLimit();
    }

    public BigDecimal getOptimisticDurationPercentageUpperLimit() {
        return BigDecimal.ONE;
    }

}

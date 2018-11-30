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

package org.libreplan.business.reports.dtos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.joda.time.LocalDate;
import org.libreplan.business.common.Registry;
import org.libreplan.business.planner.entities.DayAssignment;
import org.libreplan.business.planner.entities.DayAssignment.FilterType;
import org.libreplan.business.planner.entities.Task;
import org.libreplan.business.workingday.EffortDuration;
import org.libreplan.business.workreports.daos.IWorkReportLineDAO;
import org.libreplan.business.workreports.entities.WorkReportLine;

/**
 *
 * @author Diego Pino Garcia <dpino@igalia.com>
 *
 */
public class WorkingProgressPerTaskDTO {

    private IWorkReportLineDAO workReportLineDAO;

    private String taskName;

    private Integer estimatedHours;

    private Integer totalPlannedHours;

    private Integer partialPlannedHours;

    private EffortDuration realHours;

    private BigDecimal averageProgress;

    private Double imputedProgress;

    private Double plannedProgress;

    private BigDecimal costDifference;

    private BigDecimal planningDifference;

    private BigDecimal ratioCostDifference;

    private BigDecimal ratioPlanningDifference;

    private WorkingProgressPerTaskDTO() {
        workReportLineDAO = Registry.getWorkReportLineDAO();

    }

    public WorkingProgressPerTaskDTO(Task task, LocalDate date) {
        this();

        this.taskName = getTaskName(task);

        this.estimatedHours = task.getHoursSpecifiedAtOrder();
        this.totalPlannedHours = calculatePlannedHours(task, null);
        this.partialPlannedHours = calculatePlannedHours(task, date);
        this.realHours = calculateRealHours(task, date);
        this.averageProgress = task.getOrderElement().getAdvancePercentage(date);

        this.imputedProgress = (totalPlannedHours != 0) ?
                new Double(realHours.toHoursAsDecimalWithScale(2).doubleValue() / totalPlannedHours.doubleValue())
                : new Double(0);
        this.plannedProgress = (totalPlannedHours != 0) ?
                new Double(partialPlannedHours / totalPlannedHours.doubleValue())
                : new Double(0);
        this.costDifference = calculateCostDifference(averageProgress,
                                                        new BigDecimal(totalPlannedHours),
                                                                        realHours.toHoursAsDecimalWithScale(2));
        this.planningDifference = calculatePlanningDifference(averageProgress,
                                                            new BigDecimal(totalPlannedHours),
                                                            new BigDecimal(partialPlannedHours));
        this.ratioCostDifference = calculateRatioCostDifference(averageProgress, imputedProgress);
        this.ratioPlanningDifference = calculateRatioPlanningDifference(averageProgress, plannedProgress);
    }

    public String getTaskName(Task task) {
        String result = task.getName();
        if ( result == null || result.isEmpty() ) {
            result = task.getOrderElement().getName();
        }

        return result;
    }

    public Integer calculatePlannedHours(Task task, LocalDate date) {
        Integer result = new Integer(0);

        final List<DayAssignment> dayAssignments = task.getDayAssignments(FilterType.WITHOUT_DERIVED);
        if ( dayAssignments.isEmpty() ) {
            return result;
        }

        for (DayAssignment dayAssignment : dayAssignments) {
            if ( date == null || dayAssignment.getDay().compareTo(date) <= 0 ) {
                result += dayAssignment.getDuration().getHours();
            }
        }
        return result;
    }

    public EffortDuration calculateRealHours(Task task, LocalDate date) {
        EffortDuration result = EffortDuration.zero();

        final List<WorkReportLine> workReportLines = workReportLineDAO
                .findByOrderElementAndChildren(task.getOrderElement());
        if ( workReportLines.isEmpty() ) {
            return result;
        }

        for (WorkReportLine workReportLine : workReportLines) {
            final LocalDate workReportLineDate = new LocalDate(workReportLine.getDate());
            if ( date == null || workReportLineDate.compareTo(date) <= 0 ) {
                result = EffortDuration.sum(result, workReportLine.getEffort());
            }
        }
        return result;
    }

    public Integer getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(Integer estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public Integer getTotalPlannedHours() {
        return totalPlannedHours;
    }

    public void setTotalPlannedHours(Integer totalPlannedHours) {
        this.totalPlannedHours = totalPlannedHours;
    }

    public Integer getPartialPlannedHours() {
        return partialPlannedHours;
    }

    public void setPartialPlannedHours(Integer partialPlannedHours) {
        this.partialPlannedHours = partialPlannedHours;
    }

    public EffortDuration getRealHours() {
        return realHours;
    }

    public void setRealHours(EffortDuration realHours) {
        this.realHours = realHours;
    }

    public BigDecimal getAverageProgress() {
        return averageProgress;
    }

    public void setAverageProgress(BigDecimal averageProgress) {
        this.averageProgress = averageProgress;
    }

    public Double getImputedProgress() {
        return imputedProgress;
    }

    public void setImputedProgress(Double imputedProgress) {
        this.imputedProgress = imputedProgress;
    }

    public Double getPlannedProgress() {
        return plannedProgress;
    }

    public void setPlannedProgress(Double plannedProgress) {
        this.plannedProgress = plannedProgress;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public BigDecimal calculateCostDifference(BigDecimal averageProgress,
                                              BigDecimal totalPlannedHours,
                                              BigDecimal realHours) {
        BigDecimal result = averageProgress;
        result = result.multiply(totalPlannedHours);

        return result.subtract(realHours);
    }

    public BigDecimal calculatePlanningDifference(BigDecimal averageProgress,
                                                  BigDecimal totalPlannedHours,
                                                  BigDecimal partialPlannedHours) {
        BigDecimal result = averageProgress;
        result = result.multiply(totalPlannedHours);

        return result.subtract(partialPlannedHours);
    }

    public BigDecimal calculateRatioCostDifference(BigDecimal averageProgress, Double imputedProgress) {
        if ( imputedProgress.doubleValue() == 0 ) {
            return new BigDecimal(0);
        }

        return averageProgress.divide(new BigDecimal(imputedProgress), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateRatioPlanningDifference(BigDecimal averageProgress, Double plannedProgress) {
        if ( plannedProgress.doubleValue() == 0 ) {
            return new BigDecimal(0);
        }

        return averageProgress.divide(new BigDecimal(plannedProgress), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getCostDifference() {
        return costDifference;
    }

    public BigDecimal getPlanningDifference() {
        return planningDifference;
    }

    public BigDecimal getRatioCostDifference() {
        return ratioCostDifference;
    }

    public BigDecimal getRatioPlanningDifference() {
        return ratioPlanningDifference;
    }

}

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

import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.AssertTrue;

import org.apache.commons.lang3.Validate;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.orders.entities.OrderStatusEnum;
import org.libreplan.business.resources.daos.IResourcesSearcher;
import org.libreplan.business.scenarios.entities.Scenario;
import org.libreplan.business.util.TaskElementVisitor;
import org.libreplan.business.workingday.EffortDuration;
import org.libreplan.business.workingday.IntraDayDate;

/**
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 * @author Javier Moran Rua <jmoran@igalia.com>
 * @author Manuel Rego Casasnovas <rego@igalia.com>
 */
public class TaskMilestone extends TaskElement implements ITaskPositionConstrained {

    public static TaskMilestone create(Date initialDate) {
        Validate.notNull(initialDate);
        TaskMilestone milestone = new TaskMilestone();
        milestone.setStartDate(initialDate);
        milestone.setEndDate(initialDate);
        return createWithoutTaskSource(milestone);
    }

    private CalculatedValue calculatedValue = CalculatedValue.END_DATE;

    private TaskPositionConstraint startConstraint = new TaskPositionConstraint();

    /**
     * Constructor for hibernate. Do not use!
     */
    public TaskMilestone() {

    }

    public Set<ResourceAllocation<?>> getSatisfiedResourceAllocations() {
        return Collections.emptySet();
    }

    @Override
    public Set<ResourceAllocation<?>> getAllResourceAllocations() {
        return Collections.emptySet();
    }

    public int getAssignedHours() {
        return 0;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public List<TaskElement> getChildren() {
        return Collections.emptyList();
    }

    public CalculatedValue getCalculatedValue() {
        if (calculatedValue == null) {
            return CalculatedValue.END_DATE;
        }
        return calculatedValue;
    }

    public void setCalculatedValue(CalculatedValue calculatedValue) {
        Validate.notNull(calculatedValue);
        this.calculatedValue = calculatedValue;
    }

    public void setDaysDuration(Integer duration) {
        Validate.notNull(duration);
        Validate.isTrue(duration >= 0);
        DateTime endDate = toDateTime(getStartDate()).plusDays(duration);
        setEndDate(endDate.toDate());
    }

    public Integer getDaysDuration() {
        Days daysBetween = Days.daysBetween(toDateTime(getStartDate()),
                toDateTime(getEndDate()));
        return daysBetween.getDays();
    }

    private DateTime toDateTime(Date startDate) {
        return new DateTime(startDate.getTime());
    }

    @SuppressWarnings("unused")
    @AssertTrue(message = "a milestone cannot have a task associated")
    private boolean isTheOrderElementMustBeNullConstraint() {
        return getOrderElement() == null;
    }

    @Override
    protected IDatesHandler createDatesHandler(Scenario scenario,
            IResourcesSearcher searcher) {
        return new IDatesHandler() {

            @Override
            public void moveTo(IntraDayDate newStartDate) {
                setIntraDayStartDate(newStartDate);
                setIntraDayEndDate(newStartDate);
            }

            @Override
            public void resizeTo(IntraDayDate endDate) {
                moveTo(endDate);
            }

            @Override
            public void moveEndTo(IntraDayDate newEnd) {
                moveTo(newEnd);
            }
        };
    }

    @Override
    protected void initializeDates() {
        // do nothing
    }

    @Override
    protected boolean canBeResized() {
        return false;
    }

    @Override
    public boolean canBeExplicitlyResized() {
        return false;
    }

    @Override
    public boolean isMilestone() {
        return true;
    }

    @Override
    public boolean hasLimitedResourceAllocation() {
        return false;
    }

    public void explicityMoved(IntraDayDate startDate, IntraDayDate endDate) {
        getPositionConstraint().explicityMovedTo(startDate, endDate,
                getParent().getOrderElement().getOrder().getSchedulingMode());
    }

    public TaskPositionConstraint getPositionConstraint() {
        if (startConstraint == null) {
            startConstraint = new TaskPositionConstraint();
        }
        return startConstraint;
    }

    @Override
    public boolean isTask() {
        return false;
    }

    @Override
    public EffortDuration getTheoreticalCompletedTimeUntilDate(Date date) {
        return EffortDuration.zero();
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public boolean isInProgress() {
        return false;
    }

    @Override
    public void acceptVisitor(TaskElementVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void resetStatus() {
    }

    @Override
    public Boolean belongsClosedProject() {
        EnumSet<OrderStatusEnum> CLOSED = EnumSet.of(OrderStatusEnum.CANCELLED,
                OrderStatusEnum.FINISHED, OrderStatusEnum.STORED);

        Order order = getParent().getOrderElement().getOrder();
        if(CLOSED.contains(order.getState())) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isAnyTaskWithConstraint(PositionConstraintType type) {
        return getPositionConstraint().getConstraintType().equals(type);
    }

}

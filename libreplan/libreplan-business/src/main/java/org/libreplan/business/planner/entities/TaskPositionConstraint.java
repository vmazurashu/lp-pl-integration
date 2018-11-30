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

import java.util.Date;

import org.apache.commons.lang3.Validate;
import org.libreplan.business.orders.entities.Order.SchedulingMode;
import org.libreplan.business.workingday.IntraDayDate;

/**
 * Component class that encapsulates a {@link PositionConstraintType} and its
 * associated constraint date <br />
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 */
public class TaskPositionConstraint {

    private PositionConstraintType constraintType = PositionConstraintType.AS_SOON_AS_POSSIBLE;

    private IntraDayDate constraintDate = null;

    public TaskPositionConstraint() {
    }

    public PositionConstraintType getConstraintType() {
        return constraintType != null ? constraintType
                : PositionConstraintType.AS_SOON_AS_POSSIBLE;
    }

    public boolean isConstraintAppliedToStart() {
        return getConstraintType().appliesToTheStart();
    }

    public boolean isConstraintAppliedToEnd() {
        return !isConstraintAppliedToStart();
    }

    public Date getConstraintDateAsDate() {
        return constraintDate != null ? constraintDate.toDateTimeAtStartOfDay()
                .toDate() : null;
    }

    public void explicityMovedTo(IntraDayDate startDate, IntraDayDate endDate,
            SchedulingMode mode) {
        Validate.notNull(startDate);
        Validate.notNull(endDate);
        constraintType = constraintType.newTypeAfterMoved(mode);
        if (isConstraintAppliedToStart()) {
            constraintDate = startDate;
        } else {
            constraintDate = endDate;
        }
    }

    public IntraDayDate getConstraintDate() {
        return constraintDate;
    }

    public void notEarlierThan(IntraDayDate date) {
        Validate.notNull(date);
        this.constraintDate = date;
        this.constraintType = PositionConstraintType.START_NOT_EARLIER_THAN;
    }

    public void finishNotLaterThan(IntraDayDate date) {
        Validate.notNull(date);
        this.constraintDate = date;
        this.constraintType = PositionConstraintType.FINISH_NOT_LATER_THAN;
    }

    public void asLateAsPossible() {
        this.constraintType = PositionConstraintType.AS_LATE_AS_POSSIBLE;
        this.constraintDate = null;
    }

    public void asSoonAsPossible() {
        this.constraintType = PositionConstraintType.AS_SOON_AS_POSSIBLE;
        this.constraintDate = null;
    }

    public boolean isValid(PositionConstraintType type, IntraDayDate value) {
        return type != null
                && type.isAssociatedDateRequired() == (value != null);
    }

    public void update(PositionConstraintType type, IntraDayDate value) {
        Validate.isTrue(isValid(type, value));
        this.constraintType = type;
        this.constraintDate = value;
    }

}

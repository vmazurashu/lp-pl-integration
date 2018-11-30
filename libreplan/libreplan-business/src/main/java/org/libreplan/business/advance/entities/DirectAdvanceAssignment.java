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

package org.libreplan.business.advance.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import javax.validation.Valid;
import org.joda.time.LocalDate;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.planner.entities.consolidations.NonCalculatedConsolidation;

/**
 * Represents an {@link AdvanceAssignment} that is own of this {@link OrderElement}.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public class DirectAdvanceAssignment extends AdvanceAssignment {

    public static DirectAdvanceAssignment create() {
        DirectAdvanceAssignment directAdvanceAssignment = new DirectAdvanceAssignment();
        directAdvanceAssignment.setNewObject(true);
        return directAdvanceAssignment;
    }

    public static DirectAdvanceAssignment create(boolean reportGlobalAdvance,
                                                 BigDecimal maxValue) {
        DirectAdvanceAssignment advanceAssignment = new DirectAdvanceAssignment(
                reportGlobalAdvance, maxValue);
        advanceAssignment.setNewObject(true);
        return advanceAssignment;
    }

    private BigDecimal maxValue;

    @Valid
    private SortedSet<AdvanceMeasurement> advanceMeasurements = new TreeSet<>(new AdvanceMeasurementComparator());

    @Valid
    private Set<NonCalculatedConsolidation> nonCalculatedConsolidations = new HashSet<NonCalculatedConsolidation>();

    private boolean fake = false;

    public DirectAdvanceAssignment() {
        super();
    }

    private DirectAdvanceAssignment(boolean reportGlobalAdvance,
                                    BigDecimal maxValue) {
        super(reportGlobalAdvance);
        this.maxValue = maxValue;
        this.maxValue.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    @NotNull(message = "maximum value not specified")
    public BigDecimal getMaxValue() {
        return this.maxValue;
    }

    public void setMaxValue(BigDecimal maxValue) {
        this.maxValue = maxValue;
        if (maxValue != null) {
            this.maxValue.setScale(2);
        }
    }

    public SortedSet<AdvanceMeasurement> getAdvanceMeasurements() {
        return Collections.unmodifiableSortedSet(this.advanceMeasurements);
    }

    public void setAdvanceMeasurements(
            SortedSet<AdvanceMeasurement> advanceMeasurements) {
        this.advanceMeasurements.clear();
        this.advanceMeasurements.addAll(advanceMeasurements);
    }

    public AdvanceMeasurement getLastAdvanceMeasurement() {

        if (advanceMeasurements.isEmpty()) {
            return null;
        }

        return advanceMeasurements.first();
    }

    public AdvanceMeasurement getAdvanceMeasurementAtDateOrPrevious(LocalDate date) {
        if (advanceMeasurements.isEmpty()) {
            return null;
        }

        for (AdvanceMeasurement advanceMeasurement : advanceMeasurements) {
            if (advanceMeasurement.getDate().compareTo(date) <= 0) {
                return advanceMeasurement;
            }
        }

        return null;
    }

    public BigDecimal getAdvancePercentage() {
        return getAdvancePercentage(null);
    }

    public BigDecimal getAdvancePercentage(LocalDate date) {
        if (maxValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        AdvanceMeasurement advanceMeasurement = (date != null) ? getAdvanceMeasurementAtDateOrPrevious(date)
                : getLastAdvanceMeasurement();
        if (advanceMeasurement == null || advanceMeasurement.getValue() == null) {
            return BigDecimal.ZERO;
        }
        return advanceMeasurement.getValue().divide(maxValue, 4,
                RoundingMode.DOWN);
    }

    public boolean addAdvanceMeasurements(AdvanceMeasurement advanceMeasurement) {
        boolean result = this.advanceMeasurements.add(advanceMeasurement);
        if (result) {
            advanceMeasurement.setAdvanceAssignment(this);
            if (getOrderElement() != null) {
                getOrderElement()
                        .markAsDirtyLastAdvanceMeasurementForSpreading();
            }
        }
        return result;
    }

    public void removeAdvanceMeasurement(AdvanceMeasurement advanceMeasurement) {
        this.advanceMeasurements.remove(advanceMeasurement);
        advanceMeasurement.setAdvanceAssignment(null);
        getOrderElement().markAsDirtyLastAdvanceMeasurementForSpreading();
    }

    public void removeAdvanceMeasurements(
            Set<AdvanceMeasurement> advanceMeasurements) {
        for (AdvanceMeasurement each: advanceMeasurements) {
            each.setAdvanceAssignment(null);
        }
        this.advanceMeasurements.removeAll(advanceMeasurements);
        getOrderElement().markAsDirtyLastAdvanceMeasurementForSpreading();
    }

    public void clearAdvanceMeasurements() {
        this.advanceMeasurements.clear();
        if (getOrderElement() != null) {
            getOrderElement().markAsDirtyLastAdvanceMeasurementForSpreading();
        }
    }

    public AdvanceMeasurement getAdvanceMeasurementAtExactDate(LocalDate date) {
        if (advanceMeasurements.isEmpty()) {
            return null;
        }

        for (AdvanceMeasurement advanceMeasurement : advanceMeasurements) {
            if (advanceMeasurement.getDate().equals(date)) {
                return advanceMeasurement;
            }
        }

        return null;
    }

    @AssertTrue(message = "Progress measurements must have a value lower than their following progress measurements.")
    public boolean isValidAdvanceMeasurementsConstraint() {
        if (advanceMeasurements.isEmpty()) {
            return true;
        }

        Iterator<AdvanceMeasurement> iterator = advanceMeasurements.iterator();
        AdvanceMeasurement currentAdvance = iterator.next();
        while (iterator.hasNext()) {
            AdvanceMeasurement nextAdvance = iterator.next();
            if ((currentAdvance.getValue() != null)
                    && (nextAdvance.getValue() != null)
                    && (currentAdvance.getDate() != null)
                    && (nextAdvance.getDate() != null)
                    && (currentAdvance.getValue().compareTo(
                    nextAdvance.getValue()) < 0)) {
                return false;
            }
            currentAdvance = nextAdvance;
        }
        return true;
    }

    public void setFake(boolean fake) {
        this.fake = fake;
    }

    public boolean isFake() {
        return fake;
    }

    @AssertTrue(message = "maximum value of percentagee progress type must be 100")
    public boolean isMaxValueMustBe100ForPercentageConstraint() {
        AdvanceType advanceType = getAdvanceType();
        if ((advanceType != null) && (advanceType.getPercentage())) {
            if (maxValue.compareTo(new BigDecimal(100)) != 0) {
                return false;
            }
        }
        return true;
    }

    @AssertTrue(message = "maximum value must be greater than zero")
    public boolean isMaxValueMustBeGreaterThanZeroConstraint() {
        return maxValue.compareTo(BigDecimal.ZERO) > 0;
    }

    public void setNonCalculatedConsolidation(
            Set<NonCalculatedConsolidation> nonCalculatedConsolidation) {
        this.nonCalculatedConsolidations = nonCalculatedConsolidation;
    }

    public Set<NonCalculatedConsolidation> getNonCalculatedConsolidation() {
        return nonCalculatedConsolidations;
    }

    public static DirectAdvanceAssignment copy(
            DirectAdvanceAssignment origin,
            OrderElement orderElement) {
        DirectAdvanceAssignment copy = DirectAdvanceAssignment.create(origin
                .getReportGlobalAdvance(), origin.getMaxValue());
        copy.setAdvanceType(origin.getAdvanceType());
        copy.setAdvanceMeasurements(origin.getAdvanceMeasurements());
        copy.setOrderElement(orderElement);
        return copy;
    }

    public boolean hasAnyConsolidationValue() {
        return !nonCalculatedConsolidations.isEmpty();
    }

    public void resetAdvanceMeasurements(AdvanceMeasurement advanceMeasurement) {
        advanceMeasurements.clear();
        addAdvanceMeasurements(advanceMeasurement);
    }

}

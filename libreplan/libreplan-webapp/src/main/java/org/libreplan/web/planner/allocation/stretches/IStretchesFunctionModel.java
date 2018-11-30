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

package org.libreplan.web.planner.allocation.stretches;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.joda.time.LocalDate;
import org.libreplan.business.calendars.entities.BaseCalendar;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.planner.entities.AssignmentFunction;
import org.libreplan.business.planner.entities.ResourceAllocation;
import org.libreplan.business.planner.entities.Stretch;
import org.libreplan.business.planner.entities.StretchesFunction;
import org.libreplan.business.planner.entities.StretchesFunctionTypeEnum;


/**
 * Contract for {@link StretchesFunctionModel}.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public interface IStretchesFunctionModel {

    /**
     * Initial conversation steps.
     */

    void init(StretchesFunction stretchesFunction, ResourceAllocation<?> resourceAllocation, StretchesFunctionTypeEnum type);

    /**
     * Intermediate conversation steps.
     */

    List<Stretch> getAllStretches();

    List<Stretch> getStretchesDefinedByUser();

    List<Stretch> getStretchesPlusConsolidated();

    void addStretch();

    void removeStretch(Stretch stretch);

    AssignmentFunction getStretchesFunction();

    Date getStretchDate(Stretch stretch);

    void setStretchDate(Stretch stretch, Date date) throws IllegalArgumentException;

    void setStretchLengthPercentage(Stretch stretch, BigDecimal lengthPercentage) throws IllegalArgumentException;

    LocalDate getTaskStartDate();

    Integer getAllocationHours();

    BaseCalendar getTaskCalendar();

    ResourceAllocation<?> getResourceAllocation();

    /**
     * Final conversation steps
     */

    void confirm() throws ValidationException;

    void cancel();

}

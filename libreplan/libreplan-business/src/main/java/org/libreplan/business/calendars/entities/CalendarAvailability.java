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

package org.libreplan.business.calendars.entities;

import java.util.Comparator;
import java.util.Date;

import javax.validation.constraints.NotNull;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.libreplan.business.calendars.daos.ICalendarAvailabilityDAO;
import org.libreplan.business.common.IntegrationEntity;
import org.libreplan.business.common.Registry;

/**
 * Stores information about activating periods, that define the availability of the resource.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public class CalendarAvailability extends IntegrationEntity {

    public static final Comparator<CalendarAvailability> BY_START_DATE_COMPARATOR =
                (o1, o2) -> o1.getStartDate().compareTo(o2.getStartDate());

    @NotNull
    private LocalDate startDate;

    private LocalDate endDate;

    /**
     * Constructor for hibernate. Do not use!
     */
    public CalendarAvailability() {}

    private CalendarAvailability(LocalDate startDate, LocalDate endDate) {
        setStartDate(startDate);
        setEndDate(endDate);
    }

    public static CalendarAvailability create() {
        return create(new CalendarAvailability(new LocalDate(), null));
    }

    public static CalendarAvailability create(Date startDate, Date endDate) {
        return create(new CalendarAvailability(new LocalDate(startDate), new LocalDate(endDate)));
    }

    public static CalendarAvailability create(LocalDate startDate, LocalDate endDate) {
        return create(new CalendarAvailability(startDate, endDate));
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date must not be null");
        }
        if (endDate != null && startDate.compareTo(endDate) > 0) {
            throw new IllegalArgumentException("End date must be greater or equal than start date");
        }

        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        if (endDate != null && startDate.compareTo(endDate) > 0) {
            throw new IllegalArgumentException("End date must be greater or equal than start date");
        }
        this.endDate = endDate;
    }

    public boolean isActive(LocalDate date) {
        return startDate.compareTo(date) <= 0 && !((endDate != null) && (endDate.compareTo(date) < 0));

    }

    @Override
    protected ICalendarAvailabilityDAO getIntegrationEntityDAO() {
        return Registry.getCalendarAvailabilityDAO();
    }

    public boolean isActiveBetween(LocalDate filterStartDate, LocalDate filterEndDate) {
        if (filterStartDate == null && filterEndDate == null) {
            return true;
        }

        if (filterStartDate == null) {
            if (endDate == null) {
                return startDate.compareTo(filterEndDate) <= 0;
            }

            return startDate.compareTo(filterEndDate) <= 0 || endDate.compareTo(filterEndDate) <= 0;
        }

        if (filterEndDate == null) {
            return endDate == null || startDate.compareTo(filterStartDate) >= 0 ||
                    endDate.compareTo(filterStartDate) >= 0;
        }

        if (endDate == null) {
            return startDate.compareTo(filterStartDate) <= 0 || startDate.compareTo(filterEndDate) <= 0;
        }

        Interval filterPeriod = new Interval(filterStartDate.toDateTimeAtStartOfDay(),
                filterEndDate.plusDays(1).toDateTimeAtStartOfDay());

        Interval activationPeriod = new Interval(startDate.toDateTimeAtStartOfDay(),
                endDate.plusDays(1).toDateTimeAtStartOfDay());

        return filterPeriod.overlaps(activationPeriod);
    }

}

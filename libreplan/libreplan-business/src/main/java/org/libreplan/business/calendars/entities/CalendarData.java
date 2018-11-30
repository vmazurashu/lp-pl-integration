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

import static org.libreplan.business.workingday.EffortDuration.hours;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.joda.time.LocalDate;
import org.libreplan.business.calendars.daos.ICalendarDataDAO;
import org.libreplan.business.common.IntegrationEntity;
import org.libreplan.business.common.Registry;
import org.libreplan.business.workingday.EffortDuration;

/**
 * Represents the information about the calendar that can change through time.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public class CalendarData extends IntegrationEntity {

    public static CalendarData create() {
        return create(new CalendarData());
    }

    public static final Comparator<CalendarData> BY_EXPIRING_DATE_COMPARATOR = new Comparator<CalendarData>() {
        @Override
        public int compare(CalendarData o1, CalendarData o2) {
            if (o2.getExpiringDate() != null && o1.getExpiringDate() != null) {
                return o1.getExpiringDate().compareTo(o2.getExpiringDate());
            }
            if (o1.getExpiringDate() == null) {
                return 1;
            }
            return -1;
        }
    };

    public static CalendarData createUnvalidated(String code, LocalDate expiringDate, BaseCalendar parent) {
        CalendarData calendarData = create(new CalendarData(), code);
        calendarData.expiringDate = expiringDate;
        calendarData.parent = parent;
        return calendarData;
    }

    public void updateUnvalidated(LocalDate expiringDate, BaseCalendar parent) {
        if (expiringDate != null) {
            this.expiringDate = expiringDate;
        }

        if (parent != null) {
            this.parent = parent;
        }
    }

    public void updateCapacitiesPerDay(Map<Integer, Capacity> capacityPerDay) throws IllegalArgumentException {
        if (capacityPerDay == null) {
            return;
        }
        for (Days day : Days.values()) {
            Capacity capacity = capacityPerDay.get(day.ordinal());
            if (capacity != null) {
                setCapacityAt(day, capacity);
            }
        }
    }

    private Map<Integer, Capacity> capacityPerDay;

    private LocalDate expiringDate;

    private BaseCalendar parent;

    public enum Days {
        MONDAY(Calendar.MONDAY),
        TUESDAY(Calendar.TUESDAY),
        WEDNESDAY(Calendar.WEDNESDAY),
        THURSDAY(Calendar.THURSDAY),
        FRIDAY(Calendar.FRIDAY),
        SATURDAY(Calendar.SATURDAY),
        SUNDAY(Calendar.SUNDAY);

        private int index;

        Days(int index) {
            this.index = index;
        }

        /**
         * This is used to get the week day translated via {@link DateFormatSymbols#getWeekdays()}.
         */
        public int getIndex() {
            return index;
        }

    }

    /**
     * Constructor for hibernate. Do not use!
     */
    public CalendarData() {
        capacityPerDay = new HashMap<>();
        for (Days each : Days.values()) {
            setCapacityAt(each, null);
        }
    }

    public Map<Integer, Integer> getHoursPerDay() {
        return asHours(capacityPerDay);
    }

    private Map<Integer, Integer> asHours(Map<Integer, Capacity> capacities) {
        Map<Integer, Integer> result = new HashMap<>();
        for (Entry<Integer, Capacity> each : capacities.entrySet()) {
            EffortDuration value = toDuration(each.getValue());
            result.put(each.getKey(), value == null ? null : value.getHours());
        }
        return result;
    }

    private static EffortDuration toDuration(Capacity capacity) {
        if (capacity == null) {
            return null;
        }
        return capacity.getStandardEffort();
    }

    public Capacity getCapacityOn(Days day) {
        return capacityPerDay.get(day.ordinal());
    }

    public Map<Integer, Capacity> getCapacityPerDay() {
        return Collections.unmodifiableMap(capacityPerDay);
    }

    public void setCapacityAt(Days day, Capacity capacity) {
        capacityPerDay.put(day.ordinal(), capacity);
    }


    public boolean isDefault(Days day) {
        return getCapacityOn(day) == null;
    }

    public void setDefault(Days day) {
        setCapacityAt(day, null);
    }

    /**
     * The expiringDate.
     * It is exclusive.
     */
    public LocalDate getExpiringDate() {
        return expiringDate;
    }

    public void setExpiringDate(LocalDate expiringDate) {
        this.expiringDate = expiringDate;
    }

    public CalendarData copy() {
        CalendarData copy = create();
        copy.capacityPerDay = new HashMap<>(this.capacityPerDay);
        copy.expiringDate = this.expiringDate;
        copy.parent = this.parent;

        return copy;
    }

    public BaseCalendar getParent() {
        return parent;
    }

    public void setParent(BaseCalendar parent) {
        this.parent = parent;
    }

    public void removeExpiringDate() {
        this.expiringDate = null;
    }

    public boolean isPosteriorTo(LocalDate date) {
        return expiringDate == null || expiringDate.compareTo(date) > 0;
    }

    boolean isEmpty() {
        for (Days each : Days.values()) {
            if (!isEmptyFor(each)) {
                return false;
            }
        }
        return true;
    }

    boolean isEmptyFor(Days day) {
        if (isDefault(day)) {
            if (hasParent()) {
                return getParent().onlyGivesZeroHours(day);
            } else {
                return true;
            }
        } else {
            return getCapacityOn(day).isZero();
        }
    }

    private boolean hasParent() {
        return getParent() != null;
    }

    @Override
    protected ICalendarDataDAO getIntegrationEntityDAO() {
        return Registry.getCalendarDataDAO();
    }

    public static void resetDefaultCapacities(CalendarData calendar) {
        Capacity eightHours = Capacity.create(hours(8)).overAssignableWithoutLimit();
        calendar.setCapacityAt(Days.MONDAY, eightHours);
        calendar.setCapacityAt(Days.TUESDAY, eightHours);
        calendar.setCapacityAt(Days.WEDNESDAY, eightHours);
        calendar.setCapacityAt(Days.THURSDAY, eightHours);
        calendar.setCapacityAt(Days.FRIDAY, eightHours);
        calendar.setCapacityAt(Days.SATURDAY, Capacity.zero());
        calendar.setCapacityAt(Days.SUNDAY, Capacity.zero());
    }

}

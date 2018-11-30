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

package org.libreplan.business.planner.limiting.entities;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.libreplan.business.workingday.EffortDuration;
import org.libreplan.business.workingday.IntraDayDate;

/**
 *
 * @author Diego Pino Garcia <dpino@igalia.com>
 *
 */
public class DateAndHour implements Comparable<DateAndHour> {

    public static DateAndHour from(LocalDate date) {
        if (date != null) {
            return new DateAndHour(date, 0);
        }
        return null;
    }

    public static DateAndHour from(IntraDayDate date) {
        return new DateAndHour(date.getDate(), date.getEffortDuration().getHours());
    }

    public static DateAndHour TEN_YEARS_FROM(DateAndHour dateAndHour) {
        LocalDate date = dateAndHour.getDate() != null ? dateAndHour.getDate() : new LocalDate();
        DateAndHour result = new DateAndHour(date, dateAndHour.getHour());
        result.plusYears(10);
        return result;
    }

    private LocalDate date;

    private Integer hour;

    public DateAndHour(LocalDate date, Integer hour) {
        Validate.notNull(date);
        this.date = date;
        this.hour = hour;
    }

    public DateAndHour(DateAndHour dateAndHour) {
        Validate.notNull(date);
        this.date = dateAndHour.getDate();
        this.hour = dateAndHour.getHour();
    }

    public LocalDate getDate() {
        return date;
    }

    public Integer getHour() {
        return hour;
    }

    @Override
    public int compareTo(DateAndHour time) {
        Validate.notNull(time);
        int compareDate = date.compareTo(getDate(time));
        return (compareDate != 0) ? compareDate : compareHour(time
                .getHour());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DateAndHour) {
            DateAndHour other = (DateAndHour) obj;
            return new EqualsBuilder().append(date, other.date)
                    .append(hour, other.hour).isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(date).append(hour).toHashCode();
    }

    private LocalDate getDate(DateAndHour dateAndHour) {
        return (dateAndHour != null) ? dateAndHour.getDate() : null;
    }

    private int compareHour(int hour) {
        int deltaHour = this.hour - hour;
        return (deltaHour != 0) ? deltaHour / Math.abs(deltaHour) : 0;
    }

    public String toString() {
        return date + "; " + hour;
    }

    public DateTime toDateTime() {
        return date.toDateTimeAtStartOfDay().plusHours(hour);
    }

    public IntraDayDate toIntraDayDate() {
        return IntraDayDate.create(date, EffortDuration.hours(hour));
    }

    public static DateAndHour max(DateAndHour... dates) {
        dates = (DateAndHour[]) ArrayUtils.removeElement(dates, null);
        return dates.length > 0 ?  Collections.max(Arrays.asList(dates)) : null;
    }

    public static DateAndHour min(DateAndHour... dates) {
        dates = (DateAndHour[]) ArrayUtils.removeElement(dates, null);
        return dates.length > 0 ?  Collections.min(Arrays.asList(dates)) : null;
    }

    public boolean isBefore(DateAndHour dateAndHour) {
        return (this.compareTo(dateAndHour) < 0);
    }

    public boolean isAfter(DateAndHour dateAndHour) {
        return (this.compareTo(dateAndHour) > 0);
    }

    public boolean isEquals(DateAndHour dateAndHour) {
        return (this.compareTo(dateAndHour) == 0);
    }

    public boolean isAfter(LocalDate date) {
        return isAfter(DateAndHour.from(date));
    }

    /**
     * Creates an {@link Iterable} that returns a lazy iterator. If
     * <code>end</code> is <code>null</code> it will not stop and will keep on
     * producing days forever
     */
    public Iterable<LocalDate> daysUntil(final DateAndHour end) {
        Validate.isTrue(end == null || end.isAfter(this));
        return new Iterable<LocalDate>() {
            @Override
            public Iterator<LocalDate> iterator() {
                return new Iterator<LocalDate>() {

                    private LocalDate current = getDate();

                    @Override
                    public boolean hasNext() {
                        return end == null || end.isAfter(current);
                    }

                    @Override
                    public LocalDate next() {
                        LocalDate result = current;
                        current = current.plusDays(1);
                        return result;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    public void plusYears(int years) {
        date = date.plusYears(years);
    }

}

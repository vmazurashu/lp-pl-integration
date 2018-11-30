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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import javax.validation.Valid;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.libreplan.business.common.BaseEntity;
import org.libreplan.business.planner.entities.DayAssignment;
import org.libreplan.business.planner.entities.GenericResourceAllocation;
import org.libreplan.business.planner.entities.ResourceAllocation;
import org.libreplan.business.planner.entities.SpecificResourceAllocation;
import org.libreplan.business.planner.entities.Task;
import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.resources.entities.LimitingResourceQueue;
import org.libreplan.business.resources.entities.LimitingResourceQueueElementComparator;
import org.libreplan.business.resources.entities.Resource;
import org.libreplan.business.workingday.IntraDayDate;

/**
 * Entity which represents an element in the queue which represents the limiting
 * resources.
 * @author Diego Pino Garcia <dpino@igalia.com>
 * @author Javier Moran Rua <jmoran@igalia.com>
 */
public class LimitingResourceQueueElement extends BaseEntity {

    public static Comparator<LimitingResourceQueueElement> byStartTimeComparator() {
        return new LimitingResourceQueueElementComparator();
    }

    private ResourceAllocation<?> resourceAllocation;

    private LimitingResourceQueue limitingResourceQueue;

    private Date earlierStartDateBecauseOfGantt;

    private Date earliestEndDateBecauseOfGantt;

    private QueuePosition startQueuePosition;

    private QueuePosition endQueuePosition;

    private long creationTimestamp;

    private Set<LimitingResourceQueueDependency> dependenciesAsOrigin = new HashSet<LimitingResourceQueueDependency>();

    private Set<LimitingResourceQueueDependency> dependenciesAsDestiny = new HashSet<LimitingResourceQueueDependency>();

    public static boolean isAfter(LimitingResourceQueueElement element, DateAndHour time) {
        return element.getStartTime().isAfter(time);
    }

    public static boolean isInTheMiddle(LimitingResourceQueueElement element, DateAndHour time) {
        return (element.getStartTime().isBefore(time) || element.getStartTime().isEquals(time))
                    && (element.getEndTime().isAfter(time) || element.getEndTime().isEquals(time));
    }

    public static LimitingResourceQueueElement create() {
        return create(new LimitingResourceQueueElement());
    }

    protected LimitingResourceQueueElement() {
        creationTimestamp = (new Date()).getTime();
        startQueuePosition = new QueuePosition();
        startQueuePosition.setHour(0);
        endQueuePosition = new QueuePosition();
        endQueuePosition.setHour(0);
    }

    @Valid
    public ResourceAllocation<?> getResourceAllocation() {
        return resourceAllocation;
    }

    public void setResourceAllocation(ResourceAllocation<?> resourceAllocation) {
        this.resourceAllocation = resourceAllocation;
    }

    public void setEarlierStartDateBecauseOfGantt(Date date) {
        earlierStartDateBecauseOfGantt = date;
    }

    public LimitingResourceQueue getLimitingResourceQueue() {
        return limitingResourceQueue;
    }

    public void setLimitingResourceQueue(
            LimitingResourceQueue limitingResourceQueue) {
        this.limitingResourceQueue = limitingResourceQueue;
    }

    public LocalDate getStartDate() {
        return startQueuePosition.getDate();
    }

    public void setStartDate(LocalDate date) {
        startQueuePosition.setDate(date);
        notifyQueueElementIsMoved();
    }

    private void notifyQueueElementIsMoved() {
        if (getLimitingResourceQueue() != null) {
            getLimitingResourceQueue().queueElementMoved(this);
        }
    }

    public int getStartHour() {
        return startQueuePosition.getHour();
    }

    public void setStartHour(int hour) {
        startQueuePosition.setHour(hour);
        notifyQueueElementIsMoved();
    }

    public LocalDate getEndDate() {
        return endQueuePosition.getDate();
    }

    public void setEndDate(LocalDate date) {
        notifyQueueElementIsMoved();
        endQueuePosition.setDate(date);
    }

    public int getEndHour() {
        return endQueuePosition.getHour();
    }

    public void setEndHour(int hour) {
        endQueuePosition.setHour(hour);
        notifyQueueElementIsMoved();
    }

    public Duration getLengthBetween() {
        DateTime start = getStartDate().toDateTimeAtStartOfDay().plusHours(
                getStartHour());
        DateTime end = getEndDate().toDateTimeAtStartOfDay().plusHours(
                getEndHour());
        return new Duration(start, end);
    }

    public Date getEarliestStartDateBecauseOfGantt() {
        return earlierStartDateBecauseOfGantt;
    }

    public Date getEarliestEndDateBecauseOfGantt() {
        if (earliestEndDateBecauseOfGantt == null) {
            // can be null because it's a new column
            return earlierStartDateBecauseOfGantt;
        }
        return earliestEndDateBecauseOfGantt;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public Resource getResource() {
        if (resourceAllocation instanceof SpecificResourceAllocation) {
            final SpecificResourceAllocation specific = (SpecificResourceAllocation) resourceAllocation;
            return specific.getResource();
        }
        return null;
    }

    public Integer getIntentedTotalHours() {
        return (getResourceAllocation() != null) ? getResourceAllocation()
                .getIntendedTotalHours() : null;
    }

    public DateAndHour getStartTime() {
        return new DateAndHour(getStartDate(), getStartHour());
    }

    public DateAndHour getEndTime() {
        return new DateAndHour(getEndDate(), getEndHour());
    }

    public void add(LimitingResourceQueueDependency d) {
        Validate.notNull(d);
        if (sameInDB(d.getHasAsOrigin())) {
            dependenciesAsOrigin.add(d);
        } else if (sameInDB(d.getHasAsDestiny())) {
            dependenciesAsDestiny.add(d);
        } else {
            throw new IllegalArgumentException(
                    "It cannot be added a dependency"
                            + " in which the current queue element is neither origin"
                            + " not desinty");
        }
    }

    private boolean sameInDB(LimitingResourceQueueElement other) {
        return this == other || other.getId() != null && this.getId() != null
                && other.getId().equals(this.getId());
    }

    public void remove(LimitingResourceQueueDependency d) {
        if (dependenciesAsOrigin.contains(d)) {
            dependenciesAsOrigin.remove(d);
        }
        if (dependenciesAsDestiny.contains(d)) {
            dependenciesAsDestiny.remove(d);
        }
    }

    public Set<LimitingResourceQueueDependency> getDependenciesAsOrigin() {
        return Collections.unmodifiableSet(dependenciesAsOrigin);
    }

    public Set<LimitingResourceQueueDependency> getDependenciesAsDestiny() {
        return Collections.unmodifiableSet(dependenciesAsDestiny);
    }

    public void updateDates(IntraDayDate earliestStart, IntraDayDate earliestEnd) {
        this.earlierStartDateBecauseOfGantt = toDate(earliestStart);
        this.earliestEndDateBecauseOfGantt = toDate(earliestEnd);
    }

    private Date toDate(IntraDayDate intraDayDate) {
        return intraDayDate.toDateTimeAtStartOfDay().toDate();
    }

    public void detach() {
        setLimitingResourceQueue(null);
        setStartDate(null);
        setStartHour(0);
        setEndDate(null);
        setEndHour(0);
        getResourceAllocation().removeLimitingDayAssignments();
    }

    public boolean isDetached() {
        return getStartDate() == null;
    }

    public boolean isSpecific() {
        return resourceAllocation instanceof SpecificResourceAllocation;
    }

    public boolean isGeneric() {
        return resourceAllocation instanceof GenericResourceAllocation;
    }

    public Set<Criterion> getCriteria() {
        if (!isGeneric()) {
            throw new IllegalStateException("this is not a generic element");
        }
        final ResourceAllocation<?> resourceAllocation = getResourceAllocation();
        return ((GenericResourceAllocation) resourceAllocation).getCriterions();
    }

    public boolean hasDayAssignments() {
        return !getResourceAllocation().getAssignments().isEmpty();
    }

    public List<? extends DayAssignment> getDayAssignments() {
        return resourceAllocation.getAssignments();
    }

    public BigDecimal getAdvancePercentage() {
        return resourceAllocation.getTask().getAdvancePercentage();
    }

    public Task getTask() {
        return resourceAllocation.getTask();
    }

    public String toString() {
        return getTask().getName() + "; " + getLimitingResourceQueue();
    }

}

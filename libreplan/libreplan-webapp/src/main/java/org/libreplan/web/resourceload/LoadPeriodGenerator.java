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

package org.libreplan.web.resourceload;

import static org.libreplan.business.workingday.IntraDayDate.max;
import static org.libreplan.business.workingday.IntraDayDate.min;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.Fraction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.libreplan.business.calendars.entities.ICalendar;
import org.libreplan.business.planner.entities.ResourceAllocation;
import org.libreplan.business.resources.daos.IResourcesSearcher;
import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.resources.entities.CriterionCompounder;
import org.libreplan.business.resources.entities.ICriterion;
import org.libreplan.business.resources.entities.Resource;
import org.libreplan.business.workingday.EffortDuration;
import org.libreplan.business.workingday.IntraDayDate;
import org.libreplan.business.workingday.IntraDayDate.PartialDay;
import org.libreplan.web.planner.TaskElementAdapter;
import org.zkoss.ganttz.data.GanttDate;
import org.zkoss.ganttz.data.resourceload.LoadLevel;
import org.zkoss.ganttz.data.resourceload.LoadPeriod;


abstract class LoadPeriodGenerator {

    private static final Log LOG = LogFactory.getLog(LoadPeriodGenerator.class);

    protected final IntraDayDate start;

    protected final IntraDayDate end;

    private List<ResourceAllocation<?>> allocationsOnInterval = new ArrayList<>();

    protected LoadPeriodGenerator(
            IntraDayDate start, IntraDayDate end, List<ResourceAllocation<?>> allocationsOnInterval) {

        Validate.notNull(start);
        Validate.notNull(end);
        Validate.notNull(allocationsOnInterval);
        this.start = start;
        this.end = end;
        this.allocationsOnInterval = ResourceAllocation.getSatisfied(allocationsOnInterval);
    }

    public static LoadPeriodGeneratorFactory onResource(Resource resource) {
        return new OnResourceFactory(resource);
    }

    public static LoadPeriodGeneratorFactory onResourceSatisfying(Resource resource, Collection<Criterion> criterions) {
        return new OnResourceFactory(resource, criterions);
    }

    private static class OnResourceFactory implements LoadPeriodGeneratorFactory {

        private final Resource resource;

        private final ICriterion criterion;

        public OnResourceFactory(Resource resource) {
            this(resource, Collections.emptyList());
        }

        public OnResourceFactory(Resource resource, Collection<Criterion> criterionsToSatisfy) {
            Validate.notNull(resource);
            this.resource = resource;
            this.criterion = CriterionCompounder.buildAnd(criterionsToSatisfy).getResult();
        }

        @Override
        public LoadPeriodGenerator create(ResourceAllocation<?> allocation) {
            return new LoadPeriodGeneratorOnResource(resource, allocation, criterion);
        }

    }

    public static LoadPeriodGeneratorFactory onCriterion(
            final Criterion criterion, final IResourcesSearcher resourceSearch) {

        final List<Resource> potentialResources =
                resourceSearch.searchBoth().byCriteria(Collections.singletonList(criterion)).execute();

        return allocation -> new LoadPeriodGeneratorOnCriterion(criterion, allocation, potentialResources);
    }

    public List<LoadPeriodGenerator> join(LoadPeriodGenerator next) {
        if (!overlaps(next)) {
            return stripEmpty(this, next);
        }

        if (isIncluded(next)) {
            return stripEmpty(this.until(next.start), intersect(next), this.from(next.end));
        }
        assert overlaps(next) && !isIncluded(next);
        return stripEmpty(this.until(next.start), intersect(next), next.from(end));
    }

    protected List<ResourceAllocation<?>> getAllocationsOnInterval() {
        return allocationsOnInterval;
    }

    private List<LoadPeriodGenerator> stripEmpty(LoadPeriodGenerator... generators) {
        List<LoadPeriodGenerator> result = new ArrayList<>();
        for (LoadPeriodGenerator loadPeriodGenerator : generators) {
            if (!loadPeriodGenerator.isEmpty()) {
                result.add(loadPeriodGenerator);
            }
        }
        return result;
    }

    private boolean isEmpty() {
        return start.equals(end);
    }

    protected abstract LoadPeriodGenerator create(
            IntraDayDate start, IntraDayDate end, List<ResourceAllocation<?>> allocationsOnInterval);

    private LoadPeriodGenerator intersect(LoadPeriodGenerator other) {
        return create(max(this.start, other.start), min(this.end, other.end), plusAllocations(other));
    }

    private List<ResourceAllocation<?>> plusAllocations(LoadPeriodGenerator other) {
        List<ResourceAllocation<?>> result = new ArrayList<>();
        result.addAll(allocationsOnInterval);
        result.addAll(other.allocationsOnInterval);

        return result;
    }

    private LoadPeriodGenerator from(IntraDayDate newStart) {
        return create(newStart, end, allocationsOnInterval);
    }

    private LoadPeriodGenerator until(IntraDayDate newEnd) {
        return create(start, newEnd, allocationsOnInterval);
    }

    boolean overlaps(LoadPeriodGenerator other) {
        return start.compareTo(other.end) < 0 && other.start.compareTo(this.end) < 0;
    }

    private boolean isIncluded(LoadPeriodGenerator other) {
        return other.start.compareTo(start) >= 0 && other.end.compareTo(end) <= 0;
    }

    /**
     * @return <code>null</code> if the data is invalid
     */
    public LoadPeriod build() {
        if (start.compareTo(end) > 0) {

            LOG.warn("the start date is after end date. Inconsistent state for " +
                    allocationsOnInterval + ". LoadPeriod ignored");

            return null;
        }
        EffortDuration totalEffort = getTotalAvailableEffort();
        EffortDuration effortAssigned = getEffortAssigned();

        return new LoadPeriod(
                asGantt(start),
                asGantt(end),
                totalEffort.toFormattedString(),
                effortAssigned.toFormattedString(),
                new LoadLevel(calculateLoadPercentage(totalEffort, effortAssigned)));
    }

    private GanttDate asGantt(IntraDayDate date) {
        return TaskElementAdapter.toGantt(date, inferDayCapacity(allocationsOnInterval, PartialDay.wholeDay(date.getDate())));
    }

    private EffortDuration inferDayCapacity(List<ResourceAllocation<?>> allocationsOnInterval, PartialDay day) {
        if (allocationsOnInterval.isEmpty()) {
            return null;
        }

        EffortDuration result = EffortDuration.zero();
        for (ResourceAllocation<?> each : allocationsOnInterval) {
            ICalendar allocationCalendar = each.getAllocationCalendar();
            result = result.plus(allocationCalendar.getCapacityOn(day));
        }
        return result.divideBy(allocationsOnInterval.size());
    }

    protected abstract EffortDuration getTotalAvailableEffort();

    private int calculateLoadPercentage(EffortDuration totalEffort, EffortDuration effortAssigned) {
        if (totalEffort.isZero()) {
            return effortAssigned.isZero() ? 0 : Integer.MAX_VALUE;
        }

        if (effortAssigned.isZero()) {
            LOG.warn("total effort is " + totalEffort + " but effortAssigned is zero");
            getEffortAssigned();

            return 0;
        }
        Fraction fraction = effortAssigned.divivedBy(totalEffort);
        Fraction percentage = fraction.multiplyBy(Fraction.getFraction(100, 1));

        return percentage.intValue();
    }

    protected abstract EffortDuration getEffortAssigned();

    protected final EffortDuration sumAllocations() {
        return EffortDuration.sum(allocationsOnInterval, this::getAssignedEffortFor);
    }

    protected abstract EffortDuration getAssignedEffortFor(ResourceAllocation<?> resourceAllocation);

    public IntraDayDate getStart() {
        return start;
    }

    public IntraDayDate getEnd() {
        return end;
    }
}

class LoadPeriodGeneratorOnResource extends LoadPeriodGenerator {

    private Resource resource;

    private final ICriterion criterion;

    LoadPeriodGeneratorOnResource(Resource resource,
                                  IntraDayDate start,
                                  IntraDayDate end,
                                  List<ResourceAllocation<?>> allocationsOnInterval,
                                  ICriterion criterion) {

        super(start, end, allocationsOnInterval);
        this.resource = resource;
        this.criterion = criterion;
    }

    LoadPeriodGeneratorOnResource(Resource resource, ResourceAllocation<?> initial, ICriterion criterion) {
        super(initial.getIntraDayStartDate(), initial.getIntraDayEndDate(), Collections.singletonList(initial));
        this.resource = resource;
        this.criterion = criterion;
    }

    @Override
    protected LoadPeriodGenerator create(
            IntraDayDate start, IntraDayDate end, List<ResourceAllocation<?>> allocationsOnInterval) {

        return new LoadPeriodGeneratorOnResource(resource, start, end, allocationsOnInterval, criterion);
    }

    @Override
    protected EffortDuration getTotalAvailableEffort() {
        return resource.getTotalEffortFor(start, end, criterion);
    }

    @Override
    protected EffortDuration getAssignedEffortFor(ResourceAllocation<?> resourceAllocation) {
        return resourceAllocation.getAssignedEffort(resource, start, end);
    }

    @Override
    protected EffortDuration getEffortAssigned() {
        return sumAllocations();
    }

}

class LoadPeriodGeneratorOnCriterion extends LoadPeriodGenerator {

    private final Criterion criterion;

    private final List<Resource> resourcesSatisfyingCriterionAtSomePoint;

    public LoadPeriodGeneratorOnCriterion(Criterion criterion,
                                          ResourceAllocation<?> allocation,
                                          List<Resource> resourcesSatisfyingCriterionAtSomePoint) {

        this(
                criterion,
                allocation.getIntraDayStartDate(),
                allocation.getIntraDayEndDate(),
                Collections.singletonList(allocation),
                resourcesSatisfyingCriterionAtSomePoint);
    }

    public LoadPeriodGeneratorOnCriterion(Criterion criterion,
                                          IntraDayDate startDate, IntraDayDate endDate,
                                          List<ResourceAllocation<?>> allocations,
                                          List<Resource> resourcesSatisfyingCriterionAtSomePoint) {

        super(startDate, endDate, allocations);
        this.criterion = criterion;
        this.resourcesSatisfyingCriterionAtSomePoint = resourcesSatisfyingCriterionAtSomePoint;
    }

    @Override
    protected LoadPeriodGenerator create(
            IntraDayDate start, IntraDayDate end, List<ResourceAllocation<?>> allocationsOnInterval) {

        return new LoadPeriodGeneratorOnCriterion(
                criterion, start, end, allocationsOnInterval, resourcesSatisfyingCriterionAtSomePoint);
    }

    @Override
    protected EffortDuration getAssignedEffortFor(ResourceAllocation<?> resourceAllocation) {
        return resourceAllocation.getAssignedEffort(criterion, start, end);
    }

    @Override
    protected EffortDuration getTotalAvailableEffort() {
        return EffortDuration.sum(
                resourcesSatisfyingCriterionAtSomePoint, resource -> resource.getTotalEffortFor(start, end, criterion));
    }

    @Override
    protected EffortDuration getEffortAssigned() {
        return sumAllocations();
    }

}

interface LoadPeriodGeneratorFactory {
    LoadPeriodGenerator create(ResourceAllocation<?> allocation);
}

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

package org.libreplan.business.planner.entities.allocationalgorithms;

import static org.libreplan.business.i18n.I18nHelper._;
import static org.libreplan.business.workingday.EffortDuration.min;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.joda.time.LocalDate;
import org.libreplan.business.calendars.entities.AvailabilityTimeLine;
import org.libreplan.business.calendars.entities.CombinedWorkHours;
import org.libreplan.business.calendars.entities.ICalendar;
import org.libreplan.business.calendars.entities.ResourceCalendar;
import org.libreplan.business.calendars.entities.SameWorkHoursEveryDay;
import org.libreplan.business.planner.entities.AvailabilityCalculator;
import org.libreplan.business.planner.entities.DayAssignment;
import org.libreplan.business.planner.entities.GenericResourceAllocation;
import org.libreplan.business.planner.entities.ResourceAllocation;
import org.libreplan.business.planner.entities.ResourceAllocation.IEffortDistributor;
import org.libreplan.business.planner.entities.SpecificResourceAllocation;
import org.libreplan.business.planner.entities.allocationalgorithms.UntilFillingHoursAllocator.IAssignmentsCreator;
import org.libreplan.business.resources.daos.IResourcesSearcher;
import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.resources.entities.Resource;
import org.libreplan.business.workingday.EffortDuration;
import org.libreplan.business.workingday.IntraDayDate;
import org.libreplan.business.workingday.IntraDayDate.PartialDay;
import org.libreplan.business.workingday.ResourcesPerDay;

public abstract class ResourcesPerDayModification extends
        AllocationModification {

    private static class OnGenericAllocation extends
            ResourcesPerDayModification {
        private final GenericResourceAllocation genericAllocation;

        OnGenericAllocation(
                GenericResourceAllocation resourceAllocation,
                ResourcesPerDay resourcesPerDay,
                Collection<? extends Resource> resources) {
            super(resourceAllocation, resourcesPerDay, resources);
            this.genericAllocation = resourceAllocation;
        }

        @Override
        public void applyAllocationOnAllTaskLength() {
            genericAllocation.forResources(getResources()).allocate(getGoal());
        }

        @Override
        public void applyAllocationUntil(IntraDayDate endExclusive) {
            genericAllocation.forResources(getResources())
                    .resourcesPerDayUntil(endExclusive).allocate(getGoal());
        }

        @Override
        public void applyAllocationFromEndUntil(IntraDayDate start) {
            genericAllocation.forResources(getResources())
                    .resourcesPerDayFromEndUntil(start).allocate(getGoal());
        }

        @Override
        public AvailabilityTimeLine getAvailability() {
            return AvailabilityCalculator.buildSumOfAvailabilitiesFor(
                    (Collection<? extends Criterion>) genericAllocation
                            .getCriterions(), getResources());
        }

        @Override
        public String getNoValidPeriodsMessage() {
            String firstLine = _("There are no days available due to not satisfying the criteria.");
            String secondLine = _("Another possibility is that the resources do not have days available due to their calendars.");
            return firstLine + "\n" + secondLine;
        }

        @Override
        public String getNoValidPeriodsMessageDueToIntersectionMessage() {
            String firstLine = _("There are no days available in the days marked available by the task calendar.");
            String secondLine = _("Maybe the criteria are not satisfied in those days.");
            return firstLine + "\n" + secondLine;
        }

        @Override
        public ICalendar getResourcesCalendar() {
            return CombinedWorkHours.maxOf(resourcesCalendar());
        }

        private List<ICalendar> resourcesCalendar() {
            List<ICalendar> calendar = new ArrayList<ICalendar>();
            for (Resource each : getResources()) {
                calendar.add(calendarFor(each));
            }
            return calendar;
        }

        @Override
        protected ResourceAllocation<?> getResourceAllocation() {
            return genericAllocation;
        }

        @Override
        protected IEffortDistributor<?> toEffortDistributor() {
            return genericAllocation.createEffortDistributor(getResources());
        }

        @Override
        public boolean isSpecific() {
            return false;
        }

    }

    private static class OnSpecificAllocation extends
            ResourcesPerDayModification {

        private final SpecificResourceAllocation resourceAllocation;

        OnSpecificAllocation(
                SpecificResourceAllocation resourceAllocation,
                ResourcesPerDay resourcesPerDay,
                Collection<? extends Resource> resources) {
            super(resourceAllocation, resourcesPerDay, resources);
            this.resourceAllocation = resourceAllocation;
        }

        @Override
        public void applyAllocationOnAllTaskLength() {
            resourceAllocation.allocate(getGoal());
        }

        @Override
        public void applyAllocationUntil(IntraDayDate endExclusive) {
            resourceAllocation.resourcesPerDayUntil(endExclusive).allocate(
                    getGoal());
        }

        @Override
        public void applyAllocationFromEndUntil(IntraDayDate start) {
            resourceAllocation.resourcesPerDayFromEndUntil(start).allocate(
                    getGoal());
        }

        @Override
        public AvailabilityTimeLine getAvailability() {
            Resource resource = getAssociatedResource();
            return AvailabilityCalculator.getCalendarAvailabilityFor(resource);
        }

        @Override
        public String getNoValidPeriodsMessage() {
            return _("Resource is not available from task's start");
        }

        @Override
        public String getNoValidPeriodsMessageDueToIntersectionMessage() {
            return _("Resource is not available according to task's calendar");
        }

        private Resource getAssociatedResource() {
            return getResources().get(0);
        }

        @Override
        public ICalendar getResourcesCalendar() {
            return calendarFor(getAssociatedResource());
        }

        @Override
        protected ResourceAllocation<?> getResourceAllocation() {
            return resourceAllocation;
        }

        @Override
        protected IEffortDistributor<?> toEffortDistributor() {
            return resourceAllocation.createEffortDistributor();
        }

        @Override
        public boolean isSpecific() {
            return true;
        }

    }

    public static ResourcesPerDayModification create(
            GenericResourceAllocation resourceAllocation,
            ResourcesPerDay resourcesPerDay, List<? extends Resource> resources) {
        Validate.isTrue(!resourcesPerDay.isZero());
        return new OnGenericAllocation(resourceAllocation, resourcesPerDay,
                resources);
    }

    public static List<ResourcesPerDayModification> withNewResources(
            List<ResourceAllocation<?>> allocations,
            IResourcesSearcher resourcesSearcher) {
        List<ResourcesPerDayModification> result = fromExistent(allocations,
                resourcesSearcher);
        for (ResourcesPerDayModification each : result) {
            each.withNewResources(resourcesSearcher);
        }
        return ensureNoOneWithoutAssociatedResources(result, resourcesSearcher);
    }

    public static ResourcesPerDayModification create(
            SpecificResourceAllocation resourceAllocation,
            ResourcesPerDay resourcesPerDay) {
        Validate.isTrue(!resourcesPerDay.isZero());
        return new OnSpecificAllocation(resourceAllocation,
                resourcesPerDay, Collections.singletonList(resourceAllocation
                        .getResource()));
    }

    public static List<ResourcesPerDayModification> fromExistent(
            Collection<? extends ResourceAllocation<?>> allocations,
            IResourcesSearcher resourcesSearcher) {
        List<ResourcesPerDayModification> result = new ArrayList<ResourcesPerDayModification>();
        for (ResourceAllocation<?> resourceAllocation : allocations) {
            ResourcesPerDayModification modification = resourceAllocation
                    .asResourcesPerDayModification();
            if (modification != null) {
                result.add(modification);
            }
        }
        return ensureNoOneWithoutAssociatedResources(result, resourcesSearcher);
    }

    protected static ICalendar calendarFor(Resource associatedResource) {
        ResourceCalendar calendar = associatedResource.getCalendar();
        return calendar != null ? calendar : SameWorkHoursEveryDay
                .getDefaultWorkingDay();
    }

    private final ResourcesPerDay goal;

    private ResourcesPerDayModification(
            ResourceAllocation<?> resourceAllocation,
            ResourcesPerDay resourcesPerDay,
            Collection<? extends Resource> resources) {
        super(resourceAllocation, resources);
        this.goal = resourcesPerDay;
    }

    public ResourcesPerDay getGoal() {
        return goal;
    }

    public abstract ICalendar getResourcesCalendar();

    public abstract void applyAllocationOnAllTaskLength();

    public abstract void applyAllocationUntil(IntraDayDate endExclusive);

    public abstract void applyAllocationFromEndUntil(IntraDayDate start);

    public IAssignmentsCreator createAssignmentsCreator() {

        return new IAssignmentsCreator() {

            final IEffortDistributor<?> effortDistributor = toEffortDistributor();

            @Override
            public List<? extends DayAssignment> createAssignmentsAtDay(
                    PartialDay day, EffortDuration limit,
                    ResourcesPerDay resourcesPerDay) {
                EffortDuration toDistribute = getResourceAllocation()
                        .calculateTotalToDistribute(day, getGoal());
                EffortDuration effortLimited = min(limit, toDistribute);
                PartialDay distributeOn = day;
                if (effortLimited.equals(limit)) {
                    IntraDayDate start = day.getStart();
                    distributeOn = new PartialDay(start, start.increaseBy(
                            resourcesPerDay, effortLimited));
                }
                return effortDistributor.distributeForDay(distributeOn,
                        effortLimited);
            }
        };
    }

    protected abstract ResourceAllocation<?> getResourceAllocation();

    protected abstract IEffortDistributor<?> toEffortDistributor();

    public abstract AvailabilityTimeLine getAvailability();

    public abstract String getNoValidPeriodsMessage();

    public abstract String getNoValidPeriodsMessageDueToIntersectionMessage();

    public boolean thereAreMoreSpaceAvailableAt(IntraDayDate date) {
        LocalDate day = date.getDate();
        EffortDuration dayDuration = getBeingModified().getAllocationCalendar()
                .asDurationOn(PartialDay.wholeDay(day), goal);
        return dayDuration.compareTo(date.getEffortDuration()) > 0;
    }

    public EffortDuration durationAtDay(PartialDay day) {
        return getBeingModified().getAllocationCalendar().asDurationOn(day,
                goal);
    }

    @Override
    public boolean satisfiesModificationRequested() {
        return getGoal().equals(
                getBeingModified().getNonConsolidatedResourcePerDay());
    }

}

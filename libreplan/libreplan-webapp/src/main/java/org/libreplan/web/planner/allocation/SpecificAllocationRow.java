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

package org.libreplan.web.planner.allocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.libreplan.business.planner.entities.CalculatedValue;
import org.libreplan.business.planner.entities.ResourceAllocation;
import org.libreplan.business.planner.entities.SpecificResourceAllocation;
import org.libreplan.business.planner.entities.Task;
import org.libreplan.business.planner.entities.allocationalgorithms.EffortModification;
import org.libreplan.business.planner.entities.allocationalgorithms.ResourcesPerDayModification;
import org.libreplan.business.resources.entities.Resource;
import org.libreplan.business.resources.entities.ResourceEnum;

/**
 * The information required for creating a {@link SpecificResourceAllocation}
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 */
public class SpecificAllocationRow extends AllocationRow {

    public static List<SpecificAllocationRow> toSpecificAllocations(
            Collection<? extends ResourceAllocation<?>> resourceAllocations) {
        List<SpecificAllocationRow> result = new ArrayList<SpecificAllocationRow>();
        for (ResourceAllocation<?> resourceAllocation : resourceAllocations) {
            if (resourceAllocation instanceof SpecificResourceAllocation) {
                SpecificResourceAllocation specific = (SpecificResourceAllocation) resourceAllocation;
                result.add(from(specific));
            }
        }
        return result;
    }

    public static List<SpecificAllocationRow> withResource(
            List<SpecificAllocationRow> specific, Resource resource) {
        List<SpecificAllocationRow> result = new ArrayList<SpecificAllocationRow>();
        for (SpecificAllocationRow specificAllocationRow : specific) {
            if (areEquals(specificAllocationRow.getResource(), resource)) {
                result.add(specificAllocationRow);
            }
        }
        return result;
    }

    private static boolean areEquals(Resource one, Resource other) {
        if (one == other) {
            return true;
        }
        if (one == null || other == null) {
            return false;
        }
        return one.equals(other);
    }

    public static List<SpecificAllocationRow> getSpecific(
            Collection<? extends AllocationRow> currentAllocations) {
        List<SpecificAllocationRow> result = new ArrayList<SpecificAllocationRow>();
        for (AllocationRow each : currentAllocations) {
            if (each instanceof SpecificAllocationRow) {
                result.add((SpecificAllocationRow) each);
            }
        }
        return result;
    }

    public static SpecificAllocationRow from(SpecificResourceAllocation specific) {
        SpecificAllocationRow result = new SpecificAllocationRow(specific);
        setupResource(result, specific.getResource());
        return result;
    }

    public static SpecificAllocationRow forResource(
            CalculatedValue calculatedValue, Resource resource) {
        SpecificAllocationRow result = new SpecificAllocationRow(
                calculatedValue);
        setupResource(result, resource);
        return result;
    }

    private static void setupResource(SpecificAllocationRow specificRow,
            Resource resource) {
        specificRow.setName(resource.getShortDescription());
        specificRow.setResource(resource);
    }

    private Resource resource;

    private SpecificAllocationRow(CalculatedValue calculatedValue) {
        super(calculatedValue);
    }

    private SpecificAllocationRow(SpecificResourceAllocation origin) {
        super(origin);
    }

    @Override
    public ResourcesPerDayModification toResourcesPerDayModification(Task task) {
        return ResourcesPerDayModification.create(createSpecific(task),
                getResourcesPerDayEditedValue());
    }

    private SpecificResourceAllocation createSpecific(Task task) {
        SpecificResourceAllocation specific = SpecificResourceAllocation
                .create(task);
        specific.setResource(resource);
        SpecificResourceAllocation origin = (SpecificResourceAllocation) getOrigin();
        specific.overrideConsolidatedDayAssignments(origin);
        if (origin != null) {
            specific.setAssignmentFunctionWithoutApply(origin.getAssignmentFunction());
        }
        return specific;
    }

    @Override
    public EffortModification toHoursModification(Task task) {
        return EffortModification.create(createSpecific(task),
                getEffortFromInput());
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    @Override
    public boolean isGeneric() {
        return false;
    }

    @Override
    public List<Resource> getAssociatedResources() {
        return Collections.singletonList(resource);
    }

    @Override
    public ResourceEnum getType() {
        return resource.getType();
    }

}

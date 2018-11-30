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

package org.libreplan.business.planner.daos;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;
import org.libreplan.business.common.daos.IGenericDAO;
import org.libreplan.business.planner.entities.GenericResourceAllocation;
import org.libreplan.business.planner.entities.ResourceAllocation;
import org.libreplan.business.planner.entities.SpecificResourceAllocation;
import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.resources.entities.Resource;
import org.libreplan.business.scenarios.entities.Scenario;

/**
 * DAO interface for {@link ResourceAllocation}
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public interface IResourceAllocationDAO extends
        IGenericDAO<ResourceAllocation, Long> {

    List<ResourceAllocation<?>> findAllocationsRelatedToAnyOf(
            Scenario onScenario,
            List<Resource> resources);

    List<ResourceAllocation<?>> findAllocationsRelatedToAnyOf(
            Scenario onScenario,
            List<Resource> resources, LocalDate intervalFilterStartDate,
            LocalDate intervalFilterEndDate);

    List<SpecificResourceAllocation> findSpecificAllocationsRelatedTo(
            Scenario onScenario, List<Resource> resources,
            LocalDate intervalFilterStartDate, LocalDate intervalFilterEndDate);

    List<ResourceAllocation<?>> findAllocationsRelatedTo(Scenario onScenario,
            Resource resource,
            LocalDate intervalFilterStartDate, LocalDate intervalFilterEndDate);

    Map<Criterion, List<GenericResourceAllocation>> findGenericAllocationsByCriterion(
            Scenario onScenario,
            Date intervalFilterStartDate, Date intervalFilterEndDate);

    List<GenericResourceAllocation> findGenericAllocationsRelatedToCriterion(
            Scenario onScenario, Criterion criterion,
            Date intervalFilterStartDate, Date intervalFilterEndDate);

    /**
     * <p>
     * It searches for the {@link SpecificResourceAllocation specific
     * allocations} that have an assigned resource such that interferes with the
     * provided criterion. This means that the assigned resource for the
     * specific allocation satisfies the provided criterion in part or all the
     * specific allocation.
     * </p>
     * <p>
     * It only returns the allocations for which their tasks overlap
     * intervalFilterStartDate and intervalFilterEndDate. If any of these
     * interval parameters is null it's considered that the interval is open
     * ended. So if you provide both interval filter values as null, all
     * allocations satisfying the first requirement are returned.
     * </p>
     *
     * @param onScenario
     *            the scenario the allocations returned will belong to
     * @param criterion
     *            must be not <code>null</code>
     * @param intervalFilterStartDate
     *            It can be <code>null</code>
     * @param intervalFilterEndDate
     *            It can be <code>null</code>
     * @return the list of {@link SpecificResourceAllocation specific
     *         allocations} found
     */
    List<SpecificResourceAllocation> findSpecificAllocationsRelatedTo(
            Scenario onScenario,
            Criterion criterion,
            Date intervalFilterStartDate,
            Date intervalFilterEndDate);

}
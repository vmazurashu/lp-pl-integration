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

import java.util.Collection;
import java.util.List;

import org.joda.time.LocalDate;
import org.libreplan.business.common.daos.IGenericDAO;
import org.libreplan.business.planner.entities.DayAssignment;
import org.libreplan.business.planner.entities.DerivedDayAssignment;
import org.libreplan.business.resources.entities.Resource;
import org.libreplan.business.scenarios.entities.Scenario;

/**
 * DAO interface for {@link DayAssignment}
 *
 * @author Diego Pino García <dpino@igalia.com>
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 * @author Javier Moran Rua <jmoran@igalia.com>
 */
public interface IDayAssignmentDAO extends IGenericDAO<DayAssignment, Long> {

    public void removeDerived(
            Collection<? extends DerivedDayAssignment> derivedAllocations);

    public List<DayAssignment> getAllFor(Scenario scenario);

    public List<DayAssignment> getAllFor(Scenario scenario,
            LocalDate initInclusive, LocalDate endInclusive);

    public List<DayAssignment> getAllFor(Scenario scenario,
            LocalDate startDateInclusive, LocalDate endDateInclusive,
            Resource resource);

    List<DayAssignment> listFilteredByDate(LocalDate init, LocalDate end);

    public List<DayAssignment> findByResources(Scenario scenario, List<Resource> resources);

    public List<DayAssignment> findByResources(List<Resource> resources);

}

/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2012 Igalia, S.L.
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
package org.libreplan.web.planner.adaptplanning;

import org.libreplan.business.planner.entities.TaskElement;
import org.libreplan.web.planner.order.PlanningStateCreator.PlanningState;
import org.zkoss.ganttz.extensions.ICommand;

/**
 * Command to adapt planning of a project taking into account information from
 * the timesheets.
 *
 * @author Manuel Rego Casasnovas <rego@igalia.com>
 */
public interface IAdaptPlanningCommand extends ICommand<TaskElement> {

    public void setState(PlanningState planningState);

}

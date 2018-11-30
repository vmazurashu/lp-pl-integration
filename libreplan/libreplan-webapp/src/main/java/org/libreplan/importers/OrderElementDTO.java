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

package org.libreplan.importers;

import java.util.Date;
import java.util.List;

import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.planner.entities.TaskElement;

/**
 * Class that represents no persistent imported tasks.
 * <br />
 * At these moment it only represent the tasks that can have any subtasks.
 *
 * @author Alba Carro Pérez <alba.carro@gmail.com>
 * @todo It last hours, resources, relationships, etc.
 */
public class OrderElementDTO implements IHasTaskAssociated {

    /**
     * Name of the task
     */
    public String name;

    /**
     * Start date of the task
     */
    public Date startDate;

    /**
     * end date of the task
     */
    public Date endDate;

    /**
     * end date of the task
     */
    public Date deadline;

    /**
     * Order created with this data
     */
    public OrderElement orderElement;

    /**
     * List of task that are children of this task
     */
    public List<OrderElementDTO> children;

    /**
     * Total hours of the task.
     */
    public int totalHours;

    /**
     * Milestones of this task.
     */
    public List<MilestoneDTO> milestones;

    /**
     * Constraint of this task.
     */
    public ConstraintDTO constraint;

    /**
     * Constraint date of this task.
     */
    public Date constraintDate;

    /**
     * TaskElement associated with this data.
     */
    public TaskElement taskElement;

    /**
     * Name of the calendar that this OrderElementDTO is linked to.
     */
    public String calendarName = null;

    @Override
    public TaskElement getTaskAssociated() {
        return taskElement;
    }

}

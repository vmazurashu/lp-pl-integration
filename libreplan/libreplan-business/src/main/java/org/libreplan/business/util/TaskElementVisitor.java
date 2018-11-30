/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2011 Igalia, S.L.
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

package org.libreplan.business.util;

/**
 * This class represents an abstract visitor to traverse task graphs.
 *
 * @author Nacho Barrientos <nacho@igalia.com>
 */
import org.libreplan.business.planner.entities.Task;
import org.libreplan.business.planner.entities.TaskGroup;
import org.libreplan.business.planner.entities.TaskMilestone;

public abstract class TaskElementVisitor {

    public abstract void visit(Task task);

    public abstract void visit(TaskGroup taskGroup);

    /**
     * As most of the visitors doesn't need to process the {@link TaskMilestones} is provided a default implementation doing nothing.
     */
    public void visit(TaskMilestone taskMilestone) {
        // Do nothing
    }

}

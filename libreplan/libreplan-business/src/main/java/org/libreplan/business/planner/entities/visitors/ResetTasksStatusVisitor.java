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

package org.libreplan.business.planner.entities.visitors;

/**
 * Visits a task graph resetting task statuses.
 *
 * @author Nacho Barrientos <nacho@igalia.com>
 */
import org.libreplan.business.planner.entities.Task;
import org.libreplan.business.planner.entities.TaskElement;
import org.libreplan.business.planner.entities.TaskGroup;
import org.libreplan.business.util.TaskElementVisitor;

public class ResetTasksStatusVisitor extends TaskElementVisitor {

    public ResetTasksStatusVisitor() {
    }

    public void visit(Task task) {
        task.resetStatus();
    }

    public void visit(TaskGroup taskGroup) {
        taskGroup.resetStatus();
        for (TaskElement each: taskGroup.getChildren()) {
            each.acceptVisitor(this);
        }
    }

}

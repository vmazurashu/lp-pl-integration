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
package org.libreplan.web.planner.milestone;

import static org.libreplan.web.I18nHelper._;

import org.libreplan.business.planner.entities.TaskElement;
import org.libreplan.business.planner.entities.TaskMilestone;
import org.libreplan.web.planner.order.PlanningStateCreator.PlanningState;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.zkoss.ganttz.extensions.IContextWithPlannerTask;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 *
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteMilestoneCommand implements IDeleteMilestoneCommand {

    private PlanningState planningState;

    @Override
    public void doAction(IContextWithPlannerTask<TaskElement> context,
            TaskElement task) {
        if (isApplicableTo(task)) {
            planningState.removed(task);
            context.remove(task);
        }
    }

    @Override
    public String getIcon() {
        return "/common/img/ico_borrar.png";
    }

    @Override
    public String getName() {
        return (_("Delete Milestone"));
    }

    @Override
    public void setState(PlanningState planningState) {
        this.planningState = planningState;
    }

    @Override
    public boolean isApplicableTo(TaskElement task) {
        return task instanceof TaskMilestone;
    }

}

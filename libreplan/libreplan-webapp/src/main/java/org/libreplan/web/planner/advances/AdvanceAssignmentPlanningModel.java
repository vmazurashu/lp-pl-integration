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

package org.libreplan.web.planner.advances;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.libreplan.business.advance.entities.AdvanceAssignment;
import org.libreplan.business.advance.entities.AdvanceMeasurement;
import org.libreplan.business.advance.entities.DirectAdvanceAssignment;
import org.libreplan.business.advance.entities.IndirectAdvanceAssignment;
import org.libreplan.business.orders.daos.IOrderElementDAO;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.planner.daos.ITaskElementDAO;
import org.libreplan.business.planner.entities.Task;
import org.libreplan.business.planner.entities.TaskElement;
import org.libreplan.web.planner.order.PlanningStateCreator.PlanningState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zkoss.ganttz.extensions.IContextWithPlannerTask;

/**
 * Model for UI operations related to {@link Task}.
 *
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AdvanceAssignmentPlanningModel implements IAdvanceAssignmentPlanningModel {

    private static final Log LOG = LogFactory.getLog(AdvanceAssignmentPlanningModel.class);

    @Autowired
    private ITaskElementDAO taskElementDAO;

    @Autowired
    private IOrderElementDAO orderElementDAO;

    private TaskElement task;

    private OrderElement orderElement;

    @Override
    public void accept() {
        getOrderElement().updateAdvancePercentageTaskElement();
    }

    @Override
    @Transactional(readOnly = true)
    public void initAdvancesFor(TaskElement task,
                                IContextWithPlannerTask<TaskElement> context,
                                PlanningState planningState) {
        initTask(task);
        initOrderElement();
    }

    private void initTask(TaskElement task) {
        this.task = task;
        taskElementDAO.reattach(this.task);
    }

    private void initOrderElement() {
        if ((task != null) && (task.getOrderElement() != null)) {
            orderElement = task.getOrderElement();
            orderElementDAO.reattach(orderElement);
            loadAdvances();
        } else {
            orderElement = null;
        }
    }

    private void loadAdvances() {
        loadAdvances(orderElement);
        OrderElement parent = orderElement.getParent();
        while (parent != null) {
            loadAdvances(parent);
            for (OrderElement child : parent.getChildren()) {
                loadAdvances(child);
            }
            parent = parent.getParent();
        }
    }

    private void loadAdvances(OrderElement orderElement) {
        for (DirectAdvanceAssignment advance : orderElement.getDirectAdvanceAssignments()) {
            loadDataAdvance(advance);
            forceLoadAdvanceConsolidatedValues(advance);
            advance.getNonCalculatedConsolidation().size();
        }

        for (IndirectAdvanceAssignment advance : orderElement.getIndirectAdvanceAssignments()) {
            loadDataAdvance(advance);
            advance.getCalculatedConsolidation().size();
            DirectAdvanceAssignment fakedDirect = orderElement.calculateFakeDirectAdvanceAssignment(advance);

            if (fakedDirect != null) {
                forceLoadAdvanceConsolidatedValues(fakedDirect);
            } else {
                LOG.warn("Fake direct advance assignment shouldn't be NULL for type '"
                        + advance.getAdvanceType().getUnitName() + "'");
            }
        }
    }

    private void loadDataAdvance(AdvanceAssignment advance) {
        advance.getAdvanceType().getUnitName();
        advance.getOrderElement().getName();
    }

    private void forceLoadAdvanceConsolidatedValues(DirectAdvanceAssignment advance) {
        for (AdvanceMeasurement measure : advance.getAdvanceMeasurements()) {
            measure.getAdvanceAssignment();
            measure.getNonCalculatedConsolidatedValues().size();
        }
    }

    public OrderElement getOrderElement() {
        return orderElement;
    }

}

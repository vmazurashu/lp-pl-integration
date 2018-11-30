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

import java.util.Date;
import java.util.List;

import org.libreplan.business.common.Flagged;
import org.libreplan.business.common.ProportionalDistributor;
import org.libreplan.business.orders.entities.AggregatedHoursGroup;
import org.libreplan.business.planner.entities.Task;
import org.libreplan.business.planner.entities.TaskElement;
import org.libreplan.web.planner.allocation.AllocationRowsHandler.Warnings;
import org.libreplan.web.planner.order.PlanningStateCreator.PlanningState;
import org.zkoss.ganttz.extensions.IContextWithPlannerTask;

/**
 * Contract for {@link Task}.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 * @author Diego Pino García <dpino@igalia.com>
 * @author Javier Moran Rua <jmoran@igalia.com>
 */
public interface IResourceAllocationModel extends INewAllocationsAdder {

    public interface IResourceAllocationContext<T> {
        public T doInsideTransaction();
    }

    /**
     * Cancel operation
     */
    void cancel();

    /**
     * Save task
     *
     * @return
     */
    Flagged<AllocationResult, Warnings> accept();

    /**
     * Starts the use case
     * @param task
     * @param ganttTask
     * @param planningState
     */
    AllocationRowsHandler initAllocationsFor(Task task,
            IContextWithPlannerTask<TaskElement> context,
            PlanningState planningState);

    void accept(AllocationResult modifiedAllocationResult);

    List<AggregatedHoursGroup> getHoursAggregatedByCriterions();

    Integer getOrderHours();

    <T> T onAllocationContext(
            IResourceAllocationContext<T> resourceAllocationContext);

    /**
     * Adds the default allocations, also known as recommended allocation. If it
     * can't be done <code>null</code> is returned
     */
    ProportionalDistributor addDefaultAllocations();

    Date getTaskEnd();

    Date getTaskStart();

}

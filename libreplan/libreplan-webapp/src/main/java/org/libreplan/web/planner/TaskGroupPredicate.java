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
package org.libreplan.web.planner;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.libreplan.business.externalcompanies.entities.ExternalCompany;
import org.libreplan.business.labels.entities.Label;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.orders.entities.OrderStatusEnum;
import org.libreplan.business.planner.entities.GenericResourceAllocation;
import org.libreplan.business.planner.entities.ResourceAllocation;
import org.libreplan.business.planner.entities.SpecificResourceAllocation;
import org.libreplan.business.planner.entities.TaskElement;
import org.libreplan.business.planner.entities.TaskGroup;
import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.resources.entities.Resource;
import org.libreplan.web.common.components.finders.FilterPair;
import org.libreplan.web.common.components.finders.TaskGroupFilterEnum;
import org.zkoss.ganttz.IPredicate;

/**
 * Checks if {@link TaskGroup} in company Gantt view matches with the different
 * filters.
 *
 * @author Manuel Rego Casasnovas <rego@igalia.com>
 */
public class TaskGroupPredicate implements IPredicate {

    private List<FilterPair> filters;

    private Date startDate;

    private Date finishDate;

    private String name;

    private Boolean excludeFinishedProject;

    public TaskGroupPredicate(List<FilterPair> filters, Date startDate,
            Date finishDate, String name, Boolean excludeFinishedProject) {
        this.filters = filters;
        this.startDate = startDate;
        this.finishDate = finishDate;
        this.name = name;
        this.excludeFinishedProject = excludeFinishedProject;
    }

    @Override
    public boolean accepts(Object object) {
        final TaskGroup taskGroup = (TaskGroup) object;
        return accepts(taskGroup);
    }

    private boolean accepts(TaskGroup taskGroup) {
        if (taskGroup == null) {
            return false;
        }
        if (acceptFilters(taskGroup) && acceptFiltersDates(taskGroup)
                && acceptFilterName(taskGroup)) {
            return true;
        }
        return false;
    }

    private boolean acceptFilters(TaskGroup taskGroup) {
        if ((filters == null) || (filters.isEmpty())) {
            return true;
        }
        for (FilterPair filter : filters) {
            if (!acceptFilter(filter, taskGroup)) {
                return false;
            }
        }
        return true;
    }

    private boolean acceptFilter(FilterPair filter, TaskGroup taskGroup) {
        switch ((TaskGroupFilterEnum) filter.getType()) {
        case Criterion:
            return acceptCriterion(filter, taskGroup);
        case Label:
            return acceptLabel(filter, taskGroup);
        case ExternalCompany:
            return acceptExternalCompany(filter, taskGroup);
        case State:
            return acceptState(filter, taskGroup);
        case Code:
            return acceptCode(filter, taskGroup);
        case CustomerReference:
            return acceptCustomerReference(filter, taskGroup);
        case Resource:
            return acceptResource(filter, taskGroup);
        }
        return false;
    }

    private boolean acceptCriterion(FilterPair filter, TaskElement taskElement) {
        Criterion filterCriterion = (Criterion) filter.getValue();
        if (existCriterionInTaskElementResourceAllocations(filterCriterion,
                taskElement)) {
            return true;
        }
        return false;
    }

    private boolean existCriterionInTaskElementResourceAllocations(
            Criterion filterCriterion, TaskElement taskElement) {
        for (ResourceAllocation<?> each : taskElement
                .getAllResourceAllocations()) {
            if (acceptsCriterionInResourceAllocation(filterCriterion, each)) {
                return true;
            }
        }
        return false;
    }

    private boolean acceptsCriterionInResourceAllocation(
            Criterion filterCriterion, ResourceAllocation<?> resourceAllocation) {
        if (resourceAllocation instanceof GenericResourceAllocation) {
            Set<Criterion> criteria = ((GenericResourceAllocation) resourceAllocation)
                    .getCriterions();
            for (Criterion criterion : criteria) {
                if (criterion.getId().equals(filterCriterion.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean acceptLabel(FilterPair filter, TaskGroup taskGroup) {
        Label filterLabel = (Label) filter.getValue();
        Order order = (Order) taskGroup.getOrderElement();
        if (existLabelInOrderElement(filterLabel, order)) {
            return true;
        }
        return false;
    }

    private boolean existLabelInOrderElement(Label filterLabel,
            OrderElement order) {
        for (Label label : order.getLabels()) {
            if (label.getId().equals(filterLabel.getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean acceptFinishedProject(FilterPair filter, TaskGroup taskGroup) {
        Label filterLabel = (Label) filter.getValue();
        Order order = (Order) taskGroup.getOrderElement();
        if (order.getState() != OrderStatusEnum.FINISHED) {
            return true;
        }
        return false;
    }

    private boolean acceptExternalCompany(FilterPair filter, TaskGroup taskGroup) {
        Order order = (Order) taskGroup.getOrderElement();
        ExternalCompany filterCustomer = (ExternalCompany) filter.getValue();
        if ((order.getCustomer() != null)
                && (order.getCustomer().getId().equals(filterCustomer.getId()))) {
            return true;
        }
        return false;
    }

    private boolean acceptState(FilterPair filter, TaskGroup taskGroup) {
        Order order = (Order) taskGroup.getOrderElement();
        OrderStatusEnum filterState = (OrderStatusEnum) filter.getValue();
        if ((order.getState() != null)
                && (order.getState().equals(filterState))) {
            return true;
        }
        return false;
    }

    private boolean acceptCode(FilterPair filter, TaskGroup taskGroup) {
        Order order = (Order) taskGroup.getOrderElement();
        String filterCode = (String) filter.getValue();
        return order.getCode().equals(filterCode);
    }

    private boolean acceptCustomerReference(FilterPair filter,
            TaskGroup taskGroup) {
        Order order = (Order) taskGroup.getOrderElement();
        String filterCustomerReference = (String) filter.getValue();
        return order.getCustomerReference().equals(filterCustomerReference);
    }

    protected boolean acceptFiltersDates(TaskGroup taskGroup) {
        // Check if exist work report items into interval between the start date
        // and finish date.
        return (acceptStartDate(taskGroup.getStartDate()) && (acceptFinishDate(taskGroup
                .getEndDate())));
    }

    protected boolean acceptStartDate(Date initDate) {
        if ((initDate == null) && (startDate == null)) {
            return true;
        }
        return isLowerToFinishDate(initDate, finishDate);
    }

    protected boolean acceptFinishDate(Date deadLine) {
        if ((deadLine == null) && (finishDate == null)) {
            return true;
        }
        return isGreaterToStartDate(deadLine, startDate);
    }

    private boolean isGreaterToStartDate(Date date, Date startDate) {
        if (startDate == null) {
            return true;
        }

        if (date != null && (date.compareTo(startDate) >= 0)) {
            return true;
        }
        return false;
    }

    private boolean isLowerToFinishDate(Date date, Date finishDate) {
        if (finishDate == null) {
            return true;
        }
        if (date != null && (date.compareTo(finishDate) <= 0)) {
            return true;
        }
        return false;
    }

    private boolean acceptResource(FilterPair filter, TaskElement taskElement) {
        Resource filterResource = (Resource) filter.getValue();
        return existResourceInTaskElementResourceAllocations(filterResource,
                taskElement);
    }

    private boolean existResourceInTaskElementResourceAllocations(
            Resource filterResource, TaskElement taskElement) {
        for (ResourceAllocation<?> each : taskElement
                .getAllResourceAllocations()) {
            if (acceptsResourceInResourceAllocation(filterResource, each)) {
                return true;
            }
        }
        return false;
    }

    private boolean acceptsResourceInResourceAllocation(
            Resource filterResource, ResourceAllocation<?> resourceAllocation) {
        if (resourceAllocation instanceof SpecificResourceAllocation) {
            Resource resource = ((SpecificResourceAllocation) resourceAllocation)
                    .getResource();
            if (resource.getId().equals(filterResource.getId())) {
                return true;
            }
        }
        return false;
    }

    public List<FilterPair> getFilters() {
        if (filters == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(filters);
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getFinishDate() {
        return finishDate;
    }

    public void setFilters(List<FilterPair> listFilters) {
        filters = listFilters;
    }

    protected boolean acceptFilterName(TaskGroup taskGroup) {
        if (name == null) {
            return true;
        }
        if ((taskGroup.getName() != null)
                && (StringUtils.containsIgnoreCase(taskGroup.getName(), name))) {
            return true;
        }
        return false;
    }

    public Boolean getExcludeFinishedProjects() {
        return excludeFinishedProject;
    }

}

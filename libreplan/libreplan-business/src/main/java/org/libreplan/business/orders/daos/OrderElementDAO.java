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

package org.libreplan.business.orders.daos;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.libreplan.business.common.daos.IntegrationEntityDAO;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.email.daos.IEmailNotificationDAO;
import org.libreplan.business.expensesheet.daos.IExpenseSheetLineDAO;
import org.libreplan.business.labels.entities.Label;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.orders.entities.SchedulingDataForVersion;
import org.libreplan.business.orders.entities.TaskSource;
import org.libreplan.business.planner.daos.ITaskSourceDAO;
import org.libreplan.business.planner.entities.TaskElement;
import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.templates.entities.OrderElementTemplate;
import org.libreplan.business.workingday.EffortDuration;
import org.libreplan.business.workreports.daos.IWorkReportDAO;
import org.libreplan.business.workreports.daos.IWorkReportLineDAO;
import org.libreplan.business.workreports.entities.WorkReport;
import org.libreplan.business.workreports.entities.WorkReportLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO for {@link OrderElement}.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 * @author Diego Pino García <dpino@igalia.com>
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 * @author Jacobo Aragunde Pérez <jaragunde@igalia.com>
 **/
@Repository
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class OrderElementDAO extends IntegrationEntityDAO<OrderElement> implements IOrderElementDAO {

    @Autowired
    private IWorkReportLineDAO workReportLineDAO;

    @Autowired
    private IExpenseSheetLineDAO expenseSheetLineDAO;

    @Autowired
    private IWorkReportDAO workReportDAO;

    @Autowired
    private ITaskSourceDAO taskSourceDAO;

    @Autowired
    private IEmailNotificationDAO emailNotificationDAO;

    @Override
    public List<OrderElement> findWithoutParent() {
        return getSession()
                .createCriteria(OrderElement.class)
                .add(Restrictions.isNull("parent"))
                .list();
    }

    public List<OrderElement> findByCodeAndParent(OrderElement parent, String code) {
        Criteria c = getSession().createCriteria(OrderElement.class);
        c.add(Restrictions.eq("infoComponent.code", code));

        if ( parent != null ) {
            c.add(Restrictions.eq("parent", parent));
        } else {
            c.add(Restrictions.isNull("parent"));
        }
        return c.list();
    }

    public OrderElement findUniqueByCodeAndParent(OrderElement parent, String code) throws InstanceNotFoundException {
        List<OrderElement> list = findByCodeAndParent(parent, code);
        if ( list.isEmpty() || list.size() > 1 ) {
            throw new InstanceNotFoundException(code, OrderElement.class.getName());
        }
        return list.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public EffortDuration getAssignedDirectEffort(OrderElement orderElement) {
        List<WorkReportLine> listWRL = this.workReportLineDAO.findByOrderElement(orderElement);
        EffortDuration assignedDirectHours = EffortDuration.zero();
        for (WorkReportLine aListWRL : listWRL) {
            assignedDirectHours = assignedDirectHours.plus(aListWRL.getEffort());
        }
        return assignedDirectHours;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getHoursAdvancePercentage(OrderElement orderElement) {
        boolean condition = orderElement.getSumChargedEffort() != null;

        final EffortDuration totalChargedEffort =
                condition ? orderElement.getSumChargedEffort().getTotalChargedEffort() : EffortDuration.zero();

        BigDecimal assignedHours = totalChargedEffort.toHoursAsDecimalWithScale(2);
        BigDecimal estimatedHours = new BigDecimal(orderElement.getWorkHours()).setScale(2);

        return estimatedHours.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ZERO
                : assignedHours.divide(estimatedHours, RoundingMode.DOWN);
    }

    @Override
    public void remove(Long id) throws InstanceNotFoundException {
        OrderElement orderElement = find(id);

        removeNotifications(orderElement);

        removeTaskSourcesFor(this.taskSourceDAO, orderElement);

        for (WorkReport each : getWorkReportsPointingTo(orderElement)) {
            workReportDAO.remove(each.getId());
        }

        super.remove(id);
    }

    public void removeNotifications(OrderElement orderElement) {

        List<SchedulingDataForVersion> allVersions = orderElement.getSchedulingDataForVersionFromBottomToTop();
        for (TaskSource each : taskSourcesFrom(allVersions)) {
            TaskElement taskElement = each.getTask();
            if ( taskElement != null) {
                emailNotificationDAO.deleteByTask(taskElement);
            }
        }
    }

    public static void removeTaskSourcesFor(ITaskSourceDAO taskSourceDAO, OrderElement orderElement)
            throws InstanceNotFoundException {

        List<SchedulingDataForVersion> allVersions = orderElement.getSchedulingDataForVersionFromBottomToTop();
        for (TaskSource each : taskSourcesFrom(allVersions)) {
            each.detachAssociatedTaskFromParent();
            taskSourceDAO.remove(each.getId());
        }
    }

    private static List<TaskSource> taskSourcesFrom(List<SchedulingDataForVersion> list) {
        List<TaskSource> result = new ArrayList<>();
        for (SchedulingDataForVersion each : list) {
            if ( each.getTaskSource() != null ) {
                result.add(each.getTaskSource());
            }
        }
        return result;
    }

    private Set<WorkReport> getWorkReportsPointingTo(OrderElement orderElement) {
        Set<WorkReport> result = new HashSet<>();
        for (WorkReportLine each : workReportLineDAO.findByOrderElementAndChildren(orderElement)) {
            result.add(each.getWorkReport());
        }
        return result;
    }

    @Override
    public List<OrderElement> findAll() {
        return getSession()
                .createCriteria(getEntityClass())
                .addOrder(org.hibernate.criterion.Order.asc("infoComponent.code"))
                .list();
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public OrderElement findByCode(String code) throws InstanceNotFoundException {

        if ( StringUtils.isBlank(code) ) {
            throw new InstanceNotFoundException(null, getEntityClass().getName());
        }

        OrderElement entity = (OrderElement) getSession()
                .createCriteria(getEntityClass())
                .add(Restrictions.eq("infoComponent.code", code.trim()).ignoreCase())
                .uniqueResult();

        if ( entity == null ) {
            throw new InstanceNotFoundException(code, getEntityClass().getName());
        } else {
            return entity;
        }

    }

    public List<OrderElement> findByTemplate(OrderElementTemplate template) {
        return getSession()
                .createCriteria(OrderElement.class)
                .add(Restrictions.eq("template", template))
                .list();
    }

    @Override
    public OrderElement findUniqueByCode(String code) throws InstanceNotFoundException {
        Criteria c = getSession().createCriteria(OrderElement.class);
        c.add(Restrictions.eq("infoComponent.code", code));

        OrderElement orderElement = (OrderElement) c.uniqueResult();
        if ( orderElement == null ) {
            throw new InstanceNotFoundException(code, OrderElement.class.getName());
        } else {
            return orderElement;
        }
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public OrderElement findUniqueByCodeAnotherTransaction(String code) throws InstanceNotFoundException {
        return findUniqueByCode(code);
    }

    @Override
    @Transactional
    public List<OrderElement> getAll() {
        return list(OrderElement.class);
    }

    @Override
    public List<OrderElement> findOrderElementsWithExternalCode() {
        return getSession()
                .createCriteria(OrderElement.class)
                .add(Restrictions.isNotNull("externalCode"))
                .list();
    }


    @SuppressWarnings("unchecked")
    @Override
    public OrderElement findByExternalCode(String code) throws InstanceNotFoundException {

        if ( StringUtils.isBlank(code) ) {
            throw new InstanceNotFoundException(null, getEntityClass().getName());
        }

        OrderElement entity = (OrderElement) getSession()
                .createCriteria(OrderElement.class)
                .add(Restrictions.eq("externalCode", code.trim()).ignoreCase())
                .uniqueResult();

        if ( entity == null ) {
            throw new InstanceNotFoundException(code, getEntityClass().getName());
        } else {
            return entity;
        }

    }

    /**
     * Methods to calculate statistics with the estimated hours and worked hours of a set of order elements.
     *
     * @param list
     *            <{@link OrderElement}>
     */

    public BigDecimal calculateAverageEstimatedHours(final List<OrderElement> list) {
        return average(new BigDecimal(list.size()), sumEstimatedHours(list));
    }

    public EffortDuration calculateAverageWorkedHours(final List<OrderElement> list) {
        return list.isEmpty() ? EffortDuration.zero() : EffortDuration.average(sumWorkedHours(list), list.size());
    }

    private BigDecimal average(BigDecimal divisor, BigDecimal sum) {
        BigDecimal average = new BigDecimal(0);
        if ( sum.compareTo(new BigDecimal(0)) > 0 ) {
            average = sum.divide(divisor, new MathContext(2, RoundingMode.HALF_UP));
        }
        return average;
    }

    private BigDecimal sumEstimatedHours(final List<OrderElement> list) {
        BigDecimal sum = new BigDecimal(0);
        for (OrderElement orderElement : list) {
            sum = sum.add(new BigDecimal(orderElement.getWorkHours()));
        }
        return sum;
    }

    private EffortDuration sumWorkedHours(final List<OrderElement> list) {
        EffortDuration sum = EffortDuration.zero();
        for (OrderElement orderElement : list) {
            sum = sum.plus(getAssignedDirectEffort(orderElement));
        }
        return sum;
    }

    public BigDecimal calculateMaxEstimatedHours(final List<OrderElement> list) {
        BigDecimal max = new BigDecimal(0);
        if ( !list.isEmpty() ) {
            max = new BigDecimal(list.get(0).getWorkHours());
            for (OrderElement orderElement : list) {
                BigDecimal value = new BigDecimal(orderElement.getWorkHours());
                max = getMax(max, value);
            }
        }
        return max;
    }

    private BigDecimal getMax(BigDecimal valueA, BigDecimal valueB) {
        if ( valueA.compareTo(valueB) < 0 ) {
            return valueB;
        } else if ( valueA.compareTo(valueB) > 0 ) {
            return valueA;
        }
        return valueA;
    }

    private BigDecimal getMin(BigDecimal valueA, BigDecimal valueB) {
        if ( valueA.compareTo(valueB) > 0 ) {
            return valueB;
        } else if ( valueA.compareTo(valueB) < 0 ) {
            return valueA;
        }
        return valueA;
    }

    public BigDecimal calculateMinEstimatedHours(final List<OrderElement> list) {
        BigDecimal min = new BigDecimal(0);
        if ( !list.isEmpty() ) {
            min = new BigDecimal(list.get(0).getWorkHours());
            for (OrderElement orderElement : list) {
                BigDecimal value = new BigDecimal(orderElement.getWorkHours());
                min = getMin(min, value);
            }
        }
        return min;
    }

    @Override
    public EffortDuration calculateMaxWorkedHours(final List<OrderElement> list) {
        EffortDuration max = EffortDuration.zero();
        if ( !list.isEmpty() ) {
            max = getAssignedDirectEffort(list.get(0));
            for (OrderElement orderElement : list) {
                EffortDuration value = getAssignedDirectEffort(orderElement);
                max = EffortDuration.max(max, value);
            }
        }
        return max;
    }

    @Override
    public EffortDuration calculateMinWorkedHours(final List<OrderElement> list) {
        EffortDuration min = EffortDuration.zero();
        if ( !list.isEmpty() ) {
            min = getAssignedDirectEffort(list.get(0));
            for (OrderElement orderElement : list) {
                EffortDuration value = getAssignedDirectEffort(orderElement);
                min = EffortDuration.min(min, value);
            }
        }
        return min;
    }

    @Override
    public boolean isAlreadyInUse(OrderElement orderElement) {
        if ( orderElement.isNewObject() ) {
            return false;
        }
        boolean usedInWorkReports = !getSession()
                .createCriteria(WorkReport.class)
                .add(Restrictions.eq("orderElement", orderElement)).list().isEmpty();

        boolean usedInWorkReportLines = !getSession()
                .createCriteria(WorkReportLine.class)
                .add(Restrictions.eq("orderElement", orderElement)).list().isEmpty();

        return usedInWorkReports || usedInWorkReportLines;
    }

    @Override
    public boolean isAlreadyInUseThisOrAnyOfItsChildren(OrderElement orderElement) {
        if ( isAlreadyInUse(orderElement) ) {
            return true;
        }

        for (OrderElement child : orderElement.getChildren()) {
            if ( isAlreadyInUseThisOrAnyOfItsChildren(child) ) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public Set<String> getAllCodesExcluding(List<OrderElement> orderElements) {

        String strQuery = "SELECT order.infoComponent.code FROM OrderElement order ";

        final List<Long> ids = getNoEmptyIds(orderElements);
        if ( !ids.isEmpty() ) {
            strQuery += "WHERE order.id NOT IN (:ids)";
        }

        Query query = getSession().createQuery(strQuery);
        if ( !ids.isEmpty() ) {
            query.setParameterList("ids", ids);
        }
        return new HashSet<>(query.list());
    }

    private List<Long> getNoEmptyIds(List<OrderElement> orderElements) {
        List<Long> result = new ArrayList<>();
        for (OrderElement each: orderElements) {
            final Long id = each.getId();
            if ( id != null ) {
                result.add(id);
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly= true, propagation = Propagation.REQUIRES_NEW)
    public OrderElement findRepeatedOrderCodeInDB(OrderElement order) {
        final Map<String, OrderElement> orderElements = createMapByCode(getOrderAndAllChildren(order));
        final Map<String, OrderElement> orderElementsInDB = createMapByCode(getAll());
        boolean condition;

        for (String code : orderElements.keySet()) {
            OrderElement orderElement = orderElements.get(code);
            OrderElement orderElementInDB = orderElementsInDB.get(code);

            // There is an element in the DB with the same code and it's a different element in a different order
            condition = orderElementInDB != null &&
                    !orderElementInDB.getId().equals(orderElement.getId()) &&
                    !orderElementInDB.getOrder().getId().equals(orderElement.getOrder().getId());

            if ( condition ) {
                return orderElement;
            }
        }
        return null;
    }

    private List<OrderElement> getOrderAndAllChildren(OrderElement order) {
        List<OrderElement> result = new ArrayList<>();
        result.add(order);
        result.addAll(order.getAllChildren());

        return result;
    }

    private Map<String, OrderElement> createMapByCode(List<OrderElement> orderElements) {
        Map<String, OrderElement> result = new HashMap<>();
        for (OrderElement each: orderElements) {
            final String code = each.getCode();
            result.put(code, each);
        }
        return result;
    }

    @Override
    public boolean hasImputedExpenseSheet(Long id) throws InstanceNotFoundException {
        return !expenseSheetLineDAO.findByOrderElement(find(id)).isEmpty();
    }

    @Override
    public boolean hasImputedExpenseSheetThisOrAnyOfItsChildren(Long id) throws InstanceNotFoundException {
        return !expenseSheetLineDAO.findByOrderElementAndChildren(find(id)).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<OrderElement> findByLabelsAndCriteria(Set<Label> labels, Set<Criterion> criteria) {

        String strQuery = "SELECT oe.id ";
        strQuery += "FROM OrderElement oe ";

        String where = "";
        if ( labels != null && !labels.isEmpty() ) {
            for (int i = 0; i < labels.size(); i++) {
                if ( where.isEmpty() ) {
                    where += "WHERE ";
                } else {
                    where += "AND ";
                }
                where += ":label" + i + " IN elements(oe.labels) ";
            }
        }

        if ( criteria != null && !criteria.isEmpty() ) {
            strQuery += "JOIN oe.criterionRequirements cr ";

            if ( where.isEmpty() ) {
                where += "WHERE ";
            } else {
                where += "AND ";
            }

            where += "cr.criterion IN (:criteria) ";
            where += "AND cr.class = DirectCriterionRequirement ";
            where += "GROUP BY oe.id ";
            where += "HAVING count(oe.id) = :criteriaSize ";
        }

        strQuery += where;

        Query query = getSession().createQuery(strQuery);
        if ( labels != null && !labels.isEmpty() ) {
            int i = 0;
            for (Label label : labels) {
                query.setParameter("label" + i, label);
                i++;
            }
        }

        if ( criteria != null && !criteria.isEmpty() ) {
            query.setParameterList("criteria", criteria);
            query.setParameter("criteriaSize", (long) criteria.size());
        }

        List<Long> orderElementsIds = query.list();
        if ( orderElementsIds.isEmpty() ) {
            return Collections.emptyList();
        }

        return getSession()
                .createQuery("FROM OrderElement oe WHERE oe.id IN (:ids) ORDER BY oe.infoComponent.code")
                .setParameterList("ids", orderElementsIds).list();
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public boolean existsByCodeInAnotherOrderAnotherTransaction(OrderElement orderElement) {
        return existsByCodeInAnotherOrder(orderElement);
    }

    private boolean existsByCodeInAnotherOrder(OrderElement orderElement) {
        try {
            return !areInTheSameOrder(orderElement, findUniqueByCode(orderElement.getCode()));
        } catch (InstanceNotFoundException e) {
            return false;
        }
    }

    private boolean areInTheSameOrder(OrderElement orderElement1, OrderElement orderElement2) {
        Order order1 = orderElement1.getOrder();
        Order order2 = orderElement2.getOrder();

        return !(order1 == null || order2 == null) && Objects.equals(order1.getId(), order2.getId());
    }

}
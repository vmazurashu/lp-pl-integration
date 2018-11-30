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

package org.libreplan.business.orders.daos;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.libreplan.business.common.IAdHocTransactionService;
import org.libreplan.business.common.IOnTransaction;
import org.libreplan.business.common.daos.GenericDAOHibernate;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.orders.entities.OrderLineGroup;
import org.libreplan.business.orders.entities.SumChargedEffort;
import org.libreplan.business.util.Pair;
import org.libreplan.business.workingday.EffortDuration;
import org.libreplan.business.workreports.daos.IWorkReportLineDAO;
import org.libreplan.business.workreports.entities.WorkReportLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO for {@link SumChargedEffort}.
 *
 * @author Manuel Rego Casasnovas <rego@igalia.com>
 */
@Repository
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class SumChargedEffortDAO extends
        GenericDAOHibernate<SumChargedEffort, Long> implements
        ISumChargedEffortDAO {

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private IWorkReportLineDAO workReportLineDAO;

    @Autowired
    private IAdHocTransactionService transactionService;

    @Autowired
    private IOrderDAO orderDAO;

    @Autowired
    private IOrderElementDAO orderElementDAO;

    private Map<OrderElement, SumChargedEffort> mapSumChargedEfforts;

    @Override
    public void updateRelatedSumChargedEffortWithWorkReportLineSet(
            Set<WorkReportLine> workReportLineSet) {
        resetMapSumChargedEfforts();

        for (WorkReportLine workReportLine : workReportLineSet) {
            updateRelatedSumChargedEffortWithAddedOrModifiedWorkReportLine(workReportLine);
        }
    }

    private void updateRelatedSumChargedEffortWithAddedOrModifiedWorkReportLine(
            final WorkReportLine workReportLine) {
        boolean increase = true;
        boolean sameOrderElement = true;
        EffortDuration effort = workReportLine.getEffort();
        OrderElement orderElement = workReportLine.getOrderElement();

        EffortDuration previousEffort = null;
        OrderElement previousOrderElement = null;

        if (!workReportLine.isNewObject()) {
            Pair<EffortDuration, OrderElement> previous = transactionService
                    .runOnAnotherTransaction(new IOnTransaction<Pair<EffortDuration, OrderElement>>() {
                        @Override
                        public Pair<EffortDuration, OrderElement> execute() {
                            try {
                                WorkReportLine line = workReportLineDAO
                                        .find(workReportLine.getId());

                                OrderElement orderElement = line
                                        .getOrderElement();
                                forceLoadParents(orderElement);

                                return Pair.create(line.getEffort(),
                                        orderElement);
                            } catch (InstanceNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        private void forceLoadParents(OrderElement orderElement) {
                            OrderLineGroup parent = orderElement.getParent();
                            if (parent != null) {
                                forceLoadParents(parent);
                            }
                        }

                    });

            previousEffort = previous.getFirst();
            previousOrderElement = previous.getSecond();

            sameOrderElement = orderElement.getId().equals(
                    previousOrderElement.getId());
        }

        if (sameOrderElement) {
            if (previousEffort != null) {
                if (effort.compareTo(previousEffort) >= 0) {
                    effort = effort.minus(previousEffort);
                } else {
                    increase = false;
                    effort = previousEffort.minus(effort);
                }
            }

            if (!effort.isZero()) {
                if (increase) {
                    addDirectChargedEffort(orderElement, effort);
                } else {
                    substractDirectChargedEffort(orderElement, effort);
                }
            }
        } else {
            substractDirectChargedEffort(previousOrderElement, previousEffort);
            addDirectChargedEffort(orderElement, effort);
        }
    }

    private void addDirectChargedEffort(OrderElement orderElement,
            EffortDuration effort) {
        SumChargedEffort sumChargedEffort = getByOrderElement(orderElement);

        sumChargedEffort.addDirectChargedEffort(effort);
        save(sumChargedEffort);

        addInirectChargedEffortRecursively(orderElement.getParent(), effort);
    }

    private void addInirectChargedEffortRecursively(OrderElement orderElement,
            EffortDuration effort) {
        if (orderElement != null) {
            SumChargedEffort sumChargedEffort = getByOrderElement(orderElement);

            sumChargedEffort.addIndirectChargedEffort(effort);
            save(sumChargedEffort);

            addInirectChargedEffortRecursively(orderElement.getParent(), effort);
        }
    }

    @Override
    public void updateRelatedSumChargedEffortWithDeletedWorkReportLineSet(
            Set<WorkReportLine> workReportLineSet) {
        resetMapSumChargedEfforts();

        for (WorkReportLine workReportLine : workReportLineSet) {
            updateRelatedSumChargedEffortWithDeletedWorkReportLine(workReportLine);
        }
    }

    private void resetMapSumChargedEfforts() {
        mapSumChargedEfforts = new HashMap<OrderElement, SumChargedEffort>();
    }

    private void updateRelatedSumChargedEffortWithDeletedWorkReportLine(
            WorkReportLine workReportLine) {
        if (workReportLine.isNewObject()) {
            // If the line hasn't been saved, we have nothing to update
            return;
        }

        // Refresh data from database, because of changes not saved are not
        // useful for the following operations
        sessionFactory.getCurrentSession().refresh(workReportLine);

        substractDirectChargedEffort(workReportLine.getOrderElement(),
                workReportLine.getEffort());
    }

    private void substractDirectChargedEffort(OrderElement orderElement,
            EffortDuration effort) {
        SumChargedEffort sumChargedEffort = getByOrderElement(orderElement);

        sumChargedEffort.subtractDirectChargedEffort(effort);
        save(sumChargedEffort);

        substractInirectChargedEffortRecursively(orderElement.getParent(),
                effort);
    }

    private void substractInirectChargedEffortRecursively(OrderElement orderElement,
            EffortDuration effort) {
        if (orderElement != null) {
            SumChargedEffort sumChargedEffort = getByOrderElement(orderElement);

            sumChargedEffort.subtractIndirectChargedEffort(effort);
            save(sumChargedEffort);

            substractInirectChargedEffortRecursively(orderElement.getParent(), effort);
        }
    }

    private SumChargedEffort getByOrderElement(OrderElement orderElement) {
        SumChargedEffort sumChargedEffort = mapSumChargedEfforts
                .get(orderElement);
        if (sumChargedEffort == null) {
            sumChargedEffort = findByOrderElement(orderElement);
            if (sumChargedEffort == null) {
                sumChargedEffort = SumChargedEffort.create(orderElement);
            }
            mapSumChargedEfforts.put(orderElement, sumChargedEffort);
        }
        return sumChargedEffort;
    }

    @Override
    public SumChargedEffort findByOrderElement(OrderElement orderElement) {
        return (SumChargedEffort) getSession().createCriteria(getEntityClass())
                .add(Restrictions.eq("orderElement", orderElement))
                .uniqueResult();
    }

    @Override
    @Transactional
    public void recalculateSumChargedEfforts(Long orderId) {
        try {
            Order order = orderDAO.find(orderId);
            resetMapSumChargedEfforts();
            resetSumChargedEffort(order);
            calculateDirectChargedEffort(order);
            calculateTimesheetData(order);
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void resetSumChargedEffort(OrderElement orderElement) {
        SumChargedEffort sumChargedEffort = getByOrderElement(orderElement);
        sumChargedEffort.reset();

        for (OrderElement each : orderElement.getChildren()) {
            resetSumChargedEffort(each);
        }
    }

    private void calculateDirectChargedEffort(OrderElement orderElement) {
        for (OrderElement each : orderElement.getChildren()) {
            calculateDirectChargedEffort(each);
        }

        EffortDuration effort = EffortDuration.zero();
        for (WorkReportLine line : workReportLineDAO
                .findByOrderElement(orderElement)) {
            effort = effort.plus(line.getEffort());
        }
        addDirectChargedEffort(orderElement, effort);
    }

    private void calculateTimesheetData(OrderElement orderElement) {
        calculateTimesheetDatesAndChildren(orderElement);
        calculateFinishedTimesheetsAndChildren(orderElement);
    }

    private Pair<Date, Date> calculateTimesheetDatesAndChildren(
            OrderElement orderElement) {
        Pair<Date, Date> minMax = workReportLineDAO
                .findMinAndMaxDatesByOrderElement(orderElement);

        Set<Date> minDates = new HashSet<Date>();
        Set<Date> maxDates = new HashSet<Date>();

        addIfNotNull(minDates, minMax.getFirst());
        addIfNotNull(maxDates, minMax.getSecond());

        for (OrderElement child : orderElement.getChildren()) {
            Pair<Date, Date> minMaxChild = calculateTimesheetDatesAndChildren(child);
            addIfNotNull(minDates, minMaxChild.getFirst());
            addIfNotNull(maxDates, minMaxChild.getSecond());
        }

        Pair<Date, Date> result = Pair.create(
                minDates.isEmpty() ? null : Collections.min(minDates),
                maxDates.isEmpty() ? null : Collections.max(maxDates));

        SumChargedEffort sumChargedEffort = getByOrderElement(orderElement);
        sumChargedEffort.setTimesheetDates(result.getFirst(),
                result.getSecond());
        save(sumChargedEffort);

        return result;
    }

    private void addIfNotNull(Collection<Date> list, Date date) {
        if (date != null) {
            list.add(date);
        }
    }

    private void calculateFinishedTimesheetsAndChildren(
            OrderElement orderElement) {
        calculateFinishedTimesheets(orderElement);

        for (OrderElement child : orderElement.getChildren()) {
            calculateFinishedTimesheetsAndChildren(child);
        }
    }

    private void calculateFinishedTimesheets(OrderElement orderElement) {
        SumChargedEffort sumChargedEffort = getByOrderElement(orderElement);
        sumChargedEffort.setFinishedTimesheets(workReportLineDAO
                .isFinished(orderElement));
        save(sumChargedEffort);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<OrderElement> getOrderElementsToRecalculateTimsheetDates(
            Set<WorkReportLine> workReportLines,
            Set<WorkReportLine> deletedWorkReportLines) {
        Set<OrderElement> orderElements = new HashSet<OrderElement>();

        if (workReportLines != null) {
            for (final WorkReportLine workReportLine : workReportLines) {
                if (!workReportLine.isNewObject()) {
                    OrderElement previousOrderElement = transactionService
                            .runOnAnotherTransaction(new IOnTransaction<OrderElement>() {
                                @Override
                                public OrderElement execute() {
                                    try {
                                        WorkReportLine line = workReportLineDAO
                                                .find(workReportLine.getId());

                                        return line.getOrderElement();
                                    } catch (InstanceNotFoundException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            });
                    orderElements.add(previousOrderElement);
                }
                orderElements.add(workReportLine.getOrderElement());
            }
        }

        if (deletedWorkReportLines != null) {
            for (WorkReportLine workReportLine : deletedWorkReportLines) {
                if (workReportLine.isNewObject()) {
                    // If the line hasn't been saved, we don't take it into
                    // account
                    continue;
                }

                // Refresh data from database, because of changes not saved are
                // not
                // useful for the following operations
                sessionFactory.getCurrentSession().refresh(workReportLine);

                orderElements.add(workReportLine.getOrderElement());
            }
        }

        return orderElements;
    }

    @Override
    @Transactional
    public void recalculateTimesheetData(Set<OrderElement> orderElements) {
        try {
            for (OrderElement orderElement : orderElements) {
                saveTimesheetDatesRecursively(orderElementDAO.find(orderElement
                        .getId()));
                calculateFinishedTimesheets(orderElementDAO.find(orderElement
                        .getId()));
            }
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveTimesheetDatesRecursively(OrderElement orderElement) {
        if (orderElement != null) {
            saveTimesheetDates(orderElement);
            saveTimesheetDatesRecursively(orderElement.getParent());
        }
    }

    private void saveTimesheetDates(OrderElement orderElement) {
        Pair<Date, Date> minMax = workReportLineDAO
                .findMinAndMaxDatesByOrderElement(orderElement);

        Set<Date> minDates = new HashSet<Date>();
        Set<Date> maxDates = new HashSet<Date>();

        addIfNotNull(minDates, minMax.getFirst());
        addIfNotNull(maxDates, minMax.getSecond());

        for (OrderElement child : orderElement.getChildren()) {
            SumChargedEffort childSumChargedEffort = getByOrderElement(child);
            addIfNotNull(minDates,
                    childSumChargedEffort.getFirstTimesheetDate());
            addIfNotNull(maxDates, childSumChargedEffort.getLastTimesheetDate());
        }

        Pair<Date, Date> result = Pair.create(minDates.isEmpty() ? null
                : Collections.min(minDates), maxDates.isEmpty() ? null
                : Collections.max(maxDates));

        SumChargedEffort sumChargedEffort = getByOrderElement(orderElement);
        sumChargedEffort.setTimesheetDates(result.getFirst(),
                result.getSecond());
        save(sumChargedEffort);
    }

}

/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
 * Copyright (C) 2010-2012 Igalia, S.L.
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

package org.libreplan.business.resources.daos;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.libreplan.business.common.daos.IntegrationEntityDAO;
import org.libreplan.business.labels.entities.Label;
import org.libreplan.business.reports.dtos.HoursWorkedPerResourceDTO;
import org.libreplan.business.reports.dtos.HoursWorkedPerWorkerInAMonthDTO;
import org.libreplan.business.reports.dtos.LabelFilterType;
import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.resources.entities.LimitingResourceQueue;
import org.libreplan.business.resources.entities.Machine;
import org.libreplan.business.resources.entities.Resource;
import org.libreplan.business.resources.entities.Worker;
import org.libreplan.business.scenarios.IScenarioManager;
import org.libreplan.business.workingday.EffortDuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate DAO for the <code>Resource</code> entity.
 * @author Fernando Bellas Permuy <fbellas@udc.es>
 * @author Diego Pino Garcia <dpino@udc.es>
 */
@Repository
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Transactional
public class ResourceDAO extends IntegrationEntityDAO<Resource> implements IResourceDAO {

    @Override
    public List<Worker> getWorkers() {
        return list(Worker.class);
    }

    @Override
    public List<Worker> getVirtualWorkers() {
        List<Worker> list = getWorkers();
        for (Iterator<Worker> iterator = list.iterator(); iterator.hasNext();) {
            Worker worker = iterator.next();
            if ( worker.isReal() ) {
                iterator.remove();
            }
        }

        return list;
    }

    @Override
    public List<Worker> getRealWorkers() {
        List<Worker> list = getWorkers();
        for (Iterator<Worker> iterator = list.iterator(); iterator.hasNext();) {
            Worker worker = iterator.next();
            if ( worker.isVirtual() ) {
                iterator.remove();
            }
        }

        return list;
    }

    public List<Resource> getResources() {
        return list(Resource.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Resource> getAllLimitingResources() {
        return getSession().createCriteria(Resource.class).add(Restrictions.eq("limitingResource", true)).list();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Resource> getAllNonLimitingResources() {
        return getSession().createCriteria(Resource.class).add(Restrictions.eq("limitingResource", false)).list();
    }

    @Override
    public List<Machine> getMachines() {
        return list(Machine.class);
    }

    @Override
    public List<Resource> getRealResources() {
        List<Resource> list = new ArrayList<>();
        list.addAll(getRealWorkers());
        list.addAll(getMachines());

        return list;
    }

    @Override
    public void save(Resource resource) {
        if ( resource instanceof Worker || resource instanceof Machine ) {
            if ( resource.isLimitingResource() && resource.getLimitingResourceQueue() == null ) {
                resource.setLimitingResourceQueue(LimitingResourceQueue.create());
            }
        }

        super.save(resource);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HoursWorkedPerResourceDTO> getWorkingHoursPerWorker(
            List<Resource> resources,
            List<Label> labels,
            LabelFilterType labelFilterType,
            List<Criterion> criterions,
            Date startingDate,
            Date endingDate) {

        String strQuery =
                "SELECT new org.libreplan.business.reports.dtos.HoursWorkedPerResourceDTO(resource, wrl) " +
                "FROM Resource resource, WorkReportLine wrl " +
                "LEFT OUTER JOIN wrl.resource wrlresource " +
                "WHERE wrlresource.id = resource.id ";

        // Set date range
        if ( startingDate != null && endingDate != null ) {
            strQuery += "AND wrl.date BETWEEN :startingDate AND :endingDate ";
        }

        if ( startingDate != null && endingDate == null ) {
            strQuery += "AND wrl.date >= :startingDate ";
        }

        if ( startingDate == null && endingDate != null ) {
            strQuery += "AND wrl.date <= :endingDate ";
        }

        // Set workers
        if ( resources != null && !resources.isEmpty() ) {
            strQuery += "AND resource IN (:resources) ";
        }

        // Set labels
        if ( labels != null && !labels.isEmpty() ) {
            switch (labelFilterType) {
                case ORDER_ELEMENT:
                    strQuery += " AND ( EXISTS (FROM wrl.orderElement.labels as etq WHERE etq IN (:labels)) " +
                            "OR EXISTS (FROM wrl.workReport.orderElement.labels as etqwr WHERE etqwr IN (:labels)) ) ";
                    break;

                case WORK_REPORT:
                    strQuery += " AND ( EXISTS (FROM wrl.labels as etq WHERE etq IN (:labels)) " +
                        "OR EXISTS (FROM wrl.workReport.labels as etqwr WHERE etqwr IN (:labels)) ) ";
                    break;

                case BOTH:
                    strQuery += " AND ( EXISTS (FROM wrl.labels as etq WHERE etq IN (:labels)) " +
                            "OR EXISTS (FROM wrl.workReport.labels as etqwr WHERE etqwr IN (:labels)) ) " +
                            "AND ( EXISTS (FROM wrl.orderElement.labels as etq WHERE etq IN (:labels)) " +
                            "OR EXISTS (FROM wrl.workReport.orderElement.labels as etqwr WHERE etqwr IN (:labels)) ) ";
                    break;

                case ANY:

                default:
                    strQuery += " AND ( ( EXISTS (FROM wrl.labels as etq WHERE etq IN (:labels)) " +
                            "OR EXISTS (FROM wrl.workReport.labels as etqwr WHERE etqwr IN (:labels)) ) " +
                            "OR ( EXISTS (FROM wrl.orderElement.labels as etq WHERE etq IN (:labels)) " +
                            "OR EXISTS (FROM wrl.workReport.orderElement.labels as etqwr WHERE etqwr " +
                            "IN (:labels)) ) ) ";
                    break;
            }
        }

        // Set Criterions
        if ( criterions != null && !criterions.isEmpty() ) {
            strQuery +=
                    " AND EXISTS (FROM resource.criterionSatisfactions as satisfaction " +
                    " WHERE satisfaction.criterion IN (:criterions)) ";
        }

        // Order by
        strQuery += "ORDER BY resource.id, wrl.date";

        // Set parameters
        Query query = getSession().createQuery(strQuery);
        if ( startingDate != null ) {
            query.setParameter("startingDate", startingDate);
        }

        if ( endingDate != null ) {
            query.setParameter("endingDate", endingDate);
        }

        if ( resources != null && !resources.isEmpty() ) {
            query.setParameterList("resources", resources);
        }

        if ( labels != null && !labels.isEmpty() ) {
            query.setParameterList("labels", labels);
        }

        if ( criterions != null && !criterions.isEmpty() ) {
            query.setParameterList("criterions", Criterion.withAllDescendants(criterions));
        }

        // Get result
        return query.list();
    }

    @Override
    public List<HoursWorkedPerWorkerInAMonthDTO> getWorkingHoursPerWorker(Integer year, Integer month) {

        String strQuery =
                "SELECT wrlresource.id, SUM(wrl.effort) " +
                "FROM WorkReportLine wrl " +
                "LEFT OUTER JOIN wrl.resource wrlresource ";

        if ( year != null ) {
            strQuery += "WHERE YEAR(wrl.date) = :year ";
        }

        if ( month != null ) {
            strQuery += "AND MONTH(wrl.date) = :month ";
        }

        strQuery += "GROUP BY wrlresource.id, MONTH(wrl.date) ";

        Query query = getSession().createQuery(strQuery);
        if ( year != null ) {
            query.setParameter("year", year);
        }

        if ( month != null ) {
            query.setParameter("month", month);
        }

        List<HoursWorkedPerWorkerInAMonthDTO> result = toDTO(query.list());

        return result;
    }

    @Override
    public Number getRowCount() {
        return (Number) getSession()
                .createCriteria(Resource.class).setProjection(Projections.rowCount()).uniqueResult();
    }

    private List<HoursWorkedPerWorkerInAMonthDTO> toDTO(List<Object> rows) {
        List<HoursWorkedPerWorkerInAMonthDTO> result = new ArrayList<>();

        for (Object row: rows) {
            Object[] columns = (Object[]) row;
            Resource resource = findExistingEntity((Long) columns[0]);
            EffortDuration effort = EffortDuration.seconds(((Long) columns[1]).intValue());

            HoursWorkedPerWorkerInAMonthDTO dto = new HoursWorkedPerWorkerInAMonthDTO(resource, effort);
            result.add(dto);
        }

        return result;
    }

}

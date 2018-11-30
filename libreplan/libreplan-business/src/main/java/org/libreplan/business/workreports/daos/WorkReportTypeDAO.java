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

package org.libreplan.business.workreports.daos;

import java.util.List;

import org.apache.commons.lang3.Validate;
import org.hibernate.Criteria;
import org.hibernate.NonUniqueResultException;
import org.hibernate.criterion.Restrictions;
import org.libreplan.business.common.daos.IntegrationEntityDAO;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.workreports.entities.WorkReportType;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dao for {@link WorkReportTypeDAO}
 *
 * @author Diego Pino García <dpino@igalia.com>
 */
@Repository
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class WorkReportTypeDAO extends IntegrationEntityDAO<WorkReportType>
        implements IWorkReportTypeDAO {

    @Override
    public WorkReportType findUniqueByName(WorkReportType workReportType)
            throws InstanceNotFoundException {
        Validate.notNull(workReportType);

        return findUniqueByName(workReportType.getName());
    }

    @Override
    public WorkReportType findUniqueByName(String name)
            throws InstanceNotFoundException, NonUniqueResultException {
        Criteria c = getSession().createCriteria(WorkReportType.class);
        c.add(Restrictions.eq("name", name));
        WorkReportType workReportType = (WorkReportType) c.uniqueResult();

        if (workReportType == null) {
            throw new InstanceNotFoundException(null, "WorkReportType");
        }
        return workReportType;
    }

    @Override
    public boolean existsOtherWorkReportTypeByName(WorkReportType workReportType) {
        try {
            WorkReportType t = findUniqueByName(workReportType);
            return (t != null && t != workReportType);
        } catch (InstanceNotFoundException e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public boolean existsByNameAnotherTransaction(WorkReportType workReportType) {
        return existsOtherWorkReportTypeByName(workReportType);
    }

    @Override
    public WorkReportType findUniqueByCode(WorkReportType workReportType)
            throws InstanceNotFoundException {
        Validate.notNull(workReportType);

        return findUniqueByCode(workReportType.getCode());
    }

    @Override
    public WorkReportType findUniqueByCode(String code)
            throws InstanceNotFoundException, NonUniqueResultException {
        Criteria c = getSession().createCriteria(WorkReportType.class);
        c.add(Restrictions.eq("code", code));
        WorkReportType workReportType = (WorkReportType) c.uniqueResult();

        if (workReportType == null) {
            throw new InstanceNotFoundException(null, "WorkReportType");
        }
        return workReportType;
    }

    @Override
    public boolean existsOtherWorkReportTypeByCode(WorkReportType workReportType) {
        try {
            WorkReportType t = findUniqueByCode(workReportType);
            return (t != null && t != workReportType);
        } catch (InstanceNotFoundException e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public boolean existsByCodeAnotherTransaction(WorkReportType workReportType) {
        return existsOtherWorkReportTypeByCode(workReportType);
    }

    @Override
    public List<WorkReportType> getWorkReportTypes() {
        return list(WorkReportType.class);
    }

}

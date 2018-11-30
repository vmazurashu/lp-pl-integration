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

package org.libreplan.business.externalcompanies.daos;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.libreplan.business.common.daos.GenericDAOHibernate;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.externalcompanies.entities.ExternalCompany;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.planner.entities.SubcontractedTaskData;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate DAO for {@link ExternalCompany}.
 *
 * @author Jacobo Aragunde Perez <jaragunde@igalia.com>
 */
@Repository
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class ExternalCompanyDAO extends GenericDAOHibernate<ExternalCompany, Long> implements IExternalCompanyDAO {

    @Override
    public boolean existsByName(String name) {
        try {
            findUniqueByName(name);
            return true;
        } catch (InstanceNotFoundException e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public boolean existsByNameInAnotherTransaction(String name) {
        return existsByName(name);
    }

    @Override
    public ExternalCompany findUniqueByName(String name) throws InstanceNotFoundException {
        Criteria c = getSession().createCriteria(ExternalCompany.class);
        c.add(Restrictions.eq("name", name));

        ExternalCompany found = (ExternalCompany) c.uniqueResult();
        if (found == null) {
            throw new InstanceNotFoundException(name, ExternalCompany.class.getName());
        }

        return found;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public ExternalCompany findUniqueByNameInAnotherTransaction(String name) throws InstanceNotFoundException {
        return findUniqueByName(name);
    }

    @Override
    public boolean existsByNif(String nif) {
        try {
            findUniqueByNif(nif);
            return true;
        } catch (InstanceNotFoundException e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public boolean existsByNifInAnotherTransaction(String nif) {
        return existsByNif(nif);
    }

    @Override
    public ExternalCompany findUniqueByNif(String nif) throws InstanceNotFoundException {

        ExternalCompany found = (ExternalCompany) getSession()
                .createCriteria(ExternalCompany.class)
                .add(Restrictions.eq("nif", nif))
                .uniqueResult();

        if (found == null) {
            throw new InstanceNotFoundException(nif, ExternalCompany.class.getName());
        }

        return found;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public ExternalCompany findUniqueByNifInAnotherTransaction(String nif) throws InstanceNotFoundException {
        return findUniqueByNif(nif);
    }

    @Override
    public List<ExternalCompany> findSubcontractor() {
        return getSession()
                .createCriteria(ExternalCompany.class)
                .add(Restrictions.eq("subcontractor", true))
                .list();
    }

    public List<ExternalCompany> getAll() {
        return list(ExternalCompany.class);
    }

    @Override
    public List<ExternalCompany> getExternalCompaniesAreClient() {
        return getSession()
                .createCriteria(ExternalCompany.class)
                .add(Restrictions.eq("client", true))
                .list();
    }

    @Override
    public boolean isAlreadyInUse(ExternalCompany company) {
        if (company.isNewObject()) {
            return false;
        }

        boolean usedInOrders = !getSession()
                .createCriteria(Order.class)
                .add(Restrictions.eq("customer", company))
                .list()
                .isEmpty();

        boolean usedInSubcontractedTask = !getSession()
                .createCriteria(SubcontractedTaskData.class)
                .add(Restrictions.eq("externalCompany", company))
                .list()
                .isEmpty();

        return usedInOrders || usedInSubcontractedTask;
    }

}

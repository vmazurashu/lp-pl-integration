/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2010-2011 WirelessGalicia, S.L.
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
import org.libreplan.business.externalcompanies.entities.CustomerCommunication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

/**
 * Hibernate DAO for {@link CustomerCommunication}
 *
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
@Repository
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class CustomerCommunicationDAO extends GenericDAOHibernate<CustomerCommunication, Long>
implements ICustomerCommunicationDAO {

    @Override
    public List<CustomerCommunication> getAll(){
        return list(CustomerCommunication.class);
    }

    @Override
    public List<CustomerCommunication> getAllNotReviewed(){
        Criteria c = getSession().createCriteria(CustomerCommunication.class);
        c.add(Restrictions.eq("reviewed", false));
        return c.list();
    }

}

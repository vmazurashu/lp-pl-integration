/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2012 WirelessGalicia, S.L.
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

package org.libreplan.business.expensesheet.daos;

import java.util.List;

import org.hibernate.Query;
import org.libreplan.business.common.daos.IntegrationEntityDAO;
import org.libreplan.business.expensesheet.entities.ExpenseSheet;
import org.libreplan.business.resources.entities.Resource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

/**
 * DAO for {@link ExpenseSheet}
 *
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
@Repository
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class ExpenseSheetDAO extends IntegrationEntityDAO<ExpenseSheet> implements IExpenseSheetDAO {

    @Override
    public List<ExpenseSheet> getAll() {
        return list(ExpenseSheet.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ExpenseSheet> getPersonalExpenseSheetsByResource(
            Resource resource) {
        String hqlQuery = "SELECT DISTINCT sheet "
                + "FROM ExpenseSheet sheet "
                + "JOIN sheet.expenseSheetLines line "
                + "WHERE sheet.personal = TRUE "
                + "AND line.resource = :resource";

        Query query = getSession().createQuery(hqlQuery);
        query.setParameter("resource", resource);

        return query.list();
    }

}

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

package org.libreplan.web.users.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.libreplan.business.expensesheet.daos.IExpenseSheetDAO;
import org.libreplan.business.expensesheet.entities.ExpenseSheet;
import org.libreplan.business.users.entities.User;
import org.libreplan.web.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Model for for "Expenses" area in the user dashboard window
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ExpensesAreaModel implements IExpensesAreaModel {

    @Autowired
    private IExpenseSheetDAO expenseSheetDAO;

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseSheet> getPersonalExpenseSheets() {
        User user = UserUtil.getUserFromSession();
        if (!user.isBound()) {
            return new ArrayList<ExpenseSheet>();
        }

        List<ExpenseSheet> expenseSheets = expenseSheetDAO
                .getPersonalExpenseSheetsByResource(user.getWorker());
        sortExpenseSheetsDescendingByFirstExpense(expenseSheets);

        return expenseSheets;
    }

    private void sortExpenseSheetsDescendingByFirstExpense(
            List<ExpenseSheet> expenseSheets) {
        Collections.sort(expenseSheets, new Comparator<ExpenseSheet>() {
            @Override
            public int compare(ExpenseSheet o1, ExpenseSheet o2) {
                return o2.getFirstExpense().compareTo(o1.getFirstExpense());
            }
        });
    }

}

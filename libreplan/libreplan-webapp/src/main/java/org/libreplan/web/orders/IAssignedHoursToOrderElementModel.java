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

package org.libreplan.web.orders;

import java.math.BigDecimal;
import java.util.List;

import org.libreplan.business.expensesheet.entities.ExpenseSheetLine;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.reports.dtos.WorkReportLineDTO;
import org.libreplan.business.workingday.EffortDuration;

/**
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 * @author Manuel Rego Casasnovas <rego@igalia.com>
 */
public interface IAssignedHoursToOrderElementModel{
    public List<WorkReportLineDTO> getWorkReportLines();

    public EffortDuration getTotalAssignedEffort();

    public EffortDuration getAssignedDirectEffortChildren();
    public void initOrderElement(OrderElement orderElement);

    public EffortDuration getEstimatedEffort();
    public int getProgressWork();

    public EffortDuration getAssignedDirectEffort();

    BigDecimal getBudget();

    BigDecimal getMoneyCost();

    BigDecimal getMoneyCostPercentage();

    public String getTotalDirectExpenses();

    public String getTotalIndirectExpenses();

    public List<ExpenseSheetLine> getExpenseSheetLines();

    public String getTotalExpenses();

    public BigDecimal getCostOfExpenses();

    public BigDecimal getCostOfHours();

    public BigDecimal getCalculatedBudget();

    public BigDecimal getResourcesBudget();

}

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

import org.libreplan.business.expensesheet.entities.ExpenseSheetLine;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.reports.dtos.WorkReportLineDTO;
import org.libreplan.web.common.Util;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Vbox;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller for show the assigned hours of the selected order element.
 * <br />
 *
 * @author Susana Montes Pedreria <smontes@wirelessgalicia.com>
 * @author Manuel Rego Casasnovas <rego@igalia.com>
 */
public class AssignedHoursToOrderElementController extends GenericForwardComposer {

    private IAssignedHoursToOrderElementModel assignedHoursToOrderElementModel;

    private IOrderElementModel orderElementModel;

    private Vbox orderElementHours;

    private Progressmeter hoursProgressBar;

    private Progressmeter exceedHoursProgressBar;

    private Progressmeter moneyCostProgressBar;

    private Progressmeter exceedMoneyCostProgressBar;

    public AssignedHoursToOrderElementController() {
        if ( assignedHoursToOrderElementModel == null ) {
            assignedHoursToOrderElementModel =
                    (IAssignedHoursToOrderElementModel) SpringUtil.getBean("assignedHoursToOrderElementModel");
        }

        if ( orderElementModel == null ) {
            orderElementModel = (IOrderElementModel) SpringUtil.getBean("orderElementModel");
        }
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        comp.setAttribute("assignedHoursToOrderElementController", this, true);
    }

    public List<WorkReportLineDTO> getWorkReportLines() {
        return assignedHoursToOrderElementModel.getWorkReportLines();
    }

    public String getTotalAssignedDirectEffort() {
        return assignedHoursToOrderElementModel.getAssignedDirectEffort().toFormattedString();
    }

    public String getTotalAssignedEffort() {
        return assignedHoursToOrderElementModel.getTotalAssignedEffort().toFormattedString();
    }

    public String getTotalDirectExpenses() {
        return assignedHoursToOrderElementModel.getTotalDirectExpenses();
    }

    public String getTotalIndirectExpenses() {
        return assignedHoursToOrderElementModel.getTotalIndirectExpenses();
    }

    public String getTotalExpenses() {
        return assignedHoursToOrderElementModel.getTotalExpenses();
    }

    public String getEffortChildren() {
        return assignedHoursToOrderElementModel.getAssignedDirectEffortChildren().toFormattedString();
    }

    public String getEstimatedEffort() {
        return assignedHoursToOrderElementModel.getEstimatedEffort().toFormattedString();
    }

    public int getProgressWork() {
        return assignedHoursToOrderElementModel.getProgressWork();
    }

    public BigDecimal getBudget() {
        return assignedHoursToOrderElementModel.getBudget();
    }

    public BigDecimal getResourcesBudget() {
        return assignedHoursToOrderElementModel.getResourcesBudget();
    }

    public BigDecimal getCalculatedBudget() {
        return assignedHoursToOrderElementModel.getCalculatedBudget();
    }

    public BigDecimal getMoneyCost() {
        return assignedHoursToOrderElementModel.getMoneyCost();
    }

    public BigDecimal getCostOfHours() {
        return assignedHoursToOrderElementModel.getCostOfHours();
    }

    public BigDecimal getCostOfExpenses() {
        return assignedHoursToOrderElementModel.getCostOfExpenses();
    }

    public BigDecimal getMoneyCostPercentage() {
        return assignedHoursToOrderElementModel.getMoneyCostPercentage();
    }

    public void openWindow(IOrderElementModel orderElementModel) {
        setOrderElementModel(orderElementModel);
        assignedHoursToOrderElementModel.initOrderElement(getOrderElement());

        if (orderElementHours != null) {
            Util.createBindingsFor(orderElementHours);
            Util.reloadBindings(orderElementHours);
        }

        paintProgressBars();
    }

    void paintProgressBars() {
        viewPercentage();
        showMoneyCostPercentageBars();
    }

    public void setOrderElementModel(IOrderElementModel orderElementModel) {
        this.orderElementModel = orderElementModel;
    }

    private OrderElement getOrderElement() {
        return orderElementModel.getOrderElement();
    }

    /**
     * This method shows the percentage of the imputed hours with respect to the estimated hours.
     * If the hours imputed is greater that the hours estimated then show the exceed percentage of hours.
     */
    private void viewPercentage() {
        if (this.getProgressWork() > 100) {
            hoursProgressBar.setValue(100);

            exceedHoursProgressBar.setVisible(true);
            String exceedValue = String.valueOf(getProgressWork() - 100);
            exceedHoursProgressBar.setWidth(exceedValue + "px");
        } else {
            hoursProgressBar.setValue(getProgressWork());
            exceedHoursProgressBar.setVisible(false);
        }
    }

    private void showMoneyCostPercentageBars() {
        BigDecimal moneyCostPercentage = getMoneyCostPercentage();
        if (moneyCostPercentage.compareTo(new BigDecimal(100)) > 0) {
            moneyCostProgressBar.setValue(100);

            exceedMoneyCostProgressBar.setVisible(true);
            exceedMoneyCostProgressBar.setWidth(moneyCostPercentage.subtract(new BigDecimal(100)).intValue() + "px");
        } else {
            moneyCostProgressBar.setValue(moneyCostPercentage.intValue());
            exceedMoneyCostProgressBar.setVisible(false);
        }
    }

    public List<ExpenseSheetLine> getExpenseSheetLines() {
        return assignedHoursToOrderElementModel.getExpenseSheetLines();
    }

    public String getCurrencySymbol() {
        return Util.getCurrencySymbol();
    }

}

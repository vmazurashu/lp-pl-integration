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

package org.libreplan.web.reports;

import com.libreplan.java.zk.components.JasperreportComponent;
import net.sf.jasperreports.engine.JRDataSource;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.libreplan.business.advance.entities.AdvanceType;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.web.common.Util;
import org.libreplan.web.common.components.bandboxsearch.BandboxSearch;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.libreplan.web.I18nHelper._;

/**
 * @author Diego Pino Garcia <dpino@igalia.com>
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
public class SchedulingProgressPerOrderController extends LibrePlanReportController {

    private static final String REPORT_NAME = "schedulingProgressPerOrderReport";

    private ISchedulingProgressPerOrderModel schedulingProgressPerOrderModel;

    private Listbox lbOrders;

    private Listbox lbAdvanceType;

    private Datebox referenceDate;

    private Datebox startingDate;

    private Datebox endingDate;

    private BandboxSearch bdOrders;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        schedulingProgressPerOrderModel =
                (ISchedulingProgressPerOrderModel) SpringUtil.getBean("schedulingProgressPerOrderModel");

        comp.setAttribute("controller", this, true);
        lbAdvanceType.setSelectedIndex(0);
        schedulingProgressPerOrderModel.init();
    }

    public List<Order> getAllOrders() {
        return schedulingProgressPerOrderModel.getOrders();
    }

    public List<Order> getSelectedOrdersToFilter() {
        return getSelectedOrders().isEmpty()
                ? Collections.unmodifiableList(getAllOrders())
                : getSelectedOrders();
    }

    /**
     * Return selected orders, if none are selected return all orders in listbox
     * @return
     */
    public List<Order> getSelectedOrders() {
        return Collections.unmodifiableList(schedulingProgressPerOrderModel.getSelectedOrders());
    }

    public void onSelectOrder() {
        Order order = (Order) bdOrders.getSelectedElement();
        if (order == null) {
            throw new WrongValueException(bdOrders, _("please, select a project"));
        }

        boolean result = schedulingProgressPerOrderModel.addSelectedOrder(order);
        if (!result) {
            throw new WrongValueException(bdOrders, _("This project has already been added."));
        } else {
            Util.reloadBindings(lbOrders);
        }
        bdOrders.clear();
    }

    public void onRemoveOrder(Order order) {
        schedulingProgressPerOrderModel.removeSelectedOrder(order);
        Util.reloadBindings(lbOrders);
    }

    protected String getReportName() {
        return REPORT_NAME;
    }

    protected JRDataSource getDataSource() {
        List<Order> orders = getSelectedOrdersToFilter();

        return schedulingProgressPerOrderModel.getSchedulingProgressPerOrderReport(
                orders,
                getAdvanceType(),
                startingDate.getValue(),
                endingDate.getValue(),
                new LocalDate(getReferenceDate()));
    }

    public Date getReferenceDate() {
        Date result = referenceDate.getValue();

        if (result == null) {
            referenceDate.setValue(new Date());
        }

        return referenceDate.getValue();
    }

    public Date getStartingDate() {
        return startingDate.getValue();
    }

    public Date getEndingDate() {
        return endingDate.getValue();
    }

    protected Map<String, Object> getParameters() {
        Map<String, Object> result = super.getParameters();

        result.put("referenceDate", getReferenceDate());
        result.put("startingDate", getStartingDate());
        result.put("endingDate", getEndingDate());
        result.put("orderName", getSelectedOrderNames());
        result.put("advanceType", asString(getSelectedAdvanceType()));

        return result;
    }

    public AdvanceTypeDTO getSelectedAdvanceType() {
        return (AdvanceTypeDTO) lbAdvanceType.getSelectedItem().getValue();
    }

    private String asString(AdvanceTypeDTO advanceTypeDTO) {
        return (advanceTypeDTO != null) ? advanceTypeDTO.getName() : _("SPREAD");
    }

    public AdvanceType getAdvanceType() {
        final AdvanceTypeDTO advanceTypeDTO = getSelectedAdvanceType();
        return (advanceTypeDTO != null) ? advanceTypeDTO.getAdvanceType() : null;
    }

    public String getSelectedOrderNames() {
        List<String> orderNames = new ArrayList<>();

        final List<Listitem> listItems = lbOrders.getItems();
        for (Listitem each : listItems) {
            final Order order = each.getValue();
            orderNames.add(order.getName());
        }

        return (!orderNames.isEmpty()) ? StringUtils.join(orderNames, ",") : _("All");
    }

    public List<AdvanceTypeDTO> getAdvanceTypeDTOs() {
        List<AdvanceTypeDTO> result = new ArrayList<>();

        // Add value Spread
        AdvanceTypeDTO advanceTypeDTO = new AdvanceTypeDTO();
        advanceTypeDTO.setAdvanceType(null);
        advanceTypeDTO.setName(_("SPREAD"));
        result.add(advanceTypeDTO);

        final List<AdvanceType> advanceTypes = schedulingProgressPerOrderModel.getAdvanceTypes();
        for (AdvanceType each: advanceTypes) {
            result.add(new AdvanceTypeDTO(each));
        }

        return result;
    }

    public void checkCannotBeHigher(Datebox dbStarting, Datebox dbEnding) {
        dbStarting.clearErrorMessage(true);
        dbEnding.clearErrorMessage(true);

        final Date startingDate = dbStarting.getValue();
        final Date endingDate = dbEnding.getValue();

        if (endingDate != null && startingDate != null && startingDate.compareTo(endingDate) > 0) {
            throw new WrongValueException(dbStarting, _("Cannot be higher than Ending Date"));
        }
    }

    public void showReport(JasperreportComponent jasperreport){
        checkCannotBeHigher(startingDate, endingDate);
        super.showReport(jasperreport);
    }

    public static class AdvanceTypeDTO {

        private String name;

        private AdvanceType advanceType;

        public AdvanceTypeDTO() {}

        public AdvanceTypeDTO(AdvanceType advanceType) {
            this.name = advanceType.getUnitName().toUpperCase();
            this.advanceType = advanceType;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public AdvanceType getAdvanceType() {
            return advanceType;
        }

        public void setAdvanceType(AdvanceType advanceType) {
            this.advanceType = advanceType;
        }

    }

}

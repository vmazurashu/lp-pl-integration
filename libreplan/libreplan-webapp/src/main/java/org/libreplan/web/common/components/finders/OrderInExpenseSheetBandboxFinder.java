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

package org.libreplan.web.common.components.finders;

import org.libreplan.business.orders.daos.IOrderDAO;
import org.libreplan.business.orders.entities.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;

import java.util.List;

/**
 * Bandbox finder for {@link Order} in ExpenseSheet.
 *
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
@Repository
public class OrderInExpenseSheetBandboxFinder extends BandboxFinder implements IBandboxFinder {

    @Autowired
    private IOrderDAO orderDAO;

    private final String headers[] = { _("Project name (Project code)") };

    /**
     * Forces to mark the string as needing translation
     */
    private static String _(String string) {
        return string;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAll() {
        return orderDAO.findAll();
    }

    @Override
    public boolean entryMatchesText(Object obj, String text) {
        Order order = (Order) obj;
        if (order != null) {
            text = text.trim().toLowerCase();
            return (order.getCode().toLowerCase().contains(text) || order.getName().toLowerCase().contains(text));
        }

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public String objectToString(Object obj) {
        Order order = (Order) obj;
        if (order != null) {
            return order.getName() + " :: " + order.getCode();
        }

        return "";
    }

    @Override
    public String[] getHeaders() {
        return headers.clone();
    }

    @Override
    public ListitemRenderer getItemRenderer() {
        return orderRenderer;
    }

    private final ListitemRenderer orderRenderer = new ListitemRenderer() {

        @Override
        public void render(Listitem item, Object data, int i) {
            Order order = (Order) data;
            item.setValue(order);

            String name = "...";
            if (order != null) {
                name = order.getName() + " (" + order.getCode() + ")";
            }

            Listcell orderCode = new Listcell();
            orderCode.setLabel(name);
            orderCode.setParent(item);
        }
    };

}
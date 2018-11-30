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

package org.libreplan.web.orders;

import static org.libreplan.web.I18nHelper._;

import java.util.List;

import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.orders.entities.OrderLine;
import org.libreplan.web.tree.EntitiesTree;

/**
 * Model for a the {@link OrderElement} tree for a {@link Order} <br />
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 * @author Diego Pino García <dpino@igalia.com>
 */
public class OrderElementTreeModel extends EntitiesTree<OrderElement> {

    public OrderElementTreeModel(OrderElement root,
            List<OrderElement> orderElements) {
        super(OrderElement.class, root, orderElements);
    }

    public OrderElementTreeModel(OrderElement root) {
        super(OrderElement.class, root);
    }

    @Override
    protected OrderElement createNewElement() {
        OrderElement newOrderElement = OrderLine
                .createOrderLineWithUnfixedPercentage(0);
        newOrderElement.setName(_("New task"));
        return newOrderElement;
    }

    @Override
    protected OrderElement createNewElement(String name, int hours) {
        OrderLine newOrderElement = OrderLine
                .createOrderLineWithUnfixedPercentage(hours);
        newOrderElement.setName(name);
        return newOrderElement;
    }

}

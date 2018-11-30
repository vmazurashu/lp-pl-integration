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

import java.util.ArrayList;
import java.util.List;

import org.libreplan.business.common.Registry;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.trees.ITreeNode;
import org.libreplan.web.orders.OrderElementTreeController.OrderElementTreeitemRenderer;
import org.libreplan.web.tree.TreeComponent;
import org.libreplan.web.tree.TreeController;
import org.zkoss.zul.Treeitem;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 */
public class OrdersTreeComponent extends TreeComponent {

    protected boolean resourcesBudgetEnabled =
            Registry.getConfigurationDAO().getConfigurationWithReadOnlyTransaction().isEnabledAutomaticBudget();

    abstract class OrdersTreeColumn extends Column {
        OrdersTreeColumn(String label, String cssClass, String tooltip) {
            super(label, cssClass, tooltip);
        }

        @Override
        public <T extends ITreeNode<T>> void doCell(TreeController<T>.Renderer renderer,
                                                    Treeitem item,
                                                    T currentElement) {

            OrderElementTreeitemRenderer treeRenderer = OrderElementTreeitemRenderer.class.cast(renderer);
            doCell(treeRenderer, OrderElement.class.cast(currentElement));
        }

        protected abstract void doCell(OrderElementTreeitemRenderer treeRenderer, OrderElement currentElement);

    }

    public List<Column> getColumns() {
        List<Column> columns = new ArrayList<>();

        columns.add(schedulingStateColumn);
        columns.add(codeColumn);
        columns.add(nameAndDescriptionColumn);

        columns.add(new OrdersTreeColumn(_("Hours"), "hours", _("Total task hours")) {
            @Override
            protected void doCell(OrderElementTreeitemRenderer treeRenderer, OrderElement currentElement) {
                treeRenderer.addHoursCell(currentElement);
            }
        });

        columns.add(new OrdersTreeColumn(_("Budget"), "budget", _("Total task budget")) {
            @Override
            protected void doCell(OrderElementTreeitemRenderer treeRenderer, OrderElement currentElement) {
                treeRenderer.addBudgetCell(currentElement);
            }
        });

        if (resourcesBudgetEnabled) {
            columns.add(new OrdersTreeColumn(_("Expenses"), "budget", _("Budget minus resources costs")) {
                @Override
                protected void doCell(OrderElementTreeitemRenderer treeRenderer, OrderElement currentElement) {
                    treeRenderer.addResourcesBudgetCell(currentElement);
                }
            });
        }

        columns.add(new OrdersTreeColumn(
                _("Must start after"),
                "estimated_init",
                _("Estimated start date for the task " +
                        "(press enter in textbox to open calendar popup or type in date directly)")) {

            @Override
            protected void doCell(OrderElementTreeitemRenderer treeRenderer, OrderElement currentElement) {
                treeRenderer.addInitDateCell(currentElement);
            }
        });

        columns.add(new OrdersTreeColumn(
                _("Deadline"),
                "estimated_end",
                _("Estimated end date for the task " +
                        "(press enter in textbox to open calendar popup or type in date directly)")) {

            @Override
            protected void doCell(OrderElementTreeitemRenderer treeRenderer, OrderElement currentElement) {
                treeRenderer.addEndDateCell(currentElement);
            }
        });

        columns.add(operationsColumn);

        return columns;
    }

    @Override
    public boolean isCreateFromTemplateEnabled() {
        return true;
    }
}

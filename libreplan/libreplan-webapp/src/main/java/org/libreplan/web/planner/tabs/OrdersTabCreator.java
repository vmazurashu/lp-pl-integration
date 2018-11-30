/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
 * Copyright (C) 2010-2013 Igalia, S.L.
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
package org.libreplan.web.planner.tabs;

import static org.libreplan.web.I18nHelper._;
import static org.libreplan.web.planner.tabs.MultipleTabsPlannerController.BREADCRUMBS_SEPARATOR;
import static org.libreplan.web.planner.tabs.MultipleTabsPlannerController.getSchedulingLabel;

import java.util.HashMap;
import java.util.Map;

import org.libreplan.web.common.FilterUtils;
import org.libreplan.web.common.Util;
import org.libreplan.web.common.Util.ReloadStrategy;
import org.libreplan.web.orders.OrderCRUDController;
import org.libreplan.web.planner.order.IOrderPlanningGate;
import org.libreplan.web.planner.tabs.CreatedOnDemandTab.IComponentCreator;
import org.libreplan.web.security.SecurityUtils;
import org.zkoss.ganttz.extensions.ITab;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 *
 */
public class OrdersTabCreator {

    private final String PROJECT_DETAILS = _("Project Details");

    public static ITab create(Mode mode,
                              OrderCRUDController orderCRUDController, Component breadcrumbs,
                              IOrderPlanningGate orderPlanningGate,
                              Map<String, String[]> parameters) {
        return new OrdersTabCreator(mode, orderCRUDController, breadcrumbs,
                orderPlanningGate)
                .build();
    }

    private IComponentCreator ordersTabCreator = new IComponentCreator() {

        private org.zkoss.zk.ui.Component result;

        @Override
        public org.zkoss.zk.ui.Component create(org.zkoss.zk.ui.Component parent) {
            if (result != null) {
                return result;
            }
            Map<String, Object> args = new HashMap<>();
            args.put("orderController", setupOrderCrudController());
            result = Executions.createComponents("/orders/_ordersTab.zul", parent, args);
            Util.createBindingsFor(result);
            Util.reloadBindings(ReloadStrategy.ONE_PER_REQUEST, result);

            return result;
        }

    };

    private final Mode mode;

    private final OrderCRUDController orderCRUDController;

    private final Component breadcrumbs;

    private final IOrderPlanningGate orderPlanningGate;

    private OrdersTabCreator(Mode mode,
                             OrderCRUDController orderCRUDController, Component breadcrumbs,
                             IOrderPlanningGate orderPlanningGate) {
        this.mode = mode;
        this.orderCRUDController = orderCRUDController;
        this.breadcrumbs = breadcrumbs;
        this.orderPlanningGate = orderPlanningGate;
    }

    private ITab build() {
        return TabOnModeType.forMode(mode).forType(ModeType.GLOBAL,
                createGlobalOrdersTab()).forType(ModeType.ORDER,
                createOrderOrdersTab()).create();
    }

    private ITab createGlobalOrdersTab() {
        return new CreatedOnDemandTab(_("Projects List"), "orders",
                ordersTabCreator) {
            @Override
            protected void beforeShowAction() {
                if (!SecurityUtils.isSuperuserOrRolePlanningOrHasAnyAuthorization()) {
                    Util.sendForbiddenStatusCodeInHttpServletResponse();
                }
            }

            private boolean checkFiltersChanged() {
                return (FilterUtils.sessionExists() && FilterUtils
                        .hasProjectPlanningFilterChanged());
            }

            private void setFiltersUnchanged() {
                FilterUtils.writeProjectFilterChanged(false);
            }

            @Override
            protected void afterShowAction() {
                if (checkFiltersChanged()) {
                    orderCRUDController.readSessionFilterDates();
                    setFiltersUnchanged();
                }
                orderCRUDController.goToList();

                if (breadcrumbs.getChildren() != null) {
                    breadcrumbs.getChildren().clear();
                }
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                breadcrumbs.appendChild(new Label(getSchedulingLabel()));
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                breadcrumbs.appendChild(new Label(_("Projects List")));
            }
        };
    }

    private OrderCRUDController setupOrderCrudController() {
        orderCRUDController.setPlanningControllerEntryPoints(orderPlanningGate);
        orderCRUDController.setActionOnUp(new Runnable() {
            public void run() {
                mode.up();
                orderCRUDController.goToList();
            }
        });
        return orderCRUDController;
    }

    private ITab createOrderOrdersTab() {
        return new CreatedOnDemandTab(PROJECT_DETAILS, "order-data",
                ordersTabCreator) {
            @Override
            protected void afterShowAction() {
                breadcrumbs.getChildren().clear();
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                breadcrumbs.appendChild(new Label(getSchedulingLabel()));
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                breadcrumbs.appendChild(new Label(PROJECT_DETAILS));
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));

                if (mode.isOf(ModeType.ORDER)) {
                    orderCRUDController.showOrderElementFilter();
                    orderCRUDController.showCreateButtons(false);
                    orderCRUDController.initEdit(mode.getOrder());
                    breadcrumbs.appendChild(new Label(mode.getOrder().getName()));
                }

            }
        };
    }

}

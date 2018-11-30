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

import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.orders.daos.IOrderDAO;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.planner.entities.TaskElement;
import org.libreplan.web.common.FilterUtils;
import org.libreplan.web.common.Util;
import org.libreplan.web.planner.company.CompanyPlanningController;
import org.libreplan.web.planner.order.OrderPlanningController;
import org.libreplan.web.planner.tabs.CreatedOnDemandTab.IComponentCreator;
import org.libreplan.web.security.SecurityUtils;
import org.zkoss.ganttz.extensions.ICommandOnTask;
import org.zkoss.ganttz.extensions.IContextWithPlannerTask;
import org.zkoss.ganttz.extensions.ITab;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.libreplan.web.I18nHelper._;
import static org.libreplan.web.planner.tabs.MultipleTabsPlannerController.BREADCRUMBS_SEPARATOR;
import static org.libreplan.web.planner.tabs.MultipleTabsPlannerController.getSchedulingLabel;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 */
public class PlanningTabCreator {

    public static final int MAX_ORDERNAME_LENGTH = 90;

    private final Mode mode;

    private final CompanyPlanningController companyPlanningController;

    private final Component breadcrumbs;

    private final OrderPlanningController orderPlanningController;

    private final IOrderDAO orderDAO;

    private final Map<String, String[]> parameters;

    private MultipleTabsPlannerController tabsController;

    public static ITab create(Mode mode,
                              CompanyPlanningController companyPlanningController,
                              OrderPlanningController orderPlanningController,
                              IOrderDAO orderDAO,
                              Component breadcrumbs,
                              Map<String, String[]> parameters,
                              MultipleTabsPlannerController tabsController) {

        return new PlanningTabCreator(mode, companyPlanningController,
                orderPlanningController, breadcrumbs, orderDAO, parameters,
                tabsController).create();
    }

    private PlanningTabCreator(Mode mode,
                               CompanyPlanningController companyPlanningController,
                               OrderPlanningController orderPlanningController,
                               Component breadcrumbs,
                               IOrderDAO orderDAO,
                               Map<String, String[]> parameters,
                               MultipleTabsPlannerController tabsController) {

        this.mode = mode;
        this.companyPlanningController = companyPlanningController;
        this.orderPlanningController = orderPlanningController;
        this.breadcrumbs = breadcrumbs;
        this.orderDAO = orderDAO;
        this.parameters = parameters;
        this.tabsController = tabsController;
    }

    private ITab create() {
        return TabOnModeType.forMode(mode)
                .forType(ModeType.GLOBAL, createGlobalPlanningTab())
                .forType(ModeType.ORDER, createOrderPlanningTab())
                .create();
    }

    private ITab createGlobalPlanningTab() {
        final IComponentCreator componentCreator = new IComponentCreator() {

            @Override
            public org.zkoss.zk.ui.Component create(org.zkoss.zk.ui.Component parent) {
                List<ICommandOnTask<TaskElement>> commands = new ArrayList<>();

                ICommandOnTask<TaskElement> scheduleCommand = buildScheduleCommand();
                commands.add(scheduleCommand);
                ICommandOnTask<TaskElement> orderDetailsCommand = buildOrderDetailsCommand();
                commands.add(orderDetailsCommand);

                // TODO: Revert removal of ResourcesLoad and AdvancedAllocation
                // context entries when save and cancel commands are refactored

                companyPlanningController.setAdditional(commands);
                companyPlanningController.setTabsController(tabsController);
                companyPlanningController.setDoubleClickCommand(scheduleCommand);
                HashMap<String, Object> args = new HashMap<>();

                args.put("companyPlanningController", companyPlanningController);
                companyPlanningController.setURLParameters(parameters);

                return Executions.createComponents("/planner/_company.zul", parent, args);
            }

            private ICommandOnTask<TaskElement> buildScheduleCommand() {
                return new ICommandOnTask<TaskElement>() {

                    @Override
                    public void doAction(IContextWithPlannerTask<TaskElement> context, TaskElement task) {
                        OrderElement orderElement = task.getOrderElement();

                        if (orderElement instanceof Order) {
                            Order order = (Order) orderElement;
                            mode.goToOrderMode(order);
                        }
                    }

                    @Override
                    public String getName() {
                        return _("Schedule");
                    }

                    @Override
                    public String getIcon() {
                        return "/common/img/ico_menu_scheduling.png";
                    }

                    @Override
                    public boolean isApplicableTo(TaskElement task) {
                        return true;
                    }
                };
            }

            private ICommandOnTask<TaskElement> buildOrderDetailsCommand() {
                return new ICommandOnTask<TaskElement>() {

                    @Override
                    public void doAction(IContextWithPlannerTask<TaskElement> context, TaskElement task) {
                        OrderElement orderElement = task.getOrderElement();

                        if (orderElement instanceof Order) {
                            Order order = (Order) orderElement;
                            tabsController.goToOrderDetails(order);
                        }
                    }

                    @Override
                    public String getName() {
                        return _("Project Details");
                    }

                    @Override
                    public String getIcon() {
                        return "/common/img/ico_menu_order-details.png";
                    }

                    @Override
                    public boolean isApplicableTo(TaskElement task) {
                        return true;
                    }
                };
            }

        };
        return new CreatedOnDemandTab(_("Projects Planning"), "company-scheduling", componentCreator) {
            @Override
            protected void beforeShowAction() {
                if (!SecurityUtils.isSuperuserOrRolePlanningOrHasAnyAuthorization()) {
                    Util.sendForbiddenStatusCodeInHttpServletResponse();
                }
            }

            private boolean checkFiltersChanged() {
                return (FilterUtils.sessionExists() && FilterUtils.hasProjectFilterChanged());
            }

            private void setFiltersUnchanged() {
                FilterUtils.writeProjectPlanningFilterChanged(false);
            }

            @Override
            protected void afterShowAction() {
                if (checkFiltersChanged()) {
                    companyPlanningController.readSessionVariablesIntoComponents();
                    setFiltersUnchanged();
                }

                companyPlanningController.setConfigurationForPlanner();
                breadcrumbs.getChildren().clear();
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                breadcrumbs.appendChild(new Label(getSchedulingLabel()));
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                breadcrumbs.appendChild(new Label(_("Projects Planning")));
            }
        };
    }

    private ITab createOrderPlanningTab() {

        final IComponentCreator componentCreator = parent -> {
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("orderPlanningController", orderPlanningController);
            orderPlanningController.setURLParameters(parameters);
            Component result = Executions.createComponents("/planner/order.zul", parent, arguments);
            Util.createBindingsFor(result);
            return result;
        };
        return new CreatedOnDemandTab(_("Project Scheduling"), "order-scheduling", componentCreator) {
            @Override
            protected void afterShowAction() {

                orderPlanningController.setOrder(reload(mode.getOrder()));
                orderPlanningController.setShowedTask(null);
                Order order = orderPlanningController.getOrder();
                Map<String, Object> arguments = new HashMap<>();

                arguments.put("orderPlanningController", orderPlanningController);

                if (breadcrumbs.getChildren() != null) {
                    breadcrumbs.getChildren().clear();
                }
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                breadcrumbs.appendChild(new Label(getSchedulingLabel()));
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                breadcrumbs.appendChild(new Label(_("Project Scheduling")));
                if (mode.isOf(ModeType.ORDER)) {

                    orderPlanningController.getOrderCRUDController().checkUserCanRead(order);
                    Label nameLabel = new Label(order.getName());
                    nameLabel.setTooltiptext(order.getName() + "." + order.getDescription());
                    nameLabel.setMaxlength(MAX_ORDERNAME_LENGTH);

                    Label schedulingStateLabel = new Label(_(order.getState().toString()));

                    schedulingStateLabel.setSclass("scheduling-state " + order.getSchedulingState().getCssClass());
                    schedulingStateLabel.setTooltiptext(_(order.getSchedulingState().getStateName()));

                    breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                    breadcrumbs.appendChild(nameLabel);
                    breadcrumbs.appendChild(schedulingStateLabel);
                }

            }
        };
    }

    protected Order reload(Order order) {
        try {
            return orderDAO.find(order.getId());
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}

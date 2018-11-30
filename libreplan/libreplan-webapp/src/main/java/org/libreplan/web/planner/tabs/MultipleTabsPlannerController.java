/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
 * Copyright (C) 2010-2011 Igalia, S.L.
 *
 * Copyright (C) 2011 WirelessGalicia, S.L.
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

import org.libreplan.business.common.IAdHocTransactionService;
import org.libreplan.business.common.daos.IConfigurationDAO;
import org.libreplan.business.orders.daos.IOrderDAO;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.planner.entities.TaskElement;
import org.libreplan.business.resources.daos.IResourcesSearcher;
import org.libreplan.business.templates.entities.OrderTemplate;
import org.libreplan.business.users.entities.UserRole;
import org.libreplan.web.common.ConfirmCloseUtil;
import org.libreplan.web.common.GatheredUsageStats;
import org.libreplan.web.common.entrypoints.EntryPointsHandler;
import org.libreplan.web.common.entrypoints.URLHandlerRegistry;
import org.libreplan.web.dashboard.DashboardController;
import org.libreplan.web.dashboard.DashboardControllerGlobal;
import org.libreplan.web.limitingresources.LimitingResourcesController;
import org.libreplan.web.logs.LogsController;
import org.libreplan.web.montecarlo.MonteCarloController;
import org.libreplan.web.orders.OrderCRUDController;
import org.libreplan.web.planner.allocation.AdvancedAllocationController.IBack;
import org.libreplan.web.planner.company.CompanyPlanningController;
import org.libreplan.web.planner.order.IOrderPlanningGate;
import org.libreplan.web.planner.order.OrderPlanningController;
import org.libreplan.web.planner.order.PlanningStateCreator;
import org.libreplan.web.resourceload.ResourceLoadController;
import org.libreplan.web.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.zkoss.ganttz.TabSwitcher;
import org.zkoss.ganttz.TabsRegistry;
import org.zkoss.ganttz.TabsRegistry.IBeforeShowAction;
import org.zkoss.ganttz.adapters.State;
import org.zkoss.ganttz.adapters.TabsConfiguration;
import org.zkoss.ganttz.adapters.TabsConfiguration.ChangeableTab;
import org.zkoss.ganttz.extensions.ITab;
import org.zkoss.ganttz.extensions.TabProxy;
import org.zkoss.ganttz.util.LongOperationFeedback;
import org.zkoss.ganttz.util.LongOperationFeedback.ILongOperation;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Composer;
import org.zkoss.zkplus.spring.SpringUtil;

import java.util.Map;

import static org.libreplan.web.I18nHelper._;
import static org.zkoss.ganttz.adapters.TabsConfiguration.configure;

/**
 * Creates and handles several tabs.
 *
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 * @author Vova Perebykivskyi <vova@libreplan-enterprise.com>
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class MultipleTabsPlannerController implements Composer, IGlobalViewEntryPoints {

    /**
     * URL used for Email Template functionality
     */
    public static String WELCOME_URL = "-- no URL provided --";

    public static final String BREADCRUMBS_SEPARATOR = "/common/img/migas_separacion.gif";

    private Mode mode = Mode.initial();

    private CompanyPlanningController companyPlanningController;

    private OrderCRUDController orderCRUDController;

    private PlanningStateCreator planningStateCreator;

    /**
     * Projects Planning.
     */
    private TabWithLoadingFeedback planningTab;

    /**
     * Resources Load.
     */
    private ITab resourceLoadTab;

    /**
     * Queue-based Resources Planning.
     */
    private ITab limitingResourcesTab;

    /**
     * Monte Carlo Method.
     */
    private ITab monteCarloTab;

    /**
     * Projects List.
     */
    private ITab ordersTab;

    /**
     * Advanced Allocation.
     */
    private ITab advancedAllocationTab;

    /**
     * Dashboard.
     */
    private ITab dashboardTab;

    /**
     * Logs.
     */
    private ITab logsTab;

    private TabSwitcher tabsSwitcher;

    private org.zkoss.zk.ui.Component breadcrumbs;

    @Autowired
    private OrderPlanningController orderPlanningController;

    @Autowired
    private ResourceLoadController resourceLoadController;

    @Autowired
    private ResourceLoadController resourceLoadControllerGlobal;

    @Autowired
    private MonteCarloController monteCarloController;

    @Autowired
    private LimitingResourcesController limitingResourcesControllerGlobal;

    @Autowired
    private DashboardController dashboardController;

    @Autowired
    private DashboardControllerGlobal dashboardControllerGlobal;

    @Autowired
    private LogsController logsController;

    @Autowired
    private  LogsController logsControllerGlobal;

    @Autowired
    private IAdHocTransactionService transactionService;

    @Autowired
    private IOrderDAO orderDAO;

    @Autowired
    private IResourcesSearcher resourcesSearcher;

    @Autowired
    private IConfigurationDAO configurationDAO;

    @Autowired
    private URLHandlerRegistry registry;

    private final class TabWithLoadingFeedback extends TabProxy {

        private boolean feedback = true;

        private TabWithLoadingFeedback(ITab tab) {
            super(tab);
        }

        @Override
        public void show() {
            if ( feedback ) {
                showWithFeedback();
            } else {
                showWithoutFeedback();
            }
        }

        private void showWithFeedback() {
            LongOperationFeedback.execute(tabsSwitcher, new ILongOperation() {
                @Override
                public String getName() {
                    return _("changing perspective");
                }

                @Override
                public void doAction() {
                    proxiedTab.show();
                }
            });
        }

        private void showWithoutFeedback() {
            proxiedTab.show();
        }

        public void toggleToNoFeedback() {
            feedback = false;
        }

        public void toggleToFeedback() {
            feedback = true;
        }
    }

    public static String getSchedulingLabel() {
        return _("Planning");
    }

    public MultipleTabsPlannerController() {
        orderCRUDController = (OrderCRUDController) SpringUtil.getBean("orderCRUDController");
        companyPlanningController = (CompanyPlanningController) SpringUtil.getBean("companyPlanningController");
        planningStateCreator = (PlanningStateCreator) SpringUtil.getBean("planningStateCreator");
    }


    private TabsConfiguration buildTabsConfiguration(final Desktop desktop) {

        Map<String, String[]> parameters = getURLQueryParametersMap();

        mode.addListener((oldType, newType) -> {
            switch (newType) {

                case GLOBAL:
                    ConfirmCloseUtil.resetConfirmClose();
                    break;

                case ORDER:
                    if ( SecurityUtils.loggedUserCanWrite(mode.getOrder()) ) {
                        ConfirmCloseUtil.setConfirmClose(
                                desktop,
                                _("You are about to leave the planning editing. Unsaved changes will be lost!"));
                    }
                    break;

                default:
                    break;
            }
        });

        planningTab = doFeedbackOn(PlanningTabCreator.create(
                mode, companyPlanningController, orderPlanningController, orderDAO, breadcrumbs, parameters, this));

        resourceLoadTab = ResourcesLoadTabCreator.create(
                mode,
                resourceLoadController,
                resourceLoadControllerGlobal,
                new IOrderPlanningGate() {
                    @Override
                    public void goToScheduleOf(Order order) {
                        getTabsRegistry().show(planningTab, changeModeTo(order));
                    }

                    @Override
                    public void goToOrderDetails(Order order) {}

                    @Override
                    public void goToTaskResourceAllocation(Order order, TaskElement task) {
                        orderPlanningController.setShowedTask(task);

                        orderPlanningController
                                .setCurrentControllerToShow(orderPlanningController.getEditTaskController());

                        getTabsRegistry().show(planningTab, changeModeTo(order));
                    }

                    @Override
                    public void goToDashboard(Order order) {}
                },
                breadcrumbs);

        limitingResourcesTab = LimitingResourcesTabCreator.create(mode, limitingResourcesControllerGlobal, breadcrumbs);

        ordersTab = OrdersTabCreator.create(
                mode,
                orderCRUDController,
                breadcrumbs,
                new IOrderPlanningGate() {
                    @Override
                    public void goToScheduleOf(Order order) {
                        getTabsRegistry().show(planningTab, changeModeTo(order));
                    }

                    @Override
                    public void goToOrderDetails(Order order) {
                        getTabsRegistry().show(ordersTab, changeModeTo(order));
                    }

                    @Override
                    public void goToTaskResourceAllocation(Order order, TaskElement task) {}

                    @Override
                    public void goToDashboard(Order order) {}
                },
                parameters);

        dashboardTab = DashboardTabCreator.create(
                mode,
                planningStateCreator,
                dashboardController,
                dashboardControllerGlobal,
                orderPlanningController,
                breadcrumbs,
                resourcesSearcher);

        logsTab = LogsTabCreator.create(
                mode,
                logsController,
                logsControllerGlobal,
                breadcrumbs,
                new IOrderPlanningGate() {
                    @Override
                    public void goToScheduleOf(Order order) {
                        getTabsRegistry().show(planningTab, changeModeTo(order));
                    }

                    @Override
                    public void goToOrderDetails(Order order) {
                        getTabsRegistry().show(ordersTab, changeModeTo(order));
                    }

                    @Override
                    public void goToTaskResourceAllocation(Order order, TaskElement task) {}

                    @Override
                    public void goToDashboard(Order order) {}
                },
                parameters);

        final boolean isMontecarloVisible = isMonteCarloVisible();
        if (isMontecarloVisible) {

            monteCarloTab = MonteCarloTabCreator.create(
                    mode,
                    planningStateCreator,
                    monteCarloController,
                    orderPlanningController,
                    breadcrumbs,
                    resourcesSearcher);
        }

        final State<Void> typeChanged = typeChangedState();

        advancedAllocationTab = doFeedbackOn(AdvancedAllocationTabCreator.create(
                mode, transactionService, planningStateCreator, returnToPlanningTab(), breadcrumbs));

        TabsConfiguration tabsConfiguration = TabsConfiguration
                .create()
                .add(tabWithNameReloading(planningTab, typeChanged))
                .add(tabWithNameReloading(ordersTab, typeChanged));

        if (SecurityUtils.isSuperuserOrUserInRoles(UserRole.ROLE_PLANNING)) {

            tabsConfiguration
                    .add(tabWithNameReloading(resourceLoadTab, typeChanged))
                    .add(tabWithNameReloading(limitingResourcesTab, typeChanged));

        } else {
            tabsConfiguration.add(visibleOnlyAtOrderModeWithNameReloading(resourceLoadTab, typeChanged));
        }

        tabsConfiguration
                .add(visibleOnlyAtOrderMode(advancedAllocationTab))
                .add(tabWithNameReloading(dashboardTab, typeChanged));

        if (isMontecarloVisible) {
            tabsConfiguration.add(visibleOnlyAtOrderMode(monteCarloTab));
        }

        tabsConfiguration.add(tabWithNameReloading(logsTab, typeChanged));

        return tabsConfiguration;
    }

    private boolean isMonteCarloVisible() {
        Boolean result = configurationDAO.getConfiguration().isMonteCarloMethodTabVisible();
        return result != null && result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String[]> getURLQueryParametersMap() {
        return Executions.getCurrent().getParameterMap();
    }

    private TabWithLoadingFeedback doFeedbackOn(ITab tab) {
        return new TabWithLoadingFeedback(tab);
    }

    private IBack returnToPlanningTab() {
        return new IBack() {

            private String eventName = "onShowPlanningTab";

            {
                tabsSwitcher.addEventListener(eventName, showPlanningTab());
            }

            private EventListener showPlanningTab() {
                return event -> getTabsRegistry().show(planningTab);
            }

            @Override
            public void goBack() {
                notGoBackImmediately();
            }

            private void notGoBackImmediately() {
                Events.postEvent(new Event(eventName, tabsSwitcher));
            }

            @Override
            public boolean isAdvanceAssignmentOfSingleTask() {
                return false;
            }
        };
    }

    private ChangeableTab tabWithNameReloading(ITab tab, final State<Void> typeChanged) {
        return configure(tab).reloadNameOn(typeChanged);
    }

    private State<Void> typeChangedState() {
        final State<Void> typeChanged = State.create();
        mode.addListener((oldType, newType) -> typeChanged.changeValueTo(null));

        return typeChanged;
    }

    private ChangeableTab visibleOnlyAtOrderMode(ITab tab) {
        return visibleOnlyAtOrderModeWithNameReloading(tab, null);
    }

    private ChangeableTab visibleOnlyAtOrderModeWithNameReloading(ITab tab, final State<Void> typeChanged) {
        final State<Boolean> state = State.create(mode.isOf(ModeType.ORDER));
        ChangeableTab result;

        if (typeChanged == null) {
            result = configure(tab).visibleOn(state);
        } else {
            result = configure(tab).visibleOn(state).reloadNameOn(typeChanged);
        }
        mode.addListener((oldType, newType) -> state.changeValueTo(ModeType.ORDER == newType));

        return result;
    }

    @Override
    @Transactional(readOnly=true)
    public void doAfterCompose(org.zkoss.zk.ui.Component comp) {

        Execution execution = Executions.getCurrent();
        WELCOME_URL = "http://" + execution.getServerName() + ":" +
                execution.getServerPort() + Executions.encodeURL("/planner/index.zul");

        tabsSwitcher = (TabSwitcher) comp;
        breadcrumbs = comp.getPage().getFellow("breadcrumbs");

        tabsSwitcher.setConfiguration(buildTabsConfiguration(comp.getDesktop()));

        final EntryPointsHandler<IGlobalViewEntryPoints> handler =
                registry.getRedirectorFor(IGlobalViewEntryPoints.class);

        if (!handler.applyIfMatches(this)) {
            planningTab.toggleToNoFeedback();
            goToCompanyScheduling();
            planningTab.toggleToFeedback();
        }
        handler.registerBookmarkListener(this, comp.getPage());

        if (SecurityUtils.isSuperuserOrUserInRoles(UserRole.ROLE_CREATE_PROJECTS)) {
            org.zkoss.zk.ui.Component createOrderButton = comp.getPage().getFellowIfAny("createOrderButton");
            if (createOrderButton != null) {
                createOrderButton.addEventListener(Events.ON_CLICK, event -> goToCreateForm());

            }
        }

        // Send data to server
        if (!SecurityUtils.isGatheredStatsAlreadySent
                && (configurationDAO.getConfigurationWithReadOnlyTransaction() == null || configurationDAO.getConfigurationWithReadOnlyTransaction().isAllowedToGatherUsageStatsEnabled())) {
            sendDataToServer();
        }

    }

    private void sendDataToServer() {
        GatheredUsageStats gatheredUsageStats = new GatheredUsageStats();
        gatheredUsageStats.sendGatheredUsageStatsToServer();
        SecurityUtils.isGatheredStatsAlreadySent = true;
    }

    private TabsRegistry getTabsRegistry() {
        return tabsSwitcher.getTabsRegistry();
    }

    @Override
    public void goToCompanyScheduling() {
        LogsController.goToGlobalMode();
        getTabsRegistry().show(planningTab);
    }

    @Override
    public void goToCompanyLoad() {
        getTabsRegistry().show(resourceLoadTab);
    }

    @Override
    public void goToCompanyLimitingResources() {
        getTabsRegistry().show(limitingResourcesTab);
    }

    @Override
    public void goToOrdersList() {
        getTabsRegistry().show(ordersTab);
    }

    public void goToCreateForm() {
        orderCRUDController.prepareForCreate(tabsSwitcher.getDesktop());
        orderCRUDController.getCreationPopup().showWindow(orderCRUDController, this);
    }

    @Override
    public void goToOrder(Order order) {
        planningTab.toggleToNoFeedback();
        getTabsRegistry().show(planningTab, changeModeTo(order));
        planningTab.toggleToFeedback();
    }

    @Override
    public void goToOrderElementDetails(Order order, OrderElement orderElement) {
        getTabsRegistry().show(ordersTab, changeModeTo(order));
        orderCRUDController.highLight(orderElement);
    }

    @Override
    public void goToLimitingResources() {
        getTabsRegistry().show(limitingResourcesTab);
    }

    @Override
    public void goToOrderDetails(Order order) {
        getTabsRegistry().show(ordersTab, changeModeTo(order));
    }

    @Override
    public void goToResourcesLoad(Order order) {
        getTabsRegistry().show(resourceLoadTab, changeModeTo(order));
    }

    @Override
    public void goToAdvancedAllocation(Order order) {
        getTabsRegistry().show(advancedAllocationTab, changeModeTo(order));
    }

    @Override
    public void goToCreateOtherOrderFromTemplate(OrderTemplate template) {
        getTabsRegistry().show(ordersTab);
        orderCRUDController.showCreateFormFromTemplate(template);
    }

    @Override
    public void goToAdvanceTask(Order order,TaskElement task) {
        orderPlanningController.setShowedTask(task);

        orderPlanningController.setCurrentControllerToShow(
                orderPlanningController.getAdvanceAssignmentPlanningController());

        getTabsRegistry().show(planningTab, changeModeTo(order));
    }

    private IBeforeShowAction changeModeTo(final Order order) {
        /* Makes possible to show Risk/Issue Logs only for specific Order */
        LogsController.goToOrderMode(order);

        return () -> mode.goToOrderMode(order);
    }
}

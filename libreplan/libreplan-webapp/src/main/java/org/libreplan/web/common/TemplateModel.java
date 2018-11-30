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

package org.libreplan.web.common;

import org.apache.commons.lang3.Validate;
import org.joda.time.LocalDate;
import org.libreplan.business.common.IAdHocTransactionService;
import org.libreplan.business.common.IOnTransaction;
import org.libreplan.business.common.Registry;
import org.libreplan.business.common.daos.IConfigurationDAO;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.orders.entities.TaskSource;
import org.libreplan.business.planner.daos.ITaskSourceDAO;
import org.libreplan.business.planner.entities.Dependency;
import org.libreplan.business.planner.entities.Dependency.Type;
import org.libreplan.business.planner.entities.Task;
import org.libreplan.business.planner.entities.TaskElement;
import org.libreplan.business.resources.daos.IResourcesSearcher;
import org.libreplan.business.scenarios.daos.IOrderVersionDAO;
import org.libreplan.business.scenarios.daos.IScenarioDAO;
import org.libreplan.business.scenarios.entities.OrderVersion;
import org.libreplan.business.scenarios.entities.Scenario;
import org.libreplan.business.users.daos.IUserDAO;
import org.libreplan.business.users.entities.User;
import org.libreplan.web.UserUtil;
import org.libreplan.web.planner.tabs.GanttDiagramBuilder;
import org.libreplan.web.security.SecurityUtils;
import org.libreplan.web.users.bootstrap.PredefinedUsers;
import org.libreplan.web.users.services.CustomUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zkoss.ganttz.data.ConstraintCalculator;
import org.zkoss.ganttz.data.DependencyType;
import org.zkoss.ganttz.data.DependencyType.Point;
import org.zkoss.ganttz.data.GanttDate;
import org.zkoss.ganttz.data.IDependency;
import org.zkoss.ganttz.data.constraint.Constraint;
import org.zkoss.ganttz.util.LongOperationFeedback;
import org.zkoss.ganttz.util.LongOperationFeedback.IBackGroundOperation;
import org.zkoss.ganttz.util.LongOperationFeedback.IDesktopUpdate;
import org.zkoss.ganttz.util.LongOperationFeedback.IDesktopUpdatesEmitter;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.util.Clients;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import static org.libreplan.web.I18nHelper._;

/**
 * Model to manage UI operations from main template.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class TemplateModel implements ITemplateModel {

    @Autowired
    private IScenarioDAO scenarioDAO;

    @Autowired
    private IOrderVersionDAO orderVersionDAO;

    @Autowired
    private ITaskSourceDAO taskSourceDAO;

    @Autowired
    private IUserDAO userDAO;

    @Autowired
    private IResourcesSearcher resourcesSearcher;

    @Autowired
    private IAdHocTransactionService transactionService;

    @Autowired
    private IConfigurationDAO configurationDAO;

    public static class DependencyWithVisibility implements IDependency<TaskElement> {

        private final TaskElement source;

        private final TaskElement destination;

        private final DependencyType type;

        private final boolean visible;

        private DependencyWithVisibility(TaskElement source,
                                         TaskElement destination,
                                         DependencyType type,
                                         boolean visible) {

            Validate.notNull(source);
            Validate.notNull(destination);
            Validate.notNull(type);
            this.source = source;
            this.destination = destination;
            this.type = type;
            this.visible = visible;
        }

        static DependencyWithVisibility createInvisible(TaskElement source,
                                                        TaskElement destination,
                                                        DependencyType type) {

            return new DependencyWithVisibility(source, destination, type, false);
        }

        public static DependencyWithVisibility existent(Dependency each) {
            return new DependencyWithVisibility(
                    each.getOrigin(), each.getDestination(), toGraphicalType(each.getType()), true);
        }

        static List<Constraint<GanttDate>> getConstraints(ConstraintCalculator<TaskElement> calculator,
                                                          Set<DependencyWithVisibility> withDependencies,
                                                          Point point) {

            List<Constraint<GanttDate>> result = new ArrayList<>();
            for (DependencyWithVisibility each : withDependencies) {
                result.addAll(calculator.getConstraints(each, point));
            }

            return result;
        }

        public boolean isVisible() {
            return visible;
        }

        @Override
        public TaskElement getSource() {
            return source;
        }

        @Override
        public TaskElement getDestination() {
            return destination;
        }

        @Override
        public DependencyType getType() {
            return type;
        }

        private static DependencyType toGraphicalType(Type domainDependencyType) {
            switch (domainDependencyType) {
                case END_START:
                    return DependencyType.END_START;

                case START_START:
                    return DependencyType.START_START;

                case END_END:
                    return DependencyType.END_END;

                case START_END:
                    return DependencyType.START_END;

                default:
                    throw new RuntimeException("can't handle " + domainDependencyType);
            }
        }

    }

    @Override
    @Transactional(readOnly = true)
    public List<Scenario> getScenarios() {
        return scenarioDAO.getAll();
    }

    @Override
    @Transactional(readOnly = true)
    public String getCompanyLogoURL() {
        return configurationDAO.getConfiguration().getCompanyLogoURL();
    }

    @Override
    @Transactional
    public void setScenario(String loginName, Scenario scenario, IOnFinished onFinish) {
        Scenario scenarioReloaded = reloadScenario(scenario);
        associateToUser(scenarioReloaded, findUserByLoginName(loginName));
        doReassignations(scenarioReloaded, onFinish);
    }

    private Scenario reloadScenario(Scenario scenario) {
        return scenarioDAO.findExistingEntity(scenario.getId());
    }

    private User findUserByLoginName(String loginName) {
        try {
            return userDAO.findByLoginName(loginName);
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void associateToUser(Scenario scenario, User user) {
        user.setLastConnectedScenario(scenario);
        userDAO.save(user);
        CustomUser customUser = SecurityUtils.getLoggedUser();
        assert customUser != null : "user must be logged for this method to be called";
        customUser.setScenario(scenario);
    }

    private void doReassignations(final Scenario scenario, IOnFinished onFinish) {
        if (isOnZKExecution()) {
            doReassignationsWithFeedback(getDesktop(), scenario, onFinish);
        } else {
            doReassignations(scenario, LongOperationFeedback.doNothingEmitter());
            onFinish.onWithoutErrorFinish();
        }
    }

    private boolean isOnZKExecution() {
        Execution current = Executions.getCurrent();

        return current != null && current.getDesktop() != null;
    }

    private Desktop getDesktop() {
        return Executions.getCurrent().getDesktop();
    }

    private void doReassignationsWithFeedback(Desktop desktop, final Scenario scenario, final IOnFinished onFinish) {
        IBackGroundOperation<IDesktopUpdate> reassignations = new IBackGroundOperation<IDesktopUpdate>() {
            @Override
            public void doOperation(final IDesktopUpdatesEmitter<IDesktopUpdate> desktopUpdateEmitter) {
                Exception exceptionHappened = null;
                try {
                    transactionService.runOnTransaction(new IOnTransaction<Void>() {
                        @Override
                        public Void execute() {
                            doReassignations(reloadScenario(scenario), desktopUpdateEmitter);

                            return null;
                        }
                    });

                } catch (Exception e) {
                    exceptionHappened = e;
                } finally {
                    desktopUpdateEmitter.doUpdate(showEnd());
                }

                if (exceptionHappened == null) {
                    desktopUpdateEmitter.doUpdate(notifySuccess(onFinish));
                } else {
                    desktopUpdateEmitter.doUpdate(notifyException(onFinish, exceptionHappened));
                }
            }

        };
        LongOperationFeedback.progressive(desktop, reassignations);
    }

    private IDesktopUpdate notifySuccess(final IOnFinished onFinish) {
        return new IDesktopUpdate() {
            @Override
            public void doUpdate() {
                onFinish.onWithoutErrorFinish();
            }
        };
    }

    private IDesktopUpdate notifyException(final IOnFinished onFinish, final Exception exceptionHappened) {
        return new IDesktopUpdate() {
            @Override
            public void doUpdate() {
                onFinish.errorHappened(exceptionHappened);
            }
        };
    }

    private void doReassignations(Scenario scenario, IDesktopUpdatesEmitter<IDesktopUpdate> emitter) {
        List<Entry<Order, OrderVersion>> needingReassignation = scenario.getOrderVersionsNeedingReassignation();
        final int total = needingReassignation.size();

        if (!needingReassignation.isEmpty()) {
            emitter.doUpdate(showStart(total));
        }
        int i = 1;
        for (Entry<Order, OrderVersion> each : needingReassignation) {
            OrderVersion orderVersion = each.getValue();
            Order order = each.getKey();
            order.useSchedulingDataFor(scenario);
            if (order.isScheduled()) {
                doReassignationsOn(order, orderVersion.getOwnerScenario(), scenario);
                orderVersion.savingThroughOwner();
                orderVersionDAO.save(orderVersion);
            }
            emitter.doUpdate(showProgress(total - i));
        }

    }

    private IDesktopUpdate showStart(final int ordersNumber) {
        return sendMessage(_("Reassigning {0} projects", ordersNumber));
    }

    private IDesktopUpdate showProgress(int remaining) {
        return sendMessage(_("{0} projects remaining to reassign", remaining));
    }

    private IDesktopUpdate sendMessage(final String message) {
        return () -> Clients.showBusy((Component) new Object(), message);
    }

    private IDesktopUpdate showEnd() {
        return () -> Clients.showBusy(null, "");
    }

    private void doReassignationsOn(Order order, Scenario from, Scenario to) {
        copyAssignments(order, from, to);

        GanttDiagramBuilder.createForcingDependencies(
                order,
                TemplateModelAdapter.create(
                        to,
                        asLocalDate(order.getInitDate()),
                        asLocalDate(order.getDeadline()),
                        resourcesSearcher));

        doReassignations(order, to);
        doTheSaving(order);
    }

    private LocalDate asLocalDate(Date date) {
        return date != null ? LocalDate.fromDateFields(date) : null;
    }

    private void copyAssignments(Order order, Scenario from, Scenario to) {
        for (Task each : getTasksFrom(order)) {
            each.copyAssignmentsFromOneScenarioToAnother(from, to);
        }
    }

    private void doReassignations(Order order, Scenario scenario) {
        for (Task each : getTasksFrom(order)) {
            each.reassignAllocationsWithNewResources(scenario, resourcesSearcher);
        }
    }

    private void doTheSaving(Order order) {
        for (TaskSource each : order.getTaskSourcesFromBottomToTop()) {
            taskSourceDAO.save(each);
        }
    }

    private List<Task> getTasksFrom(Order order) {
        List<Task> result = new ArrayList<>();
        for (TaskElement each : getTaskElementsFrom(order)) {
            if (each instanceof Task) {
                result.add((Task) each);
            }
        }

        return result;
    }

    private List<TaskElement> getTaskElementsFrom(Order order) {
        List<TaskElement> result = new ArrayList<>();
        for (TaskSource each : order.getTaskSourcesFromBottomToTop()) {
            result.add(each.getTask());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isScenariosVisible() {
        return configurationDAO.getConfiguration().isScenariosVisible();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasChangedDefaultPassword(PredefinedUsers user) {
        return user.hasChangedDefaultPasswordOrDisabled();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean adminPasswordChangedAndSomeOtherNotChanged() {
        return PredefinedUsers.adminChangedAndSomeOtherNotChanged();
    }

    @Override
    @Transactional(readOnly = true)
    public String getIdUser(String login) {
        try {
            return Registry.getUserDAO().findByLoginName(login).getId().toString();
        } catch (InstanceNotFoundException e) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserAdmin() {
        User user = UserUtil.getUserFromSession();

        return user != null && user.isSuperuser();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCheckNewVersionEnabled() {
        return configurationDAO.getConfiguration().isCheckNewVersionEnabled();
    }

}

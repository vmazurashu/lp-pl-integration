/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
 * Copyright (C) 2010-2013 Igalia, S.L.
 * Copyright (C) 2010-2011 WirelessGalicia S.L.
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

package org.libreplan.web.planner.order;

import static org.libreplan.web.I18nHelper._;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.planner.entities.TaskElement;
import org.libreplan.web.common.FilterUtils;
import org.libreplan.web.common.ViewSwitcher;
import org.libreplan.web.common.components.bandboxsearch.BandboxMultipleSearch;
import org.libreplan.web.common.components.finders.FilterPair;
import org.libreplan.web.orders.OrderCRUDController;
import org.libreplan.web.planner.TaskElementPredicate;
import org.libreplan.web.planner.advances.AdvanceAssignmentPlanningController;
import org.libreplan.web.planner.calendar.CalendarAllocationController;
import org.libreplan.web.planner.consolidations.AdvanceConsolidationController;
import org.libreplan.web.planner.taskedition.AdvancedAllocationTaskController;
import org.libreplan.web.planner.taskedition.EditTaskController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.zkoss.ganttz.FilterAndParentExpandedPredicates;
import org.zkoss.ganttz.Planner;
import org.zkoss.ganttz.data.Task;
import org.zkoss.ganttz.extensions.ContextWithPlannerTask;
import org.zkoss.ganttz.extensions.ICommand;
import org.zkoss.ganttz.extensions.IContext;
import org.zkoss.ganttz.extensions.IContextWithPlannerTask;
import org.zkoss.ganttz.timetracker.zoom.ZoomLevel;
import org.zkoss.ganttz.util.LongOperationFeedback;
import org.zkoss.ganttz.util.LongOperationFeedback.ILongOperation;
import org.zkoss.ganttz.util.ProfilingLogFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.util.Composer;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Constraint;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class OrderPlanningController implements Composer {

    private static final Log PROFILING_LOG = ProfilingLogFactory.getLog(OrderPlanningController.class);

    @Autowired
    private ViewSwitcher viewSwitcher;

    private Map<String, String[]> parameters;

    @Autowired
    private IOrderPlanningModel model;

    private Planner planner;

    @Autowired
    private CalendarAllocationController calendarAllocationController;

    @Autowired
    private EditTaskController editTaskController;

    @Autowired
    private AdvancedAllocationTaskController advancedAllocationTaskController;

    @Autowired
    private AdvanceConsolidationController advanceConsolidationController;

    @Autowired
    private AdvanceAssignmentPlanningController advanceAssignmentPlanningController;

    @Autowired
    private OrderCRUDController orderCRUDController;

    private GenericForwardComposer currentControllerToShow;

    private Order order;

    private TaskElement task;

    private List<ICommand<TaskElement>> additional = new ArrayList<>();

    private Vbox orderElementFilter;

    private Datebox filterStartDateOrderElement;

    private Datebox filterFinishDateOrderElement;

    private Checkbox labelsWithoutInheritance;

    private BandboxMultipleSearch bdFiltersOrderElement;

    private Textbox filterNameOrderElement;

    private Popup filterOptionsPopup;

    public OrderPlanningController() {

    }

    public List getCriticalPath() {
        return planner != null ? planner.getCriticalPath() : null;
    }

    @SafeVarargs
    public final void setOrder(Order order, ICommand<TaskElement>... additionalCommands) {
        Validate.notNull(additionalCommands);
        Validate.noNullElements(additionalCommands);
        this.order = order;
        this.additional = Arrays.asList(additionalCommands);
        if (planner != null) {
            ensureIsInPlanningOrderView();
            updateConfiguration();
            planner.setTaskListPredicate(getFilterAndParentExpanedPredicates(createPredicate()));
        }
    }

    public void setShowedTask(TaskElement task) {
        this.task = task;
    }

    public CalendarAllocationController getCalendarAllocationController() {
        return calendarAllocationController;
    }

    private void ensureIsInPlanningOrderView() {
        viewSwitcher.goToPlanningOrderView();
    }

    public ViewSwitcher getViewSwitcher() {
        return viewSwitcher;
    }

    @Override
    public void doAfterCompose(org.zkoss.zk.ui.Component comp) {
        this.planner = (Planner) comp;
        String zoomLevelParameter = null;
        if ((parameters != null) && (parameters.get("zoom") != null) && !(parameters.isEmpty())) {
            zoomLevelParameter = parameters.get("zoom")[0];
        }
        if (zoomLevelParameter != null) {
            planner.setInitialZoomLevel(ZoomLevel.getFromString(zoomLevelParameter));
        }
        planner.setAreContainersExpandedByDefault(Planner.guessContainersExpandedByDefault(parameters));

        planner.setAreShownAdvancesByDefault(Planner.guessShowAdvancesByDefault(parameters));

        planner.setAreShownReportedHoursByDefault(Planner.guessShowReportedHoursByDefault(parameters));
        planner.setAreShownMoneyCostBarByDefault(Planner.guessShowMoneyCostBarByDefault(parameters));

        orderElementFilter = (Vbox) planner.getFellow("orderElementFilter");
        // Configuration of the order filter
        org.zkoss.zk.ui.Component filterComponent = Executions
                .createComponents("/orders/_orderElementTreeFilter.zul",
                        orderElementFilter, new HashMap<String, String>());
        filterComponent.setAttribute("treeController", this, true);
        filterOptionsPopup = (Popup) filterComponent.getFellow("filterOptionsPopup");
        filterStartDateOrderElement = (Datebox) filterOptionsPopup.getFellow("filterStartDateOrderElement");
        filterFinishDateOrderElement = (Datebox) filterOptionsPopup.getFellow("filterFinishDateOrderElement");
        labelsWithoutInheritance = (Checkbox) filterOptionsPopup.getFellow("labelsWithoutInheritance");
        bdFiltersOrderElement = (BandboxMultipleSearch) filterComponent.getFellow("bdFiltersOrderElement");
        bdFiltersOrderElement.setFinder("taskElementsMultipleFiltersFinder");
        filterNameOrderElement = (Textbox) filterComponent.getFellow("filterNameOrderElement");
        filterComponent.setVisible(true);
        updateConfiguration();
    }

    private void updateConfiguration() {
        if (order != null) {
            importOrderFiltersFromSession();

            long time = System.currentTimeMillis();
            model.setConfigurationToPlanner(planner, order, viewSwitcher,
                    editTaskController, advancedAllocationTaskController,
                    advanceAssignmentPlanningController,
                    advanceConsolidationController,
                    calendarAllocationController, additional);
            PROFILING_LOG.debug("setConfigurationToPlanner took: " + (System.currentTimeMillis() - time) + " ms");
            planner.updateSelectedZoomLevel();
            showResorceAllocationIfIsNeeded();

        }
    }

    private void importOrderFiltersFromSession() {
        importOrderFiltersFromSession(false);
    }

    private void importOrderFiltersFromSession(boolean forceReload) {
        filterNameOrderElement.setValue(FilterUtils.readOrderTaskName(order));
        filterStartDateOrderElement.setValue(FilterUtils.readOrderStartDate(order));
        filterFinishDateOrderElement.setValue(FilterUtils.readOrderEndDate(order));
        List<FilterPair> sessionFilterPairs = FilterUtils.readOrderParameters(order);
        if ((sessionFilterPairs != null) && (bdFiltersOrderElement.getSelectedElements().isEmpty() || forceReload)) {
            bdFiltersOrderElement.addSelectedElements(sessionFilterPairs);
        }
        if (FilterUtils.readOrderInheritance(order) != null) {
            labelsWithoutInheritance.setChecked(FilterUtils.readOrderInheritance(order));
        }
    }

    public EditTaskController getEditTaskController() {
        return editTaskController;
    }

    public AdvancedAllocationTaskController getAdvancedAllocationTaskController() {
        return advancedAllocationTaskController;
    }

    public OrderCRUDController getOrderCRUDController() {
        return orderCRUDController;
    }

    public void setURLParameters(Map<String, String[]> parameters) {
        this.parameters = parameters;
    }

    public Order getOrder() {
        return model.getOrder();
    }

    public void onApplyFilter() {
        filterByPredicate(createPredicate());
        List<FilterPair> listFilters = (List<FilterPair>) bdFiltersOrderElement.getSelectedElements();
        FilterUtils.writeOrderParameters(order, listFilters);
    }

    private TaskElementPredicate createPredicate() {

        if (FilterUtils.hasOrderWBSFiltersChanged(order)) {
            importOrderFiltersFromSession(true);
            FilterUtils.writeOrderWBSFiltersChanged(order, false);
        }

        List<FilterPair> listFilters = (List<FilterPair>) bdFiltersOrderElement.getSelectedElements();
        Date startDate = filterStartDateOrderElement.getValue();
        Date finishDate = filterFinishDateOrderElement.getValue();
        boolean ignoreLabelsInheritance = labelsWithoutInheritance.isChecked();
        String name = filterNameOrderElement.getValue();

        if (listFilters.isEmpty() && startDate == null && finishDate == null && name == null) {
            return null;
        }
        FilterUtils.writeOrderTaskName(order, name);
        FilterUtils.writeOrderStartDate(order, startDate);
        FilterUtils.writeOrderEndDate(order, finishDate);
        FilterUtils.writeOrderInheritance(order, ignoreLabelsInheritance);
        return new TaskElementPredicate(listFilters, startDate, finishDate, name, ignoreLabelsInheritance);
    }

    public Checkbox getLabelsWithoutInheritance() {
        return labelsWithoutInheritance;
    }

    public void setLabelsWithoutInheritance(Checkbox labelsWithoutInheritance) {
        this.labelsWithoutInheritance = labelsWithoutInheritance;
    }

    private void filterByPredicate(final TaskElementPredicate predicate) {
        LongOperationFeedback.execute(orderElementFilter, new ILongOperation() {

            @Override
            public void doAction() {
                // FIXME remove or change
                model.forceLoadLabelsAndCriterionRequirements();
                planner.setTaskListPredicate(getFilterAndParentExpanedPredicates(predicate));
            }

            @Override
            public String getName() {
                return _("filtering");
            }

        });
    }

    private FilterAndParentExpandedPredicates getFilterAndParentExpanedPredicates(
            final TaskElementPredicate predicate) {
        final IContext<?> context = planner.getContext();
        FilterAndParentExpandedPredicates newPredicate = new FilterAndParentExpandedPredicates(context) {
            @Override
            public boolean accpetsFilterPredicate(Task task) {
                if (predicate == null) {
                    return true;
                }
                TaskElement taskElement = (TaskElement) context.getMapper().findAssociatedDomainObject(task);
                return predicate.accepts(taskElement);
            }

        };
        newPredicate.setFilterContainers(planner.getPredicate().isFilterContainers());

        return newPredicate;
    }

    public Constraint checkConstraintFinishDate() {
        return new Constraint() {
            @Override
            public void validate(org.zkoss.zk.ui.Component comp, Object value) throws WrongValueException {
                Date finishDate = (Date) value;
                if ((finishDate != null) && (filterStartDateOrderElement.getValue() != null) &&
                        (finishDate.compareTo(filterStartDateOrderElement.getValue()) < 0)) {

                    filterFinishDateOrderElement.setRawValue(null);
                    throw new WrongValueException(comp, _("must be after start date"));
                }
            }

        };
    }

    public Constraint checkConstraintStartDate() {
        return new Constraint() {
            @Override
            public void validate(org.zkoss.zk.ui.Component comp, Object value) throws WrongValueException {
                Date startDate = (Date) value;
                if ((startDate != null) && (filterFinishDateOrderElement.getValue() != null) &&
                        (startDate.compareTo(filterFinishDateOrderElement.getValue()) > 0)) {

                    filterStartDateOrderElement.setRawValue(null);
                    throw new WrongValueException(comp, _("must be lower than end date"));
                }
            }
        };
    }

    private void showResorceAllocationIfIsNeeded() {
        if ((task != null) && (planner != null)) {

            planner.expandAllAlways();

            Task foundTask = null;
            TaskElement foundTaskElement = null;
            IContext<TaskElement> context = (IContext<TaskElement>) planner.getContext();
            Map<TaskElement, Task> map = context.getMapper().getMapDomainToTask();

            for (Entry<TaskElement, Task> entry : map.entrySet()) {
                if (task.getId().equals(entry.getKey().getId())) {
                    foundTaskElement = entry.getKey();
                    foundTask = entry.getValue();
                }
            }

            if ((foundTask != null) && (foundTaskElement != null)) {
                IContextWithPlannerTask<TaskElement> contextTask = ContextWithPlannerTask.create(context, foundTask);

                if (this.getCurrentControllerToShow().equals(getEditTaskController())) {

                    this.editTaskController.showEditFormResourceAllocation(contextTask, foundTaskElement,
                            model.getPlanningState());

                } else if (this.getCurrentControllerToShow().equals(this.getAdvanceAssignmentPlanningController())) {

                    getAdvanceAssignmentPlanningController().showWindow(contextTask, foundTaskElement,
                            model.getPlanningState());
                }
            }
        }
    }

    public AdvanceConsolidationController getAdvanceConsolidationController() {
        return advanceConsolidationController;
    }

    public AdvanceAssignmentPlanningController getAdvanceAssignmentPlanningController() {
        return advanceAssignmentPlanningController;
    }

    public void setCurrentControllerToShow(GenericForwardComposer currentControllerToShow) {
        this.currentControllerToShow = currentControllerToShow;
    }

    private GenericForwardComposer getCurrentControllerToShow() {
        return currentControllerToShow;
    }

}

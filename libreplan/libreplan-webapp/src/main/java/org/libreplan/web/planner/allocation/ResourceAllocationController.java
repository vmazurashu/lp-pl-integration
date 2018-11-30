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

package org.libreplan.web.planner.allocation;

import static org.libreplan.web.I18nHelper._;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.LocalDate;
import org.libreplan.business.orders.entities.AggregatedHoursGroup;
import org.libreplan.business.planner.entities.CalculatedValue;
import org.libreplan.business.planner.entities.DerivedAllocation;
import org.libreplan.business.planner.entities.ResourceAllocation;
import org.libreplan.business.planner.entities.TaskElement;
import org.libreplan.business.resources.entities.ResourceEnum;
import org.libreplan.business.planner.entities.Task;
import org.libreplan.web.I18nHelper;
import org.libreplan.web.common.EffortDurationBox;
import org.libreplan.web.common.IMessagesForUser;
import org.libreplan.web.common.LenientDecimalBox;
import org.libreplan.web.common.Util;
import org.libreplan.web.common.components.AllocationSelector;
import org.libreplan.web.common.components.NewAllocationSelector;
import org.libreplan.web.common.components.NewAllocationSelectorCombo;
import org.libreplan.web.common.components.ResourceAllocationBehaviour;
import org.libreplan.web.planner.order.PlanningStateCreator.PlanningState;
import org.libreplan.web.planner.taskedition.EditTaskController;
import org.libreplan.web.planner.taskedition.TaskPropertiesController;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.zkoss.ganttz.extensions.IContextWithPlannerTask;
import org.zkoss.ganttz.timetracker.ICellForDetailItemRenderer;
import org.zkoss.ganttz.timetracker.IConvertibleToColumn;
import org.zkoss.ganttz.timetracker.OnColumnsRowRenderer;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Window;

/**
 * Controller for {@link ResourceAllocation} view.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 * @author Diego Pino Garcia <dpino@igalia.com>
 * @author Javier Moran Rua <jmoran@igalia.com>
 * @author Vova Perebykivskyi <vova@libreplan-enterprise.com>
 */
@org.springframework.stereotype.Component("resourceAllocationController")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ResourceAllocationController extends GenericForwardComposer {

    private static final Log LOG = LogFactory.getLog(ResourceAllocationController.class);

    private IResourceAllocationModel resourceAllocationModel;

    private ResourceAllocationRenderer resourceAllocationRenderer = new ResourceAllocationRenderer();

    private TaskInformation taskInformation;

    private AllocationConfiguration allocationConfiguration;

    private Grid allocationsGrid;

    private FormBinder formBinder;

    private AllocationRowsHandler allocationRows;

    private EffortDurationBox assignedEffortComponent;

    private Checkbox extendedViewCheckbox;

    private Decimalbox allResourcesPerDay;

    private Label allOriginalEffort;

    private Label allTotalEffort;

    private Label allConsolidatedEffort;

    private Label allTotalResourcesPerDay;

    private Label allConsolidatedResourcesPerDay;

    private Button applyButton;

    private NewAllocationSelector newAllocationSelector;

    private NewAllocationSelectorCombo newAllocationSelectorCombo;

    private Tab tbResourceAllocation;

    private Tab workerSearchTab;

    private Button advancedSearchButton;

    private Window editTaskWindow;

    private EditTaskController editTaskController;

    public ResourceAllocationController() {
        if ( resourceAllocationModel == null ) {
            resourceAllocationModel = (IResourceAllocationModel) SpringUtil.getBean("resourceAllocationModel");
        }
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        allResourcesPerDay = new LenientDecimalBox();
        allResourcesPerDay.setWidth("80px");
        initAllocationLabels();
        makeReadyInputsForCalculationTypes();
    }

    public void setEditTaskController(EditTaskController editTaskController) {
        this.editTaskController = editTaskController;
    }

    private void initAllocationLabels() {
        allOriginalEffort = new Label();
        allTotalEffort = new Label();
        allConsolidatedEffort = new Label();

        allTotalResourcesPerDay = new Label();
        allConsolidatedResourcesPerDay = new Label();
    }

    private void makeReadyInputsForCalculationTypes() {
        assignedEffortComponent = new EffortDurationBox();
        assignedEffortComponent.setWidth("80px");
    }

    @Override
    public ResourceAllocationController getController() {
        return this;
    }

    /**
     * Shows Resource Allocation window.
     *
     * @param context
     * @param task
     * @param planningState
     * @param messagesForUser
     */
    public void init(IContextWithPlannerTask<TaskElement> context,
                     Task task,
                     PlanningState planningState,
                     IMessagesForUser messagesForUser) {
        try {
            if ( formBinder != null ) {
                formBinder.detach();
            }
            allocationRows = resourceAllocationModel.initAllocationsFor(task, context, planningState);

            formBinder = allocationRows.createFormBinder(planningState.getCurrentScenario(), resourceAllocationModel);
            formBinder.setBehaviour(ResourceAllocationBehaviour.NON_LIMITING);
            formBinder.setAllOriginalEffort(allOriginalEffort);
            formBinder.setAllTotalEffort(allTotalEffort);
            formBinder.setAllConsolidatedEffort(allConsolidatedEffort);
            formBinder.setAssignedEffortComponent(assignedEffortComponent);

            formBinder.setAllTotalResourcesPerDay(allTotalResourcesPerDay);
            formBinder.setAllConsolidatedResourcesPerDay(allConsolidatedResourcesPerDay);
            formBinder.setAllResourcesPerDay(allResourcesPerDay);

            TaskPropertiesController taskPropertiesController = editTaskController.getTaskPropertiesController();
            formBinder.setWorkableDays(getTaskWorkableDays(), taskPropertiesController, getTaskStart(), getTaskEnd());

            formBinder.setApplyButton(applyButton);
            formBinder.setAllocationsGrid(allocationsGrid);
            formBinder.setMessagesForUser(messagesForUser);
            formBinder.setWorkerSearchTab(workerSearchTab);
            formBinder.setNewAllocationSelectorCombo(newAllocationSelectorCombo);

            initializeTaskInformationComponent();
            initializeAllocationConfigurationComponent();
            formBinder.setAdvancedSearchButton(advancedSearchButton);

            tbResourceAllocation.setSelected(true);

            newAllocationSelector.setAllocationsAdder(resourceAllocationModel);
            newAllocationSelectorCombo.setAllocationsAdder(resourceAllocationModel);

            Util.reloadBindings(allocationsGrid);
        } catch (WrongValueException e) {
            LOG.error("there was a WrongValueException initializing window", e);
            throw e;
        }
    }

    private Intbox getTaskWorkableDays() {
        return allocationConfiguration.getTaskWorkableDays();
    }

    public Label getTaskStart() {
        return (allocationConfiguration != null) ? allocationConfiguration.getTaskStart() : null;
    }

    public Label getTaskEnd() {
        return (allocationConfiguration != null) ? allocationConfiguration.getTaskEnd() : null;
    }

    private Radiogroup getCalculationTypeSelector() {
        return allocationConfiguration.getCalculationTypeSelector();
    }

    private void initializeTaskInformationComponent() {
        taskInformation.initializeGridTaskRows(resourceAllocationModel.getHoursAggregatedByCriterions());
        formBinder.setRecommendedAllocation(taskInformation.getBtnRecommendedAllocation());
        taskInformation.onCalculateTotalHours(() -> resourceAllocationModel.getOrderHours());
    }

    private void initializeAllocationConfigurationComponent() {
        allocationConfiguration.initialize(formBinder);
    }

    public enum HoursRendererColumn {

        CRITERIONS {
            @Override
            public Component cell(HoursRendererColumn column, AggregatedHoursGroup data) {
                return new Label(data.getCriterionsJoinedByComma());
            }
        },

        RESOURCE_TYPE{
            @Override
            public Component cell(HoursRendererColumn column, AggregatedHoursGroup data) {
                return new Label(asString(data.getResourceType()));
            }
        },

        HOURS {
            @Override
            public Component cell(HoursRendererColumn column, AggregatedHoursGroup data) {
                return new Label(Integer.toString(data.getHours()));
            }
        };

        private static String asString(ResourceEnum resourceType) {
            switch (resourceType) {

                case MACHINE:
                case WORKER:
                    return _(resourceType.getDisplayName());

                default:
                    LOG.warn("no i18n for " + resourceType.name());
                    return resourceType.name();
            }
        }

        public abstract Component cell(HoursRendererColumn column, AggregatedHoursGroup data);
    }

    /**
     * Pick resources selected from {@link NewAllocationSelector} and add them to resource allocation list.
     *
     * Should be public!
     *
     * @param allocationSelector
     */
    public void onSelectWorkers( AllocationSelector allocationSelector) {
        try {
            allocationSelector.addChosen();
        } finally {
            // For email notification feature
            int rowsSize = allocationRows.getCurrentRows().size();
            if ( rowsSize >= 1 ) {
                editTaskController
                        .getTaskPropertiesController()
                        .getListToAdd()
                        .add(allocationRows.getCurrentRows().get(rowsSize - 1).getAssociatedResources().get(0));
            }
            editTaskController.getTaskPropertiesController().setResourcesAdded(true);

            tbResourceAllocation.setSelected(true);
            applyButton.setVisible(true);
            allocationSelector.clearAll();
            Util.reloadBindings(allocationsGrid);
        }
    }

    public void goToAdvancedSearch() {
        applyButton.setVisible(false);
        workerSearchTab.setSelected(true);

        LocalDate start = LocalDate.fromDateFields(resourceAllocationModel.getTaskStart());
        LocalDate end = LocalDate.fromDateFields(resourceAllocationModel.getTaskEnd());
        newAllocationSelector.open(start, end);
    }

    /**
     * Shows the extended view of the resources allocations.
     */
    public void onCheckExtendedView() {
        if ( isExtendedView() ) {
            editTaskWindow.setWidth("970px");
        } else {
            editTaskWindow.setWidth("870px");
        }
        editTaskWindow.invalidate();
        Util.reloadBindings(allocationsGrid);
    }

    public boolean isExtendedView() {
        return extendedViewCheckbox.isChecked();
    }

    /**
     * Close search worker in worker search tab.
     */
    public void onCloseSelectWorkers() {
        tbResourceAllocation.setSelected(true);
        applyButton.setVisible(true);
    }

    public enum CalculationTypeRadio {

        WORKABLE_DAYS(CalculatedValue.END_DATE) {
            @Override
            public String getName() {
                return _("Calculate Workable Days");
            }

            @Override
            public Component input(ResourceAllocationController resourceAllocationController) {
                return resourceAllocationController.getTaskWorkableDays();
            }
        },

        NUMBER_OF_HOURS(CalculatedValue.NUMBER_OF_HOURS) {
            @Override
            public String getName() {
                return _("Calculate Number of Hours");
            }

            @Override
            public Component input(ResourceAllocationController resourceAllocationController) {
                return resourceAllocationController.assignedEffortComponent;
            }
        },

        RESOURCES_PER_DAY(CalculatedValue.RESOURCES_PER_DAY) {
            @Override
            public String getName() {
                return _("Calculate Resources per Day");
            }

            @Override
            public Component input(ResourceAllocationController resourceAllocationController) {
                return resourceAllocationController.allResourcesPerDay;
            }
        };

        private final CalculatedValue calculatedValue;

        CalculationTypeRadio(CalculatedValue calculatedValue) {
            this.calculatedValue = calculatedValue;

        }

        public static CalculationTypeRadio from(CalculatedValue calculatedValue) {
            Validate.notNull(calculatedValue);
            for (CalculationTypeRadio calculationTypeRadio : CalculationTypeRadio.values()) {
                if ( calculationTypeRadio.getCalculatedValue() == calculatedValue) {
                    return calculationTypeRadio;
                }
            }
            throw new RuntimeException(
                    "not found " + CalculationTypeRadio.class.getSimpleName() + " for " + calculatedValue);
        }

        public abstract Component input(ResourceAllocationController resourceAllocationController);

        public Radio createRadio() {
            Radio result = new Radio();
            result.setLabel(getName());
            result.setValue(toString());

            return result;
        }

        public abstract String getName();

        public CalculatedValue getCalculatedValue() {
            return calculatedValue;
        }

    }

    public enum DerivedAllocationColumn implements IConvertibleToColumn {
        NAME(_("Name")) {
            @Override
            public Component cellFor(DerivedAllocation data) {
                return new Label(data.getName());
            }
        },

        ALPHA(_("Alpha")) {
            @Override
            public Component cellFor(DerivedAllocation data) {
                return new Label(String.format("%3.2f", data.getAlpha()));
            }
        },

        HOURS(_("Total Hours")) {
            @Override
            public Component cellFor(DerivedAllocation data) {
                return new Label(Integer.toString(data.getHours()));
            }
        };

        private final String name;

        private static final ICellForDetailItemRenderer<DerivedAllocationColumn, DerivedAllocation> cellRenderer =
                (column, data) -> column.cellFor(data);

        DerivedAllocationColumn(String name) {
            this.name = name;
        }

        /**
         * Forces to mark the string as needing translation.
         */
        private static String _(String string) {
            return string;
        }

        public String getName() {
            return I18nHelper._(name);
        }

        @Override
        public Column toColumn() {
            return new Column(getName());
        }

        public static void appendColumnsTo(Grid grid) {
            Columns columns = new Columns();
            grid.appendChild(columns);

            for (DerivedAllocationColumn each : values()) {
                columns.appendChild(each.toColumn());
            }
        }

        public static RowRenderer createRenderer() {
            return OnColumnsRowRenderer.create(cellRenderer, Arrays.asList(DerivedAllocationColumn.values()));
        }

        abstract Component cellFor(DerivedAllocation data);
    }

    public List<CalculationTypeRadio> getCalculationTypes() {
        return Arrays.asList(CalculationTypeRadio.values());
    }

    public List<?> getResourceAllocations() {
        return formBinder != null
                ? plusAggregatingRow(formBinder.getCurrentRows())
                : Collections.<AllocationRow> emptyList();
    }

    private List<Object> plusAggregatingRow(List<AllocationRow> currentRows) {
        List<Object> result = new ArrayList<>(currentRows);
        result.add(null);
        return result;
    }

    public ResourceAllocationRenderer getResourceAllocationRenderer() {
        return resourceAllocationRenderer;
    }

    /**
     * Triggered when closable button is clicked.
     */
    public void onClose(Event event) {
        cancel();
        event.stopPropagation();
    }

    public void cancel() {
        clear();
        resourceAllocationModel.cancel();
    }

    public void clear() {
        allocationsGrid.setModel(new SimpleListModel<>(Collections.emptyList()));
    }

    /**
     * @return <code>true</code> if it must exist <code>false</code> if exit
     *         must be prevented
     */
    public boolean accept() {
        boolean mustExit = formBinder.accept();
        if ( mustExit ) {
            clear();
        }

        return mustExit;
    }

    private class ResourceAllocationRenderer implements RowRenderer {

        @Override
        public void render(Row item, Object o, int i) throws Exception {
            if ( o instanceof AllocationRow ) {
                AllocationRow row = (AllocationRow) o;
                renderResourceAllocation(item, row);
            } else {
                renderAggregatingRow(item);
            }
        }


        private void renderResourceAllocation(Row row, final AllocationRow data) {
            row.setValue(data);
            append(row, data.createDetail());
            append(row, new Label(data.getName()));
            append(row, new Label(data.getOriginalEffort().toFormattedString()));
            append(row, new Label(data.getTotalEffort().toFormattedString()));
            append(row, new Label(data.getConsolidatedEffort().toFormattedString()));
            append(row, data.getEffortInput());
            append(row, new Label(data.getTotalResourcesPerDay().getAmount().toString()));
            append(row, new Label(data.getConsolidatedResourcesPerDay().getAmount().toString()));

            Div resourcesPerDayContainer = append(row, new Div());
            append(resourcesPerDayContainer, data.getIntendedResourcesPerDayInput());
            Label realResourcesPerDay = append(resourcesPerDayContainer, data.getRealResourcesPerDay());
            realResourcesPerDay.setStyle("float: right; padding-right: 1em;");

            Listbox assignmentFunctionListbox = data.getAssignmentFunctionListbox();
            append(row, assignmentFunctionListbox);
            assignmentFunctionListbox.addEventListener(Events.ON_SELECT, arg0 -> data.resetAssignmentFunction());

            // On click delete button
            Button deleteButton = appendDeleteButton(row);
            deleteButton.setDisabled(isAnyManualOrTaskUpdatedFromTimesheets());
            formBinder.setDeleteButtonFor(deleteButton);

            deleteButton.addEventListener("onClick", event -> {

                editTaskController
                        .getTaskPropertiesController()
                        .getListToDelete()
                        .add(data.getAssociatedResources().get(0));

                removeAllocation(data);
            });

            if (!data.isSatisfied()) {
                row.setSclass("allocation-not-satisfied");
            } else {
                row.setSclass("allocation-satisfied");
            }
        }

        private void renderAggregatingRow(Row row) {
            ResourceAllocationController controller = ResourceAllocationController.this;
            append(row, new Label());
            append(row, new Label(_("Total")));
            append(row, allOriginalEffort);
            append(row, allTotalEffort);
            append(row, allConsolidatedEffort);
            append(row, CalculationTypeRadio.NUMBER_OF_HOURS.input(controller));
            append(row, allTotalResourcesPerDay);
            append(row, allConsolidatedResourcesPerDay);
            append(row, CalculationTypeRadio.RESOURCES_PER_DAY.input(controller));
            append(row, new Label());
        }

        private void removeAllocation(AllocationRow row) {
            allocationRows.remove(row);
            Util.reloadBindings(allocationsGrid);
        }

        private Button appendDeleteButton(Row row) {
            Button button = new Button();
            button.setSclass("icono");
            button.setImage("/common/img/ico_borrar1.png");
            button.setHoverImage("/common/img/ico_borrar.png");
            button.setTooltiptext(_("Delete"));

            return append(row, button);
        }

        private <T extends Component> T append(Component parent, T component) {
            parent.appendChild(component);

            return component;
        }
    }

    public FormBinder getFormBinder() {
        return formBinder;
    }

    public void accept(AllocationResult allocation) {
        resourceAllocationModel.accept(allocation);
    }

    public boolean hasResourceAllocations() {
        return getResourceAllocations().size() > 1;
    }

    public boolean isAnyNotFlat() {
        return formBinder != null && formBinder.isAnyNotFlat();
    }

    public boolean isAnyManualOrTaskUpdatedFromTimesheets() {
        return formBinder != null && (formBinder.isAnyManual() || formBinder.isTaskUpdatedFromTimesheets());
    }

}

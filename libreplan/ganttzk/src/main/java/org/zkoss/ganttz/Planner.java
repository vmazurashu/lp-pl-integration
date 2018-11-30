/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
 * Copyright (C) 2010-2012 Igalia, S.L.
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

package org.zkoss.ganttz;

import static org.zkoss.ganttz.i18n.I18nHelper._;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.zkoss.ganttz.adapters.IDisabilityConfiguration;
import org.zkoss.ganttz.adapters.IDomainAndBeansMapper;
import org.zkoss.ganttz.adapters.PlannerConfiguration;
import org.zkoss.ganttz.data.Dependency;
import org.zkoss.ganttz.data.GanttDiagramGraph;
import org.zkoss.ganttz.data.GanttDiagramGraph.GanttZKDiagramGraph;
import org.zkoss.ganttz.data.GanttDiagramGraph.IGraphChangeListener;
import org.zkoss.ganttz.data.Position;
import org.zkoss.ganttz.data.Task;
import org.zkoss.ganttz.extensions.ICommand;
import org.zkoss.ganttz.extensions.ICommandOnTask;
import org.zkoss.ganttz.extensions.IContext;
import org.zkoss.ganttz.timetracker.TimeTracker;
import org.zkoss.ganttz.timetracker.TimeTrackerComponent;
import org.zkoss.ganttz.timetracker.TimeTrackerComponentWithoutColumns;
import org.zkoss.ganttz.timetracker.zoom.ZoomLevel;
import org.zkoss.ganttz.util.ComponentsFinder;
import org.zkoss.ganttz.util.LongOperationFeedback;
import org.zkoss.ganttz.util.LongOperationFeedback.ILongOperation;
import org.zkoss.ganttz.util.ProfilingLogFactory;
import org.zkoss.ganttz.util.WeakReferencedListeners;
import org.zkoss.zk.au.AuRequest;
import org.zkoss.zk.au.AuService;
import org.zkoss.zk.mesg.MZk;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.HtmlMacroComponent;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Combobox;

public class Planner extends HtmlMacroComponent  {

    private static final String PLANNER_COMMAND = "planner-command";

    private static final Log PROFILING_LOG = ProfilingLogFactory.getLog(Planner.class);

    private String EXPAND_ALL_BUTTON = "expandAll";

    private GanttZKDiagramGraph diagramGraph;

    private LeftPane leftPane;

    private GanttPanel ganttPanel;

    private List<? extends CommandContextualized<?>> contextualizedGlobalCommands;

    private CommandContextualized<?> goingDownInLastArrowCommand;

    private List<? extends CommandOnTaskContextualized<?>> commandsOnTasksContextualized;

    private CommandOnTaskContextualized<?> doubleClickCommand;

    private FunctionalityExposedForExtensions<?> context;

    private transient IDisabilityConfiguration disabilityConfiguration;

    private boolean isShowingCriticalPath = false;

    private boolean isShowingAdvances = false;

    private boolean isShowingLabels = false;

    private boolean isShowingReportedHours = false;

    private boolean isShowingMoneyCostBar = false;

    private boolean isShowingResources = false;

    private boolean isExpandAll = false;

    private boolean isFlattenTree = false;

    private ZoomLevel zoomLevel = null;

    private Listbox listZoomLevels = null;

    private WeakReferencedListeners<IChartVisibilityChangedListener> chartVisibilityListeners = WeakReferencedListeners.create();

    private IGraphChangeListener showCriticalPathOnChange = new IGraphChangeListener() {
        @Override
        public void execute() {
            context.showCriticalPath();
        }
    };

    private IGraphChangeListener showAdvanceOnChange = new IGraphChangeListener() {
        @Override
        public void execute() {
            context.showAdvances();
        }
    };

    private IGraphChangeListener showReportedHoursOnChange = new IGraphChangeListener() {
        @Override
        public void execute() {
            context.showReportedHours();
        }
    };

    private IGraphChangeListener showMoneyCostBarOnChange = new IGraphChangeListener() {
        @Override
        public void execute() {
            context.showMoneyCostBar();
        }
    };

    private boolean containersExpandedByDefault = false;

    private boolean shownAdvanceByDefault = false;

    private boolean shownReportedHoursByDefault = false;

    private boolean shownMoneyCostBarByDefault = false;

    private boolean shownLabelsByDefault = false;

    private boolean shownResourcesByDefault = false;

    private FilterAndParentExpandedPredicates predicate;

    private boolean visibleChart;

    public Planner() {
    }

    public static boolean guessContainersExpandedByDefaultGivenPrintParameters(Map<String, String> printParameters) {
        return guessContainersExpandedByDefault(convertToURLParameters(printParameters));
    }

    private static Map<String, String[]> convertToURLParameters(Map<String, String> printParameters) {
        Map<String, String[]> result = new HashMap<>();
        for (Entry<String, String> each : printParameters.entrySet()) {
            result.put(each.getKey(), new String[] { each.getValue() });
        }

        return result;
    }

    public static boolean guessContainersExpandedByDefault(Map<String, String[]> queryURLParameters) {
        String[] values = queryURLParameters.get("expanded");
        return values != null && toLowercaseSet(values).contains("all");
    }

    public static boolean guessShowAdvancesByDefault(Map<String, String[]> queryURLParameters) {
        String[] values = queryURLParameters.get("advances");
        return values != null && toLowercaseSet(values).contains("all");
    }

    public static boolean guessShowReportedHoursByDefault(Map<String, String[]> queryURLParameters) {
        String[] values = queryURLParameters.get("reportedHours");
        return values != null && toLowercaseSet(values).contains("all");
    }

    public static boolean guessShowMoneyCostBarByDefault(Map<String, String[]> queryURLParameters) {
        String[] values = queryURLParameters.get("moneyCostBar");
        return values != null && toLowercaseSet(values).contains("all");
    }

    private static Set<String> toLowercaseSet(String[] values) {
        Set<String> result = new HashSet<>();
        for (String each : values) {
            result.add(each.toLowerCase());
        }

        return result;
    }

    TaskList getTaskList() {
        return ganttPanel == null
                ? null
                : ComponentsFinder.findComponentsOfType(TaskList.class, ganttPanel.getChildren()).get(0);
    }

    public int getTaskNumber() {
        return getTaskList().getTasksNumber();
    }

    public int calculateMinimumWidthForTaskNameColumn(boolean expand) {
        return calculateMinimumWidthForTaskNameColumn(expand, getTaskList().getAllTasks());
    }

    private int calculateMinimumWidthForTaskNameColumn(boolean expand, List<Task> tasks) {

        IDomainAndBeansMapper<?> mapper = getContext().getMapper();
        int widest = 0;
        int pixelsPerTaskLevel = 21;
        int pixelsPerCharacter = 5;

        for (Task task : tasks) {
            int numberOfAncestors = mapper.findPositionFor(task).getAncestors().size();
            int numberOfCharacters = task.getName().length();

            widest = Math.max(
                    widest,
                    numberOfCharacters * pixelsPerCharacter + numberOfAncestors * pixelsPerTaskLevel);

            if ( expand && !task.isLeaf() ) {
                widest = Math.max(widest, calculateMinimumWidthForTaskNameColumn(expand, task.getTasks()));
            }
        }

        return widest;
    }

    public int getAllTasksNumber() {
        return diagramGraph.getTasks().size();
    }

    public String getContextPath() {
        return Executions.getCurrent().getContextPath();
    }

    public DependencyList getDependencyList() {
        if ( ganttPanel == null ) {
            return null;
        }

        List<Component> children = ganttPanel.getChildren();
        List<DependencyList> found = ComponentsFinder.findComponentsOfType(DependencyList.class, children);

        if ( found.isEmpty() ) {
            return null;
        }

        return found.get(0);
    }

    public void addTasks(Position position, Collection<? extends Task> newTasks) {
        TaskList taskList = getTaskList();

        if ( taskList != null && leftPane != null ) {
            taskList.addTasks(position, newTasks);
            leftPane.addTasks(position, newTasks);
        }
    }

    public void addTask(Position position, Task task) {
        addTasks(position, Collections.singletonList(task));
    }

    void addDependencies(Collection<? extends Dependency> dependencies) {
        DependencyList dependencyList = getDependencyList();

        if ( dependencyList == null ) {
            return;
        }

        for (DependencyComponent d : getTaskList().asDependencyComponents(dependencies)) {
            dependencyList.addDependencyComponent(d);
        }
    }

    public ListModel<ZoomLevel> getZoomLevels() {
        ZoomLevel[] selectableZoomlevels = {
                ZoomLevel.DETAIL_ONE,
                ZoomLevel.DETAIL_TWO,
                ZoomLevel.DETAIL_THREE,
                ZoomLevel.DETAIL_FOUR,
                ZoomLevel.DETAIL_FIVE
        };

        return new SimpleListModel<>(selectableZoomlevels);
    }

    public void setZoomLevel(final ZoomLevel zoomLevel, int scrollLeft) {
        if ( ganttPanel == null ) {
            return;
        }

        this.zoomLevel = zoomLevel;
        ganttPanel.setZoomLevel(zoomLevel, scrollLeft);
    }

    public void zoomIncrease() {
        if ( ganttPanel == null ) {
            return;
        }
        LongOperationFeedback.execute(ganttPanel, new ILongOperation() {

            @Override
            public String getName() {
                return _("increasing zoom");
            }

            @Override
            public void doAction() {
                ganttPanel.zoomIncrease();
            }
        });
    }

    public void zoomDecrease() {
        if ( ganttPanel == null ) {
            return;
        }

        LongOperationFeedback.execute(ganttPanel, new ILongOperation() {
            @Override
            public String getName() {
                return _("decreasing zoom");
            }

            @Override
            public void doAction() {
                ganttPanel.zoomDecrease();
            }
        });
    }

    public <T> void setConfiguration(PlannerConfiguration<T> configuration) {
        if ( configuration == null ) {
            return;
        }

        if ( isShowingLabels )
            Clients.evalJavaScript("ganttz.TaskList.getInstance().showAllTaskLabels()");

        if ( isShowingResources )
            Clients.evalJavaScript("ganttz.TaskList.getInstance().showResourceTooltips()");

        this.diagramGraph = GanttDiagramGraph.create(
                configuration.isScheduleBackwards(),
                configuration.getStartConstraints(),
                configuration.getEndConstraints(),
                configuration.isDependenciesConstraintsHavePriority());

        FunctionalityExposedForExtensions<T> newContext =
                new FunctionalityExposedForExtensions<>(this, configuration, diagramGraph);

        addGraphChangeListenersFromConfiguration(configuration);

        this.contextualizedGlobalCommands = contextualize(newContext, configuration.getGlobalCommands());

        this.commandsOnTasksContextualized = contextualize(newContext, configuration.getCommandsOnTasks());

        goingDownInLastArrowCommand = contextualize(newContext, configuration.getGoingDownInLastArrowCommand());

        doubleClickCommand = contextualize(newContext, configuration.getDoubleClickCommand());

        this.context = newContext;
        this.disabilityConfiguration = configuration;

        resettingPreviousComponentsToNull();
        long timeAddingData = System.currentTimeMillis();
        newContext.add(configuration.getData());

        PROFILING_LOG.debug("It took to add data: " + (System.currentTimeMillis() - timeAddingData) + " ms");
        long timeSetupingAndAddingComponents = System.currentTimeMillis();
        setupComponents();
        setAt("insertionPointLeftPanel", leftPane);
        leftPane.afterCompose();
        setAt("insertionPointRightPanel", ganttPanel);
        ganttPanel.afterCompose();
        leftPane.setGoingDownInLastArrowCommand(goingDownInLastArrowCommand);

        TimeTrackerComponent timetrackerheader =
                new TimeTrackerComponentWithoutColumns(ganttPanel.getTimeTracker(), "timetrackerheader");

        setAt("insertionPointTimetracker", timetrackerheader);
        timetrackerheader.afterCompose();

        Component chartComponent = configuration.getChartComponent();

        if ( chartComponent != null ) {
            setAt("insertionPointChart", chartComponent);
        }

        if ( !configuration.isCriticalPathEnabled() ) {
            Button showCriticalPathButton = (Button) getFellow("showCriticalPath");
            showCriticalPathButton.setVisible(false);
        }

        if ( !configuration.isExpandAllEnabled() ) {
            Button expandAllButton = (Button) getFellow(EXPAND_ALL_BUTTON);
            expandAllButton.setVisible(false);
        }

        if (!configuration.isFlattenTreeEnabled()) {
            Button flattenTree = (Button) getFellow("flattenTree");
            flattenTree.setVisible(false);
        }

        if ( !configuration.isShowAllResourcesEnabled() ) {
            Button showAllResources = (Button) getFellow("showAllResources");
            showAllResources.setVisible(false);
        }

        if ( !configuration.isMoneyCostBarEnabled() ) {
            Button showMoneyCostBarButton = (Button) getFellow("showMoneyCostBar");
            showMoneyCostBarButton.setVisible(false);
        }

        // view buttons toggle state so set all off prior to toggling
        if ( configuration.isShowResourcesOn() ) {
            showAllResources();
        }

        if ( configuration.isShowAdvancesOn() ) {
            showAdvances();
        }

        if ( configuration.isShowReportedHoursOn() ) {
            showReportedHours();
        }

        if ( configuration.isShowLabelsOn() ) {
           showAllLabels();
        }

        if ( configuration.isShowMoneyCostBarOn() ) {
            showMoneyCostBar();
        }

        listZoomLevels.setSelectedIndex(getZoomLevel().ordinal());

        this.visibleChart = configuration.isExpandPlanningViewCharts();
        ((South) getFellow("graphics")).setOpen(this.visibleChart);

        if (!visibleChart) {
            ((South) getFellow("graphics")).setTitle(_("Graphics are disabled"));
        }

        PROFILING_LOG.debug("it took doing the setup of components and adding them: "
                + (System.currentTimeMillis() - timeSetupingAndAddingComponents) + " ms");

        setAuService(new AuService() {
            public boolean service(AuRequest request, boolean everError) {
                String command = request.getCommand();
                int zoomindex;
                int scrollLeft;

                if ( "onZoomLevelChange".equals(command) ) {
                    zoomindex=  (Integer) retrieveData(request, "zoomindex");
                    scrollLeft = (Integer) retrieveData(request, "scrollLeft");

                    setZoomLevel(
                            (ZoomLevel)((Listbox) getFellow("listZoomLevels")).getModel().getElementAt(zoomindex),
                            scrollLeft);

                    return true;
                }

                return false;
            }

            private Object retrieveData(AuRequest request, String key) {
                Object value = request.getData().get(key);
                if ( value == null )
                    throw new UiException(MZk.ILLEGAL_REQUEST_WRONG_DATA, new Object[] { key, this });

                return value;
            }
        });
    }

    private void resettingPreviousComponentsToNull() {
        this.ganttPanel = null;
        this.leftPane = null;
    }

    private void setAt(String insertionPointId, Component component) {
        Component insertionPoint = getFellow(insertionPointId);
        insertionPoint.getChildren().clear();
        insertionPoint.appendChild(component);
    }

    private <T> List<CommandOnTaskContextualized<T>> contextualize(FunctionalityExposedForExtensions<T> context,
                                                                   List<ICommandOnTask<T>> commands) {

        List<CommandOnTaskContextualized<T>> result = new ArrayList<>();
        for (ICommandOnTask<T> c : commands) {
            result.add(contextualize(context, c));
        }

        return result;
    }

    private <T> CommandOnTaskContextualized<T> contextualize(FunctionalityExposedForExtensions<T> context,
                                                             ICommandOnTask<T> commandOnTask) {

        return CommandOnTaskContextualized.create(commandOnTask, context.getMapper(), context);
    }

    private <T> CommandContextualized<T> contextualize(IContext<T> context, ICommand<T> command) {
        return command == null ? null : CommandContextualized.create(command, context);
    }

    private <T> List<CommandContextualized<T>> contextualize(
            IContext<T> context, Collection<? extends ICommand<T>> commands) {

        ArrayList<CommandContextualized<T>> result = new ArrayList<>();
        for (ICommand<T> command : commands) {
            result.add(contextualize(context, command));
        }

        return result;
    }

    private void setupComponents() {
        insertGlobalCommands();

        predicate = new FilterAndParentExpandedPredicates(context) {
            @Override
            public boolean accpetsFilterPredicate(Task task) {
                return true;
            }
        };

        this.leftPane = new LeftPane(disabilityConfiguration, this, predicate);

        this.ganttPanel = new GanttPanel(
                this, commandsOnTasksContextualized, doubleClickCommand, disabilityConfiguration, predicate);

        Button button = (Button) getFellow("btnPrint");
        button.setDisabled(!context.isPrintEnabled());
    }

    public GanttZKDiagramGraph getDiagramGraph() {
        return this.diagramGraph;
    }

    public void updateTooltips() {
        this.ganttPanel.updateTooltips();
    }

    @SuppressWarnings("unchecked")
    private void insertGlobalCommands() {
        Component commonToolbar = getCommonCommandsInsertionPoint();
        Component plannerToolbar = getSpecificCommandsInsertionPoint();

        if ( !contextualizedGlobalCommands.isEmpty() ) {
            commonToolbar.getChildren().removeAll(commonToolbar.getChildren());
        }

        for (CommandContextualized<?> c : contextualizedGlobalCommands) {

            // Comparison through icon as name is internationalized
            if ( c.getCommand().isPlannerCommand() ) {

                // FIXME Avoid hard-coding the number of planner commands
                // At this moment we have 2 planner commands: reassign and adapt planning
                if ( plannerToolbar.getChildren().size() < 2 ) {
                    plannerToolbar.appendChild(c.toButton());
                }
            } else {
                commonToolbar.appendChild(c.toButton());
            }
        }

    }

    private Component getCommonCommandsInsertionPoint() {
        return getPage().getFellow("perspectiveButtonsInsertionPoint");
    }

    private Component getSpecificCommandsInsertionPoint() {
        return getFellow("plannerButtonsInsertionPoint");
    }

    void removeTask(Task task) {
        TaskList taskList = getTaskList();
        taskList.remove(task);
        getDependencyList().taskRemoved(task);
        leftPane.taskRemoved(task);
        setHeight(getHeight());// forcing smart update
        ganttPanel.adjustZoomColumnsHeight();
        getDependencyList().redrawDependencies();
    }

    @Override
    public void afterCompose() {
        super.afterCompose();
        listZoomLevels = (Listbox) getFellow("listZoomLevels");

        Component westContainer = getFellow("taskdetailsContainer");

        westContainer.addEventListener(
                Events.ON_SIZE,
                event -> Clients.evalJavaScript("ganttz.TaskList.getInstance().legendResize();"));

    }

    public TimeTracker getTimeTracker() {
        return ganttPanel.getTimeTracker();
    }

    public void showCriticalPath() {
        Button showCriticalPathButton = (Button) getFellow("showCriticalPath");
        if ( disabilityConfiguration.isCriticalPathEnabled() ) {
            if ( isShowingCriticalPath ) {
                context.hideCriticalPath();
                diagramGraph.removePostGraphChangeListener(showCriticalPathOnChange);
                showCriticalPathButton.setSclass(PLANNER_COMMAND);
                showCriticalPathButton.setTooltiptext(_("Show critical path"));
            } else {
                context.showCriticalPath();
                diagramGraph.addPostGraphChangeListener(showCriticalPathOnChange);
                showCriticalPathButton.setSclass(PLANNER_COMMAND + " clicked");
                showCriticalPathButton.setTooltiptext(_("Hide critical path"));
            }

            isShowingCriticalPath = !isShowingCriticalPath;
        }
    }

    public void forcedShowAdvances() {
        if ( !isShowingAdvances ) {
            showAdvances();
        }
    }

    public void showAdvances() {
        Button showAdvancesButton = (Button) getFellow("showAdvances");
        if ( disabilityConfiguration.isAdvancesEnabled() ) {
            Combobox progressTypesCombo = (Combobox) getFellow("cbProgressTypes");

            if ( isShowingAdvances ) {
                context.hideAdvances();
                diagramGraph.removePostGraphChangeListener(showAdvanceOnChange);
                showAdvancesButton.setSclass(PLANNER_COMMAND);
                showAdvancesButton.setTooltiptext(_("Show progress"));

                if ( progressTypesCombo.getItemCount() > 0 ) {
                    progressTypesCombo.setSelectedIndex(0);
                }
            } else {
                context.showAdvances();
                diagramGraph.addPostGraphChangeListener(showAdvanceOnChange);
                showAdvancesButton.setSclass(PLANNER_COMMAND + " clicked");
                showAdvancesButton.setTooltiptext(_("Hide progress"));
            }

            isShowingAdvances = !isShowingAdvances;
        }
    }

    public void showReportedHours() {
        Button showReportedHoursButton = (Button) getFellow("showReportedHours");
        if ( disabilityConfiguration.isReportedHoursEnabled() ) {
            if ( isShowingReportedHours ) {
                context.hideReportedHours();
                diagramGraph.removePostGraphChangeListener(showReportedHoursOnChange);
                showReportedHoursButton.setSclass(PLANNER_COMMAND);
                showReportedHoursButton.setTooltiptext(_("Show reported hours"));
            } else {
                context.showReportedHours();
                diagramGraph.addPostGraphChangeListener(showReportedHoursOnChange);
                showReportedHoursButton.setSclass(PLANNER_COMMAND + " clicked");
                showReportedHoursButton.setTooltiptext(_("Hide reported hours"));
            }

            isShowingReportedHours = !isShowingReportedHours;
        }
    }

    public void showMoneyCostBar() {
        Button showMoneyCostBarButton = (Button) getFellow("showMoneyCostBar");
        if ( disabilityConfiguration.isMoneyCostBarEnabled() ) {
            if ( isShowingMoneyCostBar ) {
                context.hideMoneyCostBar();
                diagramGraph.removePostGraphChangeListener(showMoneyCostBarOnChange);
                showMoneyCostBarButton.setSclass(PLANNER_COMMAND);
                showMoneyCostBarButton.setTooltiptext(_("Show money cost bar"));
            } else {
                context.showMoneyCostBar();
                diagramGraph.addPostGraphChangeListener(showMoneyCostBarOnChange);
                showMoneyCostBarButton.setSclass(PLANNER_COMMAND + " clicked");
                showMoneyCostBarButton.setTooltiptext(_("Hide money cost bar"));
            }

            isShowingMoneyCostBar = !isShowingMoneyCostBar;
        }
    }

    public void showAllLabels() {
        Button showAllLabelsButton = (Button) getFellow("showAllLabels");
        if ( disabilityConfiguration.isLabelsEnabled() ) {
	        if ( isShowingLabels ) {
	            Clients.evalJavaScript("ganttz.TaskList.getInstance().hideAllTaskLabels()");
	            showAllLabelsButton.setSclass("planner-command show-labels");
	        } else {
	            Clients.evalJavaScript("ganttz.TaskList.getInstance().showAllTaskLabels()");
	            showAllLabelsButton.setSclass("planner-command show-labels clicked");
	        }

	        isShowingLabels = !isShowingLabels;
        }
    }

    public void showAllResources() {
        Button showAllLabelsButton = (Button) getFellow("showAllResources");
        if ( disabilityConfiguration.isResourcesEnabled() ) {
	        if ( isShowingResources ) {
	            Clients.evalJavaScript("ganttz.TaskList.getInstance().hideResourceTooltips()");
	            showAllLabelsButton.setSclass("planner-command show-resources");
	        } else {
	            Clients.evalJavaScript("ganttz.TaskList.getInstance().showResourceTooltips()");
	            showAllLabelsButton.setSclass("planner-command show-resources clicked");
	        }

	        isShowingResources = !isShowingResources;

        }
    }

    public void print() {
        // Pending to raise print configuration popup.
        // Information retrieved should be passed as parameter to context print method.
        context.print();
    }

    public ZoomLevel getZoomLevel() {
        return ganttPanel == null
                ? zoomLevel != null ? zoomLevel : ZoomLevel.DETAIL_ONE
                : ganttPanel.getTimeTracker().getDetailLevel();
    }

    public void setInitialZoomLevel(final ZoomLevel zoomLevel) {
        this.zoomLevel = zoomLevel;
    }

    public boolean areContainersExpandedByDefault() {
        return containersExpandedByDefault;
    }

    public void setAreContainersExpandedByDefault(boolean containersExpandedByDefault) {
        this.containersExpandedByDefault = containersExpandedByDefault;
    }

    public boolean areShownAdvancesByDefault() {
        return shownAdvanceByDefault;
    }

    public boolean showAdvancesRightNow() {
        return areShownAdvancesByDefault() || isShowingAdvances;
    }

    public void setAreShownAdvancesByDefault(boolean shownAdvanceByDefault) {
        this.shownAdvanceByDefault = shownAdvanceByDefault;
    }

    public void setAreShownReportedHoursByDefault(boolean shownReportedHoursByDefault) {
        this.shownReportedHoursByDefault = shownReportedHoursByDefault;
    }

    public boolean areShownReportedHoursByDefault() {
        return shownReportedHoursByDefault;
    }

    public boolean showReportedHoursRightNow() {
        return areShownReportedHoursByDefault() || isShowingReportedHours;
    }

    public void setAreShownMoneyCostBarByDefault(boolean shownMoneyCostBarByDefault) {
        this.shownMoneyCostBarByDefault = shownMoneyCostBarByDefault;
    }

    public boolean areShownMoneyCostBarByDefault() {
        return shownMoneyCostBarByDefault;
    }

    public boolean showMoneyCostBarRightNow() {
        return areShownMoneyCostBarByDefault() || isShowingMoneyCostBar;
    }

    public void setAreShownLabelsByDefault(boolean shownLabelsByDefault) {
        this.shownLabelsByDefault = shownLabelsByDefault;
    }

    public boolean areShownLabelsByDefault() {
        return shownLabelsByDefault;
    }

    public boolean showLabelsRightNow() {
        return areShownLabelsByDefault() || isShowingLabels;
    }

    public void setAreShownResourcesByDefault(boolean shownResourcesByDefault) {
        this.shownResourcesByDefault = shownResourcesByDefault;
    }

    public boolean areShownResourcesByDefault() {
        return shownResourcesByDefault;
    }

    public boolean showResourcesRightNow() {
        return areShownResourcesByDefault() || isShowingResources;
    }

    public void expandAll() {
        Button expandAllButton = (Button) getFellow(EXPAND_ALL_BUTTON);
        if ( disabilityConfiguration.isExpandAllEnabled() ) {

            if ( isExpandAll ) {
                context.collapseAll();
                expandAllButton.setSclass(PLANNER_COMMAND);
            } else {
                context.expandAll();
                expandAllButton.setSclass(PLANNER_COMMAND + " clicked");
            }
        }

        isExpandAll = !isExpandAll;
    }

    public void expandAllAlways() {
        Button expandAllButton = (Button) getFellow(EXPAND_ALL_BUTTON);
        if ( disabilityConfiguration.isExpandAllEnabled() ) {
            context.expandAll();
            expandAllButton.setSclass(PLANNER_COMMAND + " clicked");
        }
    }

    public void updateSelectedZoomLevel() {
        ganttPanel.getTimeTracker().setZoomLevel(zoomLevel);
        Listitem selectedItem = listZoomLevels.getItems().get(zoomLevel.ordinal());
        listZoomLevels.setSelectedItem(selectedItem);
        listZoomLevels.invalidate();
    }

    public IContext getContext() {
        return context;
    }

    public void setTaskListPredicate(FilterAndParentExpandedPredicates predicate) {
        this.predicate = predicate;
        leftPane.setPredicate(predicate);
        getTaskList().setPredicate(predicate);
        getDependencyList().redrawDependencies();

        if ( isShowingLabels ) {
            Clients.evalJavaScript("ganttz.TaskList.getInstance().showAllTaskLabels();");
        }

        if ( isShowingResources ) {
            Clients.evalJavaScript("ganttz.TaskList.getInstance().showResourceTooltips();");
        }
    }

    public void flattenTree() {
        Button flattenTreeButton = (Button) getFellow("flattenTree");
        if ( disabilityConfiguration.isFlattenTreeEnabled() ) {
            if (isFlattenTree) {
                predicate.setFilterContainers(false);
                flattenTreeButton.setSclass(PLANNER_COMMAND);
            } else {
                predicate.setFilterContainers(true);
                flattenTreeButton.setSclass(PLANNER_COMMAND + " clicked");
            }

            setTaskListPredicate(predicate);
        }

        isFlattenTree = !isFlattenTree;
        Clients.evalJavaScript("ganttz.Planner.getInstance().adjustScrollableDimensions()");
    }

    public FilterAndParentExpandedPredicates getPredicate() {
        return predicate;
    }

    public void changeChartVisibility(boolean visible) {
        visibleChart = visible;
        chartVisibilityListeners.fireEvent(listener -> listener.chartVisibilityChanged(visibleChart));
    }

    public boolean isVisibleChart() {
        return visibleChart;
    }

    public void addChartVisibilityListener(IChartVisibilityChangedListener chartVisibilityChangedListener) {
        chartVisibilityListeners.addListener(chartVisibilityChangedListener);
    }

    public void addGraphChangeListenersFromConfiguration(PlannerConfiguration<?> configuration) {
        diagramGraph.addPreChangeListeners(configuration.getPreChangeListeners());
        diagramGraph.addPostChangeListeners(configuration.getPostChangeListeners());
    }

    public boolean isShowingCriticalPath() {
        return isShowingCriticalPath;
    }

    public boolean isShowingLabels() {
        return isShowingLabels;
    }

    public boolean isShowingResources() {
        return isShowingResources;
    }

    public boolean isShowingAdvances() {
        return isShowingAdvances;
    }

    public boolean isExpandAll() {
        return isExpandAll;
    }

    public boolean isFlattenTree() {
        return isFlattenTree;
    }

    public Button findCommandComponent(String name) {
        for (CommandContextualized<?> c : contextualizedGlobalCommands) {
            if ( c.getCommand().getName().equals(name) ) {
                return c.toButton();
            }
        }
        return null;
    }

    @Override
    public String getWidgetClass(){
        return getDefinition().getDefaultWidgetClass(this);
    }

    public List getCriticalPath() {
        return context.getCriticalPath();
    }

    public void updateCompletion(String progressType) {
        TaskList taskList = getTaskList();
        if ( taskList != null ) {
            taskList.updateCompletion(progressType);

            for (TaskComponent each : taskList.getTaskComponents()) {
                each.invalidate();
            }
        }
    }

    public TaskComponent getTaskComponentRelatedTo(Task task) {
        TaskList taskList = getTaskList();
        if ( taskList != null ) {

            for (TaskComponent each : taskList.getTaskComponents()) {

                if ( each.getTask().equals(task) ) {
                    return each;
                }
            }
        }
        return null;
    }

}

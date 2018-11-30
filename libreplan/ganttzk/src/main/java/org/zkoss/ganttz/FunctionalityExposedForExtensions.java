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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.LocalDate;
import org.zkoss.ganttz.adapters.DomainDependency;
import org.zkoss.ganttz.adapters.IAdapterToTaskFundamentalProperties;
import org.zkoss.ganttz.adapters.IDomainAndBeansMapper;
import org.zkoss.ganttz.adapters.IStructureNavigator;
import org.zkoss.ganttz.adapters.PlannerConfiguration;
import org.zkoss.ganttz.data.Dependency;
import org.zkoss.ganttz.data.DependencyType;
import org.zkoss.ganttz.data.GanttDiagramGraph;
import org.zkoss.ganttz.data.GanttDiagramGraph.GanttZKDiagramGraph;
import org.zkoss.ganttz.data.ITaskFundamentalProperties;
import org.zkoss.ganttz.data.Milestone;
import org.zkoss.ganttz.data.Position;
import org.zkoss.ganttz.data.Task;
import org.zkoss.ganttz.data.TaskContainer;
import org.zkoss.ganttz.data.TaskLeaf;
import org.zkoss.ganttz.data.criticalpath.CriticalPathCalculator;
import org.zkoss.ganttz.extensions.IContext;
import org.zkoss.ganttz.timetracker.TimeTracker;
import org.zkoss.ganttz.timetracker.zoom.IDetailItemModifier;
import org.zkoss.ganttz.timetracker.zoom.TimeTrackerState;
import org.zkoss.ganttz.util.Interval;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Window;

public class FunctionalityExposedForExtensions<T> implements IContext<T> {

    private static class OneToOneMapper<T> implements IDomainAndBeansMapper<T> {

        private Map<T, Task> fromDomainToTask = new HashMap<>();

        private Map<Task, T> fromTaskToDomain = new HashMap<>();

        private Map<Task, TaskContainer> fromTaskToParent = new HashMap<>();

        private List<Task> topLevel = new ArrayList<>();

        @Override
        public Task findAssociatedBean(T domainObject) throws IllegalArgumentException {
            if ( domainObject == null ) {
                throw new IllegalArgumentException("domainObject is null");
            }

            if ( !fromDomainToTask.containsKey(domainObject) ) {
                throw new IllegalArgumentException("not found " + domainObject);
            }

            return fromDomainToTask.get(domainObject);
        }

        public Map<T, Task> getMapDomainToTask() {
            return fromDomainToTask;
        }

        /**
         * Registers the task at the provided position.
         */
        void register(Position position, Task task, T domainObject) {
            fromDomainToTask.put(domainObject, task);
            fromTaskToDomain.put(task, domainObject);

            if ( position.isAppendToTop() ) {
                topLevel.add(task);
            } else if ( position.isAtTop() ) {
                topLevel.add(position.getInsertionPosition(), task);
            } else {
                fromTaskToParent.put(task, position.getParent());
            }
        }

        void remove(T domainObject) {
            Task toBeRemoved = findAssociatedBean(domainObject);
            fromDomainToTask.remove(domainObject);
            fromTaskToDomain.remove(toBeRemoved);
            TaskContainer parent = fromTaskToParent.get(toBeRemoved);

            if ( parent != null ) {
                parent.remove(toBeRemoved);
            }

            fromTaskToParent.remove(toBeRemoved);
            topLevel.remove(toBeRemoved);
        }

        @Override
        public T findAssociatedDomainObject(Task task) throws IllegalArgumentException {
            if ( task == null ) {
                throw new IllegalArgumentException("taskBean is null");
            }

            if ( !fromTaskToDomain.containsKey(task) ) {
                throw new IllegalArgumentException();
            }

            return fromTaskToDomain.get(task);
        }

        @Override
        public Position findPositionFor(Task task) {
            List<TaskContainer> ancestors = ancestorsOf(task);

            if ( ancestors.isEmpty() ) {
                return Position.createAtTopPosition(topLevel.indexOf(task));
            }

            TaskContainer parent = ancestors.get(0);

            return Position.createPosition(ancestors, parent.getTasks().indexOf(task));
        }

        @Override
        public Position findPositionFor(T domainObject) {
            return findPositionFor(findAssociatedBean(domainObject));
        }

        private List<TaskContainer> ancestorsOf(Task task) {
            ArrayList<TaskContainer> result = new ArrayList<>();
            TaskContainer taskContainer = fromTaskToParent.get(task);

            while (taskContainer != null) {
                result.add(taskContainer);
                taskContainer = fromTaskToParent.get(taskContainer);
            }

            return result;
        }

        @Override
        public List<? extends TaskContainer> getParents(Task task) {
            Position position = findPositionFor(task);

            return position.getAncestors();
        }

    }

    private final Planner planner;

    private final IAdapterToTaskFundamentalProperties<T> adapter;

    private final IStructureNavigator<T> navigator;

    private final OneToOneMapper<T> mapper = new OneToOneMapper<>();

    private final GanttZKDiagramGraph diagramGraph;

    private TimeTracker timeTracker;

    private final PlannerConfiguration<T> configuration;

    public FunctionalityExposedForExtensions(
            Planner planner, PlannerConfiguration<T> configuration, GanttZKDiagramGraph diagramGraph) {

        this.planner = planner;
        this.configuration = configuration;
        this.adapter = configuration.getAdapter();
        this.navigator = configuration.getNavigator();
        this.diagramGraph = diagramGraph;

        final IDetailItemModifier firstLevelModifiers = configuration.getFirstLevelModifiers();
        final IDetailItemModifier secondLevelModifiers = configuration.getSecondLevelModifiers();

        Calendar calendarRightNow = Calendar.getInstance();
        LocalDate localDateRightNow = LocalDate.fromCalendarFields(calendarRightNow);
        LocalDate initDate = localDateRightNow.minusYears(1);
        LocalDate endDate = localDateRightNow.plusYears(5);

        this.timeTracker = new TimeTracker(
                new Interval(TimeTrackerState.year(initDate.getYear()), TimeTrackerState.year(endDate.getYear())),
                planner.getZoomLevel(),
                firstLevelModifiers,
                secondLevelModifiers,
                planner);
    }

    /**
     * @param position the position in which to register the task at top level.
     *                          It can be <code>null</code>
     * @param accumulatedDependencies
     * @param data
     *
     * @return {@link Task}
     */
    private Task buildAndRegister(Position position, List<DomainDependency<T>> accumulatedDependencies, T data) {
        accumulatedDependencies.addAll(adapter.getOutcomingDependencies(data));
        accumulatedDependencies.addAll(adapter.getIncomingDependencies(data));

        final Task result = build(data);

        if ( !navigator.isLeaf(data) ) {
            TaskContainer container = (TaskContainer) result;
            int i = 0;

            for (T child : navigator.getChildren(data)) {
                container.add(buildAndRegister(position.down(container, i), accumulatedDependencies, child));
                i++;
            }

        } else if ( navigator.isMilestone(data) ) {
            Milestone milestone = (Milestone) result;
            milestone.setOwner(position.getParent());
        }

        result.setShowingReportedHours(planner.showReportedHoursRightNow());
        result.setShowingMoneyCostBar(planner.showMoneyCostBarRightNow());
        result.setShowingAdvances(planner.showAdvancesRightNow());
        result.setShowingLabels(planner.showLabelsRightNow());
        result.setShowingResources(planner.showResourcesRightNow());

        mapper.register(position, result, data);

        return result;
    }

    private Task build(T data) {
        ITaskFundamentalProperties adapted = adapter.adapt(data);
        if ( navigator.isMilestone(data) ) {
            return new Milestone(adapted);
        } else if ( navigator.isLeaf(data) ) {
            return new TaskLeaf(adapted);
        } else {
            return new TaskContainer(adapted, planner.areContainersExpandedByDefault());
        }
    }

    public void add(Position position, Collection<? extends T> domainObjects) {
        List<DomainDependency<T>> totalDependencies = new ArrayList<>();
        List<Task> tasksCreated = new ArrayList<>();

        for (T object : domainObjects) {
            Task task = buildAndRegister(position, totalDependencies, object);
            tasksCreated.add(task);
        }

        updateTimeTracker(tasksCreated);

        if ( position.isAppendToTop() || position.isAtTop() ) {
            this.diagramGraph.addTopLevel(tasksCreated);
        } else {
            this.diagramGraph.addTasks(tasksCreated);
            TaskContainer parent = position.getParent();
            parent.addAll(position.getInsertionPosition(), tasksCreated);
            this.diagramGraph.childrenAddedTo(parent);
        }

        for (Dependency dependency : DomainDependency.toDependencies(mapper, totalDependencies)) {
            this.diagramGraph.addWithoutEnforcingConstraints(dependency);
        }

        this.diagramGraph.enforceAllRestrictions();
        this.planner.addTasks(position, tasksCreated);
    }

    private void updateTimeTracker(List<Task> tasksCreated) {
        for (Task task : tasksCreated) {
            timeTracker.trackPosition(task);

            if ( task.isContainer() ) {
                TaskContainer container = (TaskContainer) task;
                updateTimeTracker(container.getTasks());
            }
        }
    }

    public void add(Collection<? extends T> domainObjects) {
        add(Position.createAppendToTopPosition(), domainObjects);
    }

    @Override
    public void add(T domainObject) {
        add(Position.createAppendToTopPosition(), domainObject);
    }

    @Override
    public void add(Position position, T domainObject) {
        add(position, Collections.singletonList(domainObject));
    }

    public IDomainAndBeansMapper<T> getMapper() {
        return mapper;
    }

    @Override
    public void reload(PlannerConfiguration<?> configuration) {
        planner.setConfiguration(configuration);
    }

    @Override
    public Position remove(T domainObject) {
        Task task = mapper.findAssociatedBean(domainObject);
        Position position = mapper.findPositionFor(task);
        adapter.doRemovalOf(mapper.findAssociatedDomainObject(task));
        mapper.remove(domainObject);
        diagramGraph.remove(task);
        task.removed();
        planner.removeTask(task);

        return position;
    }

    @Override
    public Component getRelativeTo() {
        return planner;
    }

    @Override
    public void replace(T oldDomainObject, T newDomainObject) {
        Position position = remove(oldDomainObject);
        add(position, newDomainObject);
    }

    public GanttZKDiagramGraph getDiagramGraph() {
        return diagramGraph;
    }

    private DomainDependency<T> toDomainDependency(Dependency bean) {
        T source = mapper.findAssociatedDomainObject(bean.getSource());
        T destination = mapper.findAssociatedDomainObject(bean.getDestination());

        return DomainDependency.createDependency(source, destination, bean.getType());
    }

    public void addDependency(Dependency dependency) {
        if ( !canAddDependency(dependency) ) {
            return;
        }

        diagramGraph.add(dependency);
        getDependencyList().addDependencyComponent(getTaskList().asDependencyComponent(dependency));
        adapter.addDependency(toDomainDependency(dependency));
    }

    private TaskList getTaskList() {
        return planner.getTaskList();
    }

    private boolean canAddDependency(Dependency dependency) {
        return diagramGraph.canAddDependency(dependency) && adapter.canAddDependency(toDomainDependency(dependency));
    }

    private DependencyList getDependencyList() {
        return planner.getDependencyList();
    }

    public void removeDependency(Dependency dependency) {
        adapter.removeDependency(toDomainDependency(dependency));
        diagramGraph.removeDependency(dependency);
        getDependencyList().remove(dependency);
    }

    /**
     * Substitutes the dependency for a new one with the same source and destination but with the specified type.
     * If the new dependency cannot be added, the old one remains.
     *
     * @param dependency
     * @param type
     *            the new type
     *
     * @return true only if the new dependency can be added.
     */
    public boolean changeType(Dependency dependency, DependencyType type) {
        Dependency newDependency = dependency.createWithType(type);
        boolean canAddDependency = diagramGraph.canAddDependency(newDependency);

        if ( canAddDependency ) {
            removeDependency(dependency);
            addDependency(newDependency);
        }

        return canAddDependency;
    }

    @Override
    public TimeTracker getTimeTracker() {
        return timeTracker;
    }

    @Override
    public void recalculatePosition(T domainObject) {
        Task associatedTask = mapper.findAssociatedBean(domainObject);
        diagramGraph.enforceRestrictions(associatedTask);
    }

    @Override
    public void showCriticalPath() {
        CriticalPathCalculator<Task, Dependency> criticalPathCalculator =
                CriticalPathCalculator.create(configuration.isDependenciesConstraintsHavePriority());

        List<Task> criticalPath = criticalPathCalculator.calculateCriticalPath(diagramGraph);

        for (Task task : diagramGraph.getTasks()) {
            task.setInCriticalPath(isInCriticalPath(criticalPath, task));
        }
    }

    private boolean isInCriticalPath(List<Task> criticalPath, Task task) {
        if ( task.isContainer() ) {
            List<Task> allTaskLeafs = task.getAllTaskLeafs();

            return CollectionUtils.containsAny(criticalPath, allTaskLeafs);
        } else {
            return criticalPath.contains(task);
        }
    }

    @Override
    public List<T> getCriticalPath() {
        List<T> result = new ArrayList<>();

        CriticalPathCalculator<Task, Dependency> criticalPathCalculator =
                CriticalPathCalculator.create(configuration.isDependenciesConstraintsHavePriority());

        for (Task each : criticalPathCalculator.calculateCriticalPath(diagramGraph)) {
            result.add(mapper.findAssociatedDomainObject(each));
        }

        return result;
    }

    @Override
    public void hideCriticalPath() {
        for (Task task : diagramGraph.getTasks()) {
            task.setInCriticalPath(false);
        }
    }

    @Override
    public void showAdvances() {
        for (Task task : diagramGraph.getTasks()) {
            task.setShowingAdvances(true);
        }
    }

    @Override
    public void hideAdvances() {
        for (Task task : diagramGraph.getTasks()) {
            task.setShowingAdvances(false);
        }
    }

    @Override
    public void showReportedHours() {
        for (Task task : diagramGraph.getTasks()) {
            task.setShowingReportedHours(true);
        }
    }

    @Override
    public void hideReportedHours() {
        for (Task task : diagramGraph.getTasks()) {
            task.setShowingReportedHours(false);
        }
    }

    @Override
    public void showMoneyCostBar() {
        for (Task task : diagramGraph.getTasks()) {
            task.setShowingMoneyCostBar(true);
        }
    }

    @Override
    public void hideMoneyCostBar() {
        for (Task task : diagramGraph.getTasks()) {
            task.setShowingMoneyCostBar(false);
        }
    }

    @Override
    public void reloadCharts() {
        configuration.reloadCharts();
    }

    public boolean isPrintEnabled() {
        return configuration.isPrintEnabled();
    }

    private HashMap<String, String> buildParameters(Component parent) {
        HashMap<String, String> parameters = new HashMap<>();

        Checkbox expanded = (Checkbox) parent.getFellow("print_expanded");
        Checkbox resources = (Checkbox) parent.getFellow("print_resources");
        Checkbox labels = (Checkbox) parent.getFellow("print_labels");
        Checkbox advances = (Checkbox) parent.getFellow("print_advances");
        Checkbox reportedHours = (Checkbox) parent.getFellow("print_reported_hours");
        Checkbox moneyCostBar = (Checkbox) parent.getFellow("print_money_cost_bar");

        parameters.put("extension", ".png");

        if ( expanded.isChecked() ) {
            parameters.put("expanded", "all");
        }

        if ( labels.isChecked() ) {
            parameters.put("labels", "all");
        }

        if ( advances.isChecked() ) {
            parameters.put("advances", "all");
        }

        if ( reportedHours.isChecked() ) {
            parameters.put("reportedHours", "all");
        }

        if ( moneyCostBar.isChecked() ) {
            parameters.put("moneyCostBar", "all");
        }

        if ( resources.isChecked() ) {
            parameters.put("resources", "all");
        }

        parameters.put("zoom", planner.getZoomLevel().getInternalName());

        return parameters;
    }

    public void print() {
        if ( !isPrintEnabled() ) {
            throw new UnsupportedOperationException("print is not supported");
        }

        final Window printProperties =
                (Window) Executions.createComponents("/planner/print_configuration.zul", planner, null);

        Button printButton = (Button) printProperties.getFellow("printButton");

        printButton.addEventListener(Events.ON_CLICK, new EventListener() {
            @Override
            public void onEvent(Event event) {
                printProperties.detach();
                configuration.print(buildParameters(printProperties),planner);
            }
        });

        printButton.setParent(printProperties);

        try {
            printProperties.doModal();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public List<Task> getTasksOrderedByStartDate() {
        List<Task> tasks = diagramGraph.getTasks();

        Collections.sort(tasks, new Comparator<Task>() {
            @Override
            public int compare(Task o1, Task o2) {
                return o1.getBeginDate().compareTo(o2.getBeginDate());
            }
        });

        return tasks;
    }

    public void expandAll() {
        setExpandAll(true, getTasksOrderedByStartDate());
    }

    public void collapseAll() {
        setExpandAll(false, getTasksOrderedByStartDate());
    }

    private void setExpandAll(boolean expand, List<Task> tasks) {
        for (Task task : tasks) {
            if ( task instanceof TaskContainer ) {
                ((TaskContainer) task).setExpanded(expand);
            }
        }
    }

    @Override
    public GanttDiagramGraph<Task, Dependency> getGanttDiagramGraph() {
        return diagramGraph;
    }

}

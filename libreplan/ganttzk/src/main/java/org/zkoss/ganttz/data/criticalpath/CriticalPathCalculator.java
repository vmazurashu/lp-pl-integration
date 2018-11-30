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

package org.zkoss.ganttz.data.criticalpath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.zkoss.ganttz.data.DependencyType;
import org.zkoss.ganttz.data.GanttDate;
import org.zkoss.ganttz.data.IDependency;
import org.zkoss.ganttz.data.constraint.Constraint;

/**
 * Class that calculates the critical path of a Gantt diagram graph.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public class CriticalPathCalculator<T, D extends IDependency<T>> {

    private final boolean dependenciesConstraintsHavePriority;

    public static <T, D extends IDependency<T>> CriticalPathCalculator<T, D> create(
            boolean dependenciesConstraintsHavePriority) {
        return new CriticalPathCalculator<T, D>(
                dependenciesConstraintsHavePriority);
    }

    private CriticalPathCalculator(boolean dependenciesConstraintsHavePriority) {
        this.dependenciesConstraintsHavePriority = dependenciesConstraintsHavePriority;
    }

    private ICriticalPathCalculable<T> graph;

    private LocalDate initDate;

    private Map<T, Node<T, D>> nodes;

    private InitialNode<T, D> bop;
    private LastNode<T, D> eop;

    private Map<T, Map<T, DependencyType>> dependencies;

    private class VisitorTracker {

        private Map<T, Set<T>> visitorsOn = new HashMap<T, Set<T>>();

        void visit(T visited, T visitor) {
            if (!visitorsOn.containsKey(visited)) {
                visitorsOn.put(visited, new HashSet<T>());
            }
            visitorsOn.get(visited).add(visitor);
        }

        boolean hasBeenVisitedByAll(T current,
                Collection<? extends T> collection) {
            return visitorsOn.containsKey(current)
                    && visitorsOn.get(current).containsAll(collection);
        }

    }

    public List<T> calculateCriticalPath(ICriticalPathCalculable<T> graph) {
        this.graph = graph;

        dependencies = new HashMap<T, Map<T, DependencyType>>();

        initDate = calculateInitDate();

        bop = createBeginningOfProjectNode();
        eop = createEndOfProjectNode();

        nodes = createGraphNodes();

        forward(bop, null, new VisitorTracker());
        eop.updateLatestValues();

        backward(eop, null, new VisitorTracker());

        return getTasksOnCriticalPath();
    }

    private LocalDate calculateInitDate() {
        if (graph.getTasks().isEmpty()) {
            return null;
        }
        GanttDate ganttDate = Collections.min(getStartDates());
        return LocalDate.fromDateFields(ganttDate.toDayRoundedDate());
    }

    private List<GanttDate> getStartDates() {
        List<GanttDate> result = new ArrayList<GanttDate>();
        for (T task : graph.getTasks()) {
            result.add(graph.getStartDate(task));
        }
        return result;
    }

    private Collection<T> removeContainers(Collection<T> tasks) {
        if (tasks == null) {
            return Collections.emptyList();
        }
        List<T> noConatinersTasks = new ArrayList<T>();
        for (T t : tasks) {
            if (graph.isContainer(t)) {
                List<T> children = graph.getChildren(t);
                noConatinersTasks.addAll(removeContainers(children));
            } else {
                noConatinersTasks.add(t);
            }
        }
        return noConatinersTasks;
    }

    private InitialNode<T, D> createBeginningOfProjectNode() {
        return new InitialNode<T, D>(
                removeWithVisibleIncomingDependencies(removeContainers(graph
                .getInitialTasks())));
    }


    private Set<T> removeWithVisibleIncomingDependencies(Collection<T> tasks) {
        Set<T> result = new HashSet<T>();
        for (T each : tasks) {
            if (!graph.hasVisibleIncomingDependencies(each)) {
                result.add(each);
            }
        }
        return result;
    }

    private LastNode<T, D> createEndOfProjectNode() {
        return new LastNode<T, D>(
                removeWithVisibleOutcomingDependencies(removeContainers(graph
                        .getLatestTasks())));
    }

    private Set<T> removeWithVisibleOutcomingDependencies(
            Collection<T> removeContainers) {
        Set<T> result = new HashSet<T>();
        for (T each : removeContainers) {
            if (!graph.hasVisibleOutcomingDependencies(each)) {
                result.add(each);
            }
        }
        return result;
    }

    private Map<T, Node<T, D>> createGraphNodes() {
        Map<T, Node<T, D>> result = new HashMap<T, Node<T, D>>();

        for (T task : graph.getTasks()) {
            if (!graph.isContainer(task)) {
                Set<T> in = withoutContainers(task, graph
                        .getIncomingTasksFor(task));
                Set<T> out = withoutContainers(task, graph
                        .getOutgoingTasksFor(task));

                Node<T, D> node = new Node<T, D>(task, in, out, graph
                        .getStartDate(task), graph.getEndDateFor(task));

                result.put(task, node);
            }
        }

        for (T task : graph.getTasks()) {
            if (graph.isContainer(task)) {
                Collection<T> allChildren = removeContainers(Arrays
                        .asList(task));

                Set<T> in = removeChildrenAndParents(task, graph
                        .getIncomingTasksFor(task));
                for (T t : in) {
                    IDependency<T> dependency = graph
                            .getDependencyFrom(t, task);
                    DependencyType type = DependencyType.END_START;
                    if (dependency != null) {
                        type = dependency.getType();
                    }
                    addDepedenciesAndRelatedTasks(result,
                            removeContainers(Arrays.asList(t)), allChildren,
                            type);
                }

                Set<T> out = removeChildrenAndParents(task, graph
                        .getOutgoingTasksFor(task));
                for (T t : out) {
                    IDependency<T> dependency = graph
                            .getDependencyFrom(task, t);
                    DependencyType type = DependencyType.END_START;
                    if (dependency != null) {
                        type = dependency.getType();
                    }
                    addDepedenciesAndRelatedTasks(result, allChildren,
                            removeContainers(Arrays.asList(t)), type);
                }
            }
        }

        return result;
    }

    private void addDepedenciesAndRelatedTasks(Map<T, Node<T, D>> graph,
            Collection<T> origins,
            Collection<T> destinations, DependencyType type) {
        for (T origin : origins) {
            for (T destination : destinations) {
                graph.get(origin).addNextTask(destination);
                graph.get(destination).addPreviousTask(origin);
                addDependency(origin, destination, type);
            }
        }
    }

    private Set<T> withoutContainers(T task, Set<T> tasks) {
        Set<T> result = new HashSet<T>();
        for (T t : tasks) {
            if (!graph.isContainer(t)) {
                result.add(t);
            }
        }
        return result;
    }

    private Set<T> removeChildrenAndParents(T task, Set<T> tasks) {
        Set<T> result = new HashSet<T>();
        if (!graph.isContainer(task)) {
            return result;
        }

        for (T t : tasks) {
            if (!graph.contains(task, t) && !graph.contains(t, task)) {
                result.add(t);
            }
        }

        return result;
    }

    private void addDependency(T from, T destination, DependencyType type) {
        Map<T, DependencyType> destinations = dependencies.get(from);
        if (destinations == null) {
            destinations = new HashMap<T, DependencyType>();
            dependencies.put(from, destinations);
        }
        destinations.put(destination, type);
    }

    private DependencyType getDependencyTypeEndStartByDefault(T from, T to) {
        if ((from != null) && (to != null)) {
            IDependency<T> dependency = graph.getDependencyFrom(from, to);
            if (dependency != null) {
                return dependency.getType();
            } else {
                Map<T, DependencyType> destinations = dependencies.get(from);
                if (destinations != null) {
                    DependencyType type = destinations.get(to);
                    if (type != null) {
                        return type;
                    }
                }
            }
        }
        return DependencyType.END_START;
    }

    private void forward(Node<T, D> currentNode, T previousTask,
            VisitorTracker visitorTracker) {
        T currentTask = currentNode.getTask();
        int earliestStart = currentNode.getEarliestStart();
        int earliestFinish = currentNode.getEarliestFinish();

        Set<T> nextTasks = currentNode.getNextTasks();
        if (nextTasks.isEmpty()) {
            eop.setEarliestStart(earliestFinish);
        } else {
            int countStartStart = 0;

            for (T task : nextTasks) {
                visitorTracker.visit(task, currentTask);
                if (graph.isContainer(currentTask)) {
                    if (graph.contains(currentTask, previousTask)) {
                        if (graph.contains(currentTask, task)) {
                            continue;
                        }
                    }
                }

                Node<T, D> node = nodes.get(task);
                DependencyType dependencyType = getDependencyTypeEndStartByDefault(
                        currentTask, task);
                Constraint<GanttDate> constraint = getDateConstraints(task);

                switch (dependencyType) {
                case START_START:
                    setEarliestStart(node, earliestStart, constraint);
                    countStartStart++;
                    break;
                case END_END:
                    setEarliestStart(node, earliestFinish - node.getDuration(),
                            constraint);
                    break;
                case END_START:
                default:
                    setEarliestStart(node, earliestFinish, constraint);
                    break;
                }

                if (visitorTracker.hasBeenVisitedByAll(task,
                        node.getPreviousTasks())) {
                    forward(node, currentTask, visitorTracker);
                }
            }

            if (nextTasks.size() == countStartStart) {
                eop.setEarliestStart(earliestFinish);
            }
        }
    }

    private void setEarliestStart(Node<T, D> node, int earliestStart,
            Constraint<GanttDate> constraint) {
        if (constraint != null) {
            GanttDate date = GanttDate.createFrom(initDate
                    .plusDays(earliestStart));
            date = constraint.applyTo(date);
            earliestStart = Days.daysBetween(initDate,
                    LocalDate.fromDateFields(date.toDayRoundedDate()))
                    .getDays();
        }
        node.setEarliestStart(earliestStart);
    }

    private Constraint<GanttDate> getDateConstraints(T task) {
        if (dependenciesConstraintsHavePriority || task == null) {
            return null;
        }

        List<Constraint<GanttDate>> startConstraints = graph
                .getStartConstraintsFor(task);
        List<Constraint<GanttDate>> endConstraints = graph
                .getEndConstraintsFor(task);
        if ((startConstraints == null || startConstraints.isEmpty())
                && (endConstraints == null || endConstraints.isEmpty())) {
            return null;
        }
        if (startConstraints == null || startConstraints.isEmpty()) {
            return Constraint.coalesce(endConstraints);
        }
        if (endConstraints == null || endConstraints.isEmpty()) {
            return Constraint.coalesce(startConstraints);
        }
        startConstraints.addAll(endConstraints);
        return Constraint.coalesce(startConstraints);
    }

    private void backward(Node<T, D> currentNode, T nextTask,
            VisitorTracker visitorTracker) {
        T currentTask = currentNode.getTask();
        int latestStart = currentNode.getLatestStart();
        int latestFinish = currentNode.getLatestFinish();

        Set<T> previousTasks = currentNode.getPreviousTasks();
        if (previousTasks.isEmpty()) {
            bop.setLatestFinish(latestStart);
        } else {
            int countEndEnd = 0;

            for (T task : previousTasks) {
                visitorTracker.visit(task, currentTask);
                if (graph.isContainer(currentTask)) {
                    if (graph.contains(currentTask, nextTask)) {
                        if (graph.contains(currentTask, task)) {
                            continue;
                        }
                    }
                }

                Node<T, D> node = nodes.get(task);
                DependencyType dependencyType = getDependencyTypeEndStartByDefault(
                        task, currentTask);
                Constraint<GanttDate> constraint = getDateConstraints(task);

                switch (dependencyType) {
                case START_START:
                    setLatestFinish(node, latestStart + node.getDuration(),
                            constraint);
                    break;
                case END_END:
                    setLatestFinish(node, latestFinish, constraint);
                    countEndEnd++;
                    break;
                case END_START:
                default:
                    setLatestFinish(node, latestStart, constraint);
                    break;
                }

                if (visitorTracker.hasBeenVisitedByAll(task,
                        node.getNextTasks())) {
                    backward(node, currentTask, visitorTracker);
                }
            }

            if (previousTasks.size() == countEndEnd) {
                bop.setLatestFinish(latestStart);
            }
        }
    }

    private void setLatestFinish(Node<T, D> node, int latestFinish,
            Constraint<GanttDate> constraint) {
        if (constraint != null) {
            int duration = node.getDuration();
            GanttDate date = GanttDate.createFrom(initDate.plusDays(latestFinish - duration));
            date = constraint.applyTo(date);
            int daysBetween = Days.daysBetween(initDate,
                    LocalDate.fromDateFields(date.toDayRoundedDate()))
                    .getDays();
            latestFinish = daysBetween + duration;
        }
        node.setLatestFinish(latestFinish);
    }

    private List<T> getTasksOnCriticalPath() {
        List<T> result = new ArrayList<T>();

        for (Node<T, D> node : nodes.values()) {
            if (node.getLatestStart() == node.getEarliestStart()) {
                result.add(node.getTask());
            }
        }

        return result;
    }

}

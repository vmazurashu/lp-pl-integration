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
package org.libreplan.web.limitingresources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.libreplan.business.common.BaseEntity;
import org.libreplan.business.planner.entities.GenericResourceAllocation;
import org.libreplan.business.planner.entities.ResourceAllocation;
import org.libreplan.business.planner.entities.SpecificResourceAllocation;
import org.libreplan.business.planner.limiting.entities.DateAndHour;
import org.libreplan.business.planner.limiting.entities.Gap;
import org.libreplan.business.planner.limiting.entities.Gap.GapOnQueue;
import org.libreplan.business.planner.limiting.entities.Gap.GapOnQueueWithQueueElement;
import org.libreplan.business.planner.limiting.entities.InsertionRequirements;
import org.libreplan.business.planner.limiting.entities.LimitingResourceQueueDependency;
import org.libreplan.business.planner.limiting.entities.LimitingResourceQueueDependency.QueueDependencyType;
import org.libreplan.business.planner.limiting.entities.LimitingResourceQueueElement;
import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.resources.entities.LimitingResourceQueue;
import org.libreplan.business.resources.entities.Resource;
import org.libreplan.business.resources.entities.ResourceEnum;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 */
public class QueuesState {

    private final List<LimitingResourceQueue> queues;

    private final List<LimitingResourceQueueElement> unassignedElements;

    private final DirectedGraph<LimitingResourceQueueElement, LimitingResourceQueueDependency> graph;

    private final Map<Long, LimitingResourceQueue> queuesById;

    private final Map<Long, LimitingResourceQueueElement> elementsById;

    private final Map<Long, LimitingResourceQueue> queuesByResourceId;

    private static <T extends BaseEntity> Map<Long, T> byId(Collection<? extends T> entities) {
        Map<Long, T> result = new HashMap<>();
        for (T each : entities) {
            result.put(each.getId(), each);
        }

        return result;
    }

    private static Map<Long, LimitingResourceQueue> byResourceId(
            Collection<? extends LimitingResourceQueue> limitingResourceQueues) {

        Map<Long, LimitingResourceQueue> result = new HashMap<>();
        for (LimitingResourceQueue each : limitingResourceQueues) {
            result.put(each.getResource().getId(), each);
        }

        return result;
    }

    public QueuesState(
            List<LimitingResourceQueue> limitingResourceQueues,
            List<LimitingResourceQueueElement> unassignedLimitingResourceQueueElements) {

        this.queues = new ArrayList<>(limitingResourceQueues);
        this.unassignedElements = new ArrayList<>(unassignedLimitingResourceQueueElements);
        this.queuesById = byId(queues);
        this.elementsById = byId(allElements(limitingResourceQueues, unassignedLimitingResourceQueueElements));
        this.queuesByResourceId = byResourceId(limitingResourceQueues);
        this.graph = buildGraph(getAllElements(unassignedElements, queues));
    }

    private static DirectedGraph<LimitingResourceQueueElement, LimitingResourceQueueDependency> buildGraph(
            List<LimitingResourceQueueElement> allElements) {

        DirectedGraph<LimitingResourceQueueElement, LimitingResourceQueueDependency> result =
                instantiateDirectedGraph();

        for (LimitingResourceQueueElement each : allElements) {
            result.addVertex(each);
        }

        for (LimitingResourceQueueElement each : allElements) {
            Set<LimitingResourceQueueDependency> dependenciesAsOrigin = each.getDependenciesAsOrigin();

            for (LimitingResourceQueueDependency eachDependency : dependenciesAsOrigin) {
                addDependency(result, eachDependency);
            }
        }

        return result;
    }

    private static SimpleDirectedGraph<LimitingResourceQueueElement, LimitingResourceQueueDependency>
    instantiateDirectedGraph() {

        return new SimpleDirectedGraph<>(LimitingResourceQueueDependency.class);
    }

    private static void addDependency(
            DirectedGraph<LimitingResourceQueueElement,
                    LimitingResourceQueueDependency> result,
            LimitingResourceQueueDependency dependency) {

        LimitingResourceQueueElement origin = dependency.getHasAsOrigin();
        LimitingResourceQueueElement destination = dependency.getHasAsDestiny();
        result.addVertex(origin);
        result.addVertex(destination);
        result.addEdge(origin, destination, dependency);
    }

    private static List<LimitingResourceQueueElement> getAllElements(
            List<LimitingResourceQueueElement> unassigned,
            List<LimitingResourceQueue> queues) {

        List<LimitingResourceQueueElement> result = new ArrayList<>();
        result.addAll(unassigned);

        for (LimitingResourceQueue each : queues) {
            result.addAll(each.getLimitingResourceQueueElements());
        }

        return result;
    }

    private List<LimitingResourceQueueElement> allElements(
            List<LimitingResourceQueue> queues,
            List<LimitingResourceQueueElement> unassigned) {

        List<LimitingResourceQueueElement> result = new ArrayList<>();
        for (LimitingResourceQueue each : queues) {
            result.addAll(each.getLimitingResourceQueueElements());
        }
        result.addAll(unassigned);

        return result;
    }

    public List<LimitingResourceQueue> getQueues() {
        return Collections.unmodifiableList(queues);
    }

    public ArrayList<LimitingResourceQueue> getQueuesOrderedByResourceName() {
        ArrayList<LimitingResourceQueue> result = new ArrayList<>(queues);
        Collections.sort(result, new Comparator<LimitingResourceQueue>() {

            @Override
            public int compare(LimitingResourceQueue arg0, LimitingResourceQueue arg1) {

                if ( (arg0 == null) || (arg0.getResource().getName() == null) ) {
                    return -1;
                }

                if ( (arg1 == null) || (arg1.getResource().getName() == null) ) {
                    return 1;
                }

                return (arg0.getResource().getName().toLowerCase()
                        .compareTo(arg1.getResource().getName().toLowerCase()));
            }

        });

        return result;

    }

    public List<LimitingResourceQueueElement> getUnassigned() {
        return Collections.unmodifiableList(unassignedElements);
    }

    public void assignedToQueue(LimitingResourceQueueElement element, LimitingResourceQueue queue) {
        Validate.isTrue(unassignedElements.contains(element));
        queue.addLimitingResourceQueueElement(element);
        unassignedElements.remove(element);
    }

    public LimitingResourceQueue getEquivalent(LimitingResourceQueue queue) {
        return queuesById.get(queue.getId());
    }

    public LimitingResourceQueueElement getEquivalent(LimitingResourceQueueElement element) {
        return elementsById.get(element.getId());
    }

    public void unassingFromQueue(LimitingResourceQueueElement externalElement) {
        LimitingResourceQueueElement queueElement = getEquivalent(externalElement);
        LimitingResourceQueue queue = queueElement.getLimitingResourceQueue();

        if ( queue != null ) {
            queue.removeLimitingResourceQueueElement(queueElement);
            unassignedElements.add(queueElement);
        }
    }

    public void removeUnassigned(LimitingResourceQueueElement queueElement) {
        unassignedElements.remove(queueElement);
    }

    public List<LimitingResourceQueue> getAssignableQueues(LimitingResourceQueueElement element) {
        final ResourceAllocation<?> resourceAllocation = element.getResourceAllocation();

        if ( resourceAllocation instanceof SpecificResourceAllocation ) {
            LimitingResourceQueue queue = getQueueFor(element.getResource());
            Validate.notNull(queue);
            return Collections.singletonList(queue);

        } else if ( resourceAllocation instanceof GenericResourceAllocation ) {
            final GenericResourceAllocation generic = (GenericResourceAllocation) element.getResourceAllocation();
            return findQueuesMatchingCriteria(generic);
        }

        throw new RuntimeException("unexpected type of: " + resourceAllocation);
    }

    private LimitingResourceQueue getQueueFor(Resource resource) {
        return queuesByResourceId.get(resource.getId());
    }

    public InsertionRequirements getRequirementsFor(LimitingResourceQueueElement element) {
        List<LimitingResourceQueueDependency> dependenciesStart = new ArrayList<>();
        List<LimitingResourceQueueDependency> dependenciesEnd = new ArrayList<>();
        fillIncoming(element, dependenciesStart, dependenciesEnd);

        return InsertionRequirements.forElement(getEquivalent(element), dependenciesStart, dependenciesEnd);
    }

    public InsertionRequirements getRequirementsFor(LimitingResourceQueueElement element, DateAndHour startAt) {
        List<LimitingResourceQueueDependency> dependenciesStart = new ArrayList<>();
        List<LimitingResourceQueueDependency> dependenciesEnd = new ArrayList<>();
        fillIncoming(element, dependenciesStart, dependenciesEnd);

        return InsertionRequirements.forElement(getEquivalent(element), dependenciesStart, dependenciesEnd, startAt);
    }

    private void fillIncoming(LimitingResourceQueueElement element,
            List<LimitingResourceQueueDependency> dependenciesStart,
            List<LimitingResourceQueueDependency> dependenciesEnd) {

        Set<LimitingResourceQueueDependency> incoming = graph.incomingEdgesOf(element);
        for (LimitingResourceQueueDependency each : incoming) {

            List<LimitingResourceQueueDependency> addingTo =
                    each.modifiesDestinationStart() ? dependenciesStart : dependenciesEnd;

            if ( each.isOriginNotDetached() ) {
                addingTo.add(each);
            } else {
                fillIncoming(each, addingTo);
            }
        }
    }

    private void fillIncoming(LimitingResourceQueueDependency next, List<LimitingResourceQueueDependency> result) {
        Set<LimitingResourceQueueDependency> incoming = graph.incomingEdgesOf(next.getHasAsOrigin());

        for (LimitingResourceQueueDependency each : incoming) {

            if ( each.propagatesThrough(next) ) {

                if ( each.isOriginNotDetached() ) {
                    result.add(each);
                } else {
                    fillIncoming(each, result);
                }
            }
        }
    }

    /**
     * @return all the gaps that could potentially fit <code>element</code>
     *         ordered by start date
     */
    public List<GapOnQueue> getPotentiallyValidGapsFor(InsertionRequirements requirements) {
        List<LimitingResourceQueue> assignableQueues = getAssignableQueues(requirements.getElement());
        List<List<GapOnQueue>> allGaps = gapsFor(assignableQueues, requirements);

        return GapsMergeSort.sort(allGaps);
    }

    private List<List<GapOnQueue>> gapsFor(List<LimitingResourceQueue> assignableQueues,
                                           InsertionRequirements requirements) {

        List<List<GapOnQueue>> result = new ArrayList<>();
        for (LimitingResourceQueue each : assignableQueues) {
            result.add(each.getGapsPotentiallyValidFor(requirements));
        }

        return result;
    }

    private List<LimitingResourceQueue> findQueuesMatchingCriteria(GenericResourceAllocation generic) {
        List<LimitingResourceQueue> result = new ArrayList<>();
        ResourceEnum resourceType = generic.getResourceType();
        Set<Criterion> criteria = generic.getCriterions();

        for (LimitingResourceQueue each : queues) {
            Resource resource = each.getResource();
            if ( resource.getType().equals(resourceType) && resource.satisfiesCriterionsAtSomePoint(criteria) ) {
                result.add(each);
            }
        }

        return result;
    }

    public static class Edge {
        public final LimitingResourceQueueElement source;

        public final LimitingResourceQueueElement target;

        public final QueueDependencyType type;

        public static Edge from(LimitingResourceQueueDependency dependency) {
            return new Edge(dependency.getHasAsOrigin(), dependency.getHasAsDestiny(), dependency.getType());
        }

        public static Edge insertionOrder(LimitingResourceQueueElement element,
                                          LimitingResourceQueueElement contiguousNext) {

            return new Edge(element, contiguousNext, QueueDependencyType.END_START);
        }

        private Edge(LimitingResourceQueueElement source,
                     LimitingResourceQueueElement target,
                     QueueDependencyType type) {

            Validate.notNull(source);
            Validate.notNull(target);
            Validate.notNull(type);
            this.source = source;
            this.target = target;
            this.type = type;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(source).append(target).append(type).toHashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if ( obj instanceof Edge ) {
                Edge another = (Edge) obj;

                return new EqualsBuilder()
                        .append(source, another.source)
                        .append(target, another.target)
                        .append(type, another.type).isEquals();
            }

            return false;
        }

    }

    public DirectedGraph<LimitingResourceQueueElement, Edge> getPotentiallyAffectedByInsertion(
            LimitingResourceQueueElement element) {

        DirectedMultigraph<LimitingResourceQueueElement, Edge> result;
        result = asEdges(onQueues(buildOutgoingGraphFor(getEquivalent(element))));

        Map<LimitingResourceQueue, LimitingResourceQueueElement> earliestForEachQueue =
                earliest(byQueue(result.vertexSet()));

        for (Entry<LimitingResourceQueue, LimitingResourceQueueElement> each : earliestForEachQueue.entrySet()) {
            LimitingResourceQueue queue = each.getKey();
            LimitingResourceQueueElement earliest = each.getValue();
            addInsertionOrderOnQueueEdges(result, earliest, queue.getElementsAfter(earliest));
        }

        return result;

    }

    private DirectedGraph<LimitingResourceQueueElement, LimitingResourceQueueDependency> onQueues(
            DirectedGraph<LimitingResourceQueueElement, LimitingResourceQueueDependency> graph) {

        SimpleDirectedGraph<LimitingResourceQueueElement, LimitingResourceQueueDependency> result;
        result = instantiateDirectedGraph();

        for (LimitingResourceQueueDependency each : graph.edgeSet()) {
            if ( !each.getHasAsOrigin().isDetached() && !each.getHasAsDestiny().isDetached() ) {
                addDependency(result, each);
            }
        }

        return result;
    }

    private DirectedMultigraph<LimitingResourceQueueElement, Edge> asEdges(
            DirectedGraph<LimitingResourceQueueElement, LimitingResourceQueueDependency> graph) {

        DirectedMultigraph<LimitingResourceQueueElement, Edge> result = instantiateMultiGraph();
        for (LimitingResourceQueueDependency each : graph.edgeSet()) {
            Edge edge = Edge.from(each);
            result.addVertex(edge.source);
            result.addVertex(edge.target);
            result.addEdge(edge.source, edge.target, edge);
        }

        return result;
    }

    private DirectedMultigraph<LimitingResourceQueueElement, Edge> instantiateMultiGraph() {
        return new DirectedMultigraph<>(Edge.class);
    }

    private Map<LimitingResourceQueue, List<LimitingResourceQueueElement>> byQueue(
            Collection<? extends LimitingResourceQueueElement> vertexSet) {

        Map<LimitingResourceQueue, List<LimitingResourceQueueElement>> result = new HashMap<>();
        for (LimitingResourceQueueElement each : vertexSet) {
            assert each.getLimitingResourceQueue() != null;
            forQueue(result, each.getLimitingResourceQueue()).add(each);
        }

        return result;
    }

    private List<LimitingResourceQueueElement> forQueue(
            Map<LimitingResourceQueue, List<LimitingResourceQueueElement>> map,
            LimitingResourceQueue queue) {

        List<LimitingResourceQueueElement> result = map.get(queue);
        if ( result == null ) {
            result = new ArrayList<>();
            map.put(queue, result);
        }

        return result;
    }

    private static Map<LimitingResourceQueue, LimitingResourceQueueElement> earliest(
            Map<LimitingResourceQueue, List<LimitingResourceQueueElement>> byQueue) {

        Map<LimitingResourceQueue, LimitingResourceQueueElement> result = new HashMap<>();
        for (Entry<LimitingResourceQueue, List<LimitingResourceQueueElement>> each : byQueue.entrySet()) {
            result.put(each.getKey(), earliest(each.getValue()));
        }

        return result;
    }

    private static LimitingResourceQueueElement earliest(List<LimitingResourceQueueElement> list) {
        Validate.isTrue(!list.isEmpty());

        return Collections.min(list, LimitingResourceQueueElement.byStartTimeComparator());
    }

    private void addInsertionOrderOnQueueEdges(
            DirectedGraph<LimitingResourceQueueElement, Edge> result,
            LimitingResourceQueueElement first,
            List<LimitingResourceQueueElement> elements) {

        LimitingResourceQueueElement previous = first;
        for (LimitingResourceQueueElement each : elements) {

            // Fixes bug #553, "No such vertex in graph".
            // It seems that, for some reason, some of the vertexes (queue elements) are not in graph at this point
            if ( !result.containsVertex(previous) ) {
                result.addVertex(previous);
            }

            if ( !result.containsVertex(each) ) {
                result.addVertex(each);
            }

            result.addEdge(previous, each, Edge.insertionOrder(previous, each));
            previous = each;
        }
    }

    /**
     * @param externalQueueElement
     *            the queue element to insert
     * @return the list of elements that must be reinserted due to the insertion
     *         of <code>externalQueueElement</code>
     */
    public List<LimitingResourceQueueElement> getInsertionsToBeDoneFor(
            LimitingResourceQueueElement externalQueueElement) {

        LimitingResourceQueueElement queueElement = getEquivalent(externalQueueElement);

        DirectedGraph<LimitingResourceQueueElement, LimitingResourceQueueDependency> subGraph =
                buildOutgoingGraphFor(queueElement);

        CycleDetector<LimitingResourceQueueElement, LimitingResourceQueueDependency> cycleDetector =
                cycleDetector(subGraph);

        if ( cycleDetector.detectCycles() ) {
            throw new IllegalStateException("subgraph has cycles");
        }

        List<LimitingResourceQueueElement> result = new ArrayList<>();
        result.add(queueElement);
        result.addAll(getElementsOrderedTopologically(subGraph));
        unassignFromQueues(result);

        return result;
    }

    private DirectedGraph<LimitingResourceQueueElement, LimitingResourceQueueDependency> buildOutgoingGraphFor(
            LimitingResourceQueueElement queueElement) {

        SimpleDirectedGraph<LimitingResourceQueueElement, LimitingResourceQueueDependency> result =
                instantiateDirectedGraph();

        buildOutgoingGraphFor(result, queueElement);

        return result;
    }

    private void buildOutgoingGraphFor(
            DirectedGraph<LimitingResourceQueueElement, LimitingResourceQueueDependency> result,
            LimitingResourceQueueElement element) {

        Set<LimitingResourceQueueDependency> outgoingEdgesOf = graph.outgoingEdgesOf(element);
        result.addVertex(element);

        for (LimitingResourceQueueDependency each : outgoingEdgesOf) {
            addDependency(result, each);
            buildOutgoingGraphFor(result, each.getHasAsDestiny());
        }
    }

    private CycleDetector<LimitingResourceQueueElement, LimitingResourceQueueDependency> cycleDetector(
            DirectedGraph<LimitingResourceQueueElement, LimitingResourceQueueDependency> subGraph) {

        return new CycleDetector<>(subGraph);
    }

    private List<LimitingResourceQueueElement> getElementsOrderedTopologically(
            DirectedGraph<LimitingResourceQueueElement, LimitingResourceQueueDependency> subGraph) {

        return onlyAssigned(toList(topologicalIterator(subGraph)));
    }

    public static <V, E> TopologicalOrderIterator<V, E> topologicalIterator(DirectedGraph<V, E> subGraph) {
        return new TopologicalOrderIterator<>(subGraph);
    }

    public static <T> List<T> toList(final Iterator<T> iterator) {
        List<T> result = new ArrayList<T>();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }

        return result;
    }

    private List<LimitingResourceQueueElement> onlyAssigned(List<LimitingResourceQueueElement> list) {
        List<LimitingResourceQueueElement> result = new ArrayList<>();
        for (LimitingResourceQueueElement each : list) {
            if ( !each.isDetached() ) {
                result.add(each);
            }
        }

        return result;
    }

    private void unassignFromQueues(List<LimitingResourceQueueElement> result) {
        for (LimitingResourceQueueElement each : result) {
            if ( !each.isDetached() ) {
                unassingFromQueue(each);
            }
        }
    }

    public void replaceLimitingResourceQueueElement(
            LimitingResourceQueueElement oldElement,
            LimitingResourceQueueElement newElement) {

        if ( oldElement.isNewObject() ) {
            elementsById.put(oldElement.getId(), oldElement);
        }

        LimitingResourceQueueElement element = getEquivalent(oldElement);

        if ( element.hasDayAssignments() ) {
            unassingFromQueue(element);
        }

        unassignedElements.remove(element);
        elementsById.remove(element.getId());
        graph.removeVertex(element);

        unassignedElements.add(newElement);
        elementsById.put(newElement.getId(), newElement);
        graph.addVertex(newElement);

        for (LimitingResourceQueueDependency each: newElement.getDependenciesAsOrigin()) {
            graph.addEdge(each.getHasAsOrigin(), each.getHasAsDestiny(), each);
        }

        for (LimitingResourceQueueDependency each: newElement.getDependenciesAsDestiny()) {
            graph.addEdge(each.getHasAsOrigin(), each.getHasAsDestiny(), each);
        }

    }

    public void idChangedFor(Long previousId, LimitingResourceQueueElement element) {
        elementsById.remove(previousId);
        elementsById.put(element.getId(), element);
    }

    public List<LimitingResourceQueueElement> inTopologicalOrder(List<LimitingResourceQueueElement> queueElements) {
        return toList(topologicalIterator(buildSubgraphFor(queueElements)));
    }

    /**
     * Constructs a graph composed only by queueElements
     *
     * @param queueElements
     * @return
     */
    private DirectedGraph<LimitingResourceQueueElement, LimitingResourceQueueDependency> buildSubgraphFor(
            List<LimitingResourceQueueElement> queueElements) {

        SimpleDirectedGraph<LimitingResourceQueueElement, LimitingResourceQueueDependency> result =
                instantiateDirectedGraph();

        // Iterate through elements and construct graph
        for (LimitingResourceQueueElement each : queueElements) {
            result.addVertex(each);

            for (LimitingResourceQueueDependency dependency : each.getDependenciesAsOrigin()) {

                LimitingResourceQueueElement destiny = dependency.getHasAsDestiny();

                if ( queueElements.contains(destiny) ) {
                    // Add source, destiny and edge between them
                    addDependency(result, dependency);
                }
            }
        }

        return result;
    }

    /**
     * Returns a list of gaps plus its right-adjacent queueElement starting from
     * allocationTime
     *
     * If there's already an element at allocationTime in the queue, the gap is
     * null and the element is the element at that position
     *
     * @param queue
     * @param allocationTime
     * @return
     */
    public List<GapOnQueueWithQueueElement> getGapsWithQueueElementsOnQueueSince(
            LimitingResourceQueue queue, DateAndHour allocationTime) {

        return gapsWithQueueElementsFor(queue, allocationTime);
    }

    private List<GapOnQueueWithQueueElement> gapsWithQueueElementsFor(
            LimitingResourceQueue queue, DateAndHour allocationTime) {

        List<GapOnQueueWithQueueElement> result = new ArrayList<>();

        DateAndHour previousEnd = null;
        for (LimitingResourceQueueElement each : queue.getLimitingResourceQueueElements()) {

            DateAndHour startTime = each.getStartTime();

            if ( !startTime.isBefore(allocationTime) ) {

                if ( previousEnd == null || startTime.isAfter(previousEnd) ) {
                    Gap gap = Gap.create(queue.getResource(), previousEnd, startTime);
                    result.add(GapOnQueueWithQueueElement.create(gap.onQueue(queue), each));
                }
            }

            previousEnd = each.getEndTime();
        }

        Gap gap = Gap.create(queue.getResource(), previousEnd, null);
        result.add(GapOnQueueWithQueueElement.create(gap.onQueue(queue), null));

        return result;
    }

}

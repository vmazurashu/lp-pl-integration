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

import static org.libreplan.web.I18nHelper._;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.planner.entities.DayAssignment;
import org.libreplan.business.planner.entities.GenericResourceAllocation;
import org.libreplan.business.planner.entities.ResourceAllocation;
import org.libreplan.business.planner.entities.SpecificResourceAllocation;
import org.libreplan.business.planner.entities.Task;
import org.libreplan.business.planner.limiting.entities.DateAndHour;
import org.libreplan.business.planner.limiting.entities.LimitingResourceQueueElement;
import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.resources.entities.LimitingResourceQueue;
import org.libreplan.business.workingday.IntraDayDate.PartialDay;
import org.zkoss.ganttz.DatesMapperOnInterval;
import org.zkoss.ganttz.IDatesMapper;
import org.zkoss.ganttz.timetracker.TimeTracker;
import org.zkoss.ganttz.timetracker.zoom.IZoomLevelChangedListener;
import org.zkoss.ganttz.timetracker.zoom.ZoomLevel;
import org.zkoss.ganttz.util.MenuBuilder;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.ext.AfterCompose;
import org.zkoss.zk.ui.sys.ContentRenderer;
import org.zkoss.zul.Div;
import org.zkoss.zul.impl.XulElement;


/**
 * This class wraps ResourceLoad data inside an specific HTML Div component.
 *
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 * @author Vova Perebykivskyi <vova@libreplan-enterprise.com>
 */
public class QueueComponent extends XulElement implements AfterCompose {

    private static final int DEADLINE_MARK_HALF_WIDTH = 5;

    private final QueueListComponent queueListComponent;

    private final TimeTracker timeTracker;

    private transient IZoomLevelChangedListener zoomChangedListener;

    private LimitingResourceQueue limitingResourceQueue;

    private List<QueueTask> queueTasks = new ArrayList<>();

    private QueueComponent(final QueueListComponent queueListComponent,
                           final TimeTracker timeTracker,
                           final LimitingResourceQueue limitingResourceQueue) {

        this.queueListComponent = queueListComponent;
        this.limitingResourceQueue = limitingResourceQueue;
        this.timeTracker = timeTracker;

        createChildren(limitingResourceQueue, timeTracker.getMapper());

        /* Do not replace it with lamda */
        zoomChangedListener = new IZoomLevelChangedListener() {
            @Override
            public void zoomLevelChanged(ZoomLevel detailLevel) {
                getChildren().clear();
                createChildren(limitingResourceQueue, timeTracker.getMapper());
            }
        };

        this.timeTracker.addZoomListener(zoomChangedListener);
    }

    @Override
    public void afterCompose() {
        appendContextMenus();
    }

    public static QueueComponent create(QueueListComponent queueListComponent,
                                        TimeTracker timeTracker,
                                        LimitingResourceQueue limitingResourceQueue) {

        return new QueueComponent(queueListComponent, timeTracker, limitingResourceQueue);
    }

    public List<QueueTask> getQueueTasks() {
        return queueTasks;
    }

    public void setLimitingResourceQueue(LimitingResourceQueue limitingResourceQueue) {
        this.limitingResourceQueue = limitingResourceQueue;
    }

    private void createChildren(LimitingResourceQueue limitingResourceQueue, IDatesMapper mapper) {
        List<QueueTask> queueTasks = createQueueTasks(mapper, limitingResourceQueue.getLimitingResourceQueueElements());
        appendQueueTasks(queueTasks);
    }

    public QueueListComponent getQueueListComponent() {
        return queueListComponent;
    }

    public LimitingResourcesPanel getLimitingResourcesPanel() {
        return queueListComponent.getLimitingResourcePanel();
    }

    public void invalidate() {
        removeChildren();
        appendQueueElements(limitingResourceQueue.getLimitingResourceQueueElements());
    }

    private void removeChildren() {
        for (QueueTask each: queueTasks) {
            removeChild(each);
        }
        queueTasks.clear();
    }

    private void appendQueueTasks(List<QueueTask> queueTasks) {
        for (QueueTask each: queueTasks) {
            appendQueueTask(each);
        }
    }

    private void appendQueueTask(QueueTask queueTask) {
        queueTasks.add(queueTask);

        /*
         * In this case after we migrated from ZK5 to ZK8, ZK was appending div to QueueComponent,
         * on every allocation it was creating new QueueComponents, but DOM tree was still the same.
         */
        getLimitingResourcesPanel().getFellow("insertionPointRightPanel").invalidate();

        appendChild(queueTask);
    }

    private void removeQueueTask(QueueTask queueTask) {
        queueTasks.remove(queueTask);
        removeChild(queueTask);
    }

    private List<QueueTask> createQueueTasks(IDatesMapper datesMapper, Set<LimitingResourceQueueElement> list) {

        List<QueueTask> result = new ArrayList<>();

        org.zkoss.ganttz.util.Interval interval = null;

        if ( timeTracker.getFilter() != null ) {
            timeTracker.getFilter().resetInterval();
            interval = timeTracker.getFilter().getCurrentPaginationInterval();
        }

        for (LimitingResourceQueueElement each : list) {

            if ( interval != null ) {

                if ( each.getEndDate().toDateTimeAtStartOfDay().isAfter(interval.getStart().toDateTimeAtStartOfDay()) &&
                        each.getStartDate().toDateTimeAtStartOfDay()
                                .isBefore(interval.getFinish().toDateTimeAtStartOfDay()) ) {

                    result.add(createQueueTask(datesMapper, each));
                }

            } else {
                result.add(createQueueTask(datesMapper, each));
            }
        }

        return result;
    }

    private static QueueTask createQueueTask(IDatesMapper datesMapper, LimitingResourceQueueElement element) {
        validateQueueElement(element);

        return createDivForElement(datesMapper, element);
    }

    private static OrderElement getRootOrder(Task task) {
        OrderElement order = task.getOrderElement();

        while (order.getParent() != null) {
            order = order.getParent();
        }

        return order;
    }

    private static String createTooltiptext(LimitingResourceQueueElement element) {
        final Task task = element.getResourceAllocation().getTask();
        final OrderElement order = getRootOrder(task);

        StringBuilder result = new StringBuilder();
        result.append(_("Project: {0}", order.getName())).append(" ");
        result.append(_("Task: {0}", task.getName())).append(" ");
        result.append(_("Completed: {0}%", element.getAdvancePercentage().multiply(new BigDecimal(100)))).append(" ");

        final ResourceAllocation<?> resourceAllocation = element.getResourceAllocation();

        if ( resourceAllocation instanceof SpecificResourceAllocation ) {

            final SpecificResourceAllocation specific = (SpecificResourceAllocation) resourceAllocation;
            result.append(_("Resource: {0}", specific.getResource().getName())).append(" ");

        } else if ( resourceAllocation instanceof GenericResourceAllocation ) {

            final GenericResourceAllocation generic = (GenericResourceAllocation) resourceAllocation;

            /* TODO resolve deprecated */
            result.append(_("Criteria: {0}", Criterion.getCaptionFor(generic.getCriterions()))).append(" ");

        }
        result.append(_("Allocation: [{0},{1}]", element.getStartDate().toString(), element.getEndDate()));

        return result.toString();
    }

    /**
     * Returns end date considering % of task completion.
     *
     * @param element
     * @return {@link DateAndHour}
     */
    private static DateAndHour getAdvanceEndDate(LimitingResourceQueueElement element) {
        int hoursWorked = 0;

        final List<? extends DayAssignment> dayAssignments = element.getDayAssignments();

        if ( element.hasDayAssignments() ) {

            final int estimatedWorkedHours =
                    estimatedWorkedHours(element.getIntentedTotalHours(), element.getAdvancePercentage());

            for (DayAssignment each: dayAssignments) {
                hoursWorked += each.getDuration().getHours();

                if ( hoursWorked >= estimatedWorkedHours ) {
                    int hourSlot = each.getDuration().getHours() - (hoursWorked - estimatedWorkedHours);
                    return new DateAndHour(each.getDay(), hourSlot);
                }

            }
        }

        if ( hoursWorked != 0 ) {
            DayAssignment lastDayAssignment = dayAssignments.get(dayAssignments.size() - 1);

            return new DateAndHour(lastDayAssignment.getDay(), lastDayAssignment.getDuration().getHours());
        }

        return null;
    }

    private static int estimatedWorkedHours(Integer totalHours, BigDecimal percentageWorked) {
        boolean condition = totalHours != null && percentageWorked != null;
        return condition ? percentageWorked.multiply(new BigDecimal(totalHours)).intValue() : 0;
    }

    private static QueueTask createDivForElement(IDatesMapper datesMapper, LimitingResourceQueueElement queueElement) {

        final Task task = queueElement.getResourceAllocation().getTask();
        final OrderElement order = getRootOrder(task);

        QueueTask result = new QueueTask(queueElement);
        String cssClass = "queue-element";
        result.setTooltiptext(createTooltiptext(queueElement));

        int startPixels = getStartPixels(datesMapper, queueElement);
        result.setLeft(forCSS(startPixels));

        if ( startPixels < 0 ) {
            cssClass += " truncated-start ";
        }

        int taskWidth = getWidthPixels(datesMapper, queueElement);

        if ( (startPixels + taskWidth) > datesMapper.getHorizontalSize() ) {
            taskWidth = datesMapper.getHorizontalSize() - startPixels;
            cssClass += " truncated-end ";
        } else {
            result.appendChild(generateNonWorkableShade(datesMapper, queueElement));
        }
        result.setWidth(forCSS(taskWidth));

        LocalDate deadlineDate = task.getDeadline();
        boolean isOrderDeadline = false;

        if ( deadlineDate == null ) {

            Date orderDate = order.getDeadline();

            if ( orderDate != null ) {

                deadlineDate = LocalDate.fromDateFields(orderDate);
                isOrderDeadline = true;

            }
        }

        if ( deadlineDate != null ) {

            int deadlinePosition = getDeadlinePixels(datesMapper, deadlineDate);

            if ( deadlinePosition < datesMapper.getHorizontalSize() ) {
                Div deadline = new Div();
                deadline.setSclass(isOrderDeadline ? "deadline order-deadline" : "deadline");
                deadline.setLeft((deadlinePosition - startPixels - DEADLINE_MARK_HALF_WIDTH) + "px");
                result.appendChild(deadline);
                result.appendChild(generateNonWorkableShade(datesMapper, queueElement));
            }

            if ( deadlineDate.isBefore(queueElement.getEndDate()) ) {
                cssClass += " unmet-deadline ";
            }
        }

        result.setClass(cssClass);
        result.appendChild(generateCompletionShade(datesMapper, queueElement));
        Component progressBar = generateProgressBar(datesMapper, queueElement);

        if ( progressBar != null ) {
            result.appendChild(progressBar);
        }

        return result;
    }

    private static Component generateProgressBar(IDatesMapper datesMapper, LimitingResourceQueueElement queueElement) {

        DateAndHour advancementEndDate = getAdvanceEndDate(queueElement);

        if ( advancementEndDate == null ) {
            return null;
        }

        Duration durationBetween = new Duration(
                queueElement.getStartTime().toDateTime().getMillis(), advancementEndDate.toDateTime().getMillis());

        Div progressBar = new Div();

        if ( !queueElement.getStartDate().isEqual(advancementEndDate.getDate()) ) {
            progressBar.setWidth(datesMapper.toPixels(durationBetween) + "px");
            progressBar.setSclass("queue-progress-bar");
        }

        return progressBar;
    }

    private static Div generateNonWorkableShade(IDatesMapper datesMapper, LimitingResourceQueueElement queueElement) {

        int workableHours = queueElement
                .getLimitingResourceQueue()
                .getResource()
                .getCalendar()
                .getCapacityOn(PartialDay.wholeDay(queueElement.getEndDate()))
                .roundToHours();

        Long shadeWidth = (24 - workableHours) *
                DatesMapperOnInterval.MILISECONDS_PER_HOUR / datesMapper.getMilisecondsPerPixel();

        Long lShadeLeft =  (workableHours - queueElement.getEndHour()) *
                DatesMapperOnInterval.MILISECONDS_PER_HOUR / datesMapper.getMilisecondsPerPixel();

        int shadeLeft =  lShadeLeft.intValue() + shadeWidth.intValue();

        Div notWorkableHoursShade = new Div();

        notWorkableHoursShade.setTooltiptext(_("Workable capacity for this period ") + workableHours + _(" hours"));
        notWorkableHoursShade.setContext("");
        notWorkableHoursShade.setSclass("not-workable-hours");
        notWorkableHoursShade.setStyle("left: " + shadeLeft + "px; width: " + shadeWidth.intValue() + "px;");

        return notWorkableHoursShade;
    }

    private static Div generateCompletionShade(IDatesMapper datesMapper, LimitingResourceQueueElement queueElement) {

        int workableHours = queueElement
                .getLimitingResourceQueue()
                .getResource()
                .getCalendar()
                .getCapacityOn(PartialDay.wholeDay(queueElement.getEndDate()))
                .roundToHours();

        Long shadeWidth = (24 - workableHours) *
                DatesMapperOnInterval.MILISECONDS_PER_HOUR / datesMapper.getMilisecondsPerPixel();

        Long lShadeLeft = (workableHours - queueElement.getEndHour()) *
                DatesMapperOnInterval.MILISECONDS_PER_HOUR / datesMapper.getMilisecondsPerPixel();

        int shadeLeft = lShadeLeft.intValue() + shadeWidth.intValue();

        Div notWorkableHoursShade = new Div();

        notWorkableHoursShade.setContext("");
        notWorkableHoursShade.setSclass("limiting-completion");
        notWorkableHoursShade.setStyle("left: " + shadeLeft + "px; width: " + shadeWidth.intValue() + "px;");

        return notWorkableHoursShade;
    }

    private static int getWidthPixels(IDatesMapper datesMapper, LimitingResourceQueueElement queueElement) {
        return datesMapper.toPixels(queueElement.getLengthBetween());
    }

    private static int getDeadlinePixels(IDatesMapper datesMapper, LocalDate deadlineDate) {
        // Deadline date is considered inclusively
        return datesMapper.toPixelsAbsolute(deadlineDate.plusDays(1).toDateTimeAtStartOfDay().getMillis());
    }

    private static String forCSS(int pixels) {
        return String.format("%dpx", pixels);
    }

    private static int getStartPixels(IDatesMapper datesMapper, LimitingResourceQueueElement queueElement) {
        return datesMapper.toPixelsAbsolute(
                queueElement.getStartDate().toDateTimeAtStartOfDay().getMillis() +
                        queueElement.getStartHour() * DatesMapperOnInterval.MILISECONDS_PER_HOUR);
    }

    public void appendQueueElements(SortedSet<LimitingResourceQueueElement> elements) {
        for (LimitingResourceQueueElement each : elements) {
            appendQueueElement(each);
        }
    }

    public void appendQueueElement(LimitingResourceQueueElement element) {
        QueueTask queueTask = createQueueTask(element);
        appendQueueTask(queueTask);
        appendMenu(queueTask);
        addDependenciesInPanel(element);
    }

    public void removeQueueElement(LimitingResourceQueueElement element) {
        QueueTask queueTask = findQueueTaskByElement(element);
        if ( queueTask != null ) {
            removeQueueTask(queueTask);
        }
    }

    private QueueTask findQueueTaskByElement(LimitingResourceQueueElement element) {
        for (QueueTask each: queueTasks) {
            if ( each.getLimitingResourceQueueElement().getId().equals(element.getId()) ) {
                return each;
            }
        }
        return null;
    }

    private QueueTask createQueueTask(LimitingResourceQueueElement element) {
        validateQueueElement(element);
        return createDivForElement(timeTracker.getMapper(), element);
    }

    private void addDependenciesInPanel(LimitingResourceQueueElement element) {
        getLimitingResourcesPanel().addDependenciesFor(element);
    }

    public String getResourceName() {
        return limitingResourceQueue.getResource().getName();
    }

    private static void validateQueueElement(LimitingResourceQueueElement queueElement) {
        if ( (queueElement.getStartDate() == null ) || ( queueElement.getEndDate() == null) ) {
            throw new ValidationException(_("Invalid queue element"));
        }
    }

    private void appendMenu(QueueTask divElement) {
        if ( divElement.getPage() != null ) {
            MenuBuilder<QueueTask> menuBuilder = MenuBuilder.on(divElement.getPage(), divElement);

            menuBuilder.item(
                    _("Edit"), "/common/img/ico_editar.png", (chosen, event) -> editResourceAllocation(chosen));

            menuBuilder.item(_("Unassign"), "/common/img/ico_borrar.png", (chosen, event) -> unassign(chosen));

            menuBuilder.item(_("Move"), "", (chosen, event) -> moveQueueTask(chosen));

            divElement.setContext(menuBuilder.createWithoutSettingContext());
        }
    }

    private void editResourceAllocation(QueueTask queueTask) {
        getLimitingResourcesPanel().editResourceAllocation(queueTask);
    }

    private void moveQueueTask(QueueTask queueTask) {
        getLimitingResourcesPanel().moveQueueTask(queueTask);
    }

    private void unassign(QueueTask chosen) {
        getLimitingResourcesPanel().unschedule(chosen);
    }

    private void appendContextMenus() {
        for (QueueTask each : queueTasks) {
            appendMenu(each);
        }
    }

    public void renderProperties(ContentRenderer renderer) throws IOException{
        super.renderProperties(renderer);
        render(renderer, "_resourceName", getResourceName());
    }

}

/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2012 Igalia, S.L.
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

package org.libreplan.web.users.dashboard;

import org.joda.time.LocalDate;
import org.libreplan.business.advance.entities.AdvanceMeasurement;
import org.libreplan.business.common.entities.PersonalTimesheetsPeriodicityEnum;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.planner.entities.Task;
import org.libreplan.web.common.Util;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;

import java.text.MessageFormat;
import java.util.List;

import static org.libreplan.web.I18nHelper._;

/**
 * Controller for "My tasks" area in the user dashboard window.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 * @author Vova Perebykivskyi <vova@libreplan-enterprise.com>
 */
@SuppressWarnings("serial")
public class MyTasksAreaController extends GenericForwardComposer {

    private IMyTasksAreaModel myTasksAreaModel;

    private IPersonalTimesheetController personalTimesheetController;

    private RowRenderer tasksRenderer = new RowRenderer() {

        @Override
        public void render(Row row, Object data, int i) throws Exception {

            Task task = (Task) data;
            row.setValue(task);

            OrderElement orderElement = task.getOrderElement();

            Util.appendLabel(row, orderElement.getOrder().getName());
            Util.appendLabel(row, task.getName());
            Util.appendLabel(row, orderElement.getDescription());

            Util.appendLabel(row, task.getStartAsLocalDate().toString());
            Util.appendLabel(row, task.getEndAsLocalDate().toString());

            Util.appendLabel(row, _("{0} h", task.getSumOfAssignedEffort().toHoursAsDecimalWithScale(0).toString()));
            Util.appendLabel(row, _("{0} h", orderElement.getEffortAsString()));
            Util.appendLabel(row, getProgress(orderElement));
            appendTimeTrackingButton(row, task);
        }

        private String getProgress(OrderElement orderElement) {

            AdvanceMeasurement lastAdvanceMeasurement = orderElement.getLastAdvanceMeasurement();

            return lastAdvanceMeasurement != null
                    ? MessageFormat.format(
                    "[{0} %] ({1})", lastAdvanceMeasurement.getValue(), lastAdvanceMeasurement.getDate())
                    : "";
        }

        private void appendTimeTrackingButton(Row row, final Task task) {

            EventListener trackTimeButtonListener = new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    personalTimesheetController.goToCreateOrEditForm(getPersonalTimesheetDateForTask(task));
                }

                private LocalDate getPersonalTimesheetDateForTask(Task task) {
                    LocalDate start = task.getStartAsLocalDate();
                    LocalDate end = task.getEndAsLocalDate();

                    LocalDate currentDate = new LocalDate();
                    PersonalTimesheetsPeriodicityEnum periodicity = myTasksAreaModel.getPersonalTimesheetsPeriodicity();
                    LocalDate min = periodicity.getStart(currentDate);
                    LocalDate max = periodicity.getEnd(currentDate);

                    if (dateBetween(start, min, max)) {
                        return start;
                    }

                    if (dateBetween(end, min, max)) {
                        return end;
                    }

                    if (dateBetween(currentDate, start, end)) {
                        return currentDate;
                    }

                    if (end.compareTo(min) < 0) {
                        return end;
                    }

                    if (start.compareTo(max) > 0) {
                        return start;
                    }

                    return currentDate;
                }

                private boolean dateBetween(LocalDate date, LocalDate start, LocalDate end) {
                    return (date.compareTo(start) >= 0) && (date.compareTo(end) <= 0);
                }
            };

            Button button = Util.createEditButton(trackTimeButtonListener);
            row.addEventListener(Events.ON_CLICK, trackTimeButtonListener);

            button.setTooltiptext(_("Track time"));

            row.appendChild(button);
        }

    };

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        comp.setAttribute("controller", this);
        injectObjects();
    }

    private void injectObjects() {
        if ( personalTimesheetController == null ) {

            personalTimesheetController =
                    (IPersonalTimesheetController) SpringUtil.getBean("personalTimesheetController");
        }

        if ( myTasksAreaModel == null ) {
            myTasksAreaModel = (IMyTasksAreaModel) SpringUtil.getBean("myTasksAreaModel");
        }
    }

    public List<Task> getTasks() {
        return myTasksAreaModel.getTasks();
    }

    public RowRenderer getTasksRenderer() {
        return tasksRenderer;
    }

}

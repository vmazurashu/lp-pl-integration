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

package org.libreplan.web.planner.calendar;

import org.libreplan.business.calendars.entities.BaseCalendar;
import org.libreplan.business.planner.entities.Task;
import org.libreplan.web.common.Util;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Window;

import java.util.List;

/**
 * Controller for allocate one calendar to a task view.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@org.springframework.stereotype.Component("calendarAllocationController")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CalendarAllocationController extends GenericForwardComposer {

    private ICalendarAllocationModel calendarAllocationModel;

    private Window window;

    private Combobox calendarCombo;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        window = (Window) comp;

        if ( calendarAllocationModel == null ) {
            calendarAllocationModel = (ICalendarAllocationModel) SpringUtil.getBean("calendarAllocationModel");
        }
    }

    public void showWindow(Task task) {
        calendarAllocationModel.setTask(task);

        calendarCombo = (Combobox) window.getFellow("calendarCombo");
        fillCalendarComboAndMarkSelected();

        try {
            Util.reloadBindings(window);
            window.doModal();
        } catch (SuspendNotAllowedException e) {
            throw new RuntimeException(e);
        }
    }

    private void fillCalendarComboAndMarkSelected() {
        calendarCombo.getChildren().clear();
        BaseCalendar assignedCalendar = calendarAllocationModel.getAssignedCalendar();

        List<BaseCalendar> calendars = calendarAllocationModel.getBaseCalendars();

        for (BaseCalendar calendar : calendars) {
            Comboitem item = new Comboitem(calendar.getName());
            item.setValue(calendar);
            calendarCombo.appendChild(item);

            if ((assignedCalendar != null) && calendar.getId().equals(assignedCalendar.getId()))
                calendarCombo.setSelectedItem(item);
        }
    }

    public void assign(Comboitem comboitem) {
        BaseCalendar calendar = comboitem.getValue();
        calendarAllocationModel.confirmAssignCalendar(calendar);
        window.setVisible(false);
    }

    public void cancel() {
        calendarAllocationModel.cancel();
        window.setVisible(false);
    }

    public BaseCalendar getAssignedCalendar() {
        return calendarAllocationModel.getAssignedCalendar();
    }

}
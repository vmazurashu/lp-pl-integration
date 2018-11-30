/*
 * This file is part of LibrePlan
 *
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
package org.libreplan.web.exceptionDays;

import static org.libreplan.web.I18nHelper._;

import java.util.ConcurrentModificationException;
import java.util.List;

import org.libreplan.business.calendars.entities.CalendarExceptionType;
import org.libreplan.business.calendars.entities.CalendarExceptionTypeColor;
import org.libreplan.business.calendars.entities.Capacity;
import org.libreplan.business.calendars.entities.PredefinedCalendarExceptionTypes;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.web.common.BaseCRUDController;
import org.libreplan.web.common.Level;
import org.libreplan.web.common.Util;
import org.libreplan.web.common.Util.Getter;
import org.libreplan.web.common.Util.Setter;
import org.libreplan.web.common.components.CapacityPicker;
import org.libreplan.web.common.components.EffortDurationPicker;
import org.libreplan.web.common.components.NewDataSortableGrid;
import org.zkoss.util.IllegalSyntaxException;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.CheckEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Textbox;

/**
 * @author Diego Pino <dpino@igalia.com>
 */
public class CalendarExceptionTypeCRUDController extends BaseCRUDController<CalendarExceptionType> {

    private ICalendarExceptionTypeModel calendarExceptionTypeModel;

    private Textbox tbName;

    private Checkbox overAssignable;

    private EffortDurationPicker standardEffort;

    private EffortDurationPicker extraEffort;

    private static ListitemRenderer calendarExceptionTypeColorRenderer = new ListitemRenderer() {
        @Override
        public void render(Listitem item, Object data, int i) throws Exception {
            CalendarExceptionTypeColor color = (CalendarExceptionTypeColor) data;
            item.setValue(color);
            item.appendChild(new Listcell(_(color.getName())));
        }
    };

    private RowRenderer exceptionDayTypeRenderer = new RowRenderer() {
        @Override
        public void render(Row row, Object data, int i) throws Exception {
            final CalendarExceptionType calendarExceptionType = (CalendarExceptionType) data;
            row.setValue(calendarExceptionType);

            if (calendarExceptionType.isUpdatable()) {
                row.addEventListener(Events.ON_CLICK, new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        goToEditForm(calendarExceptionType);
                    }
                });
            }

            row.appendChild(new Label(calendarExceptionType.getName()));
            row.appendChild(new Label(_(calendarExceptionType.getColor().getName())));
            row.appendChild(new Label(calendarExceptionType.getOverAssignableStr()));
            row.appendChild(new Label(calendarExceptionType.getCapacity().getStandardEffortString()));
            row.appendChild(new Label(calendarExceptionType.getCapacity().getExtraEffortString()));

            Hbox hbox = new Hbox();

            Button editButton = Util.createEditButton(new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    goToEditForm(calendarExceptionType);
                }
            });

            editButton.setDisabled(!calendarExceptionType.isUpdatable());
            hbox.appendChild(editButton);

            Button removeButton = Util.createRemoveButton(new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    confirmDelete(calendarExceptionType);
                }
            });

            removeButton.setDisabled(!calendarExceptionType.isUpdatable());
            hbox.appendChild(removeButton);

            row.appendChild(hbox);
        }
    };

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        if ( calendarExceptionTypeModel == null ) {
            calendarExceptionTypeModel = (ICalendarExceptionTypeModel) SpringUtil.getBean("calendarExceptionTypeModel");
        }

        initializeEditWindowComponents();
    }

    private void initializeCapacityPicker() {
        final CalendarExceptionType exceptionType = getExceptionDayType();

        CapacityPicker.workWith(
                overAssignable,
                standardEffort,
                extraEffort,
                new Getter<Capacity>() {
                    @Override
                    public Capacity get() {
                        return exceptionType.getCapacity();
                    }
                },
                new Setter<Capacity>() {
                    @Override
                    public void set(Capacity value) {
                        exceptionType.setCapacity(value);
                    }
                });
    }

    private void initializeEditWindowComponents() {
        tbName = (Textbox) editWindow.getFellowIfAny("tbName");
        overAssignable = Util.findComponentAt(editWindow, "overAssignable");
        standardEffort = Util.findComponentAt(editWindow, "standardEffort");
        standardEffort.initializeFor24HoursAnd0Minutes();
        extraEffort = Util.findComponentAt(editWindow, "extraEffort");
    }

    @Override
    protected void initCreate() {
        calendarExceptionTypeModel.initCreate();
        initializeCapacityPicker();
    }

    @Override
    protected void initEdit(CalendarExceptionType calendarExceptionType) {
        calendarExceptionTypeModel.initEdit(calendarExceptionType);
        initializeCapacityPicker();
    }

    public CalendarExceptionType getExceptionDayType() {
        return calendarExceptionTypeModel.getExceptionDayType();
    }

    public List<CalendarExceptionType> getExceptionDayTypes() {
        return calendarExceptionTypeModel.getExceptionDayTypes();
    }

    @Override
    protected void cancel() {
        clearFields();
    }

    private void clearFields() {
        tbName.setRawValue("");
    }

    @Override
    protected void save() throws ValidationException {
        calendarExceptionTypeModel.confirmSave();
        clearFields();
    }

    @Override
    protected boolean beforeDeleting(CalendarExceptionType calendarExceptionType) {
        if (PredefinedCalendarExceptionTypes.contains(calendarExceptionType)) {
            messagesForUser.showMessage(
                    Level.ERROR,
                    _("Cannot remove the predefined calendar exception day \"{0}\"", calendarExceptionType.getHumanId()));

            return false;
        }
        return true;
    }

    @Override
    protected void delete(CalendarExceptionType calendarExceptionType) {
        try {
            calendarExceptionTypeModel.confirmDelete(calendarExceptionType);
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IllegalSyntaxException e) {
            NewDataSortableGrid listExceptionDayTypes = (NewDataSortableGrid) listWindow.getFellowIfAny("listExceptionDayTypes");
            Row row = findRowByValue(listExceptionDayTypes, calendarExceptionType);
            throw new WrongValueException(row, e.getMessage());
        }
    }

    private Row findRowByValue(Grid grid, Object value) {
        final List<Row> rows = grid.getRows().getChildren();
        for (Row row: rows) {
            if (row.getValue().equals(value)) {
                return row;
            }
        }

        return null;
    }

    public void onCheckGenerateCode(Event e) {
        CheckEvent ce = (CheckEvent) e;
        if (ce.isChecked()) {
            // We have to auto-generate the code for new objects
            try {
                calendarExceptionTypeModel.setCodeAutogenerated(ce.isChecked());
            } catch (ConcurrentModificationException err) {
                messagesForUser.showMessage(Level.ERROR, err.getMessage());
            }
        }
        Util.reloadBindings(editWindow);
    }

    @Override
    protected String getEntityType() {
        return _("Calendar Exception Day");
    }

    @Override
    protected String getPluralEntityType() {
        return _("Calendar Exception Days");
    }

    @Override
    protected CalendarExceptionType getEntityBeingEdited() {
        return calendarExceptionTypeModel.getExceptionDayType();
    }

    public CalendarExceptionTypeColor[] getColors() {
        return CalendarExceptionTypeColor.values();
    }

    public ListitemRenderer getColorsRenderer() {
        return calendarExceptionTypeColorRenderer;
    }

    public RowRenderer getExceptionDayTypeRenderer() {
        return exceptionDayTypeRenderer;
    }

    public String getStyleColorOwnException() {
        return (getExceptionDayType() == null)
                ? ""
                : "background-color: " + getExceptionDayType().getColor().getColorOwnException();
    }

    public String getStyleColorDerivedException() {
        return (getExceptionDayType() == null)
                ? ""
                : "background-color: " + getExceptionDayType().getColor().getColorDerivedException();
    }

    public void reloadSampleColors() {
        Util.reloadBindings(editWindow.getFellow("colorSampleOwn"));
        Util.reloadBindings(editWindow.getFellow("colorSampleDerived"));
    }

}

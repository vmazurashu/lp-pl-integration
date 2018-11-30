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

package org.libreplan.web.calendars;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.joda.time.LocalDate;
import org.libreplan.business.calendars.entities.BaseCalendar;
import org.libreplan.business.calendars.entities.CalendarAvailability;
import org.libreplan.business.calendars.entities.CalendarData;
import org.libreplan.business.calendars.entities.CalendarData.Days;
import org.libreplan.business.calendars.entities.CalendarException;
import org.libreplan.business.calendars.entities.CalendarExceptionType;
import org.libreplan.business.calendars.entities.Capacity;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.workingday.EffortDuration;
import org.libreplan.web.common.IIntegrationEntityModel;

/**
 * This interface contains the operations to create/edit a {@link BaseCalendar}.
 * The creation/edition process of a {@link BaseCalendar} is conversational.
 *
 * <strong>Conversation state</strong>: the {@link BaseCalendar} instance.
 *
 * <strong>Non conversational steps</strong>: <code>getBaseCalendars</code> (to
 * return all base calendars).
 *
 * <strong>Conversation protocol:</strong>
 * <ul>
 * <li>
 * Initial conversation steps: <code>initCreate</code> (to create a
 * {@link BaseCalendar}) or (exclusive) <code>initEdit</code> (to edit an
 * existing {@link BaseCalendar}).</li>
 * <li>
 * Intermediate conversation steps: <code>getBaseCalendar</code> (to return the
 * {@link BaseCalendar} being edited/created).</li>
 * <li>
 * Final conversational steps: <code>confirmSave</code> (to save the
 * {@link BaseCalendar} being edited/created), <code>confirmRemove</code> (to
 * remove the {@link BaseCalendarModel}) or (exclusive) <code>cancel</code> (to
 * discard changes).</li>
 * </ul>
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 * @author Diego Pino Garcia <dpino@igalia.com>
 */
public interface IBaseCalendarModel extends IIntegrationEntityModel {


    /*
     * Non conversational steps
     */

    List<BaseCalendar> getBaseCalendars();

    void checkInvalidValuesCalendar(BaseCalendar entity)
            throws ValidationException;

    boolean isDefaultCalendar(BaseCalendar baseCalendar);

    List<CalendarExceptionType> getCalendarExceptionTypes();

    /*
     * Initial conversation steps
     */

    void initCreate();

    void initEdit(BaseCalendar baseCalendar);

    void initRemove(BaseCalendar baseCalendar);

    void initCreateDerived(BaseCalendar baseCalendar);

    void initCreateCopy(BaseCalendar baseCalendar);

    /*
     * Intermediate conversation steps
     */

    BaseCalendar getBaseCalendar();

    boolean isEditing();

    void setSelectedDay(LocalDate date);

    LocalDate getSelectedDay();

    EffortDuration getWorkableTime();

    Capacity getWorkableCapacity();

    void createException(CalendarExceptionType type, LocalDate startDate,
            LocalDate endDate, Capacity capacity);

    Boolean isDefault(Days day);

    void setDefault(Days day);

    void unsetDefault(Days day);

    boolean isExceptional();

    void removeException();

    boolean isDerived();

    List<BaseCalendar> getPossibleParentCalendars();

    BaseCalendar getParent();

    void setParent(BaseCalendar parent);

    boolean isParent(BaseCalendar calendar);

    LocalDate getDateValidFrom();

    void setDateValidFrom(LocalDate date);

    List<CalendarData> getHistoryVersions();

    void createNewVersion(LocalDate startDate, LocalDate expiringDate,
            BaseCalendar baseCalendar);

    boolean isLastVersion(LocalDate selectedDate);

    boolean isFirstVersion(LocalDate selectedDate);

    String getName();

    LocalDate getValidFrom(CalendarData calendarData);

    Set<CalendarException> getCalendarExceptions();

    void removeException(LocalDate date);

    CalendarExceptionType getCalendarExceptionType();

    CalendarExceptionType getCalendarExceptionType(LocalDate date);

    void updateException(CalendarExceptionType type, LocalDate startDate, LocalDate endDate, Capacity capacity);

    void removeCalendarData(CalendarData calendarData);

    CalendarData getLastCalendarData();

    CalendarData getCalendarData();

    boolean isResourceCalendar();

    List<CalendarAvailability> getCalendarAvailabilities();

    void removeCalendarAvailability(CalendarAvailability calendarAvailability);

    void createCalendarAvailability();

    void setStartDate(CalendarAvailability calendarAvailability, LocalDate startDate) throws IllegalArgumentException;

    void setEndDate(CalendarAvailability calendarAvailability, LocalDate endDate) throws IllegalArgumentException;

    /*
     * Final conversation steps
     */

    void confirmSave() throws ValidationException;

    void confirmSaveAndContinue() throws ValidationException;

    void confirmRemove(BaseCalendar calendar);

    void cancel();

    Capacity getCapacityAt(Days day);

    void setCapacityAt(Days day, Capacity capacity);

    void generateCalendarCodes();

    boolean isLastActivationPeriod(CalendarAvailability calendarAvailability);

    boolean isOwnException(CalendarException exception);

    void checkIsReferencedByOtherEntities(BaseCalendar calendar) throws ValidationException;

    BaseCalendar getCurrentParent();

    Date getCurrentStartDate();

    Date getCurrentExpiringDate();

    void checkChangeExpiringDate(CalendarData version, Date date) throws ValidationException;

    void checkAndChangeStartDate(CalendarData version, Date date) throws ValidationException;

    boolean isOwnExceptionDay();

    boolean isVirtualWorker();

    Integer getCapacity();

    void setCapacity(Integer capacity);

}

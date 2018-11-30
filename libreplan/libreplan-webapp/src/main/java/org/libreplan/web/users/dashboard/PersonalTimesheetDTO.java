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

import static org.libreplan.web.I18nHelper._;

import org.joda.time.LocalDate;
import org.libreplan.business.common.entities.PersonalTimesheetsPeriodicityEnum;
import org.libreplan.business.workingday.EffortDuration;
import org.libreplan.business.workreports.entities.WorkReport;

/**
 * Simple class to represent the personal timesheets to be shown in the list.
 * <br />
 * This is only a utility class for the UI, everything will be saved using {@link WorkReport} class.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public class PersonalTimesheetDTO {

    private static final String d_Y_PATTERN = "d Y";

    private static final String MMMM_d_PATTERN = "MMMM d";

    private static final String MMMM_Y_PATTERN = "MMMM y";

    private static final String MMMM_d_Y_PATTERN = "MMMM d y";

    private LocalDate date;

    private WorkReport workReport;

    private EffortDuration resourceCapacity;

    private EffortDuration totalHours;

    private int tasksNumber;

    /**
     * @param date
     *            The date of the timesheet.
     * @param workReport
     *            The work report of the personal timesheet, it could be
     *            <code>null</code> if it doesn't exist yet.
     * @param resourceCapacity
     *            The capacity of the resource bound to current user in the
     *            period of this timesheet.
     * @param totalHours
     *            Total hours worked by the resource bound to the current user
     *            in the personal timesheet
     * @param tasksNumber
     *            Number of tasks in the personal timesheet
     */
    public PersonalTimesheetDTO(LocalDate date, WorkReport workReport, EffortDuration resourceCapacity,
                                EffortDuration totalHours, int tasksNumber) {
        this.date = date;
        this.workReport = workReport;
        this.resourceCapacity = resourceCapacity;
        this.totalHours = totalHours;
        this.tasksNumber = tasksNumber;
    }

    public LocalDate getDate() {
        return date;
    }

    public WorkReport getWorkReport() {
        return workReport;
    }

    public EffortDuration getResourceCapacity() {
        return resourceCapacity;
    }

    public EffortDuration getTotalHours() {
        return totalHours;
    }

    public int getTasksNumber() {
        return tasksNumber;
    }

    public String toString(PersonalTimesheetsPeriodicityEnum periodicity) {
        return toString(periodicity, date);
    }

    /**
     * Returns a string representing the personal timehseet in a given
     * <code>date</code> depending on the <code>periodicity</code>.
     */
    public static String toString(PersonalTimesheetsPeriodicityEnum periodicity, LocalDate date) {
        switch (periodicity) {

            case WEEKLY:
                LocalDate start = periodicity.getStart(date);
                LocalDate end = periodicity.getEnd(date);

                String string = date.toString("w");
                    if (start.getMonthOfYear() == end.getMonthOfYear()) {
                        string += " (" + start.toString(MMMM_d_PATTERN)  + " - " + end.toString(d_Y_PATTERN) + ")";
                } else {
                    if (start.getYear() == end.getYear()) {
                        string += " (" + start.toString(MMMM_d_PATTERN) + " - " + end.toString(MMMM_d_Y_PATTERN) + ")";
                    } else {
                        string += " (" + start.toString(MMMM_d_Y_PATTERN) + " - " + end.toString(MMMM_d_Y_PATTERN) + ")";
                    }
                }
                return _("Week {0}", string);

            case TWICE_MONTHLY:
                return (date.getDayOfMonth() <= 15)
                        ? _("{0} 1st fortnight", date.toString(MMMM_d_Y_PATTERN))
                        : _("{0} 2nd fortnight", date.toString(MMMM_d_Y_PATTERN));

            case MONTHLY:
            default:
                return date.toString(MMMM_Y_PATTERN);
        }
    }

}

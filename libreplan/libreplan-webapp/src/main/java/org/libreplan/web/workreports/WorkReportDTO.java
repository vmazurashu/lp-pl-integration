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

package org.libreplan.web.workreports;

import java.util.Date;

import org.libreplan.business.workingday.EffortDuration;
import org.libreplan.business.workreports.entities.WorkReport;
import org.libreplan.business.workreports.entities.WorkReportLine;
import org.libreplan.business.workreports.entities.WorkReportType;

/**
 * DTO used to show the list of {@link WorkReport WorkReports}.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public class WorkReportDTO {

    public WorkReportDTO(WorkReport workReport) {
        this.workReport = workReport;
        this.dateStart = this.getDateStartWorkReport(workReport);
        this.dateFinish = this.getDateFinishWorkReport(workReport);

        WorkReportType workReportType = workReport.getWorkReportType();
        this.type = workReportType.getName();
        if (workReportType.isPersonalTimesheetsType()) {
            this.type += " - " + workReport.getResource().getShortDescription();
        }

        this.hours = workReport.getTotalEffortDuration();
    }

    private WorkReport workReport;

    private Date dateStart;

    private Date dateFinish;

    private String type;

    private EffortDuration hours;

    public WorkReport getWorkReport() {
        return workReport;
    }

    public Date getDateStart() {
        return dateStart;
    }

    public Date getDateFinish() {
        return dateFinish;
    }

    public String getType() {
        return type;
    }

    public Date getDateStartWorkReport(WorkReport workReport) {
        if (workReport != null) {

            if (workReport.getWorkReportType().getDateIsSharedByLines()) {
                return workReport.getDate();
            }

            // find the start date in its lines
            Date dateStart = null;
            if (workReport.getWorkReportLines().size() > 0) {
                WorkReportLine line0 = (WorkReportLine) workReport
                    .getWorkReportLines().toArray()[0];
                dateStart = line0.getDate();
                for (WorkReportLine line : workReport.getWorkReportLines()) {
                     if ((dateStart != null) && (line.getDate() != null)
                            && (line.getDate().before(dateStart))) {
                        dateStart = line.getDate();
                    }
                }
            }
            return dateStart;
        }
        return null;
    }

    public Date getDateFinishWorkReport(WorkReport workReport) {
        if (workReport != null) {
            if (workReport.getWorkReportType().getDateIsSharedByLines()) {
                return workReport.getDate();
            }

            Date dateFinish = null;
            if (workReport.getWorkReportLines().size() > 0) {
                WorkReportLine line0 = (WorkReportLine) workReport
                    .getWorkReportLines().toArray()[0];
                dateFinish = line0.getDate();
                for (WorkReportLine line : workReport.getWorkReportLines()) {
                    if ((dateFinish != null) && (line.getDate() != null)
                            && (line.getDate().after(dateFinish))) {
                        dateFinish = line.getDate();
                    }
                }
            }
            return dateFinish;
        }
        return null;
    }

    public String getCode() {
        return workReport.getCode();
    }

    public EffortDuration getHours() {
        return hours;
    }

}

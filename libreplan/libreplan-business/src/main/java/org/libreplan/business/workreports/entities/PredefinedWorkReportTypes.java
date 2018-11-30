/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2011 CafédeRed Solutions, S.L.
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
package org.libreplan.business.workreports.entities;

import org.libreplan.business.workreports.valueobjects.DescriptionField;

/**
 * Defines the default {@link WorkReportType WorkReportTypes}.
 *
 * @author Ignacio Díaz Teijido <ignacio.diaz@cafedered.com>
 * @author Manuel Rego Casasnovas <rego@igalia.com>
 */
public enum PredefinedWorkReportTypes {
    DEFAULT("Default", false, false, false),
    PERSONAL_TIMESHEETS("Personal timesheets", false, true, false),
    JIRA_TIMESHEETS("JIRA timesheets", false, false, false,
            DescriptionField.create("Comment", 255));

    private WorkReportType workReportType;

    private PredefinedWorkReportTypes(String name, boolean dateIsSharedByLines,
            boolean resourceIsSharedInLines, boolean orderElementIsSharedInLines) {
        workReportType = WorkReportType.create();
        workReportType.setName(name);
        workReportType.setDateIsSharedByLines(dateIsSharedByLines);
        workReportType.setResourceIsSharedInLines(resourceIsSharedInLines);
        workReportType
                .setOrderElementIsSharedInLines(orderElementIsSharedInLines);
    }

    private PredefinedWorkReportTypes(String name, boolean dateIsSharedByLines,
            boolean resourceIsSharedInLines,
            boolean orderElementIsSharedInLines,
            DescriptionField lineDescriptionField) {
        this(name, dateIsSharedByLines, resourceIsSharedInLines,
                orderElementIsSharedInLines);
        workReportType.addDescriptionFieldToEndLine(lineDescriptionField);
    }

    public WorkReportType getWorkReportType() {
        return workReportType;
    }

    public String getName() {
        return workReportType.getName();
    }

}

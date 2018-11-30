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

package org.libreplan.ws.calendars.api;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.libreplan.business.calendars.entities.BaseCalendar;
import org.libreplan.ws.common.api.IntegrationEntityDTO;

/**
 * DTO for {@link BaseCalendar} entity.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@XmlRootElement(name = "base-calendar")
public class BaseCalendarDTO extends IntegrationEntityDTO {

    public final static String ENTITY_TYPE = "base-calendar";

    @XmlAttribute
    public String name;

    @XmlAttribute
    public String parent;

    @XmlElementWrapper(name = "calendar-exception-list")
    @XmlElement(name = "calendar-exception")
    public List<CalendarExceptionDTO> calendarExceptions = new ArrayList<>();

    @XmlElementWrapper(name = "calendar-data-list")
    @XmlElement(name = "calendar-data")
    public List<CalendarDataDTO> calendarData = new ArrayList<>();

    public BaseCalendarDTO() {
    }

    public BaseCalendarDTO(String code, String name, String parent, List<CalendarExceptionDTO> calendarExceptions,
                           List<CalendarDataDTO> calendarData) {
        super(code);
        this.name = name;
        this.parent = parent;
        this.calendarExceptions = calendarExceptions;
        this.calendarData = calendarData;
    }

    public BaseCalendarDTO(String name, String parent, List<CalendarExceptionDTO> calendarExceptions,
                           List<CalendarDataDTO> calendarData) {

        this(generateCode(), name, parent, calendarExceptions, calendarData);
    }

    @Override
    public String getEntityType() {
        return ENTITY_TYPE;
    }

}
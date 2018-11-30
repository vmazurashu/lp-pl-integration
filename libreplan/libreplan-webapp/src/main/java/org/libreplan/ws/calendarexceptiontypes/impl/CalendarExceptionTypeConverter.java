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

package org.libreplan.ws.calendarexceptiontypes.impl;

import org.libreplan.business.calendars.entities.CalendarExceptionType;
import org.libreplan.business.workingday.EffortDuration;
import org.libreplan.ws.calendarexceptiontypes.api.CalendarExceptionTypeColorDTO;
import org.libreplan.ws.calendarexceptiontypes.api.CalendarExceptionTypeDTO;

/**
 * Converter from/to {@link CalendarExceptionType} related entities to/from
 * DTOs.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public final class CalendarExceptionTypeConverter {

    private CalendarExceptionTypeConverter() {
    }

    public final static CalendarExceptionTypeDTO toDTO(
            CalendarExceptionType calendarExceptionType) {
        EffortDuration duration = calendarExceptionType.getDuration();
        int seconds = (duration != null) ? duration.getSeconds() : 0;

        CalendarExceptionTypeColorDTO colorDTO = CalendarExceptionTypeColorConverter
                .toDTO(calendarExceptionType.getColor());

        return new CalendarExceptionTypeDTO(calendarExceptionType.getCode(),
                calendarExceptionType.getName(), colorDTO,
                calendarExceptionType.isOverAssignableWithoutLimit(),
                seconds);
    }

    public final static CalendarExceptionType toEntity(
            CalendarExceptionTypeDTO entityDTO) {
        return CalendarExceptionType.create(entityDTO.code, entityDTO.name,
                CalendarExceptionTypeColorConverter.toEntity(entityDTO.color),
                entityDTO.overAssignable,
                EffortDuration.seconds(entityDTO.duration));
    }

    public static void updateCalendarExceptionType(
            CalendarExceptionType entity, CalendarExceptionTypeDTO entityDTO) {
        entity.setName(entityDTO.name);
        entity.setColor(CalendarExceptionTypeColorConverter
                .toEntity(entityDTO.color));
        entity.setOverAssignable(entityDTO.overAssignable);
        entity.setDuration(EffortDuration.seconds(entityDTO.duration));
    }

}

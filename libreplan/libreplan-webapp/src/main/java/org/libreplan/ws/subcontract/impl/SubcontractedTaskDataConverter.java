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

package org.libreplan.ws.subcontract.impl;

import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

import org.libreplan.business.planner.entities.SubcontractedTaskData;
import org.libreplan.web.subcontract.UpdateDeliveringDateDTO;
import org.libreplan.ws.common.api.OrderElementDTO;
import org.libreplan.ws.subcontract.api.SubcontractedTaskDataDTO;

import static org.libreplan.ws.common.impl.DateConverter.toXMLGregorianCalendar;

/**
 * Converter from/to {@link SubcontractedTaskData} entities to/from DTOs.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public final class SubcontractedTaskDataConverter {

    private SubcontractedTaskDataConverter() {}

    public static final SubcontractedTaskDataDTO toDTO(String companyCode,
                                                       SubcontractedTaskData subcontractedTaskData,
                                                       OrderElementDTO orderElementDTO) {

        return new SubcontractedTaskDataDTO(companyCode,
                subcontractedTaskData.getWorkDescription(),
                subcontractedTaskData.getSubcontractPrice(),
                subcontractedTaskData.getSubcontractedCode(), orderElementDTO,
                toXmlDate(getDeliverDate(subcontractedTaskData)));
    }

    public static final UpdateDeliveringDateDTO toUpdateDeliveringDateDTO(String companyCode,
                                                                          SubcontractedTaskData subTaskData) {
        String customerReference = subTaskData.getSubcontractedCode();
        XMLGregorianCalendar deliverDate = toXmlDate(getDeliverDate(subTaskData));

        if (!subTaskData.getRequiredDeliveringDates().isEmpty()){
            deliverDate = toXmlDate(subTaskData.getRequiredDeliveringDates().first().getSubcontractorDeliverDate());
        }

        String externalCode = subTaskData.getTask().getOrderElement().getCode();

        return new UpdateDeliveringDateDTO(companyCode, customerReference, externalCode, deliverDate);
    }

    private static XMLGregorianCalendar toXmlDate(Date date) {
        return (date != null) ? toXMLGregorianCalendar(date) : null;
    }

    private static Date getDeliverDate(SubcontractedTaskData subcontractedTaskData){
        Date deliverDate = null;

        if (subcontractedTaskData != null && !subcontractedTaskData.getRequiredDeliveringDates().isEmpty()) {
            deliverDate = subcontractedTaskData.getRequiredDeliveringDates().first().getSubcontractorDeliverDate();
        }

        return deliverDate;
    }
}
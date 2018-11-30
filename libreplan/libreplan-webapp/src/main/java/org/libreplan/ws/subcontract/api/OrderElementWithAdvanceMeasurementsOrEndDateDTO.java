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

package org.libreplan.ws.subcontract.api;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.ws.common.api.AdvanceMeasurementDTO;

/**
 * DTO for {@link OrderElement} just with information about advances.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@XmlRootElement(name = "order-element")
public class OrderElementWithAdvanceMeasurementsOrEndDateDTO {

    @XmlAttribute
    public String code;

    @XmlElementWrapper(name = "advance-measurements")
    @XmlElement(name = "advance-measurement")
    public Set<AdvanceMeasurementDTO> advanceMeasurements = new HashSet<AdvanceMeasurementDTO>();

    @XmlElement(name = "end-date-communication-to-customer")
    public EndDateCommunicationToCustomerDTO endDateCommunicationToCustomerDTO;

    public OrderElementWithAdvanceMeasurementsOrEndDateDTO() {
    }

    public OrderElementWithAdvanceMeasurementsOrEndDateDTO(String code,
            Set<AdvanceMeasurementDTO> advanceMeasurements,
            EndDateCommunicationToCustomerDTO endDateCommunicationToCustomerDTO) {
        this.code = code;
        this.advanceMeasurements = advanceMeasurements;
        this.endDateCommunicationToCustomerDTO = endDateCommunicationToCustomerDTO;
    }

}

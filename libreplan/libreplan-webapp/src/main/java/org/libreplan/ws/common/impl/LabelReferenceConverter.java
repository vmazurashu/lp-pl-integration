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

package org.libreplan.ws.common.impl;

import java.util.HashSet;
import java.util.Set;

import org.libreplan.business.common.Registry;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.labels.entities.Label;
import org.libreplan.ws.common.api.LabelReferenceDTO;

/**
 * Converter from/to {@link Label} entities to/from reference DTOs.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public final class LabelReferenceConverter {

    private LabelReferenceConverter() {
    }

    public final static Set<LabelReferenceDTO> toDTO(Set<Label> labels) {
        Set<LabelReferenceDTO> labelDTOs = new HashSet<>();
        for (Label label : labels) {
            labelDTOs.add(toDTO(label));
        }
        return labelDTOs;
    }

    public static final LabelReferenceDTO toDTO(Label label) {
        return new LabelReferenceDTO(label.getCode());
    }

    public static Set<Label> toEntity(Set<LabelReferenceDTO> labels) throws InstanceNotFoundException {
        Set<Label> result = new HashSet<>();
        for (LabelReferenceDTO labelReferenceDTO : labels) {
            result.add(Registry.getLabelDAO().findByCode(labelReferenceDTO.code));
        }
        return result;
    }

}

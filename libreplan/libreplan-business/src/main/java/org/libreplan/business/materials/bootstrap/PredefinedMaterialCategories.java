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

package org.libreplan.business.materials.bootstrap;

import org.libreplan.business.common.Registry;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.materials.entities.MaterialCategory;


/**
 * Defines the default {@link MaterialCategory}.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public enum PredefinedMaterialCategories {

    IMPORTED_MATERIALS_WITHOUT_CATEGORY("Imported materials without category");

    private final String name;

    private PredefinedMaterialCategories(String name) {
        this.name = name;
    }

    public MaterialCategory createMaterialCategory() {
        return MaterialCategory.create(name);
    }

    public String getName() {
        return name;
    }

    public MaterialCategory getMaterialCategory() {
        try {
            return Registry.getMaterialCategoryDAO().findUniqueByName(name);
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
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

package org.libreplan.business.resources.entities;

/**
 * @author Diego Pino Garcia<dpino@igalia.com>
 */
public enum ResourceEnum {

    WORKER(Worker.class, _("WORKER")),
    MACHINE(Machine.class, _("MACHINE"));

    private Class<? extends Resource> klass;

    private final String displayName;

    ResourceEnum(Class<? extends Resource> klass, String displayName) {
        this.klass = klass;
        this.displayName = displayName;
    }

    /**
     * Forces to mark the string as needing translation.
     */
    private static String _(String string) {
        return string;
    }

    public Class<? extends Resource> asClass() {
        return klass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ResourceEnum getDefault() {
        return WORKER;
    }

    public String toString() {
        return klass.getSimpleName().toUpperCase();
    }

    public String toLowerCase() {
        return klass.getSimpleName().toLowerCase();
    }

    public boolean isAssignableFrom(Class<?> clase) {
        return asClass().isAssignableFrom(clase);
    }
}

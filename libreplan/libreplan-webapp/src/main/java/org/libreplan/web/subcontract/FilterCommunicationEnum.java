/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2011 WirelessGalicia, S.L.
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

package org.libreplan.web.subcontract;

/**
 * Enum to filter the {@link CustomerCommunication} list.
 *
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
public enum FilterCommunicationEnum {
    ALL(_("All")),
    NOT_REVIEWED(_("Not Reviewed"));

    private String displayName;

    FilterCommunicationEnum(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Forces to mark the string as needing translation.
     */
    private static String _(String string) {
        return string;
    }

    public static FilterCommunicationEnum getDefault() {
        return ALL;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

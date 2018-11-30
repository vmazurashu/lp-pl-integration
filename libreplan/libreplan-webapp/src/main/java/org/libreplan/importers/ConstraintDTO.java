/*
 * This file is part of LibrePlan
 *
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
package org.libreplan.importers;

/**
 * Class that represents the different types of constraints avaliables in LP
 *
 * @author Alba Carro Pérez <alba.carro@gmail.com>
 */
public enum ConstraintDTO {

    AS_SOON_AS_POSSIBLE, AS_LATE_AS_POSSIBLE, START_IN_FIXED_DATE, START_NOT_EARLIER_THAN, FINISH_NOT_LATER_THAN

}

/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2011 ComtecSF, S.L.
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

package org.libreplan.web.users.settings;

import org.libreplan.business.common.exceptions.ValidationException;

/**
 * Model for UI operations related to user password
 *
 * @author Cristina Alvarino Perez <cristina.alvarino@comtecsf.es>
 * @author Ignacio Diaz Teijido <ignacio.diaz@comtecsf.es>
 */
public interface IPasswordModel {

    void initEditLoggedUser();

    void confirmSave() throws ValidationException;

    /**
     * Sets the password attribute to the inner {@ link User} object.
     *
     * @param password String with the <b>unencrypted</b> password.
     */
    void setPassword(String password);

    boolean validateCurrentPassword(String value);

    boolean isLdapAuthEnabled();

}

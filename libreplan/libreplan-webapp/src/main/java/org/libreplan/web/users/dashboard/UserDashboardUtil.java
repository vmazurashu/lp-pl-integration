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

package org.libreplan.web.users.dashboard;

import java.util.ArrayList;
import java.util.List;

import org.libreplan.business.resources.entities.Resource;
import org.libreplan.business.users.entities.User;
import org.libreplan.web.UserUtil;

/**
 * Utilities class for user dashboard window
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public class UserDashboardUtil {

    public static List<Resource> getBoundResourceAsList(User user) {
        List<Resource> resource = new ArrayList<Resource>();
        resource.add(user.getWorker());
        return resource;
    }

    public static Resource getBoundResourceFromSession() {
        User user = UserUtil.getUserFromSession();
        if (user.isBound()) {
            return user.getWorker();
        }
        return null;
    }

}

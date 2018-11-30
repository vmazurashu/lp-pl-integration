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
package org.libreplan.business.users.bootstrap;

import org.libreplan.business.users.daos.IProfileDAO;
import org.libreplan.business.users.entities.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the default {@link org.libreplan.business.users.entities.Profile
 * Profiles} and saves them into the database if there isn't any profile defined
 * yet.
 *
 * @author Manuel Rego Casasnovas <rego@igalia.com>
 */
@Component
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class ProfilesBootstrap implements IProfileBootstrap {

    @Autowired
    private IProfileDAO profileDAO;

    @Override
    @Transactional
    public void loadRequiredData() {
        if (profileDAO.list(Profile.class).isEmpty()) {
            for (PredefinedProfiles each : PredefinedProfiles.values()) {
                Profile profile = each.createProfile();
                profileDAO.save(profile);
                profile.dontPoseAsTransientObjectAnymore();
            }
        }
    }

}

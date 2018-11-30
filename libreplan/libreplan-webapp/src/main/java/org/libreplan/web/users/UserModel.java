/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
 * Copyright (C) 2010-2012 Igalia, S.L.
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

package org.libreplan.web.users;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.libreplan.business.common.Configuration;
import org.libreplan.business.common.daos.IConfigurationDAO;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.resources.daos.IWorkerDAO;
import org.libreplan.business.resources.entities.Worker;
import org.libreplan.business.users.daos.IProfileDAO;
import org.libreplan.business.users.daos.IUserDAO;
import org.libreplan.business.users.entities.Profile;
import org.libreplan.business.users.entities.User;
import org.libreplan.business.users.entities.UserRole;
import org.libreplan.web.common.concurrentdetection.OnConcurrentModification;
import org.libreplan.web.users.services.IDBPasswordEncoderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Model for UI operations related to {@link User}
 *
 * @author Jacobo Aragunde Perez <jaragunde@igalia.com>
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 * @author Manuel Rego Casasnovas <rego@igalia.com>
 * @author Vova Perebykivskyi <vova@libreplan-enterprise.com>
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@OnConcurrentModification(goToPage = "/users/users.zul")
public class UserModel implements IUserModel {

    @Autowired
    private IUserDAO userDAO;

    @Autowired
    private IConfigurationDAO configurationDAO;

    @Autowired
    private IDBPasswordEncoderService dbPasswordEncoderService;

    @Autowired
    private IWorkerDAO workerDAO;

    @Autowired
    private IProfileDAO profileDAO;

    private User user;

    private String clearNewPassword;

    private Worker unboundWorker;

    @Override
    @Transactional(readOnly = true)
    public List<User> getUsers() {
        List<User> users = userDAO.list(User.class);
        initializeUsers(users);
        return users;
    }

    private void initializeUsers(List<User> users) {
        for (User user : users) {
            user.getRoles().size();
            for (Profile profile : user.getProfiles()) {
                profile.getRoles().size();
            }
        }
    }

    @Override
    @Transactional
    public void confirmSave() throws ValidationException {
        try {
            // user.getLoginName() has to be validated before encoding password,
            // because it must exist to perform the encoding
            Validate.notEmpty(user.getLoginName());

            if (getClearNewPassword() != null) {

                /**
                 * it ckecks if the user password who have admin role has
                 * changed and if so sets true in the field
                 * changedDefaultAdminPassword.
                 */
                if (Configuration.isDefaultPasswordsControl()) {
                    PasswordUtil.checkIfChangeDefaultPasswd(user,
                            getClearNewPassword());
                }

                user.setPassword(dbPasswordEncoderService.encodePassword(
                        getClearNewPassword(), user.getLoginName()));
            }
        } catch (IllegalArgumentException e) {
        }

        user.validate();
        userDAO.save(user);

        if (unboundWorker != null) {
            unboundWorker.setUser(null);
            workerDAO.save(unboundWorker);
        }
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void initCreate() {
        this.user = User.create();
        this.unboundWorker = null;
    }

    @Override
    @Transactional(readOnly = true)
    public void initEdit(User user) {
        Validate.notNull(user);
        this.user = getFromDB(user);
        this.setClearNewPassword(null);
        this.unboundWorker = null;
    }

    @Transactional(readOnly = true)
    private User getFromDB(User user) {
        return getFromDB(user.getId());
    }

    @Transactional(readOnly = true)
    private User getFromDB(Long id) {
        try {
            User result = userDAO.find(id);
            forceLoadEntities(result);
            return result;
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Load entities that will be needed in the conversation
     * @param costCategory
     */
    private void forceLoadEntities(User user) {
        user.getLoginName();
        for (UserRole each : user.getRoles()) {
            each.name();
        }
        for (Profile each : user.getProfiles()) {
            each.getProfileName();
        }
    }

    @Override
    public List<UserRole> getRoles() {
        List<UserRole> list = new ArrayList<>();
        if (user != null) {
            list.addAll(user.getRoles());
        }
        return list;
    }

    @Override
    public void removeRole(UserRole role) {
        user.removeRole(role);
    }

    @Override
    public void addRole(UserRole role) {
        user.addRole(role);
    }

    @Override
    public List<Profile> getProfiles() {
        List<Profile> list = new ArrayList<>();
        if (user != null) {
            list.addAll(user.getProfiles());
        }
        return list;
    }

    @Override
    public void removeProfile(Profile profile) {
        user.removeProfile(profile);
    }

    @Override
    public void addProfile(Profile profile) {
        user.addProfile(profile);
    }

    @Override
    public void setPassword(String password) {
        // password is not encrypted right away, because
        // user.getLoginName must exist to do that, and we're
        // not sure at this point
        if (password != "") {
            setClearNewPassword(password);
        } else {
            setClearNewPassword(null);
        }
    }

    public void setClearNewPassword(String clearNewPassword) {
        this.clearNewPassword = clearNewPassword;
    }

    @Override
    public String getClearNewPassword() {
        return clearNewPassword;
    }

    @Override
    @Transactional
    public void confirmRemove(User user) throws InstanceNotFoundException {
        Worker worker = user.getWorker();
        if (worker != null) {
            worker.setUser(null);
            workerDAO.save(worker);
        }
        userDAO.remove(user);
    }

    @Transactional(readOnly = true)
    @Override
    public boolean isLDAPBeingUsed() {
        return configurationDAO.getConfiguration().getLdapConfiguration()
                .getLdapAuthEnabled();
    }

    @Transactional(readOnly = true)
    @Override
    public boolean isLDAPRolesBeingUsed() {
        return configurationDAO.getConfiguration().getLdapConfiguration()
                .getLdapSaveRolesDB();
    }

    @Override
    public void unboundResource() {
        unboundWorker = user.getWorker();
        user.setWorker(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Profile> getAllProfiles() {
        return profileDAO.listSorted();
    }


    @Override
    @Transactional(readOnly = true)
    public Number getRowCount() {
        return userDAO.getRowCount();
    }

}

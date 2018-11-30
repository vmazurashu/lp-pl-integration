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

package org.libreplan.web.users;

import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.users.entities.Profile;
import org.libreplan.business.users.entities.UserRole;
import org.libreplan.web.common.BaseCRUDController;
import org.libreplan.web.common.Util;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zkplus.spring.SpringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.libreplan.web.I18nHelper._;

/**
 * Controller for CRUD actions over a {@link Profile}.
 *
 * @author Jacobo Aragunde Perez <jaragunde@igalia.com>
 * @author Diego Pino García <dpino@igalia.com>
 */
@SuppressWarnings("serial")
public class ProfileCRUDController extends BaseCRUDController<Profile> {

    private IProfileModel profileModel;

    private Combobox userRolesCombo;

    public ProfileCRUDController() {
        if ( profileModel == null ) {
            profileModel = (IProfileModel) SpringUtil.getBean("profileModel");
        }
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        userRolesCombo = (Combobox) editWindow.getFellowIfAny("userRolesCombo");
        appendAllUserRolesExceptRoleBoundUser(userRolesCombo);
    }

    /**
     * Appends the existing UserRoles to the Combobox passed.
     *
     * @param combo
     */
    private void appendAllUserRolesExceptRoleBoundUser(Combobox combo) {
        List<UserRole> roles = new ArrayList<>(Arrays.asList(UserRole.values()));
        roles.remove(UserRole.ROLE_BOUND_USER);

        for (UserRole role : roles) {
            Comboitem item = combo.appendItem(_(role.getDisplayName()));
            item.setValue(role);
        }
    }

    protected void save() throws ValidationException {
        profileModel.confirmSave();
    }

    public List<Profile> getProfiles() {
        return profileModel.getProfiles();
    }

    public Profile getProfile() {
        return profileModel.getProfile();
    }

    public void addSelectedRole() {
        Comboitem comboItem = userRolesCombo.getSelectedItem();
        if (comboItem != null) {
            addRole(comboItem.getValue());
        }
    }

    public List<UserRole> getRoles() {
        return profileModel.getRoles();
    }

    private void addRole(UserRole role) {
        profileModel.addRole(role);
        Util.reloadBindings(editWindow);
    }

    private void removeRole(UserRole role) {
        profileModel.removeRole(role);
        Util.reloadBindings(editWindow);
    }

    @Override
    protected String getEntityType() {
        return _("Profile");
    }

    @Override
    protected String getPluralEntityType() {
        return _("Profiles");
    }

    @Override
    protected void initCreate() {
        profileModel.initCreate();
    }

    @Override
    protected void initEdit(Profile profile) {
        profileModel.initEdit(profile);
    }

    @Override
    protected Profile getEntityBeingEdited() {
        return profileModel.getProfile();
    }

    private boolean isReferencedByOtherEntities(Profile profile) {
        try {
            profileModel.checkHasUsers(profile);
            return false;
        } catch (ValidationException e) {
            showCannotDeleteProfileDialog(e.getInvalidValue().getMessage());
        }

        return true;
    }

    private void showCannotDeleteProfileDialog(String message) {
        Messagebox.show(_(message), _("Warning"), Messagebox.OK, Messagebox.EXCLAMATION);
    }
    @Override
    protected boolean beforeDeleting(Profile profile){
        return !isReferencedByOtherEntities(profile);
    }

    @Override
    protected void delete(Profile profile) throws InstanceNotFoundException {
        profileModel.confirmRemove(profile);
    }

    public RowRenderer getRolesRenderer() {
        return new RowRenderer() {
            @Override
            public void render(Row row, Object data, int i) throws Exception {
                final UserRole role = (UserRole) data;

                row.appendChild(new Label(_(role.getDisplayName())));

                row.appendChild(Util.createRemoveButton(new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        removeRole(role);
                    }
                }));
            }
        };
    }

}

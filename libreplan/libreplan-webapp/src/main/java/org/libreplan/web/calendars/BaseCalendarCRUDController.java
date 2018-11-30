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

package org.libreplan.web.calendars;

import static org.libreplan.web.I18nHelper._;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.libreplan.business.calendars.entities.BaseCalendar;
import org.libreplan.business.calendars.entities.CalendarData;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.web.common.BaseCRUDController.CRUDControllerState;
import org.libreplan.web.common.ConstraintChecker;
import org.libreplan.web.common.IMessagesForUser;
import org.libreplan.web.common.Level;
import org.libreplan.web.common.MessagesForUser;
import org.libreplan.web.common.OnlyOneVisible;
import org.libreplan.web.common.Util;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.DefaultTreeNode;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Window;

/**
 * Controller for CRUD actions over a {@link BaseCalendar}.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 * @author Diego Pino Garcia <dpino@igalia.com>
 */
public class BaseCalendarCRUDController extends GenericForwardComposer {

    private IBaseCalendarModel baseCalendarModel;

    private Window listWindow;

    private Window editWindow;

    private Window createNewVersion;

    private OnlyOneVisible visibility;

    private IMessagesForUser messagesForUser;

    private Component messagesContainer;

    private BaseCalendarsTreeitemRenderer baseCalendarsTreeitemRenderer = new BaseCalendarsTreeitemRenderer();

    private BaseCalendarEditionController createController;

    private BaseCalendarEditionController editionController;

    private CRUDControllerState state = CRUDControllerState.LIST;

    public BaseCalendar getBaseCalendar() {
        return baseCalendarModel.getBaseCalendar();
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        if ( baseCalendarModel == null ) {
            baseCalendarModel = (IBaseCalendarModel) SpringUtil.getBean("baseCalendarModel");
        }

        messagesForUser = new MessagesForUser(messagesContainer);
        comp.setAttribute("calendarController", this, true);
        getVisibility().showOnly(listWindow);
    }

    public void cancel() {
        baseCalendarModel.cancel();
        goToList();
    }

    public void goToList() {
        state = CRUDControllerState.LIST;
        Util.reloadBindings(listWindow);
        getVisibility().showOnly(listWindow);
    }

    public void goToEditForm(BaseCalendar baseCalendar) {
        state = CRUDControllerState.EDIT;
        baseCalendarModel.initEdit(baseCalendar);
        assignEditionController();
        setSelectedDay(new LocalDate());
        highlightDaysOnCalendar();
        getVisibility().showOnly(editWindow);
        Util.reloadBindings(editWindow);
        updateWindowTitle();
    }

    private void highlightDaysOnCalendar() {
        if (editionController != null) {
            editionController.highlightDaysOnCalendar();
        }
    }

    public void save() {
        try {
            baseCalendarModel.generateCalendarCodes();
            Util.reloadBindings(editWindow);
            ConstraintChecker.isValid(editWindow);
            validateCalendarExceptionCodes();
            baseCalendarModel.confirmSave();

            messagesForUser.showMessage(
                    Level.INFO, _("Base calendar \"{0}\" saved", baseCalendarModel.getBaseCalendar().getName()));

            goToList();
        } catch (ValidationException e) {
            messagesForUser.showInvalidValues(e);
        }
    }

    public void saveAndContinue() {
        try {
            baseCalendarModel.generateCalendarCodes();
            Util.reloadBindings(editWindow);
            ConstraintChecker.isValid(editWindow);
            validateCalendarExceptionCodes();
            baseCalendarModel.confirmSaveAndContinue();

            messagesForUser.showMessage(
                    Level.INFO, _("Base calendar \"{0}\" saved", baseCalendarModel.getBaseCalendar().getName()));

        } catch (ValidationException e) {
            messagesForUser.showInvalidValues(e);
        }
    }

    public void goToCreateForm() {
        state = CRUDControllerState.CREATE;
        baseCalendarModel.initCreate();
        assignCreateController();
        setSelectedDay(new LocalDate());
        highlightDaysOnCalendar();
        getVisibility().showOnly(editWindow);
        Util.reloadBindings(editWindow);
        updateWindowTitle();
    }

    public void setSelectedDay(LocalDate date) {
        baseCalendarModel.setSelectedDay(date);
        reloadDayInformation();
    }

    private BaseCalendarEditionController createInstanceForController(){
        return new BaseCalendarEditionController(
                baseCalendarModel,
                editWindow,
                createNewVersion,
                messagesForUser) {

            @Override
            public void goToList() {
                BaseCalendarCRUDController.this.goToList();
            }

            @Override
            public void cancel() {
                BaseCalendarCRUDController.this.cancel();
            }

            @Override
            public void save() {
                BaseCalendarCRUDController.this.save();
            }

            @Override
            public void saveAndContinue() {
                BaseCalendarCRUDController.this.saveAndContinue();
            }

        };
    }

    private void assignEditionController() {
        editionController = createInstanceForController();

        try {
            editionController.doAfterCompose(editWindow);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void assignCreateController() {
        createController = createInstanceForController();

        try {
            createController.doAfterCompose(editWindow);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private OnlyOneVisible getVisibility() {
        if (visibility == null) {
            visibility = new OnlyOneVisible(listWindow, editWindow);
        }

        return visibility;
    }

    private void reloadDayInformation() {
        Util.reloadBindings(editWindow.getFellow("dayInformation"));
        highlightDaysOnCalendar();
    }

    public void goToCreateDerivedForm(BaseCalendar baseCalendar) {
        state = CRUDControllerState.CREATE;
        baseCalendarModel.initCreateDerived(baseCalendar);
        assignCreateController();
        setSelectedDay(new LocalDate());
        highlightDaysOnCalendar();
        getVisibility().showOnly(editWindow);
        Util.reloadBindings(editWindow);
        updateWindowTitle();
    }

    public boolean isEditing() {
        return baseCalendarModel.isEditing();
    }

    public void goToCreateCopyForm(BaseCalendar baseCalendar) {
        state = CRUDControllerState.CREATE;
        baseCalendarModel.initCreateCopy(baseCalendar);
        assignCreateController();
        setSelectedDay(new LocalDate());
        highlightDaysOnCalendar();
        getVisibility().showOnly(editWindow);
        Util.reloadBindings(editWindow);
        updateWindowTitle();
    }

    public BaseCalendarsTreeModel getBaseCalendarsTreeModel() {
        return new BaseCalendarsTreeModel(new BaseCalendarTreeRoot(baseCalendarModel.getBaseCalendars()));
    }

    public BaseCalendarsTreeitemRenderer getBaseCalendarsTreeitemRenderer() {
        return baseCalendarsTreeitemRenderer;
    }

    public class BaseCalendarsTreeitemRenderer implements TreeitemRenderer {

        @Override
        public void render(Treeitem item, Object data, int i) {
            DefaultTreeNode simpleTreeNode = (DefaultTreeNode) data;
            final BaseCalendar baseCalendar = (BaseCalendar) simpleTreeNode.getData();
            item.setValue(data);

            Treerow treerow = new Treerow();

            Treecell nameTreecell = new Treecell();
            Label nameLabel = new Label(baseCalendar.getName());
            nameTreecell.appendChild(nameLabel);
            treerow.appendChild(nameTreecell);

            // Append start date of the current work week
            Treecell startDateTreecell = new Treecell();
            Label startDateLabel = new Label("---");
            CalendarData version = baseCalendar.getCalendarData(LocalDate.fromDateFields(new Date()));
            CalendarData prevVersion = baseCalendar.getPrevious(version);

            if ((prevVersion != null) && (prevVersion.getExpiringDate() != null)) {
                startDateLabel.setValue(prevVersion.getExpiringDate().toString());
            }

            startDateTreecell.appendChild(startDateLabel);
            treerow.appendChild(startDateTreecell);

            // Append expiring date of the current work week
            Treecell expiringDateTreecell = new Treecell();
            Label expiringDateLabel = new Label("---");

            if (version.getExpiringDate() != null) {
                expiringDateLabel.setValue(version.getExpiringDate().toString());
            }

            expiringDateTreecell.appendChild(expiringDateLabel);
            treerow.appendChild(expiringDateTreecell);

            Treecell operationsTreecell = new Treecell();

            Button createDerivedButton = new Button();
            createDerivedButton.setTooltiptext(_("Create derived"));
            createDerivedButton.setSclass("icono");
            createDerivedButton.setImage("/common/img/ico_derived1.png");
            createDerivedButton.setHoverImage("/common/img/ico_derived.png");

            createDerivedButton.addEventListener(Events.ON_CLICK, event -> goToCreateDerivedForm(baseCalendar));
            operationsTreecell.appendChild(createDerivedButton);
            Button createCopyButton = new Button();
            createCopyButton.setSclass("icono");
            createCopyButton.setTooltiptext(_("Create copy"));
            createCopyButton.setImage("/common/img/ico_copy1.png");
            createCopyButton.setHoverImage("/common/img/ico_copy.png");

            createCopyButton.addEventListener(Events.ON_CLICK, event -> goToCreateCopyForm(baseCalendar));
            operationsTreecell.appendChild(createCopyButton);

            Button editButton = new Button();
            editButton.setTooltiptext(_("Edit"));
            editButton.setSclass("icono");
            editButton.setImage("/common/img/ico_editar1.png");
            editButton.setHoverImage("/common/img/ico_editar.png");

            editButton.addEventListener(Events.ON_CLICK, event -> goToEditForm(baseCalendar));
            operationsTreecell.appendChild(editButton);

            Button removeButton = new Button();
            removeButton.setTooltiptext(_("Remove"));
            removeButton.setSclass("icono");
            removeButton.setImage("/common/img/ico_borrar1.png");
            removeButton.setHoverImage("/common/img/ico_borrar.png");

            removeButton.addEventListener(Events.ON_CLICK, event -> confirmRemove(baseCalendar));

            if (baseCalendarModel.isDefaultCalendar(baseCalendar)) {
                removeButton.setDisabled(true);
                removeButton.setImage("/common/img/ico_borrar_out.png");
                removeButton.setHoverImage("/common/img/ico_borrar_out.png");
            }
            operationsTreecell.appendChild(removeButton);

            treerow.appendChild(operationsTreecell);

            item.appendChild(treerow);

            // Show the tree expanded at start
            item.setOpen(true);
        }

        private void confirmRemove(BaseCalendar calendar) {

            if (hasParent(calendar)) {
                messagesForUser.showMessage(
                        Level.ERROR, _("Calendar cannot be removed as it has other derived calendars from it"));
                return;
            }

            if (isDefault(calendar)) {
                messagesForUser.showMessage(
                        Level.ERROR,
                        _("Default calendar cannot be removed. "
                                + "Please, change the default calendar in the Main Settings window before."));
                return;
            }
            removeCalendar(calendar);
        }

        private void removeCalendar(BaseCalendar calendar) {
            if (!isReferencedByOtherEntities(calendar)) {
                int result = showConfirmDeleteCalendar(calendar);

                if (result == Messagebox.OK) {
                    final String calendarName = calendar.getName();
                    baseCalendarModel.confirmRemove(calendar);
                    messagesForUser.showMessage(Level.INFO, _("Removed calendar \"{0}\"", calendarName));
                    Util.reloadBindings(listWindow);
                }
            }
        }

        private int showConfirmDeleteCalendar(BaseCalendar calendar) {
            return Messagebox.show(
                    _("Confirm deleting {0}. Are you sure?", calendar.getName()),
                    _("Delete"), Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION);
        }

        private boolean isReferencedByOtherEntities(BaseCalendar calendar) {
            try {
                baseCalendarModel.checkIsReferencedByOtherEntities(calendar);
                return false;
            } catch (ValidationException e) {
                showCannotDeleteCalendarDialog(e.getInvalidValue().getMessage());
            }

            return true;
        }

        private void showCannotDeleteCalendarDialog(String message) {
            Messagebox.show(_(message), _("Warning"), Messagebox.OK, Messagebox.EXCLAMATION);
        }

    }

    public boolean isDefault(BaseCalendar calendar) {
        return baseCalendarModel.isDefaultCalendar(calendar);
    }

    public boolean hasParent(BaseCalendar calendar) {
        return baseCalendarModel.isParent(calendar);
    }

    public BaseCalendarEditionController getEditionController() {
        return isEditing() ? editionController : createController;
    }

    private void validateCalendarExceptionCodes() {
        if (baseCalendarModel.isEditing()) {
            this.editionController.validateCalendarExceptionCodes();
        } else {
            this.createController.validateCalendarExceptionCodes();
        }
    }

    public void updateWindowTitle() {
        if (editWindow != null && state != CRUDControllerState.LIST) {
            String entityType = _("Calendar");
            String humanId = getBaseCalendar().getHumanId();

            String title;

            switch (state) {

                case CREATE:
                    if (StringUtils.isEmpty(humanId)) {
                        title = _("Create {0}", entityType);
                    } else {
                        title = _("Create {0}: {1}", entityType, humanId);
                    }
                    break;

                case EDIT:
                    title = _("Edit {0}: {1}", entityType, humanId);
                    break;

                default:
                    throw new IllegalStateException("You should be in creation or edition mode to use this method");
            }

            ((Caption) editWindow.getFellow("caption")).setLabel(title);
        }
    }

}

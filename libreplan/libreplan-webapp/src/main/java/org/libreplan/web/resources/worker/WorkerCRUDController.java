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

package org.libreplan.web.resources.worker;

import static org.libreplan.web.I18nHelper._;

import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.libreplan.business.calendars.entities.BaseCalendar;
import org.libreplan.business.calendars.entities.ResourceCalendar;
import org.libreplan.business.common.entities.Limits;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.resources.daos.IResourceDAO;
import org.libreplan.business.resources.entities.ResourceType;
import org.libreplan.business.resources.entities.VirtualWorker;
import org.libreplan.business.resources.entities.Worker;
import org.libreplan.business.users.entities.User;
import org.libreplan.business.users.entities.UserRole;
import org.libreplan.web.calendars.BaseCalendarEditionController;
import org.libreplan.web.calendars.IBaseCalendarModel;
import org.libreplan.web.common.ConstraintChecker;
import org.libreplan.web.common.IMessagesForUser;
import org.libreplan.web.common.Level;
import org.libreplan.web.common.MessagesForUser;
import org.libreplan.web.common.OnlyOneVisible;
import org.libreplan.web.common.Util;
import org.libreplan.web.common.ILimitsModel;
import org.libreplan.web.common.BaseCRUDController.CRUDControllerState;
import org.libreplan.web.common.components.bandboxsearch.BandboxMultipleSearch;
import org.libreplan.web.common.components.bandboxsearch.BandboxSearch;
import org.libreplan.web.common.entrypoints.EntryPointsHandler;
import org.libreplan.web.common.entrypoints.IURLHandlerRegistry;
import org.libreplan.web.costcategories.ResourcesCostCategoryAssignmentController;
import org.libreplan.web.resources.search.ResourcePredicate;
import org.libreplan.web.security.SecurityUtils;
import org.libreplan.web.users.IUserCRUDController;
import org.libreplan.web.users.services.IDBPasswordEncoderService;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.CheckEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.ComboitemRenderer;
import org.zkoss.zul.Constraint;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Radio;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Window;
import org.zkoss.zul.Bandbox;

/**
 * Controller for {@link Worker} resource.
 * <br />
 *
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 * @author Manuel Rego Casasnovas <rego@igalia.com>
 * @author Vova Perebykivskyi <vova@libreplan-enterprise.com>
 */
public class WorkerCRUDController extends GenericForwardComposer implements IWorkerCRUDControllerEntryPoints {

    private IDBPasswordEncoderService dbPasswordEncoderService;

    private ILimitsModel limitsModel;

    private IWorkerModel workerModel;

    private IResourceDAO resourceDAO;

    private IUserCRUDController userCRUD;

    private Window listWindow;

    private Window editWindow;

    private IURLHandlerRegistry URLHandlerRegistry;

    private OnlyOneVisible visibility;

    private IMessagesForUser messages;

    private Component messagesContainer;

    private CriterionsController criterionsController;

    private LocalizationsController localizationsForEditionController;

    private LocalizationsController localizationsForCreationController;

    private ResourcesCostCategoryAssignmentController resourcesCostCategoryAssignmentController;

    private IWorkerCRUDControllerEntryPoints workerCRUD;

    private Window editCalendarWindow;

    private BaseCalendarEditionController baseCalendarEditionController;

    private IBaseCalendarModel resourceCalendarModel;

    private Window createNewVersionWindow;

    private BaseCalendarsComboitemRenderer baseCalendarsComboitemRenderer = new BaseCalendarsComboitemRenderer();

    private Grid listing;

    private Datebox filterStartDate;

    private Datebox filterFinishDate;

    private Listbox filterLimitingResource;

    private BandboxMultipleSearch bdFilters;

    private Textbox txtfilter;

    private Tab personalDataTab;

    private Tab assignedCriteriaTab;

    private Tab costCategoryAssignmentTab;

    private CRUDControllerState state = CRUDControllerState.LIST;

    private Groupbox userBindingGroupbox;

    private Radiogroup userBindingRadiogroup;

    private BandboxSearch userBandbox;

    private Textbox loginNameTextbox;

    private Textbox emailTextbox;

    private Textbox passwordTextbox;

    private Textbox passwordConfirmationTextbox;

    private enum UserBindingOption {
        NOT_BOUND(_("Not bound")),
        EXISTING_USER(_("Existing user")),
        CREATE_NEW_USER(_("Create new user"));

        private String label;

        UserBindingOption(String label) {
            this.label = label;
        }

        /**
         * Helper function to mark text to be translated.
         */
        private static String _(String text) {
            return text;
        }

    }

    public WorkerCRUDController() {
        workerCRUD = (IWorkerCRUDControllerEntryPoints) SpringUtil.getBean("workerCRUD");
        userCRUD = (IUserCRUDController) SpringUtil.getBean("userCRUD");
    }

    public WorkerCRUDController(Window listWindow,
                                Window editWindow,
                                Window editCalendarWindow,
                                IWorkerModel workerModel,
                                IMessagesForUser messages,
                                IWorkerCRUDControllerEntryPoints workerCRUD) {

        this.listWindow = listWindow;
        this.editWindow = editWindow;
        this.workerModel = workerModel;
        this.messages = messages;
        this.workerCRUD = workerCRUD;
        this.editCalendarWindow = editCalendarWindow;
    }

    public Worker getWorker() {
        return workerModel.getWorker();
    }

    public List<Worker> getWorkers() {
        return workerModel.getWorkers();
    }

    public List<Worker> getRealWorkers() {
        return workerModel.getRealWorkers();
    }

    public List<Worker> getVirtualWorkers() {
        return workerModel.getVirtualWorkers();
    }

    public LocalizationsController getLocalizations() {
        if (workerModel.isCreating()) {
            return localizationsForCreationController;
        }

        return localizationsForEditionController;
    }

    public void saveAndExit() {
        if (save()) {
            goToList();
        }
    }

    public void saveAndContinue() {
        if (save()) {
            if (!getWorker().isVirtual()) {
                goToEditForm(getWorker());
            } else {
                this.goToEditVirtualWorkerForm(getWorker());
            }
        }
    }

    public boolean save() {
        validateConstraints();

        setUserBindingInfo();

        // Validate 'Cost category assignment' tab is correct
        if (resourcesCostCategoryAssignmentController != null) {
            if (!resourcesCostCategoryAssignmentController.validate()) {
                return false;
            }
        }

        try {

            if (baseCalendarEditionController != null) {
                baseCalendarEditionController.save();
            }

            if ( criterionsController != null && !criterionsController.validate() ) {
                return false;
            }

            if (workerModel.getCalendar() == null) {
                createCalendar();
            }

            workerModel.save();
            messages.showMessage(Level.INFO, _("Worker saved"));

            return true;
        } catch (ValidationException e) {
            messages.showInvalidValues(e);
        }

        return false;
    }

    private void setUserBindingInfo() {
        int option = userBindingRadiogroup.getSelectedIndex();

        if (UserBindingOption.NOT_BOUND.ordinal() == option) {
            getWorker().setUser(null);
        }

        if (UserBindingOption.EXISTING_USER.ordinal() == option) {
            if (getWorker().getUser() == null) {
                throw new WrongValueException(userBandbox, _("please select a user to bound"));
            }
            getWorker().updateUserData();
        }

        if (UserBindingOption.CREATE_NEW_USER.ordinal() == option) {
            getWorker().setUser(createNewUserForBinding());
        }
    }

    private User createNewUserForBinding() {
        String loginName = loginNameTextbox.getValue();
        if (StringUtils.isBlank(loginName)) {
            throw new WrongValueException(loginNameTextbox, _("cannot be empty"));
        }

        String password = passwordTextbox.getValue();
        if (StringUtils.isBlank(loginName)) {
            throw new WrongValueException(passwordTextbox, _("cannot be empty"));
        }

        String passwordConfirmation = passwordConfirmationTextbox.getValue();
        if (!password.equals(passwordConfirmation)) {
            throw new WrongValueException(passwordConfirmationTextbox, _("passwords do not match"));
        }

        String encodedPassword = dbPasswordEncoderService.encodePassword(password, loginName);

        User newUser = User.create(loginName, encodedPassword, emailTextbox.getValue());

        Worker worker = getWorker();
        newUser.setFirstName(worker.getFirstName());
        newUser.setLastName(worker.getSurname());

        return newUser;
    }

    private void validateConstraints() {
        Tab selectedTab = personalDataTab;
        try {
            validatePersonalDataTab();

            selectedTab = assignedCriteriaTab;
            validateAssignedCriteriaTab();

            selectedTab = costCategoryAssignmentTab;
            validateCostCategoryAssignmentTab();

        } catch (WrongValueException e) {
            selectedTab.setSelected(true);
            throw e;
        }
    }

    private void validatePersonalDataTab() {
        ConstraintChecker.isValid(editWindow.getFellowIfAny("personalDataTabpanel"));
    }

    private void validateAssignedCriteriaTab() {
        criterionsController.validateConstraints();
    }

    private void validateCostCategoryAssignmentTab() {
        resourcesCostCategoryAssignmentController.validateConstraints();
    }

    public void cancel() {
        goToList();
    }

    @Override
    public void goToList() {
        state = CRUDControllerState.LIST;
        getVisibility().showOnly(listWindow);
        Util.reloadBindings(listWindow);
    }

    @Override
    public void goToEditForm(Worker worker) {
        state = CRUDControllerState.EDIT;
        getBookmarker().goToEditForm(worker);
        workerModel.prepareEditFor(worker);
        resourcesCostCategoryAssignmentController.setResource(workerModel.getWorker());

        if (isCalendarNotNull()) {
            editCalendar();
        }

        editAssignedCriterions();
        updateUserBindingComponents();
        showEditWindow(_("Edit Worker: {0}", worker.getHumanId()));

        Textbox workerFirstname = (Textbox) editWindow.getFellow("workerFirstname");
        workerFirstname.focus();
    }

    private void updateUserBindingComponents() {
        User user = getBoundUser();
        if (user == null) {
            userBindingRadiogroup.setSelectedIndex(UserBindingOption.NOT_BOUND.ordinal());
        } else {
            userBindingRadiogroup.setSelectedIndex(UserBindingOption.EXISTING_USER.ordinal());
        }

        // Reset new user fields
        loginNameTextbox.setValue("");
        emailTextbox.setValue("");
        passwordTextbox.setValue("");
        passwordConfirmationTextbox.setValue("");

        Util.reloadBindings(userBindingGroupbox);
    }

    public void goToEditVirtualWorkerForm(Worker worker) {
        state = CRUDControllerState.EDIT;
        workerModel.prepareEditFor(worker);
        resourcesCostCategoryAssignmentController.setResource(workerModel.getWorker());

        if (isCalendarNotNull()) {
            editCalendar();
        }

        editAssignedCriterions();
        showEditWindow(_("Edit Virtual Workers Group: {0}", worker.getHumanId()));
    }

    public void goToEditForm() {
        state = CRUDControllerState.EDIT;
        if (isCalendarNotNull()) {
            editCalendar();
        }
        showEditWindow(_("Edit Worker: {0}", getWorker().getHumanId()));
    }

    @Override
    public void goToCreateForm() {
        state = CRUDControllerState.CREATE;
        getBookmarker().goToCreateForm();
        workerModel.prepareForCreate();
        createAssignedCriterions();
        resourcesCostCategoryAssignmentController.setResource(workerModel.getWorker());
        updateUserBindingComponents();
        showEditWindow(_("Create Worker"));
        resourceCalendarModel.cancel();
        Textbox workerFirstname = (Textbox) editWindow.getFellow("workerFirstname");
        workerFirstname.focus();
    }

    private void showEditWindow(String title) {
        personalDataTab.setSelected(true);
        ((Caption) editWindow.getFellow("caption")).setLabel(title);
        getVisibility().showOnly(editWindow);
        Util.reloadBindings(editWindow);
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        workerModel = (IWorkerModel) SpringUtil.getBean("workerModel");
        dbPasswordEncoderService = (IDBPasswordEncoderService) SpringUtil.getBean("dbPasswordEncoderService");
        limitsModel = (ILimitsModel) SpringUtil.getBean("limitsModel");
        resourceDAO = (IResourceDAO) SpringUtil.getBean("resourceDAO");
        URLHandlerRegistry = (IURLHandlerRegistry) SpringUtil.getBean("URLHandlerRegistry");
        resourceCalendarModel = (IBaseCalendarModel) SpringUtil.getBean("resourceCalendarModel");

        localizationsForEditionController = createLocalizationsController(comp, "editWindow");
        localizationsForCreationController = createLocalizationsController(comp, "editWindow");
        comp.setAttribute("controller", this, true);

        if (messagesContainer == null) {
            throw new RuntimeException(_("MessagesContainer is needed"));
        }

        messages = new MessagesForUser(messagesContainer);
        setupResourcesCostCategoryAssignmentController();

        getVisibility().showOnly(listWindow);
        initFilterComponent();
        setupFilterLimitingResourceListbox();
        initializeTabs();
        initUserBindingComponents();
        setWidthOfUserBandBoxSearch();

        final EntryPointsHandler<IWorkerCRUDControllerEntryPoints> handler =
                URLHandlerRegistry.getRedirectorFor(IWorkerCRUDControllerEntryPoints.class);

        handler.register(this, page);
    }

    private void initUserBindingComponents() {
        userBindingGroupbox = (Groupbox) editWindow.getFellowIfAny("userBindingGroupbox");
        userBindingRadiogroup = (Radiogroup) editWindow.getFellowIfAny("userBindingRadiogroup");
        initUserBindingOptions();
        userBandbox = (BandboxSearch) editWindow.getFellowIfAny("userBandbox");
        loginNameTextbox = (Textbox) editWindow.getFellowIfAny("loginName");
        passwordTextbox = (Textbox) editWindow.getFellowIfAny("password");
        passwordConfirmationTextbox = (Textbox) editWindow.getFellowIfAny("passwordConfirmation");
        emailTextbox = (Textbox) editWindow.getFellowIfAny("email");
    }

    private void initUserBindingOptions() {
        UserBindingOption[] values = UserBindingOption.values();
        for (UserBindingOption option : values) {
            Radio radio = new Radio(_(option.label));

            if ( option.equals(UserBindingOption.CREATE_NEW_USER) &&
                    !SecurityUtils.isSuperuserOrUserInRoles(UserRole.ROLE_USER_ACCOUNTS) ) {

                radio.setDisabled(true);
                radio.setTooltiptext(_("You do not have permissions to create new users"));
            }
            userBindingRadiogroup.appendChild(radio);
        }
    }

    private void setWidthOfUserBandBoxSearch() {
        Bandbox userBandboxComponent = (Bandbox) this.userBandbox.getChildren().get(1);
        userBandboxComponent.setWidth("300px");

        Listbox userListbox = (Listbox)
                userBandbox.getChildren().get(1).getChildren().get(0).getChildren().get(0).getChildren().get(0);
        userListbox.setWidth("300px");
    }

    private void initializeTabs() {
        personalDataTab = (Tab) editWindow.getFellow("personalDataTab");
        assignedCriteriaTab = (Tab) editWindow.getFellow("assignedCriteriaTab");
        costCategoryAssignmentTab = (Tab) editWindow.getFellow("costCategoryAssignmentTab");
    }

    private void initFilterComponent() {
        this.filterFinishDate = (Datebox) listWindow.getFellowIfAny("filterFinishDate");
        this.filterStartDate = (Datebox) listWindow.getFellowIfAny("filterStartDate");
        this.filterLimitingResource = (Listbox) listWindow.getFellowIfAny("filterLimitingResource");
        this.bdFilters = (BandboxMultipleSearch) listWindow.getFellowIfAny("bdFilters");
        this.txtfilter = (Textbox) listWindow.getFellowIfAny("txtfilter");
        this.listing = (Grid) listWindow.getFellowIfAny("listing");
        clearFilterDates();
    }

    private void setupResourcesCostCategoryAssignmentController() {
        Component costCategoryAssignmentContainer = editWindow.getFellowIfAny("costCategoryAssignmentContainer");

        resourcesCostCategoryAssignmentController = (ResourcesCostCategoryAssignmentController)
                costCategoryAssignmentContainer.getAttribute("assignmentController", true);
    }

    private void editAssignedCriterions() {
        try {
            setupCriterionsController();
            criterionsController.prepareForEdit(workerModel.getWorker());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createAssignedCriterions() {
        try {
            setupCriterionsController();
            criterionsController.prepareForCreate(workerModel.getWorker());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setupCriterionsController() throws Exception {
        criterionsController = new CriterionsController(workerModel);
        criterionsController.doAfterCompose(getCurrentWindow().getFellow("criterionsContainer"));
    }

    public BaseCalendarEditionController getEditionController() {
        return baseCalendarEditionController;
    }

    private LocalizationsController createLocalizationsController(Component comp,
                                                                  String localizationsContainerName) throws Exception {

        LocalizationsController localizationsController = new LocalizationsController(workerModel);

        localizationsController
                .doAfterCompose(comp.getFellow(localizationsContainerName).getFellow("localizationsContainer"));

        return localizationsController;
    }

    private OnlyOneVisible getVisibility() {
        if (visibility == null) {
            visibility = new OnlyOneVisible(listWindow, editWindow);
        }

        return visibility;
    }

    private IWorkerCRUDControllerEntryPoints getBookmarker() {
        return workerCRUD;
    }

    public List<BaseCalendar> getBaseCalendars() {
        return workerModel.getBaseCalendars();
    }

    public boolean isCalendarNull() {
        return workerModel.getCalendar() == null;
    }

    public boolean isCalendarNotNull() {
        return !isCalendarNull();
    }

    private void createCalendar() {
        Combobox combobox = (Combobox) getCurrentWindow().getFellow("createDerivedCalendar");
        Comboitem selectedItem = combobox.getSelectedItem();

        if (selectedItem == null) {
            throw new WrongValueException(combobox, "You should select one calendar");
        }

        BaseCalendar parentCalendar = combobox.getSelectedItem().getValue();
        if (parentCalendar == null) {
            parentCalendar = workerModel.getDefaultCalendar();
        }

        resourceCalendarModel.initCreateDerived(parentCalendar);
        resourceCalendarModel.generateCalendarCodes();
        workerModel.setCalendar((ResourceCalendar) resourceCalendarModel.getBaseCalendar());
    }

    private void editCalendar() {
        updateCalendarController();
        resourceCalendarModel.initEdit(workerModel.getCalendar());
        try {
            baseCalendarEditionController.doAfterCompose(editCalendarWindow);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        baseCalendarEditionController.setSelectedDay(new LocalDate());
        Util.reloadBindings(editCalendarWindow);
        Util.reloadBindings(createNewVersionWindow);
    }

    public BaseCalendarEditionController getBaseCalendarEditionController() {
        return baseCalendarEditionController;
    }

    private void reloadCurrentWindow() {
        Util.reloadBindings(getCurrentWindow());
    }

    private Window getCurrentWindow() {
        return editWindow;
    }

    private void updateCalendarController() {
        editCalendarWindow = (Window) getCurrentWindow().getFellow("editCalendarWindow");
        createNewVersionWindow = (Window) getCurrentWindow().getFellow("createNewVersion");

        createNewVersionWindow.setVisible(true);
        createNewVersionWindow.setVisible(false);

        baseCalendarEditionController = new BaseCalendarEditionController(
                resourceCalendarModel,
                editCalendarWindow,
                createNewVersionWindow,
                messages) {

            @Override
            public void goToList() {
                workerModel.setCalendar((ResourceCalendar) resourceCalendarModel.getBaseCalendar());
                reloadCurrentWindow();
            }

            @Override
            public void cancel() {
                workerModel.removeCalendar();
                resourceCalendarModel.cancel();
                reloadCurrentWindow();
            }

            @Override
            public void save() {
                validateCalendarExceptionCodes();
                ResourceCalendar calendar = (ResourceCalendar) resourceCalendarModel.getBaseCalendar();

                if (calendar != null) {
                    resourceCalendarModel.generateCalendarCodes();
                    workerModel.setCalendar(calendar);
                }

                reloadCurrentWindow();
            }

            @Override
            public void saveAndContinue() {
                save();
            }

        };

        editCalendarWindow.setAttribute("calendarController", this, true);
        createNewVersionWindow.setAttribute("calendarController", this, true);
    }

    public BaseCalendarsComboitemRenderer getBaseCalendarsComboitemRenderer() {
        return baseCalendarsComboitemRenderer;
    }

    private class BaseCalendarsComboitemRenderer implements ComboitemRenderer {

        @Override
        public void render(Comboitem comboitem, Object o, int i) throws Exception {
            BaseCalendar calendar = (BaseCalendar) o;
            comboitem.setLabel(calendar.getName());
            comboitem.setValue(calendar);

            if (isDefaultCalendar(calendar)) {
                Combobox combobox = (Combobox) comboitem.getParent();
                combobox.setSelectedItem(comboitem);
            }
        }

        private boolean isDefaultCalendar(BaseCalendar calendar) {
            BaseCalendar defaultCalendar = workerModel.getDefaultCalendar();

            return defaultCalendar.getId().equals(calendar.getId());
        }
    }

    public void goToCreateVirtualWorkerForm() {
        state = CRUDControllerState.CREATE;
        workerModel.prepareForCreate(true);
        createAssignedCriterions();
        resourcesCostCategoryAssignmentController.setResource(workerModel.getWorker());
        showEditWindow(_("Create Virtual Workers Group"));
        resourceCalendarModel.cancel();
    }

    public boolean isVirtualWorker() {
        boolean isVirtual = false;
        if ( this.workerModel != null && this.workerModel.getWorker() != null ) {
            if (this.workerModel.getWorker() != null) {
                isVirtual = this.workerModel.getWorker().isVirtual();
            }
        }
        return isVirtual;
    }

    public boolean isRealWorker() {
        return !isVirtualWorker();
    }

    public String getVirtualWorkerObservations() {
        if (isVirtualWorker()) {
            return ((VirtualWorker) this.workerModel.getWorker()).getObservations();
        } else {
            return "";
        }
    }

    public void setVirtualWorkerObservations(String observations) {
        if (isVirtualWorker()) {
            ((VirtualWorker) this.workerModel.getWorker()).setObservations(observations);
        }
    }


    /**
     * Operations to filter the machines by multiple filters
     */

    public Constraint checkConstraintFinishDate() {
        return (comp, value) -> {
            Date finishDate = (Date) value;
            if ( (finishDate != null) && (filterStartDate.getValue() != null) &&
                    (finishDate.compareTo(filterStartDate.getValue()) < 0) ) {
                filterFinishDate.setValue(null);
                throw new WrongValueException(comp, _("must be after start date"));
            }
        };
    }
    public Constraint checkConstraintStartDate() {
        return (comp, value) -> {
            Date startDate = (Date) value;
            if ( (startDate != null) && (filterFinishDate.getValue() != null) && (
                    startDate.compareTo(filterFinishDate.getValue()) > 0) ) {
                filterStartDate.setValue(null);
                throw new WrongValueException(comp, _("must be lower than end date"));
            }
        };
    }

    public void onApplyFilter() {
        ResourcePredicate predicate = createPredicate();
        if (predicate != null) {
            filterByPredicate(predicate);
        } else {
            showAllWorkers();
        }
    }

    private ResourcePredicate createPredicate() {
        List listFilters = bdFilters.getSelectedElements();

        String personalFilter = txtfilter.getValue();

        // Get the dates filter
        LocalDate startDate = null;
        LocalDate finishDate = null;
        if (filterStartDate.getValue() != null) {
            startDate = LocalDate.fromDateFields(filterStartDate.getValue());
        }
        if (filterFinishDate.getValue() != null) {
            finishDate = LocalDate.fromDateFields(filterFinishDate.getValue());
        }

        final Listitem item = filterLimitingResource.getSelectedItem();
        Boolean isLimitingResource = (item != null)
                ? LimitingResourceEnum.valueOf((LimitingResourceEnum) item.getValue()) : null;

        if ( listFilters.isEmpty() && (personalFilter == null || personalFilter.isEmpty()) &&
                startDate == null && finishDate == null && isLimitingResource == null ) {
            return null;
        }

        return new ResourcePredicate(listFilters, personalFilter, startDate, finishDate, isLimitingResource);
    }

    private void filterByPredicate(ResourcePredicate predicate) {
        List<Worker> filteredResources = workerModel.getFilteredWorker(predicate);
        listing.setModel(new SimpleListModel<>(filteredResources.toArray()));
        listing.invalidate();
    }

    private void clearFilterDates() {
        filterStartDate.setValue(null);
        filterFinishDate.setValue(null);
    }

    public void showAllWorkers() {
        listing.setModel(new SimpleListModel<>(workerModel.getAllCurrentWorkers().toArray()));
        listing.invalidate();
    }

    public enum LimitingResourceEnum {
        ALL(_("All")),
        LIMITING_RESOURCE(_("Queue-based resource")),
        NON_LIMITING_RESOURCE(_("Normal resource"));

        private String option;

        LimitingResourceEnum(String option) {
            this.option = option;
        }

        @Override
        public String toString() {
            return _(option);
        }

        public static LimitingResourceEnum valueOf(Boolean isLimitingResource) {
            return Boolean.TRUE.equals(isLimitingResource) ? LIMITING_RESOURCE : NON_LIMITING_RESOURCE;
        }

        public static Boolean valueOf(LimitingResourceEnum option) {
            if (LIMITING_RESOURCE.equals(option))
                return true;
            else if (NON_LIMITING_RESOURCE.equals(option))
                return false;
            else return null;

        }

        public static Set<LimitingResourceEnum> getLimitingResourceOptionList() {
            return EnumSet.of(LimitingResourceEnum.LIMITING_RESOURCE, LimitingResourceEnum.NON_LIMITING_RESOURCE);
        }

        public static Set<LimitingResourceEnum> getLimitingResourceFilterOptionList() {
            return EnumSet.of(LimitingResourceEnum.ALL, LimitingResourceEnum.LIMITING_RESOURCE, LimitingResourceEnum.NON_LIMITING_RESOURCE);
        }

        public static ResourceType toResourceType(LimitingResourceEnum limitingResource) {
            if (LIMITING_RESOURCE.equals(limitingResource)) {
                return ResourceType.LIMITING_RESOURCE;
            }

            return ResourceType.NON_LIMITING_RESOURCE;
        }

    }

    private void setupFilterLimitingResourceListbox() {
        for (LimitingResourceEnum resourceEnum : LimitingResourceEnum.getLimitingResourceFilterOptionList()) {
            Listitem item = new Listitem();
            item.setParent(filterLimitingResource);
            item.setValue(resourceEnum);
            item.appendChild(new Listcell(resourceEnum.toString()));
            filterLimitingResource.appendChild(item);
        }

        filterLimitingResource.setSelectedIndex(0);
    }

    public Set<LimitingResourceEnum> getLimitingResourceOptionList() {
        return LimitingResourceEnum.getLimitingResourceOptionList();
    }

    public LimitingResourceEnum getLimitingResource() {
        final Worker worker = getWorker();

        return (worker != null)
                ? LimitingResourceEnum.valueOf(worker.isLimitingResource())
                : LimitingResourceEnum.NON_LIMITING_RESOURCE;         // Default option
    }

    public void setLimitingResource(LimitingResourceEnum option) {
        Worker worker = getWorker();
        if (worker != null) {
            worker.setResourceType(LimitingResourceEnum.toResourceType(option));
            if (worker.isLimitingResource()) {
                worker.setUser(null);
            }
            Util.reloadBindings(userBindingGroupbox);
        }
    }

    public boolean isEditing() {
        return (getWorker() != null && !getWorker().isNewObject());
    }

    public void onCheckGenerateCode(Event e) {
        CheckEvent ce = (CheckEvent) e;
        if (ce.isChecked()) {
            // We have to auto-generate the code if it is unsaved
            try {
                workerModel.setCodeAutogenerated(ce.isChecked());
            } catch (ConcurrentModificationException err) {
                messages.showMessage(Level.ERROR, err.getMessage());
            }
        }
        Util.reloadBindings(editWindow);
    }

    public void confirmRemove(Worker worker) {
        try {
            if (!workerModel.canRemove(worker)) {
                messages.showMessage(
                        Level.WARNING,
                        _("This worker cannot be deleted because it has assignments to projects or imputed hours"));
                return;
            }

            int status = Messagebox.show(
                    _("Confirm deleting this worker. Are you sure?"),
                    _("Delete"), Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION);

            if (Messagebox.OK != status) {
                return;
            }

            boolean removeBoundUser = false;
            User user = workerModel.getBoundUserFromDB(worker);
            if (user != null && !user.isSuperuser()) {

                removeBoundUser = Messagebox.show(
                        _("Do you want to remove bound user \"{0}\" too?", user.getLoginName()),
                        _("Delete bound user"),
                        Messagebox.YES | Messagebox.NO, Messagebox.QUESTION) == Messagebox.YES;
            }

            workerModel.confirmRemove(worker, removeBoundUser);
            messages.showMessage(Level.INFO,
                    removeBoundUser ? _("Worker and bound user deleted") : _("Worker deleted"));
            goToList();
        } catch (InstanceNotFoundException e) {
            messages.showMessage(Level.INFO, _("This worker was already removed by other user"));
        }
    }

    public RowRenderer getWorkersRenderer() {
        return (row, data, i) -> {
            final Worker worker = (Worker) data;
            row.setValue(worker);

            row.addEventListener(Events.ON_CLICK, event -> goToEditForm(worker));

            row.appendChild(new Label(worker.getSurname()));
            row.appendChild(new Label(worker.getFirstName()));
            row.appendChild(new Label(worker.getNif()));
            row.appendChild(new Label(worker.getCode()));
            row.appendChild(new Label(Boolean.TRUE.equals(worker.isLimitingResource()) ? _("yes") : _("no")));

            Hbox hbox = new Hbox();
            hbox.appendChild(Util.createEditButton(event -> goToEditForm(worker)));
            hbox.appendChild(Util.createRemoveButton(event -> confirmRemove(worker)));
            row.appendChild(hbox);
        };
    }

    public void updateWindowTitle() {
        if (editWindow != null && state != CRUDControllerState.LIST) {
            Worker worker = getWorker();

            String entityType = _("Worker");
            if (worker.isVirtual()) {
                entityType = _("Virtual Workers Group");
            }

            String humanId = worker.getHumanId();

            String title;
            switch (state) {
                case CREATE:
                    if (StringUtils.isEmpty(humanId))
                        title = _("Create {0}", entityType);
                    else
                        title = _("Create {0}: {1}", entityType, humanId);
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

    public List<User> getPossibleUsersToBound() {
        return workerModel.getPossibleUsersToBound();
    }

    public User getBoundUser() {
        return workerModel.getBoundUser();
    }

    public void setBoundUser(User user) {
        workerModel.setBoundUser(user);
        Util.reloadBindings(userBindingGroupbox.getFellow("existingUserPanel"));
    }

    public boolean isUserSelected() {
        return userBandbox.getSelectedElement() != null;
    }

    public String getLoginName() {
        User user = getBoundUser();
        if (user != null) {
            return user.getLoginName();
        }

        return "";
    }

    public String getEmail() {
        User user = getBoundUser();
        if (user != null) {
            return user.getEmail();
        }

        return "";
    }

    public boolean isExistingUser() {
        int option = userBindingRadiogroup.getSelectedIndex();

        return UserBindingOption.EXISTING_USER.ordinal() == option;
    }

    public boolean isCreateNewUser() {
        int option = userBindingRadiogroup.getSelectedIndex();

        return UserBindingOption.CREATE_NEW_USER.ordinal() == option;
    }

    public void updateUserBindingView() {
        Util.reloadBindings(userBindingGroupbox);
    }

    public boolean isNotLimitingOrVirtualResource() {
        Worker worker = getWorker();

        return worker != null && !(worker.isLimitingResource() || worker.isVirtual());
    }

    public void goToUserEdition() {
        User user = getWorker().getUser();
        if (user != null && showConfirmUserEditionDialog() == Messagebox.OK) {
            userCRUD.goToEditForm(user);
        }
    }

    private int showConfirmUserEditionDialog() {
        return Messagebox.show(
                _("Unsaved changes will be lost. Would you like to continue?"),
                _("Confirm editing user"), Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION);
    }

    public boolean isNoRoleUserAccounts() {
        return !SecurityUtils.isSuperuserOrUserInRoles(UserRole.ROLE_USER_ACCOUNTS);
    }

    public String getUserEditionButtonTooltip() {
        return isNoRoleUserAccounts()
                ? _("You do not have permissions to go to edit user window")
                : "";
    }

    /**
     * Should be public!
     * Used in resources/worker/_list.zul
     */
    public boolean isCreateButtonDisabled() {
        Limits resourcesTypeLimit = limitsModel.getResourcesType();
        if (isNullOrZeroValue(resourcesTypeLimit)) {
            return false;
        } else {
            Integer resources = resourceDAO.getRowCount().intValue();
            return resources >= resourcesTypeLimit.getValue();
        }
    }

    /**
     * Should be public!
     * Used in resources/worker/_list.zul
     */
    public String getShowCreateFormLabel() {
        Limits resourcesTypeLimit = limitsModel.getResourcesType();

        if (isNullOrZeroValue(resourcesTypeLimit)) {
            return _("Create");
        }

        Integer resources = resourceDAO.getRowCount().intValue();
        int resourcesLeft = resourcesTypeLimit.getValue() - resources;

        return resources >= resourcesTypeLimit.getValue()
                ? _("Workers limit reached")
                : _("Create") + " ( " + resourcesLeft + " " + _("left") + " )";
    }

    private boolean isNullOrZeroValue (Limits resourcesTypeLimit) {
        return resourcesTypeLimit == null ||
                resourcesTypeLimit.getValue() == null ||
                resourcesTypeLimit.getValue().equals(0);
    }

}

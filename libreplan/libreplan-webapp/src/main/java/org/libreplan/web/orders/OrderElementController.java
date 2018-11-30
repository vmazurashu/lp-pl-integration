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

package org.libreplan.web.orders;

import org.apache.commons.lang3.StringUtils;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.orders.entities.OrderLine;
import org.libreplan.business.orders.entities.OrderLineGroup;
import org.libreplan.web.common.Util;
import org.libreplan.web.orders.criterionrequirements.AssignedCriterionRequirementToOrderElementController;
import org.libreplan.web.orders.criterionrequirements.OrderElementCriterionRequirementComponent;
import org.libreplan.web.orders.labels.AssignedLabelsToOrderElementController;
import org.libreplan.web.orders.labels.LabelsAssignmentToOrderElementComponent;
import org.libreplan.web.orders.materials.AssignedMaterialsToOrderElementController;
import org.libreplan.web.orders.materials.OrderElementMaterialAssignmentsComponent;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Window;

import static org.libreplan.web.I18nHelper._;

/**
 * Controller for {@link OrderElement} view of {@link Order} entities.
 * <br />
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 * @author Diego Pino Garcia <dpino@igalia.com>
 */
public class OrderElementController extends GenericForwardComposer {

    /**
     * {@link IOrderElementModel} with the data needed for this controller
     */
    private IOrderElementModel orderElementModel;

    private Component orderElementDetails;

    private DetailsOrderElementController detailsController;

    private Component orderElementHours;

    private AssignedHoursToOrderElementController assignedHoursToOrderElementController;

    private Component orderElementAdvances;

    private ManageOrderElementAdvancesController manageOrderElementAdvancesController;

    private LabelsAssignmentToOrderElementComponent orderElementLabels;

    private AssignedLabelsToOrderElementController assignedLabelsController;

    private Component orderElementTaskQualityForms;

    private AssignedTaskQualityFormsToOrderElementController assignedTaskQualityFormsController;

    private OrderElementCriterionRequirementComponent orderElementCriterionRequirements;

    private AssignedCriterionRequirementToOrderElementController assignedCriterionRequirementController;

    private OrderElementMaterialAssignmentsComponent orderElementMaterials;

    private AssignedMaterialsToOrderElementController assignedMaterialsController;

    public OrderElementController() {
        orderElementModel = (IOrderElementModel) SpringUtil.getBean("orderElementModel");

        assignedCriterionRequirementController = (AssignedCriterionRequirementToOrderElementController)
                SpringUtil.getBean("assignedCriterionRequirementToOrderElementController");
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        comp.setAttribute("orderElementController", this, true);
        setupDetailsOrderElementController();
    }

    private void setupDetailsOrderElementController() {
        detailsController = (DetailsOrderElementController)
                orderElementDetails.getAttribute("detailsController", true);
    }

    private void redraw(Component comp) {
        Util.createBindingsFor(comp);
        Util.reloadBindings(comp);
    }

    public void setupAssignedHoursToOrderElementController() {
        if (assignedHoursToOrderElementController == null) {

            assignedHoursToOrderElementController = (AssignedHoursToOrderElementController)
                    orderElementHours.getAttribute("assignedHoursToOrderElementController", true);

            assignedHoursToOrderElementController.openWindow(orderElementModel);
        } else {
            redraw(orderElementHours);
            assignedHoursToOrderElementController.paintProgressBars();
        }
    }

    private String getOrderElementName() {
        String name = "";
        if ((getOrderElement() != null) && (!StringUtils.isBlank(getOrderElement().getName()))) {
            name = ": " + getOrderElement().getName();
        }

        return _("Edit task {0}", name);
    }

    public void setupManageOrderElementAdvancesController() {
        if (manageOrderElementAdvancesController == null) {

            manageOrderElementAdvancesController = (ManageOrderElementAdvancesController)
                    orderElementAdvances.getAttribute("manageOrderElementAdvancesController", true);

            manageOrderElementAdvancesController.openWindow(orderElementModel);
        } else {
            manageOrderElementAdvancesController.refreshChangesFromOrderElement();
            manageOrderElementAdvancesController.createAndLoadBindings();
            manageOrderElementAdvancesController.refreshSelectedAdvance();
        }
    }

    public void setupAssignedLabelsToOrderElementController(){
        if (assignedLabelsController == null) {
            assignedLabelsController = orderElementLabels.getController();
            assignedLabelsController.openWindow(orderElementModel);
        } else {
            redraw(orderElementLabels);
        }
    }

    public void setupAssignedCriterionRequirementToOrderElementController() {
        if ( assignedCriterionRequirementController == null ) {
            assignedCriterionRequirementController = orderElementCriterionRequirements.getController();
            assignedCriterionRequirementController.openWindow(orderElementModel);
        } else {
            redraw(orderElementCriterionRequirements);
        }
    }

    public void setupAssignedMaterialsToOrderElementController() {
        if (assignedMaterialsController == null) {
            assignedMaterialsController = orderElementMaterials.getController();
            assignedMaterialsController.openWindow(getOrderElement());
        } else {
            redraw(orderElementMaterials);
        }
    }

    public void setupAssignedTaskQualityFormsToOrderElementController() {
        if (assignedTaskQualityFormsController == null) {

            assignedTaskQualityFormsController = (AssignedTaskQualityFormsToOrderElementController)
                    orderElementTaskQualityForms.getAttribute("assignedTaskQualityFormsController", true);

            assignedTaskQualityFormsController.openWindow(orderElementModel);
        } else {
            redraw(orderElementTaskQualityForms);
        }
    }

    public OrderElement getOrderElement() {
        return (orderElementModel == null) ? OrderLine.create() : orderElementModel.getOrderElement();
    }

    /**
     * Open the window to edit a {@link OrderElement}.
     * If it's a {@link OrderLineGroup} less fields will be enabled.
     * @param model
     *            The {@link OrderElement} to be edited
     */
    public void openWindow(IOrderElementModel model) {
        clearAll();
        setOrderElementModel(model);
        detailsController.openWindow(model);

        // Initialize the controllers
        manageOrderElementAdvancesController = null;
        assignedHoursToOrderElementController = null;
        assignedCriterionRequirementController = null;
        assignedLabelsController = null;
        assignedMaterialsController = null;
        assignedTaskQualityFormsController = null;

        try {
            ((Window) self).setTitle(getOrderElementName());
            ((Window) self).doModal();
        } catch (SuspendNotAllowedException e) {
            throw new RuntimeException(e);
        }
    }

    private void setOrderElementModel(IOrderElementModel orderElementModel) {
        this.orderElementModel = orderElementModel;
    }

    public void clearAll() {
        Tabpanel tabPanel = (Tabpanel) self.getFellow("tabPanelDetails");
        Util.createBindingsFor(tabPanel);
        Util.reloadBindings(tabPanel);
        clear();
    }

    private void clear() {
        selectDefaultTab();
    }

    private void selectDefaultTab() {
        selectTab("tabDetails");
    }

    private void selectTab(String str) {
        Tab tab = (Tab) self.getFellowIfAny(str);
        if (tab != null) {
            tab.setSelected(true);
        }
    }

    public void back() {
        close();
    }

    private void close() {
        if (validateTabs()) {
            self.setVisible(false);
            Util.reloadBindings(self.getParent());
        }
    }

    private boolean validateTabs() {
        return (validateProgressTab() && validateTaskQualityTab() && validateCriterionRequirementsTab());
    }

    private boolean validateTaskQualityTab() {
        try {
            if (assignedTaskQualityFormsController != null) {
                assignedTaskQualityFormsController.confirm();
            }
        } catch (WrongValueException e) {
            selectTab("tabTaskQualityForm");
            throw e;
        }

        return true;
    }

    private boolean validateCriterionRequirementsTab() {
        try {
            if (assignedCriterionRequirementController != null) {
                assignedCriterionRequirementController.close();
            }
        } catch (WrongValueException e) {
            selectTab("tabRequirements");
            throw e;
        }

        return true;
    }

    private boolean validateProgressTab() {
        try {
            if ((manageOrderElementAdvancesController != null) && (!manageOrderElementAdvancesController.close())) {
                selectTab("tabAdvances");
                return false;
            }
        } catch (WrongValueException e) {
            selectTab("tabAdvances");
            throw e;
        }

        return true;
    }

    public void close(Event event) {
        close();
        event.stopPropagation();
    }

}
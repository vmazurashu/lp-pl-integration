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

package org.libreplan.web.orders.criterionrequirements;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.libreplan.business.orders.entities.HoursGroup;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.orders.entities.OrderLine;
import org.libreplan.business.resources.entities.CriterionType;
import org.libreplan.business.resources.entities.CriterionWithItsType;
import org.libreplan.web.common.Util;
import org.libreplan.web.orders.CriterionRequirementWrapper;
import org.libreplan.web.orders.HoursGroupWrapper;
import org.libreplan.web.orders.IOrderElementModel;
import org.zkoss.zkplus.spring.SpringUtil;

/**
 * Controller for showing OrderElement assigned labels.
 *
 * @author Diego Pino Garcia <dpino@igalia.com>
 */
public class AssignedCriterionRequirementToOrderElementController
        extends AssignedCriterionRequirementController<OrderElement, IOrderElementModel> {


    protected IAssignedCriterionRequirementToOrderElementModel assignedCriterionRequirementToOrderElementModel;

    public AssignedCriterionRequirementToOrderElementController() {
        if ( assignedCriterionRequirementToOrderElementModel == null ) {
            assignedCriterionRequirementToOrderElementModel = (IAssignedCriterionRequirementToOrderElementModel)
                    SpringUtil.getBean("assignedCriterionRequirementToOrderElementModel");
        }
    }

    @Override
    public OrderElement getElement() {
        return getOrderElement();
    }

    public OrderElement getOrderElement() {
        return assignedCriterionRequirementToOrderElementModel.getElement();
    }

    public Set<CriterionType> getCriterionTypes() {
        return assignedCriterionRequirementToOrderElementModel.getTypes();
    }

    public void setOrderElement(OrderElement orderElement) {
        assignedCriterionRequirementToOrderElementModel.setElement(orderElement);
    }

    public void openWindow(IOrderElementModel orderElementModel) {
        assignedCriterionRequirementToOrderElementModel.setModel(orderElementModel.getOrderModel());
        openWindow(orderElementModel.getOrderElement());
    }

    public void openWindow(OrderElement orderElement) {
        assignedCriterionRequirementToOrderElementModel.init(orderElement);
        Util.createBindingsFor(self);
        Util.reloadBindings(self);
    }

    public void confirm() {
        assignedCriterionRequirementToOrderElementModel.confirm();
    }

    public List<CriterionRequirementWrapper> criterionRequirementWrappers() {
        return assignedCriterionRequirementToOrderElementModel.getCriterionRequirementWrappers();
    }

    public List<CriterionWithItsType> getCriterionWithItsTypes() {
        return assignedCriterionRequirementToOrderElementModel.getCriterionWithItsTypes();
    }

    public void addCriterionRequirementWrapper() {
        assignedCriterionRequirementToOrderElementModel.assignCriterionRequirementWrapper();
        reload();
    }

    public void remove(CriterionRequirementWrapper requirement) {
        assignedCriterionRequirementToOrderElementModel.deleteCriterionRequirementWrapper(requirement);
        reload();
    }

    public void invalidate(CriterionRequirementWrapper requirement) {
        assignedCriterionRequirementToOrderElementModel.setValidCriterionRequirementWrapper(requirement, false);
        reload();
    }

    public void validate(CriterionRequirementWrapper requirement) {
        assignedCriterionRequirementToOrderElementModel.setValidCriterionRequirementWrapper(requirement, true);
        reload();
    }

    protected void changeCriterionAndType(CriterionRequirementWrapper requirementWrapper,
                                          CriterionWithItsType newCriterionAndType) {

        assignedCriterionRequirementToOrderElementModel.changeCriterionAndType(requirementWrapper, newCriterionAndType);
    }

    protected void updateCriterionsWithDifferentResourceType(HoursGroupWrapper hoursGroupWrapper) {
        assignedCriterionRequirementToOrderElementModel.updateCriterionsWithDifferentResourceType(hoursGroupWrapper);
    }

    protected CriterionRequirementWrapper validateWrappers(List<CriterionRequirementWrapper> list) {
        return assignedCriterionRequirementToOrderElementModel.validateWrappers(criterionRequirementWrappers());
    }

    protected CriterionRequirementWrapper validateHoursGroupWrappers() {
        return assignedCriterionRequirementToOrderElementModel.validateHoursGroupWrappers();
    }

    public List<HoursGroupWrapper> getHoursGroupWrappers() {
        return assignedCriterionRequirementToOrderElementModel.getHoursGroupsWrappers();
    }

    public void addHoursGroup() {
        assignedCriterionRequirementToOrderElementModel.addNewHoursGroupWrapper();
        Util.reloadBindings(listHoursGroups);
    }

    protected void deleteHoursGroupWrapper(HoursGroupWrapper hoursGroupWrapper) {
        assignedCriterionRequirementToOrderElementModel.deleteHoursGroupWrapper(hoursGroupWrapper);
    }

    protected void addCriterionToHoursGroupWrapper(HoursGroupWrapper hoursGroupWrapper) {
        assignedCriterionRequirementToOrderElementModel.addCriterionToHoursGroupWrapper(hoursGroupWrapper);
    }

    protected CriterionRequirementWrapper addExceptionToHoursGroupWrapper(HoursGroupWrapper hoursGroupWrapper) {
        return assignedCriterionRequirementToOrderElementModel.addExceptionToHoursGroupWrapper(hoursGroupWrapper);
    }

    public void deleteCriterionToHoursGroup(HoursGroupWrapper hoursGroupWrapper,
                                            CriterionRequirementWrapper requirementWrapper) {

        assignedCriterionRequirementToOrderElementModel
                .deleteCriterionToHoursGroup(hoursGroupWrapper, requirementWrapper);
    }

    protected void selectCriterionToHoursGroup(HoursGroupWrapper hoursGroupWrapper,
                                               CriterionRequirementWrapper requirementWrapper,
                                               CriterionWithItsType criterionAndType) {

        assignedCriterionRequirementToOrderElementModel
                .selectCriterionToHoursGroup(hoursGroupWrapper, requirementWrapper, criterionAndType);
    }

    public void recalculateHoursGroup() {
        ((OrderLine) assignedCriterionRequirementToOrderElementModel.getElement()).recalculateHoursGroups();
        reload();
    }

    public boolean isCodeAutogenerated() {
        return assignedCriterionRequirementToOrderElementModel.isCodeAutogenerated();
    }

    public List<HoursGroup> getHoursGroups() {
        return (getElement() == null) || (assignedCriterionRequirementToOrderElementModel == null)
                ? new ArrayList<>()
                : super.getHoursGroups();
    }

    @Override
    protected List<HoursGroup> getHoursGroups(OrderElement orderElement) {
        return orderElement.getHoursGroups();
    }

    public boolean isEditableHoursGroup() {
        OrderElement element = getElement();
        return element != null && element instanceof OrderLine && !element.isJiraIssue();
    }

}

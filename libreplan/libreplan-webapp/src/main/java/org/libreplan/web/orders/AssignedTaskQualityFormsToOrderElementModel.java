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

import static org.libreplan.business.common.exceptions.ValidationException.invalidValue;
import static org.libreplan.web.I18nHelper._;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.joda.time.LocalDate;
import org.libreplan.business.advance.daos.IAdvanceTypeDAO;
import org.libreplan.business.advance.entities.AdvanceAssignment;
import org.libreplan.business.advance.entities.AdvanceMeasurement;
import org.libreplan.business.advance.entities.AdvanceType;
import org.libreplan.business.advance.entities.DirectAdvanceAssignment;
import org.libreplan.business.advance.exceptions.DuplicateAdvanceAssignmentForOrderElementException;
import org.libreplan.business.advance.exceptions.DuplicateValueTrueReportGlobalAdvanceException;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.orders.daos.IOrderElementDAO;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.qualityforms.daos.IQualityFormDAO;
import org.libreplan.business.qualityforms.entities.QualityForm;
import org.libreplan.business.qualityforms.entities.TaskQualityForm;
import org.libreplan.business.qualityforms.entities.TaskQualityFormItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 * @author Vova Perebykivskyi <vova@libreplan-enterprise.com>
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AssignedTaskQualityFormsToOrderElementModel implements IAssignedTaskQualityFormsToOrderElementModel {

    @Autowired
    private IOrderElementDAO orderDAO;

    @Autowired
    private IQualityFormDAO qualityFormDAO;

    @Autowired
    private IAdvanceTypeDAO advanceTypeDAO;

    private OrderElement orderElement;

    private IOrderModel orderModel;


    @Override
    public OrderElement getOrderElement() {
        return orderElement;
    }

    @Override
    public void setOrderElement(OrderElement orderElement) {
        this.orderElement = orderElement;
    }

    @Override
    @Transactional(readOnly = true)
    public void init(OrderElement orderElement) {
        this.orderElement = orderElement;
        initializeOrderElement(this.orderElement);
    }

    private void initializeOrderElement(OrderElement orderElement) {
        reattachQualityForms();
        orderDAO.reattach(orderElement);
        orderElement.getName();
        initializeTaskQualityForms(orderElement.getTaskQualityForms());
    }

    private void reattachQualityForms() {
        for (QualityForm qualityForm : orderModel.getQualityForms()) {
            qualityFormDAO.reattach(qualityForm);
        }
    }

    private void initializeTaskQualityForms(Collection<TaskQualityForm> taskQualityForms) {
        for (TaskQualityForm taskQualityForm : taskQualityForms) {
            taskQualityForm.getQualityForm().getName();
            initializeTaskQualityFormItems(taskQualityForm.getTaskQualityFormItems());
        }
    }

    public void initializeTaskQualityFormItems(Collection<TaskQualityFormItem> taskQualityFormItems) {
        for (TaskQualityFormItem taskQualityFormItem : taskQualityFormItems) {
            taskQualityFormItem.getName();
        }
    }

    @Override
    public List<QualityForm> getNotAssignedQualityForms() {
        return orderElement != null ? getListNotAssignedQualityForms() : new ArrayList<>();
    }

    private List<QualityForm> getListNotAssignedQualityForms() {
        List<QualityForm> result = new ArrayList<>();
        for (QualityForm qualityForm : orderModel.getQualityForms()) {
            if ( !isAssigned(qualityForm) ) {
                result.add(qualityForm);
            }
        }

        return result;
    }

    @Override
    public List<QualityForm> getAssignedQualityForms() {
        List<QualityForm> result = new ArrayList<>();
        for (QualityForm qualityForm : qualityFormDAO.getAll()) {
            if ( isAssigned(qualityForm) ) {
                result.add(qualityForm);
            }
        }

        return result;
    }

    @Override
    public List<TaskQualityForm> getTaskQualityForms() {
        List<TaskQualityForm> result = new ArrayList<>();
        if ( orderElement != null ) {
            result.addAll(orderElement.getTaskQualityForms());
        }

        return result;
    }

    @Override
    public void assignTaskQualityForm(QualityForm qualityForm) {
        orderElement.addTaskQualityForm(qualityForm);
    }

    @Override
    public void deleteTaskQualityForm(TaskQualityForm taskQualityForm) {
        orderElement.removeTaskQualityForm(taskQualityForm);
    }

    private AdvanceAssignment getAdvanceAssignment(TaskQualityForm taskQualityForm) {
        AdvanceType advanceType = taskQualityForm.getQualityForm().getAdvanceType();
        if ( advanceType == null ) {
            return null;
        }
        else {
            advanceTypeDAO.reattach(advanceType);
            return taskQualityForm.getOrderElement().getDirectAdvanceAssignmentByType(advanceType);
        }
    }

    @Override
    public boolean isAssigned(QualityForm qualityForm) {
        // orderDAO used for gathered data to be sent to LibrePlan server.
        // In general case orderElement will be not null and only that part of code will be triggered.

        if ( orderElement != null ) {
            for (TaskQualityForm taskQualityForm : orderElement.getTaskQualityForms()) {
                if ( qualityForm.equals(taskQualityForm.getQualityForm()) ) {
                    return true;
                }
            }
        } else {
            for (OrderElement currentElement : orderDAO.getAll()) {
                for ( TaskQualityForm taskQualityForm : currentElement.getTaskQualityForms() )
                    if ( qualityForm.equals(taskQualityForm.getQualityForm()) ) {
                        return true;
                    }
            }
        }

        return false;
    }

    @Override
    public void setOrderModel(IOrderModel orderModel) {
        this.orderModel = orderModel;
    }

    public boolean isDisabledPassedItem(TaskQualityForm taskQualityForm, TaskQualityFormItem item) {
        if ( (taskQualityForm == null) || (item == null) ) {
            return true;
        }

        if ( !taskQualityForm.isByItems() ) {
            return !(item.getPassed() || taskQualityForm.isPassedPreviousItem(item));
        }
        return false;
    }

    public boolean isDisabledDateItem(TaskQualityForm taskQualityForm, TaskQualityFormItem item) {
        return (taskQualityForm == null) || (item == null) || !taskQualityForm.isByItems() && !item.getPassed();
    }

    public boolean isCorrectConsecutiveDate(TaskQualityForm taskQualityForm, TaskQualityFormItem item) {
        if ( (taskQualityForm == null) || (item == null) ) {
            return true;
        }

        if ( taskQualityForm.isByItems() ) {
            return true;
        }
        return taskQualityForm.isCorrectConsecutiveDate(item);
    }

    public void updatePassedTaskQualityFormItems(TaskQualityForm taskQualityForm) {
        if (taskQualityForm != null) {
            Integer position = getFirstNotPassedPosition(taskQualityForm);
            List<TaskQualityFormItem> items = taskQualityForm.getTaskQualityFormItems();

            for (int i = position; i < items.size(); i++) {
                items.get(i).setPassed(false);
                items.get(i).setDate(null);
            }
        }
    }

    private Integer getFirstNotPassedPosition(TaskQualityForm taskQualityForm) {
        Integer position = 0;
        for (TaskQualityFormItem item : taskQualityForm.getTaskQualityFormItems()) {
            if (!item.getPassed()) {
                return position;
            }
            position++;
        }
        return position;
    }

    /**
     * Operation to confirm and validate.
     */
    @Override
    public void validate() {
        if (getOrderElement() != null) {
            for (TaskQualityForm taskQualityForm : orderElement.getTaskQualityForms()) {
                validateTaskQualityForm(taskQualityForm);
            }
        }
    }

    private void validateTaskQualityForm(TaskQualityForm taskQualityForm) {
        validateTaskQualityFormItems(taskQualityForm);
    }

    private void validateTaskQualityFormItems(TaskQualityForm taskQualityForm) {
        for (TaskQualityFormItem item : taskQualityForm.getTaskQualityFormItems()) {

            if ((!taskQualityForm.isByItems()) && (!taskQualityForm.isCorrectConsecutivePassed(item))) {

                throw new ValidationException(invalidValue(
                        _("cannot be checked until the previous item is checked before"),
                        "passed",
                        item.getName(),
                        taskQualityForm));

            }

            if ((!taskQualityForm.isByItems()) && (!taskQualityForm.isCorrectConsecutiveDate(item))) {
                throw new ValidationException(invalidValue(
                        _("must be after the previous date"),
                        "date",
                        item.getName(),
                        taskQualityForm));
            }

            if (!item.isIfDateCanBeNullConstraint()) {
                throw new ValidationException(invalidValue(
                        _("date not specified"),
                        "date",
                        item.getName(),
                        taskQualityForm));
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void addAdvanceAssignmentIfNeeded(TaskQualityForm taskQualityForm)
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        AdvanceType advanceType = taskQualityForm.getQualityForm().getAdvanceType();
        advanceTypeDAO.reattach(advanceType);

        AdvanceAssignment advanceAssignment =
                taskQualityForm.getOrderElement().getDirectAdvanceAssignmentByType(advanceType);

        if ( advanceAssignment == null ) {
            DirectAdvanceAssignment newAdvanceAssignment = DirectAdvanceAssignment.create(false, new BigDecimal(100));
            newAdvanceAssignment.setAdvanceType(advanceType);
            taskQualityForm.getOrderElement().addAdvanceAssignment(newAdvanceAssignment);
            addAdvanceMeasurements(taskQualityForm, newAdvanceAssignment);
        }
    }

    private void addAdvanceMeasurements(TaskQualityForm taskQualityForm, DirectAdvanceAssignment newAdvanceAssignment) {
        for (TaskQualityFormItem taskQualityFormItem : taskQualityForm.getTaskQualityFormItems()) {
            if ( taskQualityFormItem.getPassed() && (taskQualityFormItem.getDate() != null) ) {
                LocalDate date = LocalDate.fromDateFields(taskQualityFormItem.getDate());
                BigDecimal value = taskQualityFormItem.getPercentage();
                newAdvanceAssignment.addAdvanceMeasurements(AdvanceMeasurement.create(date, value));
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void removeAdvanceAssignmentIfNeeded(TaskQualityForm taskQualityForm) throws ValidationException {
        AdvanceAssignment advanceAssignment = this.getAdvanceAssignment(taskQualityForm);
        if ( advanceAssignment != null ) {
            if ( advanceAssignment.getReportGlobalAdvance() ) {
                showMessageDeleteSpread();
            } else {
                taskQualityForm.getOrderElement().removeAdvanceAssignment(advanceAssignment);
            }
        }
    }

    private void showMessageDeleteSpread() throws ValidationException {
        throw new ValidationException(_("Quality form cannot be removed as it is spreading progress"));
    }

    @Override
    public void updateAdvancesIfNeeded() {
        if ( orderElement != null ) {
            for (TaskQualityForm taskQualityForm : getTaskQualityForms()) {
                if ( taskQualityForm.isReportAdvance() ) {

                    DirectAdvanceAssignment advanceAssignment =
                            orderElement.getAdvanceAssignmentByType(taskQualityForm.getQualityForm().getAdvanceType());

                    advanceAssignment.clearAdvanceMeasurements();
                    addAdvanceMeasurements(taskQualityForm, advanceAssignment);
                }
            }
        }
    }

}

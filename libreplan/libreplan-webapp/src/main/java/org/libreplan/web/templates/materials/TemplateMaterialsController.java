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
package org.libreplan.web.templates.materials;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;

import org.libreplan.business.materials.entities.Material;
import org.libreplan.business.materials.entities.MaterialAssignmentTemplate;
import org.libreplan.business.templates.entities.OrderElementTemplate;
import org.libreplan.web.orders.materials.AssignedMaterialsController;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.TreeModel;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 *
 */
public class TemplateMaterialsController extends
        AssignedMaterialsController<OrderElementTemplate, MaterialAssignmentTemplate> {

    private IAssignedMaterialsToOrderElementTemplateModel assignedMaterialsToOrderElementTemplateModel;

    public TemplateMaterialsController(){
        assignedMaterialsToOrderElementTemplateModel =
                (IAssignedMaterialsToOrderElementTemplateModel) SpringUtil
                        .getBean("assignedMaterialsToOrderElementTemplateModel");
    }

    @Override
    protected MaterialAssignmentTemplate copyFrom(MaterialAssignmentTemplate assignment) {
        return MaterialAssignmentTemplate.copyFrom(assignment);
    }

    @Override
    protected void createAssignmentsBoxComponent(Component parent) {
        Executions.createComponents("/templates/_materialAssignmentsBox.zul", parent, new HashMap<String, String>());
    }

    @Override
    public TreeModel getAllMaterialCategories() {
        return assignedMaterialsToOrderElementTemplateModel.getAllMaterialCategories();
    }

    @Override
    protected Material getMaterial(MaterialAssignmentTemplate materialAssignment) {
        return materialAssignment.getMaterial();
    }

    @Override
    public TreeModel getMaterialCategories() {
        return assignedMaterialsToOrderElementTemplateModel.getMaterialCategories();
    }

    @Override
    protected IAssignedMaterialsToOrderElementTemplateModel getModel() {
        return assignedMaterialsToOrderElementTemplateModel;
    }

    @Override
    public BigDecimal getTotalPrice() {
        OrderElementTemplate template = assignedMaterialsToOrderElementTemplateModel.getTemplate();

        return template == null
                ? BigDecimal.ZERO
                : template.getTotalMaterialAssignmentPrice().setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    protected Double getTotalPrice(MaterialAssignmentTemplate materialAssignment) {
        return materialAssignment.getTotalPrice().doubleValue();
    }

    @Override
    public BigDecimal getTotalUnits() {
        OrderElementTemplate template = assignedMaterialsToOrderElementTemplateModel.getTemplate();
        return template == null ? BigDecimal.ZERO : template.getTotalMaterialAssignmentUnits();
    }

    @Override
    protected BigDecimal getUnits(MaterialAssignmentTemplate assignment) {
        return assignment.getUnits() == null ? BigDecimal.ZERO : assignment.getUnits();
    }

    @Override
    protected void initializeEdition(OrderElementTemplate template) {
        assignedMaterialsToOrderElementTemplateModel.initEdit(template);
    }

    @Override
    protected void setUnits(MaterialAssignmentTemplate assignment, BigDecimal units) {
        assignment.setUnits(units);
    }

}

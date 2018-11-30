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
package org.libreplan.web.resources.machine;

import static org.libreplan.web.I18nHelper._;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.resources.entities.CriterionWithItsType;
import org.libreplan.business.resources.entities.MachineWorkerAssignment;
import org.libreplan.business.resources.entities.MachineWorkersConfigurationUnit;
import org.libreplan.business.resources.entities.Worker;
import org.libreplan.web.common.IMessagesForUser;
import org.libreplan.web.common.Level;
import org.libreplan.web.common.MessagesForUser;
import org.libreplan.web.common.Util;
import org.libreplan.web.common.components.Autocomplete;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Constraint;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.ListModelList;


/**
 * @author Lorenzo Tilve <ltilve@igalia.com>
 */
public class MachineConfigurationController extends GenericForwardComposer {

    private IMachineModel machineModel;

    private IMessagesForUser messages;

    private Component messagesContainer;

    private Grid configurationUnitsGrid;

    public MachineConfigurationController() {
        machineModel = (IMachineModel) SpringUtil.getBean("machineModel");
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        comp.setAttribute("configurationController", this, true);
        messages = new MessagesForUser(messagesContainer);
    }

    public void addConfigurationUnit() {

        MachineWorkersConfigurationUnit unit =
                MachineWorkersConfigurationUnit.create(machineModel.getMachine(), "New configuration unit", new BigDecimal(1));

        machineModel.getMachine().addMachineWorkersConfigurationUnit(unit);
        configurationUnitsGrid.invalidate();
        Util.reloadBindings(configurationUnitsGrid);
    }

    public void reload() {
        Util.reloadBindings(configurationUnitsGrid);
    }

    public IMachineModel getMachineModel() {
        return machineModel;
    }

    public void setMachineModel(IMachineModel machineModel) {
        this.machineModel = machineModel;
    }

    void initConfigurationController(IMachineModel machineModel) {
        this.machineModel = machineModel;
        Util.reloadBindings(configurationUnitsGrid);
    }

    public List<MachineWorkersConfigurationUnit> getConfigurationUnits() {
        return this.machineModel.getConfigurationUnitsOfMachine();
    }

    public List<MachineWorkerAssignment> getWorkerAssignments() {
        MachineWorkersConfigurationUnit unit = this.machineModel.getConfigurationUnitsOfMachine().iterator().next();
        return (List<MachineWorkerAssignment>) unit.getWorkerAssignments();
    }

    public List<Criterion> getRequiredCriterions() {
        MachineWorkersConfigurationUnit unit = this.machineModel.getConfigurationUnitsOfMachine().iterator().next();
        return (List<Criterion>) unit.getRequiredCriterions();
    }

    public void addWorkerAssignment(MachineWorkersConfigurationUnit unit, Component c) {
        Autocomplete a = (Autocomplete) c.getPreviousSibling();
        Worker worker = (Worker) a.getItemByText(a.getValue());
        if (worker == null) {
            messages.showMessage(Level.ERROR, _("No worker selected"));
        } else {
            machineModel.addWorkerAssignmentToConfigurationUnit(unit, worker);
            Util.reloadBindings(c.getNextSibling());
        }
    }

    private boolean checkExistingCriterion(MachineWorkersConfigurationUnit unit, Criterion criterion) {
        boolean repeated = false;
        for (Criterion each : unit.getRequiredCriterions()) {
            if (each.getId().equals(criterion.getId())) {
                repeated = true;
            }
        }
        return repeated;
    }

    public void addCriterionRequirement(MachineWorkersConfigurationUnit unit, Button button) {
        Bandbox bandbox = (Bandbox) button.getPreviousSibling();
        Listitem item = ((Listbox) bandbox.getFirstChild().getFirstChild()).getSelectedItem();
        if (item != null) {
            CriterionWithItsType criterionAndType = item.getValue();
            bandbox.setValue(criterionAndType.getNameAndType());
            if (checkExistingCriterion(unit, criterionAndType.getCriterion())) {
                messages.showMessage(Level.ERROR, _("Criterion previously selected"));
            } else {
                machineModel.addCriterionRequirementToConfigurationUnit(unit, criterionAndType.getCriterion());
                bandbox.setValue("");
            }
        }
        Util.reloadBindings(button.getNextSibling());
    }

    public void selectCriterionRequirement(Listitem item, Bandbox bandbox) {
        if (item != null) {
            CriterionWithItsType criterionAndType = item.getValue();
            bandbox.setValue(criterionAndType.getNameAndType());
        } else {
            bandbox.setValue("");
        }
        bandbox.close();
        Util.reloadBindings(bandbox.getNextSibling().getNextSibling());
    }


    public void deleteConfigurationUnit(Row row) {
        machineModel.removeConfigurationUnit(row.getValue());
        configurationUnitsGrid.getChildren().get(1).removeChild(row);
        configurationUnitsGrid.getChildren().get(1).invalidate();
        Util.reloadBindings(configurationUnitsGrid);
    }


    public void deleteWorkerAssignment(Component component) {
        MachineWorkerAssignment assignment = ((Row) component).getValue();
        MachineWorkersConfigurationUnit conf = assignment.getMachineWorkersConfigurationUnit();
        conf.removeMachineWorkersConfigurationUnit(assignment);
        Util.reloadBindings(component.getParent().getParent());
    }


    public void deleteRequiredCriterion(Criterion criterion, Rows component) {

        MachineWorkersConfigurationUnit unit = ((Row) component
                .getParent().getParent().getParent().getParent().getParent())
                .getValue();

        unit.removeRequiredCriterion(criterion);
        Util.reloadBindings(component.getParent().getParent());
    }

    public Constraint validateEndDate() {
        return new Constraint() {
            @Override
            public void validate(Component comp, Object value) throws WrongValueException {
                validateEndDate(comp, value);
            }
        };
    }

    private void validateEndDate(Component comp, Object value) {
        if (value == null) {
            throw new WrongValueException(comp, _("End date is not valid, the date field can not be blank"));
        }
        else {
            Datebox startDateBox = (Datebox) comp.getPreviousSibling();
            if (startDateBox != null) {
                if (startDateBox.getValue() != null) {
                    if (startDateBox.getValue().compareTo((Date) value) > 0) {

                        throw new WrongValueException(
                                comp, _("End date is not valid, the new end date must be after start date"));
                    }
                }
            }
        }
    }

    /**
     * Should be public!
     * Used in _machineConfigurationUnits.zul
     */
    public ListModelList<Worker> getAllWorkers() {
        ListModelList <Worker> modelList = new ListModelList<>();
        modelList.addAll(machineModel.getWorkers());
        return modelList;
    }

    /**
     * Should be public!
     * Used in _machineConfigurationUnits.zul
     */
    public void redrawConfigurationUnitsGrid () {
        configurationUnitsGrid.invalidate();
    }

}

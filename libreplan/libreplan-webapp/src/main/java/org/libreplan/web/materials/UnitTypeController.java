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
package org.libreplan.web.materials;

import org.apache.commons.lang3.StringUtils;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.materials.entities.UnitType;
import org.libreplan.web.common.BaseCRUDController;
import org.libreplan.web.common.Level;
import org.libreplan.web.common.Util;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.CheckEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Constraint;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zkplus.spring.SpringUtil;

import java.util.ConcurrentModificationException;
import java.util.List;

import static org.libreplan.web.I18nHelper._;

/**
 * Controller for the listing and editing unit types.
 *
 * @author Javier Moran Rua <jmoran@igalia.com>
 * @author Cristina Alvarino Perez <cristina.alvarino@comtecsf.es>
 */

public class UnitTypeController extends BaseCRUDController<UnitType> {

    private IUnitTypeModel unitTypeModel;

    public UnitTypeController(){
        unitTypeModel = (IUnitTypeModel) SpringUtil.getBean("unitTypeModel");
    }

    public List<UnitType> getUnitTypes() {
        return unitTypeModel.getUnitTypes();
    }

    public RowRenderer getUnitTypeRenderer() {

        return new RowRenderer() {
            @Override
            public void render(Row row, Object data, int i) {
                final UnitType unitType = (UnitType) data;

                appendUnitTypeName(row, unitType);
                appendOperations(row, unitType);
                row.addEventListener(Events.ON_CLICK, new EventListener() {
                    @Override
                    public void onEvent(Event event) {
                        goToEditForm(unitType);
                    }
                });
            }

            private void appendUnitTypeName(Row row, UnitType unitType) {
                row.appendChild(new Label(unitType.getMeasure()));
            }

            private void appendOperations(Row row, final UnitType unitType) {
                Hbox hbox = new Hbox();

                hbox.appendChild(Util.createEditButton(new EventListener() {

                    @Override
                    public void onEvent(Event event) {
                        goToEditForm(unitType);
                    }
                }));

                Button removeButton = Util.createRemoveButton(new EventListener() {

                    @Override
                    public void onEvent(Event event) {
                        confirmDelete(unitType);
                    }
                });
                removeButton.setDisabled(unitTypeModel.isUnitTypeUsedInAnyMaterial(unitType));
                hbox.appendChild(removeButton);

                row.appendChild(hbox);
            }
        };
    }

    public UnitType getUnitType() {
        return unitTypeModel.getCurrentUnitType();
    }

    public Constraint uniqueMeasureName() {
        return new Constraint() {

            @Override
            public void validate(Component comp, Object value) {
                String strValue = (String) value;
                if (StringUtils.isBlank(strValue)) {
                    throw new WrongValueException(comp, _("cannot be empty"));
                }

                if (unitTypeModel.existsAnotherUnitTypeWithName(strValue)) {
                    throw new WrongValueException(comp,
                            _("The meausure name is not valid. There is " +
                                    "another unit type with the same " +
                                    "measure name"));
                }
            }
        };
    }

    public Constraint uniqueCode() {
        return new Constraint() {

            @Override
            public void validate(Component comp, Object value) {
                String strValue = (String) value;
                if (StringUtils.isBlank(strValue)) {
                    throw new WrongValueException(comp, _("cannot be empty"));
                }

                if (unitTypeModel.existsAnotherUnitTypeWithCode(strValue)) {
                    throw new WrongValueException(comp,
                            _("The code is not valid. There is another " +
                                    "unit type with the same code"));
                }
            }

        };
    }

    @Override
    protected void save() throws ValidationException {
        unitTypeModel.confirmSave();
    }

    public void onCheckGenerateCode(Event e) {
        CheckEvent ce = (CheckEvent) e;
        if (ce.isChecked()) {
            // We have to auto-generate the code for new objects
            try {
                unitTypeModel.setCodeAutogenerated(ce.isChecked());
            } catch (ConcurrentModificationException err) {
                messagesForUser.showMessage(Level.ERROR, err.getMessage());
            }
        }
        Util.reloadBindings(editWindow);
    }

    @Override
    protected String getEntityType() {
        return _("Material Unit");
    }

    @Override
    protected String getPluralEntityType() {
        return _("Material Units");
    }

    @Override
    protected void initCreate() {
        unitTypeModel.initCreate();
    }

    @Override
    protected void initEdit(UnitType unitType) {
        unitTypeModel.initEdit(unitType);
    }

    @Override
    protected UnitType getEntityBeingEdited() {
        return unitTypeModel.getCurrentUnitType();
    }

    @Override
    protected void delete(UnitType unitType) throws InstanceNotFoundException {
        unitTypeModel.remove(unitType);
    }

    @Override
    protected boolean beforeDeleting(UnitType unitType) {
        return !unitTypeModel.isUnitTypeUsedInAnyMaterial(unitType);
    }
}

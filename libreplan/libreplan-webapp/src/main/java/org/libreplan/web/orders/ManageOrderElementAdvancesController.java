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


import static org.libreplan.web.I18nHelper._;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.LocalDate;
import org.libreplan.business.advance.bootstrap.PredefinedAdvancedTypes;
import org.libreplan.business.advance.entities.AdvanceAssignment;
import org.libreplan.business.advance.entities.AdvanceMeasurement;
import org.libreplan.business.advance.entities.AdvanceType;
import org.libreplan.business.advance.entities.DirectAdvanceAssignment;
import org.libreplan.business.advance.entities.IndirectAdvanceAssignment;
import org.libreplan.business.advance.exceptions.DuplicateAdvanceAssignmentForOrderElementException;
import org.libreplan.business.advance.exceptions.DuplicateValueTrueReportGlobalAdvanceException;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.web.common.IMessagesForUser;
import org.libreplan.web.common.Level;
import org.libreplan.web.common.MessagesForUser;
import org.libreplan.web.common.Util;
import org.zkoss.util.IllegalSyntaxException;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Chart;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Constraint;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.XYModel;

/**
 * Controller for show the advances of the selected order element.
 * <br />
 *
 * @author Susana Montes Pedreria <smontes@wirelessgalicia.com>
 */

public class ManageOrderElementAdvancesController extends GenericForwardComposer {

    private static final Log LOG = LogFactory .getLog(ManageOrderElementAdvancesController.class);

    private IMessagesForUser messagesForUser;

    private int indexSelectedItem = -1;

    private IManageOrderElementAdvancesModel manageOrderElementAdvancesModel;

    private AdvanceTypeListRenderer advanceTypeListRenderer = new AdvanceTypeListRenderer();

    private AdvanceMeasurementRenderer advanceMeasurementRenderer = new AdvanceMeasurementRenderer();

    private Set<AdvanceAssignment> selectedAdvances = new HashSet<>();

    private Component messagesContainerAdvances;

    private Tabbox tabboxOrderElement;

    private IOrderElementModel orderElementModel;

    private Listbox editAdvances;

    private Listbox editAdvancesMeasurement;

    private Chart chart;

    public ManageOrderElementAdvancesController() {
        manageOrderElementAdvancesModel =
                (IManageOrderElementAdvancesModel) SpringUtil.getBean("manageOrderElementAdvancesModel");
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        comp.setAttribute("manageOrderElementAdvancesController", this, true);
        messagesForUser = new MessagesForUser(messagesContainerAdvances);
    }

    public List<AdvanceMeasurement> getAdvanceMeasurements() {
        List<AdvanceMeasurement> measurements = manageOrderElementAdvancesModel.getAdvanceMeasurements();
        Collections.reverse(measurements);

        return measurements;
    }

    public List<AdvanceAssignment> getAdvanceAssignments() {
        return manageOrderElementAdvancesModel.getAdvanceAssignments();
    }

    public boolean close()  {
        return save();
    }

    private void validate() {
        if ( !validateDataForm() )
            throw new IllegalSyntaxException(_("values are not valid, the values must not be null"));


        if ( !validateReportGlobalAdvance() )
            throw new IllegalSyntaxException(_("Invalid Spread values. At least one value should be true"));
    }

    public boolean save() {
        try {
            validate();
            manageOrderElementAdvancesModel.confirmSave();

            return true;
        } catch (DuplicateAdvanceAssignmentForOrderElementException e) {
            messagesForUser.showMessage(Level.ERROR, _("Cannot create another progress of the same type"));
        } catch (DuplicateValueTrueReportGlobalAdvanceException e) {
            messagesForUser.showMessage(Level.ERROR, _("Invalid Spread values. At least one value should be true"));
        } catch (IllegalSyntaxException e) {
            messagesForUser.showMessage(Level.ERROR, e.getMessage());
        } catch (InstanceNotFoundException e) {
            messagesForUser.showMessage(Level.ERROR, e.getMessage());
            LOG.error("Couldn't find element: " + e.getKey(), e);
        }
        increaseScreenHeight();

        return false;
    }

    public void openWindow(IOrderElementModel orderElementModel) {
        setOrderElementModel(orderElementModel);
        manageOrderElementAdvancesModel.initEdit(getOrderElement());
        selectedAdvances.clear();
        selectedAdvances.addAll(getAdvanceAssignments());
        createAndLoadBindings();
        selectSpreadAdvanceLine();
    }

    public void openWindow(OrderElement orderElement) {
        manageOrderElementAdvancesModel.initEdit(orderElement);
        selectedAdvances.clear();
        selectedAdvances.addAll(getAdvanceAssignments());
        createAndLoadBindings();
        selectSpreadAdvanceLine();
    }

    void createAndLoadBindings() {
        Util.createBindingsFor(self);
        Util.reloadBindings(self);
    }

    public void setOrderElementModel(IOrderElementModel orderElementModel) {
        this.orderElementModel = orderElementModel;
    }

    private OrderElement getOrderElement() {
        return orderElementModel.getOrderElement();
    }

    private void increaseScreenHeight() {
        if ( (tabboxOrderElement != null) && (tabboxOrderElement.getHeight() == null ||
                !"680px".equals(tabboxOrderElement.getHeight())) ) {

            tabboxOrderElement.setHeight("680px");
            tabboxOrderElement.invalidate();
        }
    }

    private void reloadAdvances() {
        Util.reloadBindings(self);
        setSelectedAdvanceLine();
    }

    private void setSelectedAdvanceLine() {
        if ( (indexSelectedItem > -1) && (indexSelectedItem < editAdvances.getItemCount()) ) {
            editAdvances.setSelectedItem(editAdvances.getItemAtIndex(indexSelectedItem));
            editAdvances.invalidate();
        }
    }

    public void selectAdvanceLine(Listitem selectedItem) {
        /* Validate the previous advance line before changing the selected advance */
        setSelectedAdvanceLine();
        findErrorsInMeasurements();

        /*
         * Preparation to select the advance line.
         * Set the current selected index that will show when the grid reloads.
         */
        if ( selectedItem != null ) {
            AdvanceAssignment advance = selectedItem.getValue();
            indexSelectedItem = editAdvances.getIndexOfItem(selectedItem);
            showInfoAbout(advance);
            prepareEditAdvanceMeasurements(advance);
            reloadAdvances();
        }
    }

    public void selectAdvanceLine(int index) {
        indexSelectedItem = index;
        if ( (indexSelectedItem >= 0) && (indexSelectedItem < getAdvanceAssignments().size()) ) {
            prepareEditAdvanceMeasurements(getAdvanceAssignments().get(
                    indexSelectedItem));
        }
        reloadAdvances();
    }

    public void selectSpreadAdvanceLine() {
        AdvanceAssignment advance = manageOrderElementAdvancesModel.getSpreadAdvance();
        if ( advance != null ) {
            indexSelectedItem = getAdvanceAssignments().indexOf(advance);
            showInfoAbout(advance);
            prepareEditAdvanceMeasurements(advance);
        } else {
            selectAdvanceLine(getAdvanceAssignments().size() - 1);
        }
        reloadAdvances();
    }

    private void showInfoAbout(AdvanceAssignment advance) {
        if ( manageOrderElementAdvancesModel.isSubcontractedAdvanceTypeAndSubcontractedTask(advance) )
            showErrorMessage(_("Subcontractor values are read only " +
                    "because they were reported by the subcontractor company."));

    }

    public void prepareEditAdvanceMeasurements(AdvanceAssignment advance) {
        if ( advance != null && advance.getAdvanceType() != null ) {
            manageOrderElementAdvancesModel.prepareEditAdvanceMeasurements(advance);
        }
    }

    /** It should be public! */
    public void goToCreateLineAdvanceAssignment() {
        findErrorsInMeasurements();
        boolean fineResult = manageOrderElementAdvancesModel.addNewLineAdvanceAssignment();
        if ( fineResult ) {
            int position = getAdvanceAssignments().size() - 1;
            selectAdvanceLine(position);
            selectedAdvances.add(getAdvanceAssignments().get(position));
        } else {
            showMessageNotAddMoreAdvances();
        }
    }

    public void goToCreateLineAdvanceMeasurement() {
        AdvanceMeasurement newMeasure = manageOrderElementAdvancesModel.addNewLineAdvanceMeasurement();
        if ( (newMeasure != null) &&
                (manageOrderElementAdvancesModel.hasConsolidatedAdvances(newMeasure.getAdvanceAssignment())) )
            newMeasure.setDate(null);

        reloadAdvances();
    }

    public void goToRemoveLineAdvanceAssignment(Listitem listItem) {
        AdvanceAssignment advance = listItem.getValue();

        if ( (editAdvances.getItemCount() > 1) && (advance.getReportGlobalAdvance()) )
            showMessageDeleteSpread();

        else if ( manageOrderElementAdvancesModel.hasConsolidatedAdvances(advance) )
            showErrorMessage(_("Progress Assignment cannot be deleted or changed. " +
                    "Progress Assignment contains Progress Consolidations values"));
        else {
            manageOrderElementAdvancesModel.removeLineAdvanceAssignment(advance);
            selectedAdvances.remove(advance);

            if ( indexSelectedItem == editAdvances.getIndexOfItem(listItem) )
                selectSpreadAdvanceLine();
            else
            if ( indexSelectedItem > editAdvances.getIndexOfItem(listItem) )
                selectAdvanceLine(indexSelectedItem - 1);
            else {
                prepareEditAdvanceMeasurements(getAdvanceAssignments().get(indexSelectedItem));
                reloadAdvances();
            }

        }
    }

    public void goToRemoveLineAdvanceMeasurement(Listitem listItem) {
        AdvanceMeasurement advance = listItem.getValue();
        if ( manageOrderElementAdvancesModel.canRemoveOrChange(advance) ) {
            manageOrderElementAdvancesModel.removeLineAdvanceMeasurement(advance);
            reloadAdvances();
        } else
            showErrorMessage(_("Progress Measurement cannot be deleted. Progress Measurement already consolidated"));
    }

    /** It should be public! */
    public String getInfoAdvance() {
        String infoAdvanceAssignment = manageOrderElementAdvancesModel.getInfoAdvanceAssignment();

        return infoAdvanceAssignment.isEmpty()
                ? _("Progress measurements")
                : _("Progress measurements") + ": " + infoAdvanceAssignment;
    }

    public boolean isReadOnlyAdvanceMeasurements() {
        return manageOrderElementAdvancesModel.isReadOnlyAdvanceMeasurements();
    }

    /** It should be public! */
    public AdvanceTypeListRenderer getAdvancesRenderer() {
        return advanceTypeListRenderer;
    }

    public void updatesValue() {
        this.setPercentage();
        this.setCurrentValue();
    }

    public class AdvanceTypeListRenderer implements ListitemRenderer {

        @Override
        public void render(Listitem listitem, Object o, int i) throws Exception {
            final AdvanceAssignment advance = (AdvanceAssignment) o;
            listitem.setValue(advance);

            Boolean readOnlyAdvance = false;
            boolean isQualityForm = false;

            if ( advance.getAdvanceType() != null ) {
                isQualityForm = manageOrderElementAdvancesModel.isQualityForm(advance);
                readOnlyAdvance = manageOrderElementAdvancesModel.isReadOnly(advance);

                if ( !readOnlyAdvance &&
                        manageOrderElementAdvancesModel.isSubcontractedAdvanceTypeAndSubcontractedTask(advance) )

                    readOnlyAdvance = true;

            }

            if ( (advance instanceof DirectAdvanceAssignment) &&
                    ((DirectAdvanceAssignment) advance).getAdvanceMeasurements().isEmpty() &&
                    !isQualityForm &&
                    !readOnlyAdvance )

                appendComboboxAdvanceType(listitem);
            else
                appendLabelAdvanceType(listitem);

            appendDecimalBoxMaxValue(listitem, isQualityForm || readOnlyAdvance);
            appendDecimalBoxValue(listitem);
            appendLabelPercentage(listitem);
            appendDateBoxDate(listitem);
            appendRadioSpread(listitem);
            appendCalculatedCheckbox(listitem);
            appendChartCheckbox(listitem);
            appendOperations(listitem, readOnlyAdvance);
        }

        private void appendDecimalBoxMaxValue(final Listitem listItem, boolean isQualityFormOrReadOnly) {
            final AdvanceAssignment advanceAssignment = listItem.getValue();
            final Decimalbox maxValue = new Decimalbox();
            maxValue.setScale(2);
            maxValue.setSclass("decimal-max-value");

            final DirectAdvanceAssignment directAdvanceAssignment;

            boolean isAdvanceAssignmentEquals = advanceAssignment.getAdvanceType() != null &&
                    advanceAssignment.getAdvanceType().getPercentage();

            if ( (advanceAssignment instanceof IndirectAdvanceAssignment) ||
                    isQualityFormOrReadOnly || isAdvanceAssignmentEquals ||
                    manageOrderElementAdvancesModel.hasConsolidatedAdvances(advanceAssignment) ) {

                maxValue.setDisabled(true);
            }

            if ( advanceAssignment instanceof IndirectAdvanceAssignment ) {

                directAdvanceAssignment = manageOrderElementAdvancesModel
                        .calculateFakeDirectAdvanceAssignment((IndirectAdvanceAssignment) advanceAssignment);
            } else {
                directAdvanceAssignment = (DirectAdvanceAssignment) advanceAssignment;
            }

            Util.bind(
                    maxValue,
                    () -> directAdvanceAssignment.getMaxValue(),
                    value -> {
                        if (!manageOrderElementAdvancesModel.hasConsolidatedAdvances(advanceAssignment))
                            directAdvanceAssignment.setMaxValue(value);
                    });

            maxValue.addEventListener(Events.ON_CHANGE, (EventListener) event -> {
                if (manageOrderElementAdvancesModel.hasConsolidatedAdvances(advanceAssignment))
                    throw new WrongValueException(
                            maxValue,
                            _("Progress Assignment cannot be deleted or changed. " +
                                    "Progress Assignment contains Progress Consolidations values"));
                else {
                    setPercentage();
                    reloadAdvances();
                }
            });

            Listcell listCell = new Listcell();
            listCell.appendChild(maxValue);
            listItem.appendChild(listCell);
            maxValue.setConstraint(checkMaxValue());
        }

        private void appendComboboxAdvanceType(final Listitem listItem) {
            final DirectAdvanceAssignment advance = listItem.getValue();
            final Combobox comboAdvanceTypes = new Combobox();
            final List<AdvanceType> listAdvanceType = manageOrderElementAdvancesModel.getPossibleAdvanceTypes(advance);

            for(AdvanceType advanceType : listAdvanceType){

                if ( !advanceType.getUnitName().equals(PredefinedAdvancedTypes.CHILDREN.getTypeName()) &&
                        !advanceType.isQualityForm() &&
                        !advanceType.isReadOnly() ) {

                    Comboitem comboItem = new Comboitem();
                    comboItem.setValue(advanceType);
                    comboItem.setLabel(advanceType.getUnitName());
                    comboItem.setParent(comboAdvanceTypes);

                    if ( (advance.getAdvanceType() != null) &&
                            (advance.getAdvanceType().getId().equals(advanceType.getId())) ) {

                        comboAdvanceTypes.setSelectedItem(comboItem);
                    }
                }
            }

            comboAdvanceTypes.addEventListener(Events.ON_SELECT, (EventListener) event -> {
                setMaxValue(listItem, comboAdvanceTypes);
                cleanFields(advance);
                setPercentage();
                reloadAdvances();
            });

            Util.bind(
                    comboAdvanceTypes,
                    () -> comboAdvanceTypes.getSelectedItem(),
                    comboItem -> {
                        if ( (comboItem!=null) &&
                                (comboItem.getValue() != null) &&
                                (comboItem.getValue() instanceof AdvanceType) ) {

                            AdvanceType advanceType = comboItem.getValue();
                            advance.setAdvanceType(advanceType);
                            advance.setMaxValue(manageOrderElementAdvancesModel.getMaxValue(advanceType));
                        }
                    });

            Listcell listCell = new Listcell();
            listCell.appendChild(comboAdvanceTypes);
            listItem.appendChild(listCell);
        }

        private void appendLabelAdvanceType(final Listitem listItem) {
            final AdvanceAssignment advance = listItem.getValue();
            Label unitName = new Label(advance.getAdvanceType().getUnitName());
            Listcell listCell = new Listcell();
            listCell.appendChild(unitName);
            listItem.appendChild(listCell);
        }

        private void appendDecimalBoxValue(final Listitem listItem) {
            final AdvanceAssignment advanceAssignment = listItem.getValue();
            Decimalbox value = new Decimalbox();
            value.setScale(2);
            value.setDisabled(true);

            DirectAdvanceAssignment directAdvanceAssignment;

            if (advanceAssignment instanceof IndirectAdvanceAssignment ) {

                directAdvanceAssignment = manageOrderElementAdvancesModel
                        .calculateFakeDirectAdvanceAssignment((IndirectAdvanceAssignment) advanceAssignment);
            } else {
                directAdvanceAssignment = (DirectAdvanceAssignment) advanceAssignment;
            }

            final AdvanceMeasurement advanceMeasurement =
                    manageOrderElementAdvancesModel.getLastAdvanceMeasurement(directAdvanceAssignment);

            if ( advanceMeasurement != null ) {
                Util.bind(value, () -> advanceMeasurement.getValue());
            }

            Listcell listCell = new Listcell();
            listCell.appendChild(value);
            listItem.appendChild(listCell);
        }


        private void appendLabelPercentage(final Listitem listItem) {
            final AdvanceAssignment advanceAssignment = listItem.getValue();
            Label percentage = new Label();

            DirectAdvanceAssignment directAdvanceAssignment;

            if ( advanceAssignment instanceof IndirectAdvanceAssignment ) {

                directAdvanceAssignment = manageOrderElementAdvancesModel
                        .calculateFakeDirectAdvanceAssignment((IndirectAdvanceAssignment) advanceAssignment);
            } else {
                directAdvanceAssignment = (DirectAdvanceAssignment) advanceAssignment;
            }

            final AdvanceMeasurement advanceMeasurement =
                    manageOrderElementAdvancesModel.getLastAdvanceMeasurement(directAdvanceAssignment);

            if ( advanceMeasurement != null ) {
                percentage.setValue(
                        manageOrderElementAdvancesModel
                                .getPercentageAdvanceMeasurement(advanceMeasurement).toString() + " %");
            }

            Listcell listCell = new Listcell();
            listCell.appendChild(percentage);
            listItem.appendChild(listCell);
        }

        private void appendDateBoxDate(final Listitem listItem){
            final AdvanceAssignment advanceAssignment = listItem.getValue();
            Datebox date = new Datebox();
            date.setDisabled(true);

            DirectAdvanceAssignment directAdvanceAssignment;

            if ( advanceAssignment instanceof IndirectAdvanceAssignment ) {
                directAdvanceAssignment = manageOrderElementAdvancesModel
                        .calculateFakeDirectAdvanceAssignment((IndirectAdvanceAssignment) advanceAssignment);
            } else {
                directAdvanceAssignment = (DirectAdvanceAssignment) advanceAssignment;
            }

            final AdvanceMeasurement advanceMeasurement =
                    manageOrderElementAdvancesModel.getLastAdvanceMeasurement(directAdvanceAssignment);

            if ( advanceMeasurement != null ) {
                Util.bind(
                        date,
                        () -> advanceMeasurement.getDate() == null
                                ? null
                                : advanceMeasurement.getDate().toDateTimeAtStartOfDay().toDate()
                );
            }

            Listcell listCell = new Listcell();
            listCell.appendChild(date);
            listItem.appendChild(listCell);
        }

        private void appendRadioSpread(final Listitem listItem){
            final AdvanceAssignment advanceAssignment = listItem.getValue();

            final Radio reportGlobalAdvance = Util.bind(
                    new Radio(),
                    () -> advanceAssignment.getReportGlobalAdvance(),
                    value -> {
                        advanceAssignment.setReportGlobalAdvance(value);
                        setReportGlobalAdvance(listItem);
                    });

            Listcell listCell = new Listcell();
            listCell.appendChild(reportGlobalAdvance);
            listItem.appendChild(listCell);

            if ( ((AdvanceAssignment) listItem.getValue()).getReportGlobalAdvance() ) {
                reportGlobalAdvance.getRadiogroup().setSelectedItem(reportGlobalAdvance);
                reportGlobalAdvance.getRadiogroup().invalidate();
            }
        }

        private void appendCalculatedCheckbox(final Listitem listItem){
            final AdvanceAssignment advance = listItem.getValue();
            Checkbox calculated = new Checkbox();
            boolean isCalculated = advance instanceof IndirectAdvanceAssignment;
            calculated.setChecked(isCalculated);
            calculated.setDisabled(true);

            Listcell listCell = new Listcell();
            listCell.appendChild(calculated);
            listItem.appendChild(listCell);
        }

        private void appendChartCheckbox(final Listitem listItem) {
            final AdvanceAssignment advance = listItem.getValue();
            final Checkbox chartCheckbox = new Checkbox();

            chartCheckbox.setChecked(selectedAdvances.contains(advance));

            chartCheckbox.addEventListener(Events.ON_CHECK,(EventListener) event -> {
                if ( chartCheckbox.isChecked() )
                    selectedAdvances.add(advance);
                else
                    selectedAdvances.remove(advance);

                reloadAdvances();
            });

            Listcell listCell = new Listcell();
            listCell.appendChild(chartCheckbox);
            listItem.appendChild(listCell);
        }

        private void appendOperations(final Listitem listItem, Boolean readOnly) {
            Hbox hbox = new Hbox();
            appendAddMeasurement(hbox, listItem, readOnly);
            appendRemoveButton(hbox, listItem, readOnly);

            Listcell listCell = new Listcell();
            listCell.appendChild(hbox);
            listItem.appendChild(listCell);
        }

        private void appendAddMeasurement(final Hbox hbox, final Listitem listItem, Boolean readOnly) {
            final AdvanceAssignment advance = listItem.getValue();
            final Button addMeasurementButton = createAddMeasurementButton();

            addMeasurementButton.addEventListener(Events.ON_CLICK, (EventListener) event -> {
                if ( !listItem.equals(editAdvances.getSelectedItem()) )
                    selectAdvanceLine(listItem);

                goToCreateLineAdvanceMeasurement();
            });

            if ( (advance.getAdvanceType() != null) && (advance.getAdvanceType().isQualityForm()) ) {
                addMeasurementButton.setDisabled(true);
                addMeasurementButton.setTooltiptext(_("Progress that are reported by quality forms can not be modified"));

            } else if ( (advance.getAdvanceType() != null) && (advance.getAdvanceType().isReadOnly()) ) {
                addMeasurementButton.setDisabled(true);
                addMeasurementButton.setTooltiptext(_("This progress type cannot be modified"));

            } else if ( advance instanceof IndirectAdvanceAssignment ) {
                addMeasurementButton.setDisabled(true);
                addMeasurementButton.setTooltiptext(_("Calculated progress can not be modified"));

            } else if ( readOnly ) {
                addMeasurementButton.setDisabled(true);
                addMeasurementButton.setTooltiptext(_("Subcontractor values are read only " +
                        "because they were reported by the subcontractor company."));
            }

            hbox.appendChild(addMeasurementButton);

        }

        private void appendRemoveButton(final Hbox hbox, final Listitem listItem, Boolean readOnly) {
            final AdvanceAssignment advance = listItem.getValue();
            final Button removeButton = createRemoveButton();

            removeButton.addEventListener(Events.ON_CLICK, event -> goToRemoveLineAdvanceAssignment(listItem));

            if ( (advance.getAdvanceType() != null) && (advance.getAdvanceType().isQualityForm()) ) {
                removeButton.setDisabled(true);
                removeButton.setTooltiptext(_("Progress that are reported by quality forms cannot be modified"));

            } else if ( (advance.getAdvanceType() != null) && (advance.getAdvanceType().isReadOnly()) ) {
                removeButton.setDisabled(true);
                removeButton.setTooltiptext(_("This progress type cannot be modified"));

            } else if ( advance instanceof IndirectAdvanceAssignment ) {
                removeButton.setDisabled(true);
                removeButton.setTooltiptext(_("Calculated progress cannot be removed"));

            } else if ( manageOrderElementAdvancesModel.hasConsolidatedAdvances(advance) ) {
                removeButton.setDisabled(true);
                removeButton.setTooltiptext(_("Consolidated progress cannot be removed"));

            } else if ( readOnly ) {
                removeButton.setDisabled(true);
                removeButton.setTooltiptext(_("Subcontractor values are read only " +
                        "because they were reported by the subcontractor company"));

            } else if ( manageOrderElementAdvancesModel.hasReportedProgress(advance) ) {
                removeButton.setDisabled(true);
                removeButton.setTooltiptext(_("Advance assignment cannot be removed as " +
                        "it has advance measures that have already been reported to the customer"));
            }

            hbox.appendChild(removeButton);
        }

        private void setReportGlobalAdvance(final Listitem item) {
            boolean spread = true;

            if ( manageOrderElementAdvancesModel.hasAnyConsolidatedAdvanceCurrentOrderElement() ) {
                showErrorMessage(_("Spread progress cannot be changed " +
                        "if there is a consolidation in any progress assignment from root task"));
                spread = false;
            } else if ( !radioSpreadIsConsolidated() )
                for (AdvanceAssignment advance : getAdvanceAssignments()) {
                    advance.setReportGlobalAdvance(false);
                }
            else {
                spread = false;
            }


            ((AdvanceAssignment) item.getValue()).setReportGlobalAdvance(spread);
            Util.reloadBindings(editAdvances);
            setSelectedAdvanceLine();
        }

        private boolean radioSpreadIsConsolidated() {
            for (AdvanceAssignment advance : getAdvanceAssignments())

                if ( (advance.getReportGlobalAdvance()) &&
                        (manageOrderElementAdvancesModel.hasConsolidatedAdvances(advance)) ) {

                    showErrorMessage(_("Spread progress cannot be changed " +
                            "if there is a consolidation in any progress assignment"));

                    return true;
                }

            return false;
        }

    }

    private void setMaxValue(final Listitem item,Combobox comboAdvanceTypes) {
        Listcell listCell = (Listcell)item.getChildren().get(1);
        Decimalbox miBox = (Decimalbox) listCell.getFirstChild();
        Comboitem selectedItem = comboAdvanceTypes.getSelectedItem();

        if ( selectedItem != null ) {
            AdvanceType advanceType = selectedItem.getValue();

            if ( advanceType != null ) {
                DirectAdvanceAssignment advance = item.getValue();
                advance.setMaxValue(manageOrderElementAdvancesModel.getMaxValue(advanceType));
                miBox.setValue(manageOrderElementAdvancesModel.getMaxValue(advanceType));
                miBox.invalidate();
            }
        }
    }

    private Constraint checkMaxValue() {
        return (comp, value) -> {
            Listitem item = (Listitem) comp.getParent().getParent();
            DirectAdvanceAssignment advance = item.getValue();
            if ( !manageOrderElementAdvancesModel.hasConsolidatedAdvances(advance) &&
                    (value == null || (BigDecimal.ZERO.compareTo((BigDecimal) value) >= 0)) ){
                ((Decimalbox) comp).setValue(advance.getAdvanceType().getDefaultMaxValue());(comp).invalidate();

                throw new WrongValueException(comp, _("The max value must be greater than 0"));
            }
        };
    }

    private void setPercentage(){
        if ( (this.indexSelectedItem < editAdvances.getItemCount()) && (this.indexSelectedItem >= 0) ) {

            Listitem selectedItem = editAdvances.getItemAtIndex(indexSelectedItem);
            AdvanceAssignment advanceAssignment = selectedItem.getValue();

            DirectAdvanceAssignment directAdvanceAssignment;

            if ( advanceAssignment instanceof IndirectAdvanceAssignment )

                directAdvanceAssignment = manageOrderElementAdvancesModel
                        .calculateFakeDirectAdvanceAssignment((IndirectAdvanceAssignment) advanceAssignment);
            else
                directAdvanceAssignment = (DirectAdvanceAssignment) advanceAssignment;

            final AdvanceMeasurement greatAdvanceMeasurement =
                    this.manageOrderElementAdvancesModel.getLastAdvanceMeasurement(directAdvanceAssignment);

            if ( greatAdvanceMeasurement != null ) {
                Listcell percentage = (Listcell) selectedItem.getChildren().get(3);

                ((Label) percentage
                        .getFirstChild())
                        .setValue(this.manageOrderElementAdvancesModel
                                .getPercentageAdvanceMeasurement(greatAdvanceMeasurement)
                                .toString() + " %");

                (percentage.getFirstChild()).invalidate();
            }
        }
    }

    private void setCurrentValue() {
        if ( this.indexSelectedItem >= 0 ) {

            Listitem selectedItem = editAdvances.getItemAtIndex(indexSelectedItem);
            AdvanceAssignment advanceAssignment = selectedItem.getValue();

            DirectAdvanceAssignment directAdvanceAssignment;
            if ( advanceAssignment instanceof IndirectAdvanceAssignment )

                directAdvanceAssignment = manageOrderElementAdvancesModel
                        .calculateFakeDirectAdvanceAssignment((IndirectAdvanceAssignment) advanceAssignment);

            else
                directAdvanceAssignment = (DirectAdvanceAssignment) advanceAssignment;

            final AdvanceMeasurement greatAdvanceMeasurement =
                    this.manageOrderElementAdvancesModel.getLastAdvanceMeasurement(directAdvanceAssignment);

            if ( greatAdvanceMeasurement != null ) {
                Listcell value = (Listcell)selectedItem.getChildren().get(2);
                ((Decimalbox) value.getFirstChild()).setValue(greatAdvanceMeasurement.getValue());
                (value.getFirstChild()).invalidate();
            }
        }
    }


    public void setCurrentDate() {
        this.manageOrderElementAdvancesModel.sortListAdvanceMeasurement();
        Util.reloadBindings(editAdvancesMeasurement);

        this.setPercentage();
        this.setCurrentValue();
        Util.reloadBindings(chart);
    }


    private void cleanFields(DirectAdvanceAssignment advance) {
        this.manageOrderElementAdvancesModel.cleanAdvance(advance);
    }

    private boolean validateDataForm(){
        return (validateListAdvanceAssignment()) &&(validateListAdvanceMeasurement());
    }

    private boolean validateListAdvanceAssignment(){
        for (int i = 0; i < editAdvances.getChildren().size(); i++)

            if ( editAdvances.getChildren().get(i) instanceof Listitem ) {
                Listitem listItem = (Listitem) editAdvances.getChildren().get(i);
                AdvanceAssignment advance = listItem.getValue();

                if ( advance != null ) {

                    if (advance.getAdvanceType() == null )
                        throw new WrongValueException(getComboboxTypeBy(listItem), _("cannot be empty"));

                    DirectAdvanceAssignment directAdvanceAssignment;

                    if ( advance instanceof IndirectAdvanceAssignment ) {

                        directAdvanceAssignment = manageOrderElementAdvancesModel
                                .calculateFakeDirectAdvanceAssignment((IndirectAdvanceAssignment) advance);
                    } else
                        directAdvanceAssignment = (DirectAdvanceAssignment) advance;

                    if ( directAdvanceAssignment != null && directAdvanceAssignment.getMaxValue() == null )
                        throw new WrongValueException(getDecimalboxMaxValueBy(listItem), _("cannot be empty"));
                }
            }

        return true;
    }

    private boolean validateListAdvanceMeasurement(){
        for (int i = 0; i < editAdvancesMeasurement.getChildren().size(); i++)

            if ( editAdvancesMeasurement.getChildren().get(i) instanceof Listitem ) {
                Listitem listItem = (Listitem) editAdvancesMeasurement.getChildren().get(i);
                AdvanceMeasurement advance = listItem.getValue();

                if ( advance != null ) {
                    // Validate the value of the advance measurement
                    Decimalbox valueBox = getDecimalboxBy(listItem);
                    validateMeasurementValue(valueBox, advance.getValue());

                    // Validate the date of the advance measurement
                    Datebox dateBox = getDateboxBy(listItem);

                    if ( advance.getDate() == null ) {
                        validateMeasurementDate(dateBox, null);
                    } else {
                        validateMeasurementDate(dateBox, advance.getDate().toDateTimeAtStartOfDay().toDate());
                    }
                }
            }

        return true;
    }

    private boolean validateAdvanceMeasurement(AdvanceMeasurement advance) {
        boolean result = true;
        // Validate the value of advance measurement
        if ( advance.getValue() == null )
            result = false;
        else {
            String errorMessage = validateValueAdvanceMeasurement(advance);
            if ( errorMessage != null )
                result = false;
        }

        // Validate the date of advance measurement
        if ( advance.getDate() == null )
            result = false;
        else {
            String errorMessage = validateDateAdvanceMeasurement(advance.getDate(), advance);
            if ( errorMessage != null )
                result = false;
        }

        return result;
    }

    private Combobox getComboboxTypeBy(Listitem item) {
        return (Combobox) (item.getChildren().get(0)).getFirstChild();
    }

    private Combobox getDecimalboxMaxValueBy(Listitem item) {
        return (Combobox) (item.getChildren().get(1)).getFirstChild();
    }

    private Decimalbox getDecimalboxBy(Listitem item) {
        return (Decimalbox) (item.getChildren().get(0)).getFirstChild();
    }

    private Datebox getDateboxBy(Listitem item) {
        return (Datebox) (item.getChildren().get(2)).getFirstChild();
    }

    private boolean validateReportGlobalAdvance(){
        boolean existItems = false;
        for (AdvanceAssignment advance : this.getAdvanceAssignments()) {
            existItems = true;
            if ( advance.getReportGlobalAdvance() )
                return true;
        }

        return !existItems;
    }

    /** It should be public! */
    public AdvanceMeasurementRenderer getAdvanceMeasurementRenderer() {
        return advanceMeasurementRenderer;
    }

    private class AdvanceMeasurementRenderer implements ListitemRenderer {

        @Override
        public void render(Listitem listitem, Object o, int i) throws Exception {
            AdvanceMeasurement advanceMeasurement = (AdvanceMeasurement) o;
            listitem.setValue(advanceMeasurement);

            appendDecimalBoxValue(listitem);
            appendLabelPercentage(listitem);
            appendDateboxDate(listitem);
            appendRemoveButton(listitem);
        }

        private void appendDecimalBoxValue(final Listitem listitem) {
            final AdvanceMeasurement advanceMeasurement =  listitem.getValue();
            final Decimalbox decimalbox = new Decimalbox();
            Listcell listcell = new Listcell();
            listcell.appendChild(decimalbox);
            listitem.appendChild(listcell);

            decimalbox.setScale(calculateScale(advanceMeasurement));

            decimalbox.setDisabled(isReadOnlyAdvanceMeasurements() ||
                    manageOrderElementAdvancesModel.hasConsolidatedAdvances(advanceMeasurement) ||
                    manageOrderElementAdvancesModel.isAlreadyReportedProgress(advanceMeasurement));

            decimalbox.addEventListener(Events.ON_CHANGE, (EventListener) event -> {
                if ( manageOrderElementAdvancesModel.canRemoveOrChange(advanceMeasurement) ) {
                    updatesValue();
                    validateMeasurementValue(decimalbox, decimalbox.getValue());
                } else
                    throw new WrongValueException(decimalbox, _("Progress Measurement cannot be deleted." +
                            " Progress Measurement already consolidated"));
            });

            Util.bind(
                    decimalbox,
                    advanceMeasurement::getValue,
                    value -> {
                        if (manageOrderElementAdvancesModel.canRemoveOrChange(advanceMeasurement)) {
                            advanceMeasurement.setValue(value);
                            reloadAdvances();
                        }
                    });

            decimalbox.focus();
        }

        private void appendLabelPercentage(final Listitem listitem) {
            final AdvanceMeasurement advanceMeasurement = listitem.getValue();

            BigDecimal percentage = manageOrderElementAdvancesModel.getPercentageAdvanceMeasurement(advanceMeasurement);
            Label percentageLabel = new Label(percentage.toString() + " %");

            Listcell listcell = new Listcell();
            listcell.appendChild(percentageLabel);
            listitem.appendChild(listcell);
        }

        private void appendDateboxDate(final Listitem listitem) {
            final AdvanceMeasurement advanceMeasurement = listitem.getValue();
            final Datebox date = new Datebox();

            Listcell listcell = new Listcell();
            listcell.appendChild(date);
            listitem.appendChild(listcell);

            date.setDisabled(isReadOnlyAdvanceMeasurements() ||
                    manageOrderElementAdvancesModel.hasConsolidatedAdvances(advanceMeasurement)
                    || manageOrderElementAdvancesModel.isAlreadyReportedProgress(advanceMeasurement));

            date.addEventListener(Events.ON_CHANGE,(EventListener) event -> {
                if ( manageOrderElementAdvancesModel.canRemoveOrChange(advanceMeasurement) ) {
                    validateMeasurementDate(date, date.getValue());
                    setCurrentDate();
                } else
                    throw new WrongValueException(date, _("Progress Measurement cannot be deleted." +
                            " Progress Measurement already consolidated"));
            });

            Util.bind(
                    date,
                    () -> advanceMeasurement.getDate() == null
                            ? null
                            : advanceMeasurement.getDate().toDateTimeAtStartOfDay().toDate(),
                    value -> {
                        if ( manageOrderElementAdvancesModel.canRemoveOrChange(advanceMeasurement) ) {

                            LocalDate oldDate = advanceMeasurement.getDate();
                            advanceMeasurement.setDate(new LocalDate(value));

                            if ( manageOrderElementAdvancesModel.hasConsolidatedAdvances(advanceMeasurement) ) {
                                showMessagesConsolidation(new LocalDate(value));
                                advanceMeasurement.setDate(oldDate);
                            }

                            manageOrderElementAdvancesModel.sortListAdvanceMeasurement();
                            reloadAdvances();
                        }
                    });
        }

        private void appendRemoveButton(final Listitem listItem) {

            final AdvanceMeasurement measure = listItem.getValue();
            final Button removeButton = createRemoveButton();

            DirectAdvanceAssignment advance = (DirectAdvanceAssignment) measure.getAdvanceAssignment();

            if ( (advance.getAdvanceType() != null) && (advance.getAdvanceType().isQualityForm()) ) {
                removeButton.setDisabled(true);

                removeButton.setTooltiptext(_("Progress measurements that are reported " +
                        "by quality forms cannot be removed"));

            } else if ( (advance.getAdvanceType() != null) && (advance.getAdvanceType().isReadOnly()) ) {
                removeButton.setDisabled(true);
                removeButton.setTooltiptext(_("This progress type cannot cannot be removed"));

            } else if ( advance.isFake() ) {
                removeButton.setDisabled(true);
                removeButton.setTooltiptext(_("Calculated progress measurements cannot be removed") );

            } else if ( manageOrderElementAdvancesModel.hasConsolidatedAdvances(measure) ) {
                removeButton.setDisabled(true);
                removeButton.setTooltiptext(_("Consolidated progress measurement cannot be removed"));

            } else if ( manageOrderElementAdvancesModel.isAlreadyReportedProgress(measure) ) {
                removeButton.setDisabled(true);
                removeButton.setTooltiptext(_("Values already sent to the customer. Values cannot be changed "));

            } else if ( isReadOnlyAdvanceMeasurements() ) {
                removeButton.setDisabled(isReadOnlyAdvanceMeasurements());

                removeButton.setTooltiptext(_("Subcontractor values are read only" +
                        " because they were reported by the subcontractor company."));
            }

            removeButton.addEventListener(Events.ON_CLICK, event -> goToRemoveLineAdvanceMeasurement(listItem));

            Listcell listCell = new Listcell();
            listCell.appendChild(removeButton);
            listItem.appendChild(listCell);
        }

        private int calculateScale(AdvanceMeasurement advanceMeasurement) {
            return advanceMeasurement
                    .getAdvanceAssignment()
                    .getAdvanceType()
                    .getUnitPrecision()
                    .stripTrailingZeros()
                    .scale();
        }

        private void showMessagesConsolidation(LocalDate date) {
            String message = _("Progress measurement cannot be canged to {0}, because it is consolidated", date);
            showErrorMessage(message);
        }

    }

    private Button createAddMeasurementButton() {
        Button addButton = new Button();
        addButton.setLabel(_("Add measure"));
        addButton.setClass("add-button");
        addButton.setTooltiptext(_("Add new progress measurement"));

        return addButton;
    }

    /** It should be public! */
    public XYModel getChartData() {
        return this.manageOrderElementAdvancesModel.getChartData(selectedAdvances);
    }

    private Button createRemoveButton() {
        Button removeButton = new Button();
        removeButton.setSclass("icono");
        removeButton.setImage("/common/img/ico_borrar1.png");
        removeButton.setHoverImage("/common/img/ico_borrar.png");
        removeButton.setTooltiptext(_("Delete"));

        return removeButton;
    }

    public void refreshChangesFromOrderElement() {
        manageOrderElementAdvancesModel.refreshChangesFromOrderElement();
    }

    private void showMessageNotAddMoreAdvances() {
        String message = _("All progress types have already been assigned.");
        increaseScreenHeight();
        messagesForUser.showMessage(Level.ERROR, message);
    }

    public void refreshSelectedAdvance() {
        if ( (indexSelectedItem < 0) || (indexSelectedItem >= getAdvanceAssignments().size()) )
            selectSpreadAdvanceLine();

        selectAdvanceLine(indexSelectedItem);
    }

    private void showMessageDeleteSpread() {
        String message = _("Spread progress cannot be removed. Please select another progress as spread.");
        showErrorMessage(message);
    }

    private void showErrorMessage(String message) {
        increaseScreenHeight();
        messagesForUser.showMessage(Level.ERROR, message);
    }

    private String validateValueAdvanceMeasurement(AdvanceMeasurement measurement) {
        if ( manageOrderElementAdvancesModel.greatThanMaxValue(measurement) )
            return _("Value is not valid. It must be smaller than max value");

        if ( !manageOrderElementAdvancesModel.isPrecisionValid(measurement) )
            return _("Value must be a multiple of the precision value of the progress type: {0}",
                    manageOrderElementAdvancesModel.getUnitPrecision().stripTrailingZeros().toPlainString());

        if ( manageOrderElementAdvancesModel.lessThanPreviousMeasurements() )
            return _("Invalid value. Value must be greater than the value of previous progress.");

        return null;
    }

    private String validateDateAdvanceMeasurement(LocalDate value, AdvanceMeasurement measurement) {
        LocalDate oldDate = measurement.getDate();
        measurement.setDate(value);

        if ( !manageOrderElementAdvancesModel.isDistinctValidDate(value, measurement))
            return _("Invalid date. Date must be unique for this Progress Assignment");

        if ( manageOrderElementAdvancesModel.hasConsolidatedAdvances(measurement) )
            measurement.setDate(oldDate);
        else {
            manageOrderElementAdvancesModel.sortListAdvanceMeasurement();
            if ( manageOrderElementAdvancesModel.lessThanPreviousMeasurements() )
                return _("Invalid value. Value must be greater than the value of previous progress.");
        }

        if ( !isReadOnlyAdvanceMeasurements() ) {

            LocalDate consolidatedUntil = manageOrderElementAdvancesModel
                    .getLastConsolidatedMeasurementDate(measurement.getAdvanceAssignment());

            if ( consolidatedUntil != null  && consolidatedUntil.compareTo(measurement.getDate()) >= 0 ) {
                return _("Date is not valid, it must be later than the last progress consolidation");
            }
            if ( manageOrderElementAdvancesModel.isAlreadyReportedProgressWith(value) ) {
                return _("Date is not valid, it must be later than the last progress reported to the customer");
            }
        }

        return null;
    }

    public boolean findErrorsInMeasurements() {
        boolean result = findPageWithError();
        validateListAdvanceMeasurement();

        return result;
    }

    private boolean findPageWithError() {
        int currentPage = editAdvancesMeasurement.getActivePage();
        int i = 0;
        int page = 0;
        changePage(page);

        for (Listitem item : editAdvancesMeasurement.getItems()) {
            AdvanceMeasurement advance = item.getValue();

            if ( advance != null ) {

                if ( !validateAdvanceMeasurement(advance) )
                    return true;


                i++;
                if ( i == editAdvancesMeasurement.getPageSize() ) {
                    i = 0;
                    changePage(++page);
                }
            }
        }
        changePage(currentPage);

        return false;
    }

    private void changePage(int page) {
        if ( (page >= 0) && (page < editAdvancesMeasurement.getPageCount()) ) {
            editAdvancesMeasurement.setActivePage(page);
            editAdvancesMeasurement.invalidate();
        }
    }


    /** It should be public! */
    public void onPagingMeasurement() {
        validateListAdvanceMeasurement();
    }

    public void validateMeasurementDate(Component comp, Date value) {
        AdvanceMeasurement advanceMeasurement = getAdvanceMeasurementByComponent(comp);
        if (manageOrderElementAdvancesModel.canRemoveOrChange(advanceMeasurement)) {

            if ( value == null && advanceMeasurement != null ) {
                advanceMeasurement.setDate(null);
                ((Datebox) comp).setValue(null);
                throw new WrongValueException(comp, _("cannot be empty"));

            } else {
                String errorMessage = validateDateAdvanceMeasurement(new LocalDate(value), advanceMeasurement);
                LocalDate date = null;

                if ( advanceMeasurement != null )
                    date = advanceMeasurement.getDate();

                if ( date != null )
                    ((Datebox) comp).setValue(date.toDateTimeAtStartOfDay().toDate());

                if ( errorMessage != null )
                    throw new WrongValueException(comp, errorMessage);
            }
        }
    }

    public void validateMeasurementValue(Component comp, Object value) {
        AdvanceMeasurement advanceMeasurement = getAdvanceMeasurementByComponent(comp);
        if ( (advanceMeasurement != null) && (manageOrderElementAdvancesModel.canRemoveOrChange(advanceMeasurement)) ) {

            advanceMeasurement.setValue((BigDecimal) value);
            ((Decimalbox) comp).setValue((BigDecimal) value);

            if ( (value) == null )
                throw new WrongValueException(comp, _("cannot be empty"));
            else {
                String errorMessage = validateValueAdvanceMeasurement(advanceMeasurement);
                if ( errorMessage != null )
                    throw new WrongValueException(comp, errorMessage);
            }
        }
    }

    private AdvanceMeasurement getAdvanceMeasurementByComponent(Component comp) {
        try {
            Listitem item = (Listitem) comp.getParent().getParent();

            return (AdvanceMeasurement) item.getValue();
        } catch (Exception e) {
            return null;
        }
    }
}

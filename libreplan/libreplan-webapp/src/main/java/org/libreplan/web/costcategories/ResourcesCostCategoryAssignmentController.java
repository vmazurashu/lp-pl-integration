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

package org.libreplan.web.costcategories;

import static org.libreplan.web.I18nHelper._;

import java.util.Date;
import java.util.List;

import org.joda.time.LocalDate;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.costcategories.entities.CostCategory;
import org.libreplan.business.costcategories.entities.ResourcesCostCategoryAssignment;
import org.libreplan.business.resources.entities.Resource;
import org.libreplan.web.common.ConstraintChecker;
import org.libreplan.web.common.Util;
import org.libreplan.web.common.components.Autocomplete;
import org.libreplan.web.util.ValidationExceptionPrinter;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;

/**
 * Controller for CRUD actions over a {@link ResourcesCostCategoryAssignment}.
 *
 * @author Jacobo Aragunde Perez <jaragunde@igalia.com>
 */
@SuppressWarnings("serial")
public class ResourcesCostCategoryAssignmentController extends GenericForwardComposer {

    private IResourcesCostCategoryAssignmentModel resourcesCostCategoryAssignmentModel;

    private CostCategoryAssignmentRenderer costCategoryAssignmentRenderer = new CostCategoryAssignmentRenderer();

    private Grid listResourcesCostCategoryAssignments;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        if ( resourcesCostCategoryAssignmentModel == null ) {
            resourcesCostCategoryAssignmentModel =
                    (IResourcesCostCategoryAssignmentModel) SpringUtil.getBean("resourcesCostCategoryAssignmentModel");
        }

        comp.setAttribute("assignmentController", this, true);
        this.listResourcesCostCategoryAssignments = (Grid) comp.getFellowIfAny("listResourcesCostCategoryAssignments");
    }

    public void addCostCategory() {
        resourcesCostCategoryAssignmentModel.addCostCategory();
        Util.reloadBindings(listResourcesCostCategoryAssignments);
    }

    public List<ResourcesCostCategoryAssignment> getCostCategoryAssignments() {
        return resourcesCostCategoryAssignmentModel.getCostCategoryAssignments();
    }

    private void removeCostCategoryAssignment(ResourcesCostCategoryAssignment assignment) {
        resourcesCostCategoryAssignmentModel.removeCostCategoryAssignment(assignment);
        Util.reloadBindings(listResourcesCostCategoryAssignments);
    }

    private CostCategory getCostCategory(Row listitem) {
        ResourcesCostCategoryAssignment assignment = listitem.getValue();
        return assignment.getCostCategory();
    }

    /**
     * Append a Autocomplete @{link CostCategory} to row.
     *
     * @param row
     */
    private void appendAutocompleteType(final Row row) {
        final Autocomplete autocomplete = new Autocomplete();
        autocomplete.setAutodrop(true);
        autocomplete.applyProperties();
        autocomplete.setFinder("CostCategoryFinder");
        autocomplete.setConstraint("no empty:" + _("A category must be selected"));

        // Getter, show type selected
        if (getCostCategory(row) != null) {
            autocomplete.setSelectedItem(getCostCategory(row));
        }

        // Setter, set type selected to HourCost.type
        autocomplete.addEventListener("onSelect", new EventListener() {

            @Override
            public void onEvent(Event event) {
                final Comboitem comboitem = autocomplete.getSelectedItem();

                if (comboitem != null) {
                    // Update resourcesCostCategoryAssignment
                    ResourcesCostCategoryAssignment assignment = row.getValue();
                    assignment.setCostCategory(comboitem.getValue());
                    row.setValue(assignment);
                }
            }
        });
        row.appendChild(autocomplete);
    }

    public void confirmRemove(ResourcesCostCategoryAssignment assignment) {
        int status = Messagebox.show(
                _("Confirm deleting this hour cost. Are you sure?"), _("Delete"),
                Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION);

        if (Messagebox.OK == status) {
            removeCostCategoryAssignment(assignment);
        }
    }

    /**
     * Append a delete {@link Button} to {@link Row}.
     *
     * @param row
     */
    private void appendDeleteButton(final Row row) {
        Button delete = new Button("", "/common/img/ico_borrar1.png");
        delete.setHoverImage("/common/img/ico_borrar.png");
        delete.setSclass("icono");
        delete.setTooltiptext(_("Delete"));

        delete.addEventListener(Events.ON_CLICK, new EventListener() {
            @Override
            public void onEvent(Event event) {
                confirmRemove(row.getValue());
            }
        });
        row.appendChild(delete);
    }

    /**
     * Append a Datebox "init date" to row.
     *
     * @param row
     */
    private void appendDateboxInitDate(final Row row) {
        Datebox initDateBox = new Datebox();
        bindDateboxInitDate(initDateBox, row.getValue());
        initDateBox.setConstraint("no empty:" + _("Start date cannot be empty"));
        row.appendChild(initDateBox);

        initDateBox.addEventListener("onChange", new EventListener() {

            @Override
            public void onEvent(Event event) {
                // Updates the constraint of the endDate box with the new date
                LocalDate initDate = ((ResourcesCostCategoryAssignment)row.getValue()).getInitDate();
                Datebox endDateBox = (Datebox) row.getChildren().get(2);
                endDateBox.setConstraint("after " +
                        String.format("%04d", initDate.getYear()) +
                        String.format("%02d", initDate.getMonthOfYear()) +
                        String.format("%02d", initDate.getDayOfMonth()));
            }
        });
    }

    /**
     * Binds Datebox "init date" to the corresponding attribute of a {@link ResourcesCostCategoryAssignment}.
     *
     * @param dateBoxInitDate
     * @param assignment
     */
    private void bindDateboxInitDate(final Datebox dateBoxInitDate, final ResourcesCostCategoryAssignment assignment) {

        Util.bind(
                dateBoxInitDate,
                new Util.Getter<Date>() {
                    @Override
                    public Date get() {
                        LocalDate dateTime = assignment.getInitDate();
                        /* TODO resolve deprecated */
                        return dateTime != null
                                ? new Date(dateTime.getYear() - 1900, dateTime.getMonthOfYear() - 1, dateTime.getDayOfMonth())
                                : null;
                    }
                },
                new Util.Setter<Date>() {
                    @Override
                    public void set(Date value) {
                        if (value != null) {
                            /* TODO resolve deprecated */
                            assignment.setInitDate(new LocalDate(value.getYear() + 1900, value.getMonth() + 1,value.getDate()));
                        }
                        else {
                            assignment.setInitDate(null);
                        }
                    }
                });
    }

    /**
     * Append a Datebox "end date" to row.
     *
     * @param row
     */
    private void appendDateboxEndDate(Row row) {
        Datebox endDateBox = new Datebox();
        bindDateboxEndDate(endDateBox, row.getValue());
        LocalDate initDate = ((ResourcesCostCategoryAssignment)row.getValue()).getInitDate();

        if (initDate != null) {
            endDateBox.setConstraint("after " +
                    String.format("%04d", initDate.getYear()) +
                    String.format("%02d", initDate.getMonthOfYear()) +
                    String.format("%02d", initDate.getDayOfMonth()));
        }
        row.appendChild(endDateBox);
    }

    /**
     * Binds Datebox "init date" to the corresponding attribute of a {@link ResourcesCostCategoryAssignment}.
     *
     * @param dateBoxEndDate
     * @param assignment
     */
    private void bindDateboxEndDate(final Datebox dateBoxEndDate, final ResourcesCostCategoryAssignment assignment) {
        Util.bind(
                dateBoxEndDate,
                new Util.Getter<Date>() {
                    @Override
                    public Date get() {
                        LocalDate dateTime = assignment.getEndDate();
                        /* TODO resolve deprecated */
                        return dateTime != null
                                ? new Date(dateTime.getYear() - 1900, dateTime.getMonthOfYear() - 1,dateTime.getDayOfMonth())
                                : null;
                    }
                },
                new Util.Setter<Date>() {
                    @Override
                    public void set(Date value) {
                        if (value != null) {
                            /* TODO resolve deprecated */
                            assignment.setEndDate(new LocalDate(value.getYear() + 1900, value.getMonth() + 1, value.getDate()));
                        }
                        else {
                            assignment.setEndDate(null);
                        }
                    }
                });
    }

    public CostCategoryAssignmentRenderer getCostCategoryAssignmentsRenderer() {
        return costCategoryAssignmentRenderer;
    }

    /**
     * RowRenderer for a @{HourCost} element.
     *
     * @author Jacobo Aragunde Perez <jaragunde@igalia.com>
     *
     */
    private class CostCategoryAssignmentRenderer implements RowRenderer {

        @Override
        public void render(Row row, Object data, int i) {
            ResourcesCostCategoryAssignment assignment = (ResourcesCostCategoryAssignment) data;

            row.setValue(assignment);

            // Create boxes
            appendAutocompleteType(row);
            appendDateboxInitDate(row);
            appendDateboxEndDate(row);

            appendDeleteButton(row);
        }
    }

    public void setResource(Resource resource) {
        resourcesCostCategoryAssignmentModel.setResource(resource);
        Util.reloadBindings(listResourcesCostCategoryAssignments);
    }

    public void validateConstraints() {
        ConstraintChecker.isValid(self);
    }

    /**
     * Check there are not category assignment overlaps.
     *
     * @return boolean
     */
    public boolean validate() {
        List<ResourcesCostCategoryAssignment> costCategoryAssignments =
                resourcesCostCategoryAssignmentModel.getCostCategoryAssignments();
        try {
            CostCategory.validateCostCategoryOverlapping(costCategoryAssignments);
        } catch (ValidationException e) {
            ValidationExceptionPrinter.showAt(listResourcesCostCategoryAssignments, e);
        }

        return true;
    }

}

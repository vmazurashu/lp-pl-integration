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

import java.math.BigDecimal;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.joda.time.LocalDate;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.costcategories.entities.CostCategory;
import org.libreplan.business.costcategories.entities.HourCost;
import org.libreplan.business.costcategories.entities.TypeOfWorkHours;
import org.libreplan.web.common.BaseCRUDController;
import org.libreplan.web.common.Level;
import org.libreplan.web.common.Util;
import org.zkoss.ganttz.util.ComponentsFinder;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.CheckEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

/**
 * Controller for CRUD actions over a {@link CostCategory}.
 *
 * @author Jacobo Aragunde Perez <jaragunde@igalia.com>
 * @author Diego Pino García <dpino@igalia.com>
 */
@SuppressWarnings("serial")
public class CostCategoryCRUDController extends BaseCRUDController<CostCategory> {

    private ICostCategoryModel costCategoryModel;

    private Grid listHourCosts;

    private Grid listCostCategories;

    private ListModel allHoursType;

    private HourCostListRenderer hourCostListRenderer = new HourCostListRenderer();

    public CostCategoryCRUDController() {
        if ( costCategoryModel == null ) {
            costCategoryModel = (ICostCategoryModel) SpringUtil.getBean("costCategoryModel");
        }
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        listHourCosts = (Grid) editWindow.getFellowIfAny("listHourCosts");
        listCostCategories = (Grid) listWindow.getFellowIfAny("listing");

        // Renders grid and disables delete button in case it cannot be removed
        listCostCategories.addEventListener("onInitRender", new EventListener() {

            @Override
            public void onEvent(Event event) {
                listCostCategories.renderAll();

                final Rows rows = listCostCategories.getRows();
                for (Iterator i = rows.getChildren().iterator(); i.hasNext(); ) {
                    final Row row = (Row) i.next();
                    final CostCategory category = row.getValue();
                    Button btnDelete = (Button) row.getChildren().get(2).getChildren().get(1);
                    if (!canRemoveCostCategory(category)) {
                        btnDelete.setDisabled(true);
                        btnDelete.setImage("/common/img/ico_borrar_out.png");
                        btnDelete.setHoverImage("/common/img/ico_borrar_out.png");
                    }
                }
            }
        });
        initializeHoursType();
    }

    private void initializeHoursType() {
        allHoursType = new SimpleListModel<>(costCategoryModel.getAllHoursType());
    }

    @Override
    protected void save() throws ValidationException{
        costCategoryModel.confirmSave();
    }

    @Override
    protected void beforeSaving() throws ValidationException {
        costCategoryModel.validateHourCostsOverlap();
    }

    private void showInvalidValues(ValidationException e) {
        Object value = e.getInvalidValue().getRootBean();
        if (value instanceof HourCost) {
            showInvalidValue((HourCost) value);
        }
        messagesForUser.showInvalidValues(e);
    }

    private void showInvalidValue(HourCost hourCost) {
        Row row = ComponentsFinder.findRowByValue(listHourCosts, hourCost);
        if (row != null) {
            if (hourCost.getType() == null) {
                Listbox workHoursType = getWorkHoursType(row);

                String message = workHoursType.getItems().isEmpty()
                        ? _("Hours types are empty. Please, create some hours types before proceeding")
                        : _("cannot be empty");

                throw new WrongValueException(getWorkHoursType(row), message);
            }
            if (hourCost.getPriceCost() == null) {
                throw new WrongValueException(getPricePerHour(row), _("cannot be empty"));
            }
        }
    }

    private Listbox getWorkHoursType(Row row) {
        return (Listbox) row.getChildren().get(1);
    }

    private Component getPricePerHour(Row row) {
        return row.getChildren().get(2);
    }

    public CostCategory getCostCategory() {
        return costCategoryModel.getCostCategory();
    }

    public List<CostCategory> getCostCategories() {
        return costCategoryModel.getCostCategories();
    }

    public Set<HourCost> getHourCosts() {
        return costCategoryModel.getHourCosts();
    }


    private TypeOfWorkHours getTypeOfWorkHours(Row listitem) {
        HourCost hourCost = listitem.getValue();
        return hourCost.getType();
    }

    /**
     * Append a textbox code to row.
     *
     * @param row
     */
    private void appendTextboxCode(final Row row) {
        final HourCost hourCost = row.getValue();
        final Textbox txtCode = new Textbox();
        txtCode.setWidth("200px");
        if (hourCost != null) {
            CostCategory costCategory = hourCost.getCategory();
            txtCode.setDisabled(costCategory.isCodeAutogenerated());

            Util.bind(
                    txtCode,
                    new Util.Getter<String>() {
                        @Override
                        public String get() {
                            return hourCost.getCode();
                        }
                    },
                    new Util.Setter<String>() {
                        @Override
                        public void set(String value) {
                            hourCost.setCode(value);
                        }
                    });

            if (!hourCost.getCategory().isCodeAutogenerated()) {
                txtCode.setConstraint("no empty:"
                        + _("cannot be empty"));
            } else {
                txtCode.setConstraint("");
            }
        }
        row.appendChild(txtCode);
    }

    /**
     * Append Selectbox of {@link TypeOfWorkHours} to row.
     *
     * @param row
     */
    private void appendHoursType(final Row row) {
        final HourCost hourCost = row.getValue();
        final Listbox lbHoursType = new Listbox();
        lbHoursType.setMold("select");
        lbHoursType.setModel(allHoursType);
        lbHoursType.renderAll();
        lbHoursType.applyProperties();

        if (lbHoursType.getItems().isEmpty()) {
            row.appendChild(lbHoursType);
            return;
        }

        // First time is rendered, select first item
        TypeOfWorkHours type = hourCost.getType();
        if ( hourCost.isNewObject() && type == null ) {
            Listitem item = lbHoursType.getItemAtIndex(0);
            item.setSelected(true);
            setHoursType(hourCost, item);
        } else {
            // If hoursCost has a type, select item with that type
            Listitem item = ComponentsFinder.findItemByValue(lbHoursType, type);
            if (item != null) {
                lbHoursType.selectItem(item);
            }
        }

        lbHoursType.addEventListener(Events.ON_SELECT, new EventListener() {
            @Override
            public void onEvent(Event event) {
                Listitem item = lbHoursType.getSelectedItem();
                if (item != null) {
                    setHoursType(row.getValue(), item);
                }
            }

        });

        row.appendChild(lbHoursType);
    }

    private void setHoursType(HourCost hourCost, Listitem item) {
        TypeOfWorkHours value = item != null ? (TypeOfWorkHours) item.getValue() : null;
        hourCost.setType(value);
        if (value != null) {
            final BigDecimal defaultPrice = value.getDefaultPrice();
            final Decimalbox dbPricePerHour = (Decimalbox) item.getParent().getNextSibling();
            hourCost.setPriceCost(defaultPrice);
            if (dbPricePerHour != null) {
                dbPricePerHour.setValue(defaultPrice);
            }
        } else if (item != null) {
            hourCost.setPriceCost(BigDecimal.ZERO);
            throw new WrongValueException(item.getParent(), _("Please, select an item"));
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
                confirmRemove((HourCost) row.getValue());
            }
        });

        row.appendChild(delete);
    }

    /**
     * Append a Textbox "hour cost" to row.
     *
     * @param row
     */
    private void appendDecimalboxCost(Row row) {
        Decimalbox boxCost = new Decimalbox();
        bindDecimalboxCost(boxCost, row.getValue());
        boxCost.setConstraint("no empty:" + _("cannot be empty"));
        boxCost.setFormat(Util.getMoneyFormat());
        row.appendChild(boxCost);
    }

    /**
     * Binds Decimalbox "hour cost" to the corresponding attribute of a {@link HourCost}.
     *
     * @param boxCost
     * @param hourCost
     */
    private void bindDecimalboxCost(final Decimalbox boxCost, final HourCost hourCost) {
        Util.bind(
                boxCost,
                new Util.Getter<BigDecimal>() {
                    @Override
                    public BigDecimal get() {
                        if (hourCost.getPriceCost() != null) {
                            return hourCost.getPriceCost();
                        }
                        return new BigDecimal(0);
                    }
                },
                new Util.Setter<BigDecimal>() {
                    @Override
                    public void set(BigDecimal value) {
                        hourCost.setPriceCost(value);
                    }
                });
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
                LocalDate initDate = ((HourCost)row.getValue()).getInitDate();
                Datebox endDateBox = (Datebox) row.getChildren().get(4);
                endDateBox.setConstraint("after " +
                        String.format("%04d", initDate.getYear()) +
                        String.format("%02d", initDate.getMonthOfYear()) +
                        String.format("%02d", initDate.getDayOfMonth()));
            }
        });
    }

    /**
     * Binds Datebox "init date" to the corresponding attribute of a {@link HourCost}.
     *
     * @param dateBoxInitDate
     * @param hourCost
     */
    private void bindDateboxInitDate(final Datebox dateBoxInitDate, final HourCost hourCost) {
        Util.bind(
                dateBoxInitDate,
                new Util.Getter<Date>() {
                    @Override
                    public Date get() {
                        LocalDate dateTime = hourCost.getInitDate();
                        if (dateTime != null) {
                            /* TODO resolve deprecated */
                            return new Date(dateTime.getYear() - 1900, dateTime.getMonthOfYear() - 1, dateTime.getDayOfMonth());
                        } else {
                            Date now = new Date();
                            hourCost.setInitDate(new LocalDate(now));
                            return now;
                        }
                    }
                },
                new Util.Setter<Date>() {
                    @Override
                    public void set(Date value) {
                        if (value != null) {
                            /* TODO resolve deprecated */
                            hourCost.setInitDate(new LocalDate(value.getYear() + 1900, value.getMonth() + 1, value.getDate()));
                        }
                        else {
                            hourCost.setInitDate(null);
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
        LocalDate initDate = ((HourCost)row.getValue()).getInitDate();
        if (initDate != null) {
            endDateBox.setConstraint("after " +
                    String.format("%04d", initDate.getYear()) +
                    String.format("%02d", initDate.getMonthOfYear()) +
                    String.format("%02d", initDate.getDayOfMonth()));
        }
        row.appendChild(endDateBox);
    }

    /**
     * Binds Datebox "init date" to the corresponding attribute of a {@link HourCost}.
     *
     * @param dateBoxEndDate
     * @param hourCost
     */
    private void bindDateboxEndDate(final Datebox dateBoxEndDate, final HourCost hourCost) {
        Util.bind(
                dateBoxEndDate,
                new Util.Getter<Date>() {
                    @Override
                    public Date get() {
                        LocalDate dateTime = hourCost.getEndDate();
                        if (dateTime != null) {
                            /* TODO resolve deprecated */
                            return new Date(
                                    dateTime.getYear() - 1900, dateTime.getMonthOfYear() - 1, dateTime.getDayOfMonth());
                        }
                        return null;
                    }
                },
                new Util.Setter<Date>() {
                    @Override
                    public void set(Date value) {
                        if (value != null) {
                            /* TODO resolve deprecated */
                            hourCost.setEndDate(
                                    new LocalDate(value.getYear() + 1900, value.getMonth() + 1, value.getDate()));
                        }
                        else {
                            hourCost.setEndDate(null);
                        }
                    }
                });
    }

    public void confirmRemove(HourCost hourCost) {
        int status = Messagebox.show(
                _("Confirm deleting this hour cost. Are you sure?"), _("Delete"),
                Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION);

        if (Messagebox.OK == status) {
            removeHourCost(hourCost);
        }
    }

    public HourCostListRenderer getRenderer() {
        return hourCostListRenderer;
    }

    /**
     * Adds a new {@link HourCost} to the list of rows.
     */
    public void addHourCost() {
        costCategoryModel.addHourCost();
        Util.reloadBindings(listHourCosts);
    }

    private void removeHourCost(HourCost hourCost) {
        costCategoryModel.removeHourCost(hourCost);
        Util.reloadBindings(listHourCosts);
    }

    /**
     * RowRenderer for a @{HourCost} element.
     */
    private class HourCostListRenderer implements RowRenderer {

        @Override
        public void render(Row row, Object o, int i) throws Exception {
            HourCost hourCost = (HourCost) o;

            row.setValue(hourCost);

            // Create boxes
            appendTextboxCode(row);
            appendHoursType(row);
            appendDecimalboxCost(row);
            appendDateboxInitDate(row);
            appendDateboxEndDate(row);

            appendDeleteButton(row);
        }
    }

    public void onCheckGenerateCode(Event e) {
        CheckEvent ce = (CheckEvent) e;
        if (ce.isChecked()) {
            // We have to auto-generate the code for new objects
            try {
                costCategoryModel.setCodeAutogenerated(ce.isChecked());
            } catch (ConcurrentModificationException err) {
                messagesForUser.showMessage(Level.ERROR, err.getMessage());
            }
        }
        Util.reloadBindings(listCostCategories);
        Util.reloadBindings(listHourCosts);
    }

    @Override
    protected String getEntityType() {
        return _("Cost Category");
    }

    @Override
    protected String getPluralEntityType() {
        return _("Cost Categories");
    }

    @Override
    protected void initCreate() {
        costCategoryModel.initCreate();
    }

    @Override
    protected void initEdit(CostCategory costCategory) {
        costCategoryModel.initEdit(costCategory);
    }

    @Override
    protected CostCategory getEntityBeingEdited() {
        return costCategoryModel.getCostCategory();
    }

    @Override
    protected void delete(CostCategory category) throws InstanceNotFoundException{
        costCategoryModel.confirmRemoveCostCategory(category);
        Util.reloadBindings(listCostCategories);
    }

    protected boolean beforeDeleting(CostCategory category) {
        return costCategoryModel.canRemoveCostCategory(category);
    }

    private boolean canRemoveCostCategory(CostCategory category) {
        return costCategoryModel.canRemoveCostCategory(category);
    }

}

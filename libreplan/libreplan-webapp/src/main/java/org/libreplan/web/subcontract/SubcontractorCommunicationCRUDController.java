/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2011 WirelessGalicia, S.L.
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

package org.libreplan.web.subcontract;

import org.libreplan.business.externalcompanies.entities.CommunicationType;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.planner.entities.SubcontractedTaskData;
import org.libreplan.business.planner.entities.SubcontractorCommunication;
import org.libreplan.business.planner.entities.SubcontractorCommunicationValue;
import org.libreplan.business.planner.entities.TaskElement;
import org.libreplan.web.common.IMessagesForUser;
import org.libreplan.web.common.MessagesForUser;
import org.libreplan.web.common.Util;
import org.libreplan.web.planner.tabs.IGlobalViewEntryPoints;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.SimpleListModel;

import java.util.List;

import static org.libreplan.web.I18nHelper._;

/**
 * Controller for CRUD actions over a {@link SubcontractorCommunication}
 *
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
@SuppressWarnings("serial")
public class SubcontractorCommunicationCRUDController extends GenericForwardComposer {

    private ISubcontractorCommunicationModel subcontractorCommunicationModel;

    private SubcontractorCommunicationRenderer subcontractorCommunicationRenderer = new SubcontractorCommunicationRenderer();

    private IGlobalViewEntryPoints globalView;

    protected IMessagesForUser messagesForUser;

    private Component messagesContainer;

    private Grid listing;

    private Grid listingValues;

    private Popup pp;

    public SubcontractorCommunicationCRUDController() {}

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        comp.setAttribute("controller", this);

        injectsObjects();
        messagesForUser = new MessagesForUser(messagesContainer);
    }

    private void injectsObjects() {
        subcontractorCommunicationModel =
                (ISubcontractorCommunicationModel) SpringUtil.getBean("subcontractorCommunicationModel");
        globalView = (IGlobalViewEntryPoints) SpringUtil.getBean("globalView");
    }

    public void goToEdit(SubcontractorCommunication subcontractorCommunication) {
        if (subcontractorCommunication != null) {
            TaskElement task = subcontractorCommunication.getSubcontractedTaskData().getTask();
            OrderElement orderElement = task.getOrderElement();
            Order order = subcontractorCommunicationModel.getOrder(orderElement);

            if (subcontractorCommunication.getCommunicationType().equals(CommunicationType.PROGRESS_UPDATE)) {
                globalView.goToAdvanceTask(order,task);
            } else {
                globalView.goToOrderDetails(order);
            }
        }
    }

    public FilterCommunicationEnum[] getFilterItems(){
        return FilterCommunicationEnum.values();
    }

    public FilterCommunicationEnum getCurrentFilterItem() {
        return subcontractorCommunicationModel.getCurrentFilter();
    }

    public void setCurrentFilterItem(FilterCommunicationEnum selected) {
        subcontractorCommunicationModel.setCurrentFilter(selected);
        refreshSubcontractorCommunicationsList();
    }

    private void refreshSubcontractorCommunicationsList() {
        // Update the subcontractor communication list
        listing.setModel(new SimpleListModel<>(getSubcontractorCommunications()));
        listing.invalidate();
    }

    protected void save(SubcontractorCommunication subcontractorCommunication) {
        subcontractorCommunicationModel.confirmSave(subcontractorCommunication);
    }

    public List<SubcontractorCommunication> getSubcontractorCommunications() {
        FilterCommunicationEnum currentFilter = subcontractorCommunicationModel.getCurrentFilter();
        switch(currentFilter) {
            case NOT_REVIEWED:
                return subcontractorCommunicationModel.getSubcontractorCommunicationWithoutReviewed();

            case ALL:
            default:
                return subcontractorCommunicationModel.getSubcontractorAllCommunications();
        }
    }

    public SubcontractorCommunicationRenderer getSubcontractorCommunicationRenderer() {
        return subcontractorCommunicationRenderer;
    }

    private class SubcontractorCommunicationRenderer implements RowRenderer {

        @Override
        public void render(Row row, Object data, int i) {
            SubcontractorCommunication subcontractorCommunication = (SubcontractorCommunication) data;
            row.setValue(subcontractorCommunication);

            final boolean reviewed = subcontractorCommunication.getReviewed();
            if (!reviewed) {
                row.setSclass("communication-not-reviewed");
            }

            appendLabel(row, subcontractorCommunication.getCommunicationType().toString());
            appendLabel(row, subcontractorCommunication.getSubcontractedTaskData().getTask().getName());
            appendLabel(row, getOrderName(subcontractorCommunication.getSubcontractedTaskData()));
            appendLabel(row, getOrderCode(subcontractorCommunication.getSubcontractedTaskData()));
            appendLabel(row, subcontractorCommunication.getSubcontractedTaskData().getExternalCompany().getName());
            appendLabel(row, Util.formatDateTime(subcontractorCommunication.getCommunicationDate()));
            appendLabelWithTooltip(row, subcontractorCommunication);
            appendCheckbox(row, subcontractorCommunication);
            appendOperations(row, subcontractorCommunication);
        }

        private String getOrderCode(SubcontractedTaskData subcontractedTaskData) {
            return subcontractorCommunicationModel.getOrderCode(subcontractedTaskData);
        }

        private String getOrderName(SubcontractedTaskData subcontractedTaskData) {
            return subcontractorCommunicationModel.getOrderName(subcontractedTaskData);
        }

        private String getLastValue(SubcontractorCommunication subcontractorCommunication) {
            SubcontractorCommunicationValue value =
                    subcontractorCommunication.getLastSubcontractorCommunicationValues();

            return (value != null) ? value.toString() : "";
        }

        private void appendLabel(Row row, String label) {
            row.appendChild(new Label(label));
        }

        private void appendLabelWithTooltip(final Row row,
                                            final SubcontractorCommunication subcontractorCommunication) {

            String lastValue = getLastValue(subcontractorCommunication);
            final Label compLabel = new Label(lastValue);

            if (subcontractorCommunication.getCommunicationType().equals(CommunicationType.PROGRESS_UPDATE)) {
                compLabel.setTooltip(pp);
                compLabel.addEventListener(Events.ON_MOUSE_OVER, arg0 -> {

                    List<SubcontractorCommunicationValue> model =
                            subcontractorCommunication.getSubcontractorCommunicationValues();

                    listingValues.setModel(new SimpleListModel<>(model));
                    listingValues.invalidate();
                });
            }
            row.appendChild(compLabel);
        }

        private void appendCheckbox(final Row row, final SubcontractorCommunication subcontractorCommunication) {
            final Checkbox checkBoxReviewed = new Checkbox();
            checkBoxReviewed.setChecked(subcontractorCommunication.getReviewed());

            checkBoxReviewed.addEventListener(Events.ON_CHECK, arg0 -> {
                subcontractorCommunication.setReviewed(checkBoxReviewed.isChecked());
                save(subcontractorCommunication);
                updateRowClass(row, checkBoxReviewed.isChecked());
            });

            row.appendChild(checkBoxReviewed);
        }

        private void updateRowClass(final Row row, Boolean reviewed){
            row.setSclass("");

            if (!reviewed) {
                row.setSclass("communication-not-reviewed");
            }
        }

        private void appendOperations(Row row, final SubcontractorCommunication subcontractorCommunication) {
            Button buttonEdit = new Button();
            buttonEdit.setSclass("icono");
            buttonEdit.setImage("/common/img/ico_editar1.png");
            buttonEdit.setHoverImage("/common/img/ico_editar.png");
            buttonEdit.setTooltiptext(_("Edit"));
            buttonEdit.addEventListener(Events.ON_CLICK, arg0 -> goToEdit(subcontractorCommunication));
            row.appendChild(buttonEdit);
        }
    }

    /**
     * Apply filter to subcontractors communications.
     */
    public void onApplyFilter() {
        refreshSubcontractorCommunicationsList();
    }
}

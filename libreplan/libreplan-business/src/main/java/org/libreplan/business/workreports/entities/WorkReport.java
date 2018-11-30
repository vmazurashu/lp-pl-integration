/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
 * Copyright (C) 2010-2012 Igalia, S.L.
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

package org.libreplan.business.workreports.entities;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import javax.validation.Valid;
import org.joda.time.LocalDate;
import org.libreplan.business.common.IntegrationEntity;
import org.libreplan.business.common.Registry;
import org.libreplan.business.common.Util;
import org.libreplan.business.common.entities.EntitySequence;
import org.libreplan.business.common.entities.PersonalTimesheetsPeriodicityEnum;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.labels.entities.Label;
import org.libreplan.business.labels.entities.LabelType;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.resources.entities.Resource;
import org.libreplan.business.resources.entities.Worker;
import org.libreplan.business.workingday.EffortDuration;
import org.libreplan.business.workreports.daos.IWorkReportDAO;
import org.libreplan.business.workreports.valueobjects.DescriptionField;
import org.libreplan.business.workreports.valueobjects.DescriptionValue;

/**
 * @author Diego Pino García <dpino@igalia.com>
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public class WorkReport extends IntegrationEntity implements IWorkReportsElements {

    private Date date;

    private WorkReportType workReportType;

    private Resource resource;

    private OrderElement orderElement;

    private Set<Label> labels = new HashSet<>();

    private Set<WorkReportLine> workReportLines = new HashSet<>();

    private Set<DescriptionValue> descriptionValues = new HashSet<>();

    private Integer lastWorkReportLineSequenceCode = 0;

    /**
     * Constructor for hibernate. Do not use!
     */
    public WorkReport() {

    }

    private WorkReport(WorkReportType workReportType) {
        this.setWorkReportType(workReportType);
    }

    private WorkReport(
            Date date,
            WorkReportType workReportType,
            Set<WorkReportLine> workReportLines,
            Resource resource,
            OrderElement orderElement) {

        this.date = date;
        this.setWorkReportType(workReportType);
        this.workReportLines = workReportLines;
        this.resource = resource;
        this.orderElement = orderElement;
    }

    public static WorkReport create() {
        return create(new WorkReport());
    }

    public static WorkReport create(WorkReportType workReportType) {
        return create(new WorkReport(workReportType));
    }

    public static WorkReport create(Date date,
                                    WorkReportType workReportType,
                                    Set<WorkReportLine> workReportLines,
                                    Resource resource,
                                    OrderElement orderElement) {

        WorkReport workReport = new WorkReport(date, workReportType, workReportLines, resource, orderElement);

        return create(workReport);
    }

    @Override
    public Date getDate() {
        return date != null ? new Date(date.getTime()) : null;
    }

    @Override
    public void setDate(Date date) {
        this.date = date != null ? new Date(date.getTime()) : null;
        if (workReportType != null) {
            if (workReportType.getDateIsSharedByLines()) {
                updateSharedDateByLines();
            } else {
                this.date = null;
            }
        }
    }

    @NotNull(message = "timesheet template not specified")
    public WorkReportType getWorkReportType() {
        return workReportType;
    }

    /**
     * Set the new {@link WorkReportType} and validate if the new
     * {@link WorkReportType} is different to the old {@link WorkReportType}.If
     * the new {@link WorkReportType} is different it updates the assigned
     * fields and labels of the new {@link WorkReportType}.
     * @param {@link WorkReportType}
     */
    public void setWorkReportType(WorkReportType workReportType) {
        this.workReportType = workReportType;

        updateSharedDateByLines();
        updateSharedResourceByLines();
        updateSharedOrderElementByLines();
        updateItsFieldsAndLabels(workReportType);
    }

    @Valid
    public Set<WorkReportLine> getWorkReportLines() {
        return Collections.unmodifiableSet(workReportLines);
    }

    public void addWorkReportLine(WorkReportLine workReportLine) {
        workReportLines.add(workReportLine);
    }

    public void removeWorkReportLine(WorkReportLine workReportLine) {
        workReportLines.remove(workReportLine);
    }

    @Override
    @Valid
    public Set<DescriptionValue> getDescriptionValues() {
        return Collections.unmodifiableSet(descriptionValues);
    }

    @Override
    public void setDescriptionValues(Set<DescriptionValue> descriptionValues) {
        this.descriptionValues = descriptionValues;
    }

    @Override
    public Set<Label> getLabels() {
        return labels;
    }

    @Override
    public void setLabels(Set<Label> labels) {
        this.labels = labels;
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public void setResource(Resource resource) {
        this.resource = resource;
        if (workReportType != null) {
            if (workReportType.getResourceIsSharedInLines()) {
                updateSharedResourceByLines();
            } else {
                this.resource = null;
            }
        }
    }

    @Override
    public OrderElement getOrderElement() {
        return orderElement;
    }

    @Override
    public void setOrderElement(OrderElement orderElement) {
        this.orderElement = orderElement;
        if (workReportType != null) {
            if (workReportType.getOrderElementIsSharedInLines()) {
                this.updateSharedOrderElementByLines();
            } else {
                this.orderElement = null;
            }
        }
    }

    @SuppressWarnings("unused")
    @AssertTrue(message = "date cannot be empty if it is shared by lines")
    public boolean isDateMustBeNotNullIfIsSharedByLinesConstraint() {
        return !firstLevelValidationsPassed() || !workReportType.getDateIsSharedByLines() || (getDate() != null);

    }

    @SuppressWarnings("unused")
    @AssertTrue(message = "resource cannot be empty if it is shared by lines")
    public boolean isResourceMustBeNotNullIfIsSharedByLinesConstraint() {
        return !firstLevelValidationsPassed() ||
                !workReportType.getResourceIsSharedInLines() ||
                (getResource() != null);

    }

    @SuppressWarnings("unused")
    @AssertTrue(message = "task cannot be empty if it is shared by lines")
    public boolean isOrderElementMustBeNotNullIfIsSharedByLinesConstraint() {
        return !firstLevelValidationsPassed() ||
                !workReportType.getOrderElementIsSharedInLines() ||
                (getOrderElement() != null);

    }

    @SuppressWarnings("unused")
    @AssertTrue(message = "label type: the timesheet have not assigned this label type")
    public boolean isAssignedLabelTypesConstraint() {
        if (this.workReportType == null) {
            return true;
        }

        if (this.workReportType.getHeadingLabels().size() != this.labels.size()) {
            return false;
        }

        for (WorkReportLabelTypeAssignment typeAssignment : this.workReportType.getHeadingLabels()) {
            try {
                getLabelByType(typeAssignment.getLabelType());
            } catch (InstanceNotFoundException e) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unused")
    @AssertTrue(message = "description value: the timesheet has some description field missing")
    public boolean isAssignedDescriptionValuesConstraint() {
        if (this.workReportType == null) {
            return true;
        }

        if (this.workReportType.getHeadingFields().size() > this.descriptionValues.size()) {
            return false;
        }

        for(DescriptionField field : this.workReportType.getHeadingFields()){
            try{
                getDescriptionValueByFieldName(field.getFieldName());
            }
            catch(InstanceNotFoundException e){
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unused")
    @AssertTrue(message = "There are repeated description values in the timesheet ")
    public boolean isAssignedRepeatedDescriptionValuesConstraint() {

        Set<String> textFields = new HashSet<>();

        for (DescriptionValue v : this.descriptionValues) {

            String name = v.getFieldName();

            if (!StringUtils.isBlank(name)) {
                if (textFields.contains(name.toLowerCase())) {
                    return false;
                } else {
                    textFields.add(name.toLowerCase());
                }
            }
        }
        return true;
    }

    public DescriptionValue getDescriptionValueByFieldName(String fieldName) throws InstanceNotFoundException {

        if (StringUtils.isBlank(fieldName)) {
            throw new InstanceNotFoundException(fieldName, DescriptionValue.class.getName());
        }

        for (DescriptionValue v : this.descriptionValues) {
            if (v.getFieldName().equalsIgnoreCase(StringUtils.trim(fieldName))) {
                return v;
            }
        }

        throw new InstanceNotFoundException(fieldName, DescriptionValue.class.getName());
    }

    public Label getLabelByType(LabelType type) throws InstanceNotFoundException {
        Validate.notNull(type);

        for (Label l : this.labels) {
            if (l.getType().getId().equals(type.getId())) {
                return l;
            }
        }

        throw new InstanceNotFoundException(type, LabelType.class.getName());
    }

    private void updateItsFieldsAndLabels(WorkReportType workReportType) {
        assignItsDescriptionValues(workReportType);
        assignItsLabels(workReportType);

        // it updates the fields and labels of its timesheet lines
        for (WorkReportLine line : getWorkReportLines()) {
            line.updateItsFieldsAndLabels();
        }
    }

    private void assignItsLabels(WorkReportType workReportType){
        if (workReportType != null) {
            labels.clear();
            for (WorkReportLabelTypeAssignment labelTypeAssignment : workReportType.getHeadingLabels()) {
                labels.add(labelTypeAssignment.getDefaultLabel());
            }
        }
    }

    private void assignItsDescriptionValues(WorkReportType workReportType) {
        if (workReportType != null) {
            descriptionValues.clear();
            for (DescriptionField descriptionField : workReportType.getHeadingFields()) {
                DescriptionValue descriptionValue = DescriptionValue.create(descriptionField.getFieldName(), null);
                descriptionValues.add(descriptionValue);
            }
        }
    }

    private void updateSharedDateByLines() {
        for (WorkReportLine line : getWorkReportLines()) {
            line.updateSharedDateByLines();
        }
    }

    private void updateSharedResourceByLines() {
        for (WorkReportLine line : getWorkReportLines()) {
            line.updateSharedResourceByLines();
        }
    }

    private void updateSharedOrderElementByLines() {
        for (WorkReportLine line : getWorkReportLines()) {
            line.updateSharedOrderElementByLines();
        }
    }

    @Override
    protected IWorkReportDAO getIntegrationEntityDAO() {
        return Registry.getWorkReportDAO();
    }

    private boolean firstLevelValidationsPassed() {
        return (workReportType != null);
    }

    public WorkReportLine getWorkReportLineByCode(String code) throws InstanceNotFoundException {

        if (StringUtils.isBlank(code)) {
            throw new InstanceNotFoundException(code, WorkReportLine.class.getName());
        }

        for (WorkReportLine l : this.workReportLines) {
            if (l.getCode().equalsIgnoreCase(StringUtils.trim(code))) {
                return l;
            }
        }

        throw new InstanceNotFoundException(code, WorkReportLine.class.getName());

    }

    @AssertTrue(message = "The timesheet line codes must be unique.")
    public boolean isNonRepeatedWorkReportLinesCodesConstraint() {
        return getFirstRepeatedCode(this.workReportLines) == null;
    }

    public void generateWorkReportLineCodes(int numberOfDigits) {
        for (WorkReportLine line : this.getWorkReportLines()) {

            if ( (line.getCode() == null) ||
                    (line.getCode().isEmpty()) ||
                    (!line.getCode().startsWith(this.getCode())) ) {

                this.incrementLastWorkReportLineSequenceCode();

                String lineCode = EntitySequence.formatValue(numberOfDigits, this.getLastWorkReportLineSequenceCode());

                line.setCode(this.getCode() + EntitySequence.CODE_SEPARATOR_CHILDREN + lineCode);
            }
        }
    }

    public void incrementLastWorkReportLineSequenceCode() {
        if (lastWorkReportLineSequenceCode == null) {
            lastWorkReportLineSequenceCode = 0;
        }
        lastWorkReportLineSequenceCode++;
    }

    @NotNull(message = "last timesheet line sequence code not specified")
    public Integer getLastWorkReportLineSequenceCode() {
        return lastWorkReportLineSequenceCode;
    }

    public EffortDuration getTotalEffortDuration() {
        EffortDuration result = EffortDuration.zero();
        for (WorkReportLine line : workReportLines) {
            result = result.plus(line.getEffort());
        }
        return result;
    }

    @AssertTrue(message = "only one timesheet line per day and task is allowed in personal timesheets")
    public boolean isOnlyOneWorkReportLinePerDayAndOrderElementInPersonalTimesheetConstraint() {
        if (!getWorkReportType().isPersonalTimesheetsType()) {
            return true;
        }

        Map<OrderElement, Set<LocalDate>> map = new HashMap<>();
        for (WorkReportLine line : workReportLines) {
            OrderElement orderElement = line.getOrderElement();
            if (map.get(orderElement) == null) {
                map.put(orderElement, new HashSet<LocalDate>());
            }

            LocalDate date = LocalDate.fromDateFields(line.getDate());
            if (map.get(orderElement).contains(date)) {
                return false;
            }
            map.get(orderElement).add(date);
        }
        return true;
    }

    @AssertTrue(message = "In personal timesheets, all timesheet lines should be in the same period")
    public boolean isAllWorkReportLinesInTheSamePeriodInPersonalTimesheetConstraint() {
        if (!getWorkReportType().isPersonalTimesheetsType()) {
            return true;
        }

        if (workReportLines.isEmpty()) {
            return true;
        }

        LocalDate workReportDate = LocalDate.fromDateFields(workReportLines.iterator().next().getDate());

        PersonalTimesheetsPeriodicityEnum periodicity = Registry
                .getConfigurationDAO()
                .getConfigurationWithReadOnlyTransaction()
                .getPersonalTimesheetsPeriodicity();

        LocalDate min = periodicity.getStart(workReportDate);
        LocalDate max = periodicity.getEnd(workReportDate);

        for (WorkReportLine line : workReportLines) {
            LocalDate date = LocalDate.fromDateFields(line.getDate());
            if ((date.compareTo(min) < 0) || (date.compareTo(max) > 0)) {
                return false;
            }
        }
        return true;
    }

    @AssertTrue(message = "resource has to be bound to a user in personal timesheets")
    public boolean isResourceIsBoundInPersonalTimesheetConstraint() {
        if (!getWorkReportType().isPersonalTimesheetsType()) {
            return true;
        }

        if (resource != null) {
            try {
                Worker worker = Registry.getWorkerDAO().find(resource.getId());
                return worker.getUser() != null;
            } catch (InstanceNotFoundException e) {
                // Do nothing
            }
        }

        return false;
    }

    @AssertTrue(message = "the same task is marked as finished by more than one timesheet line")
    public boolean isSameOrderElementFinishedBySeveralWorkReportLinesConstraint() {
        Set<OrderElement> finishedOrderElements = new HashSet<>();

        for (WorkReportLine line : workReportLines) {
            if (line.isFinished()) {
                if (Util.contains(finishedOrderElements, line.getOrderElement())) {
                    return false;
                }
                finishedOrderElements.add(line.getOrderElement());
            }
        }

        return true;
    }

    public boolean isFinished(OrderElement orderElement) {
        for (WorkReportLine line : workReportLines) {
            if (line.isFinished() && Util.equals(line.getOrderElement(), orderElement)) {
                return true;
            }
        }
        return false;
    }

}

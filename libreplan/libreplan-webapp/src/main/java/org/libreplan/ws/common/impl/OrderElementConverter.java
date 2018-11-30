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

package org.libreplan.ws.common.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.Validate;
import org.joda.time.LocalDate;
import org.libreplan.business.advance.entities.AdvanceMeasurement;
import org.libreplan.business.advance.entities.DirectAdvanceAssignment;
import org.libreplan.business.advance.exceptions.DuplicateAdvanceAssignmentForOrderElementException;
import org.libreplan.business.advance.exceptions.DuplicateValueTrueReportGlobalAdvanceException;
import org.libreplan.business.calendars.entities.BaseCalendar;
import org.libreplan.business.common.Registry;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.externalcompanies.entities.EndDateCommunication;
import org.libreplan.business.labels.entities.Label;
import org.libreplan.business.materials.bootstrap.PredefinedMaterialCategories;
import org.libreplan.business.materials.entities.Material;
import org.libreplan.business.materials.entities.MaterialAssignment;
import org.libreplan.business.materials.entities.MaterialCategory;
import org.libreplan.business.orders.daos.IHoursGroupDAO;
import org.libreplan.business.orders.entities.HoursGroup;
import org.libreplan.business.orders.entities.ICriterionRequirable;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.orders.entities.OrderLine;
import org.libreplan.business.orders.entities.OrderLineGroup;
import org.libreplan.business.requirements.entities.CriterionRequirement;
import org.libreplan.business.requirements.entities.DirectCriterionRequirement;
import org.libreplan.business.requirements.entities.IndirectCriterionRequirement;
import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.resources.entities.ResourceEnum;
import org.libreplan.business.scenarios.entities.OrderVersion;
import org.libreplan.business.scenarios.entities.Scenario;
import org.libreplan.ws.common.api.AdvanceMeasurementDTO;
import org.libreplan.ws.common.api.CriterionRequirementDTO;
import org.libreplan.ws.common.api.DirectCriterionRequirementDTO;
import org.libreplan.ws.common.api.HoursGroupDTO;
import org.libreplan.ws.common.api.IndirectCriterionRequirementDTO;
import org.libreplan.ws.common.api.LabelReferenceDTO;
import org.libreplan.ws.common.api.MaterialAssignmentDTO;
import org.libreplan.ws.common.api.OrderDTO;
import org.libreplan.ws.common.api.OrderElementDTO;
import org.libreplan.ws.common.api.OrderLineDTO;
import org.libreplan.ws.common.api.OrderLineGroupDTO;
import org.libreplan.ws.common.api.ResourceEnumDTO;
import org.libreplan.ws.subcontract.api.EndDateCommunicationToCustomerDTO;

/**
 * Converter from/to {@link OrderElement} entities to/from DTOs.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public final class OrderElementConverter {

    private OrderElementConverter() {
    }

    public static final OrderElementDTO toDTO(OrderElement orderElement, ConfigurationOrderElementConverter configuration) {
        String name = orderElement.getName();
        String code = orderElement.getCode();
        XMLGregorianCalendar initDate = DateConverter.toXMLGregorianCalendar(orderElement.getInitDate());
        XMLGregorianCalendar deadline = DateConverter.toXMLGregorianCalendar(orderElement.getDeadline());
        String description = orderElement.getDescription();

        Set<LabelReferenceDTO> labels = new HashSet<>();
        if (configuration.isLabels()) {
            for (Label label : orderElement.getLabels()) {
                labels.add(LabelReferenceConverter.toDTO(label));
            }
        }

        Set<MaterialAssignmentDTO> materialAssignments = new HashSet<>();
        if (configuration.isMaterialAssignments()) {
            for (MaterialAssignment materialAssignment : orderElement.getMaterialAssignments()) {
                materialAssignments.add(toDTO(materialAssignment));
            }
        }

        Set<AdvanceMeasurementDTO> advanceMeasurements = new HashSet<>();
        if (configuration.isAdvanceMeasurements()) {
            advanceMeasurements = toDTO(orderElement.getReportGlobalAdvanceAssignment());
        }

        Set<CriterionRequirementDTO> criterionRequirements = new HashSet<>();
        if (configuration.isCriterionRequirements()) {
            for (CriterionRequirement criterionRequirement : orderElement.getCriterionRequirements()) {
                criterionRequirements.add(toDTO(criterionRequirement));
            }
        }

        if (orderElement instanceof OrderLine) {
            Set<HoursGroupDTO> hoursGroups = new HashSet<>();
            if (configuration.isHoursGroups()) {
                for (HoursGroup hoursGroup : orderElement.getHoursGroups()) {
                    hoursGroups.add(toDTO(hoursGroup, configuration));
                }
            }

            return new OrderLineDTO(
                    name, code, initDate,
                    deadline, description, labels,
                    materialAssignments, advanceMeasurements, criterionRequirements,
                    hoursGroups);

        } else { // orderElement instanceof OrderLineGroup
            List<OrderElementDTO> children = new ArrayList<>();
            for (OrderElement element : orderElement.getChildren()) {
                children.add(toDTO(element, configuration));
            }

            if (orderElement instanceof Order) {

                Boolean dependenciesConstraintsHavePriority =
                        ((Order) orderElement).getDependenciesConstraintsHavePriority();

                BaseCalendar calendar = ((Order) orderElement).getCalendar();
                String calendarName = null;

                if (calendar != null) {
                    calendarName = calendar.getName();
                }

                return new OrderDTO(
                        name, code, initDate, deadline,
                        description, labels, materialAssignments, advanceMeasurements,
                        criterionRequirements, children, dependenciesConstraintsHavePriority, calendarName);

            } else { // orderElement instanceof OrderLineGroup
                return new OrderLineGroupDTO(
                        name, code,
                        initDate, deadline,
                        description, labels,
                        materialAssignments, advanceMeasurements,
                        criterionRequirements, children);
            }
        }
    }

    public static CriterionRequirementDTO toDTO(CriterionRequirement criterionRequirement) {
        String name = criterionRequirement.getCriterion().getName();
        String type = criterionRequirement.getCriterion().getType().getName();

        if (criterionRequirement instanceof IndirectCriterionRequirement) {
            boolean isValid = criterionRequirement.isValid();
            return new IndirectCriterionRequirementDTO(name, type, isValid);
        } else { // criterionRequirement instanceof DirectCriterionRequirement
            return new DirectCriterionRequirementDTO(name, type);
        }
    }

    public static final Set<AdvanceMeasurementDTO> toDTO(DirectAdvanceAssignment advanceAssignment) {
        Set<AdvanceMeasurementDTO> advanceMeasurements = new HashSet<>();

        if (advanceAssignment != null) {
            BigDecimal maxValue = advanceAssignment.getMaxValue();
            for (AdvanceMeasurement advanceMeasurement : advanceAssignment.getAdvanceMeasurements()) {

                advanceMeasurements.add(
                        toDTO(maxValue, advanceAssignment.getAdvanceType().getPercentage(), advanceMeasurement));
            }
        }
        return advanceMeasurements;
    }

    public static final AdvanceMeasurementDTO toDTO(
            BigDecimal maxValue, boolean isPercentage, AdvanceMeasurement advanceMeasurement) {

        BigDecimal value;
        if (isPercentage) {
            value = advanceMeasurement.getValue();
        } else {
            value = advanceMeasurement.getValue().divide(maxValue,
                    RoundingMode.DOWN);
        }

        return new AdvanceMeasurementDTO(DateConverter.toXMLGregorianCalendar(advanceMeasurement.getDate()), value);
    }

    public final static MaterialAssignmentDTO toDTO(MaterialAssignment materialAssignment) {

        XMLGregorianCalendar estimatedAvailability =
                DateConverter.toXMLGregorianCalendar(materialAssignment.getEstimatedAvailability());

        return new MaterialAssignmentDTO(
                materialAssignment.getMaterial().getCode(),
                materialAssignment.getUnits(),
                materialAssignment.getUnitPrice(),
                estimatedAvailability);
    }

    public final static HoursGroupDTO toDTO(HoursGroup hoursGroup, ConfigurationOrderElementConverter configuration) {
        ResourceEnumDTO resourceType = ResourceEnumConverter.toDTO(hoursGroup.getResourceType());

        Set<CriterionRequirementDTO> criterionRequirements = new HashSet<>();
        if (configuration.isCriterionRequirements()) {
            for (CriterionRequirement criterionRequirement : hoursGroup.getCriterionRequirements()) {
                criterionRequirements.add(toDTO(criterionRequirement));
            }
        }

        return new HoursGroupDTO(hoursGroup.getCode(), resourceType, hoursGroup.getWorkingHours(), criterionRequirements);
    }

    public final static OrderElement toEntity(OrderElementDTO orderElementDTO, ConfigurationOrderElementConverter configuration)
            throws ValidationException {

        return toEntity(null, orderElementDTO, configuration);
    }

    public static final OrderElement toEntity(
            OrderVersion orderVersion, OrderElementDTO orderElementDTO, ConfigurationOrderElementConverter configuration) {

        OrderVersion newOrderVersion = orderVersion;

        if (orderVersion == null) {
            Scenario current = Registry.getScenarioManager().getCurrent();
            newOrderVersion = OrderVersion.createInitialVersion(current);
        }
        OrderElement orderElement = toEntityExceptCriterionRequirements(newOrderVersion, orderElementDTO, configuration);

        // FIXME Review why this validation is needed here, it breaks the subcontract service.
        // This was introduced in commit 341145a5
        // Validate OrderElement.code and HoursGroup.code must be unique
        // Order.checkConstraintOrderUniqueCode(orderElement);
        // HoursGroup.checkConstraintHoursGroupUniqueCode(orderElement);

        if (configuration.isCriterionRequirements()) {
            addOrCriterionRequirements(orderElement, orderElementDTO);
        }

        return orderElement;
    }

    private static void checkOrderElementDTOCode(OrderElementDTO orderElementDTO, String instance) {
        if (orderElementDTO.code == null) {
            throw new ValidationException(MessageFormat.format("{0}: code not found", instance));
        }
    }

    private static void addOrCriterionRequirements(OrderElement orderElement, OrderElementDTO orderElementDTO) {
        addOrCriterionRequirementsEntities(orderElement, orderElementDTO.criterionRequirements);

        if (orderElement != null) {
        if (orderElementDTO instanceof OrderLineDTO) {
            for (HoursGroupDTO hoursGroupDTO : ((OrderLineDTO) orderElementDTO).hoursGroups) {
                HoursGroup hoursGroup = ((OrderLine) orderElement).getHoursGroup(hoursGroupDTO.code);
                if (hoursGroup != null) {
                    addOrCriterionRequirementsEntities(hoursGroup, hoursGroupDTO.criterionRequirements);
                }
            }
        } else { // orderElementDTO instanceof OrderLineGroupDTO
            for (OrderElementDTO childDTO : ((OrderLineGroupDTO) orderElementDTO).children) {
                OrderElement child = orderElement.getOrderElement(childDTO.code);
                addOrCriterionRequirements(child, childDTO);
            }
        }
        }
    }

    private static void addOrCriterionRequirementsEntities(
            ICriterionRequirable criterionRequirable, Set<CriterionRequirementDTO> criterionRequirements) {

        for (CriterionRequirementDTO criterionRequirementDTO : criterionRequirements) {
            Criterion criterion = getCriterion(criterionRequirementDTO.name, criterionRequirementDTO.type);
            if (criterion != null) {
                if (criterionRequirementDTO instanceof DirectCriterionRequirementDTO) {

                    DirectCriterionRequirement directCriterionRequirement =
                            getDirectCriterionRequirementByCriterion(criterionRequirable, criterion);

                    if (directCriterionRequirement == null) {
                        try {
                            criterionRequirable.addCriterionRequirement(DirectCriterionRequirement.create(criterion));
                        } catch (IllegalStateException e) {
                            throw new ValidationException(e.getMessage());
                        }
                    }
                } else { // criterionRequirementDTO instanceof IndirectCriterionRequirementDTO
                    IndirectCriterionRequirement indirectCriterionRequirement =
                            getIndirectCriterionRequirementByCriterion(criterionRequirable, criterion);

                    if (indirectCriterionRequirement != null) {
                        indirectCriterionRequirement.setValid(((IndirectCriterionRequirementDTO) criterionRequirementDTO).valid);
                    }
                }
            } else {
                if (criterionRequirementDTO.name == null || criterionRequirementDTO.type == null) {
                    throw new ValidationException("the criterion format is incorrect");
                } else {
                    throw new ValidationException("the criterion " +
                            criterionRequirementDTO.name + " which type is " +
                            criterionRequirementDTO.type + " not found");
                }
            }
        }
    }

    private static DirectCriterionRequirement getDirectCriterionRequirementByCriterion(
            ICriterionRequirable criterionRequirable, Criterion criterion) {

        for (CriterionRequirement criterionRequirement : criterionRequirable.getCriterionRequirements()) {
            if (criterionRequirement instanceof DirectCriterionRequirement) {
                if (criterionRequirement.getCriterion().isEquivalent(criterion)) {
                    return (DirectCriterionRequirement) criterionRequirement;
                }
            }
        }
        return null;
    }

    private static IndirectCriterionRequirement getIndirectCriterionRequirementByCriterion(
            ICriterionRequirable criterionRequirable, Criterion criterion) {

        for (CriterionRequirement criterionRequirement : criterionRequirable.getCriterionRequirements()) {
            if (criterionRequirement instanceof IndirectCriterionRequirement) {
                if (criterionRequirement.getCriterion().isEquivalent(criterion)) {
                    return (IndirectCriterionRequirement) criterionRequirement;
                }
            }
        }
        return null;
    }

    private static final OrderElement toEntityExceptCriterionRequirements(
            OrderVersion parentOrderVersion,
            OrderElementDTO orderElementDTO,
            ConfigurationOrderElementConverter configuration) {

        Validate.notNull(parentOrderVersion);
        OrderElement orderElement;

        if (orderElementDTO instanceof OrderLineDTO) {
            checkOrderElementDTOCode(orderElementDTO, "OrderLineDTO");

            if ((configuration.isHoursGroups()) && (!((OrderLineDTO) orderElementDTO).hoursGroups.isEmpty())) {

                orderElement = OrderLine.createUnvalidated(orderElementDTO.code);
                for (HoursGroupDTO hoursGroupDTO : ((OrderLineDTO) orderElementDTO).hoursGroups) {
                    HoursGroup hoursGroup = toEntity(hoursGroupDTO);
                    ((OrderLine) orderElement).addHoursGroup(hoursGroup);
                }
            } else {
                orderElement = OrderLine.createUnvalidatedWithUnfixedPercentage(orderElementDTO.code, 0);
                if (!orderElement.getHoursGroups().isEmpty()) {
                    orderElement.getHoursGroups().get(0).setCode(UUID.randomUUID().toString());
                }
            }
        } else { // orderElementDTO instanceof OrderLineGroupDTO

            if (orderElementDTO instanceof OrderDTO) {
                checkOrderElementDTOCode(orderElementDTO, "OrderDTO");
                orderElement = Order.createUnvalidated(orderElementDTO.code);
                Scenario current = Registry.getScenarioManager().getCurrent();
                ((Order) orderElement).setVersionForScenario(current, parentOrderVersion);

                ((Order) orderElement).setDependenciesConstraintsHavePriority(
                        ((OrderDTO) orderElementDTO).dependenciesConstraintsHavePriority);

                List<BaseCalendar> calendars =
                        Registry.getBaseCalendarDAO().findByName(((OrderDTO) orderElementDTO).calendarName);

                BaseCalendar calendar;
                if ((calendars != null) && (calendars.size() == 1)) {
                    calendar = calendars.get(0);
                } else {
                    calendar = Registry.getConfigurationDAO().getConfiguration().getDefaultCalendar();
                }
                ((Order) orderElement).setCalendar(calendar);

            } else { // orderElementDTO instanceof OrderLineGroupDTO
                checkOrderElementDTOCode(orderElementDTO, "OrderLineGroupDTO");
                orderElement = OrderLineGroup.createUnvalidated(orderElementDTO.code);
            }
            orderElement.useSchedulingDataFor(parentOrderVersion);
            List<OrderElement> children = new ArrayList<>();
            for (OrderElementDTO element : ((OrderLineGroupDTO) orderElementDTO).children) {
                children.add(toEntity(parentOrderVersion, element, configuration));
            }

            for (OrderElement child : children) {
                ((OrderLineGroup) orderElement).add(child);
            }
        }

        orderElement.setName(orderElementDTO.name);
        orderElement.setCode(orderElementDTO.code);
        orderElement.setInitDate(DateConverter.toDate(orderElementDTO.initDate));
        orderElement.setDeadline(DateConverter.toDate(orderElementDTO.deadline));
        orderElement.setDescription(orderElementDTO.description);

        if (configuration.isLabels()) {
            for (LabelReferenceDTO labelDTO : orderElementDTO.labels) {
                try {
                orderElement.addLabel(Registry.getLabelDAO().findByCode(labelDTO.code));
                } catch (InstanceNotFoundException e) {
                    throw new ValidationException("Label " + labelDTO.code + " not found.");
                }
            }
        }

        if (configuration.isMaterialAssignments()) {
            for (MaterialAssignmentDTO materialAssignmentDTO : orderElementDTO.materialAssignments) {
                orderElement.addMaterialAssignment(toEntity(materialAssignmentDTO));
            }
        }

        if (configuration.isAdvanceMeasurements()) {
            addAdvanceMeasurements(orderElement, orderElementDTO);
        }

        return orderElement;
    }

    private static Criterion getCriterion(String name, String type) {
        List<Criterion> criterions = Registry.getCriterionDAO().findByNameAndType(name, type);
        return criterions.size() != 1 ? null : criterions.get(0);
    }

    public static DirectCriterionRequirement toEntity(DirectCriterionRequirementDTO criterionRequirementDTO) {
        Criterion criterion = getCriterion(criterionRequirementDTO.name, criterionRequirementDTO.type);
        return criterion == null ? null : DirectCriterionRequirement.create(criterion);
    }

    public static final MaterialAssignment toEntity(MaterialAssignmentDTO materialAssignmentDTO) {
        Material material;

        try {
            material = Registry.getMaterialDAO().findUniqueByCodeInAnotherTransaction(materialAssignmentDTO.materialCode);
        } catch (InstanceNotFoundException e) {
            material = Material.create(materialAssignmentDTO.materialCode);
            material.setDescription("material-" + materialAssignmentDTO.materialCode);

            MaterialCategory defaultMaterialCategory =
                    PredefinedMaterialCategories.IMPORTED_MATERIALS_WITHOUT_CATEGORY.getMaterialCategory();

            material.setCategory(defaultMaterialCategory);

            /* "validate" method avoids that "material" goes to the Hibernate's session if "material" is not valid */
            material.validate();
            Registry.getMaterialDAO().save(material);
            material.dontPoseAsTransientObjectAnymore();
        }

        MaterialAssignment materialAssignment = MaterialAssignment.create(material);
        materialAssignment.setUnitsWithoutNullCheck(materialAssignmentDTO.units);
        materialAssignment.setUnitPriceWithoutNullCheck(materialAssignmentDTO.unitPrice);

        Date estimatedAvailability = DateConverter.toDate(materialAssignmentDTO.estimatedAvailability);
        materialAssignment.setEstimatedAvailability(estimatedAvailability);

        return materialAssignment;
    }

    public static final HoursGroup toEntity(HoursGroupDTO hoursGroupDTO) {
        ResourceEnum resourceType = ResourceEnumConverter.fromDTO(hoursGroupDTO.resourceType);
        return HoursGroup.createUnvalidated(hoursGroupDTO.code, resourceType, hoursGroupDTO.workingHours);
    }

    public static final void update(
            OrderElement orderElement, OrderElementDTO orderElementDTO, ConfigurationOrderElementConverter configuration) {

        update(null, orderElement, orderElementDTO, configuration);
    }

    private static final void update(
            OrderVersion orderVersion, OrderElement orderElement, OrderElementDTO orderElementDTO,
            ConfigurationOrderElementConverter configuration) {

        updateExceptCriterionRequirements(orderVersion, orderElement, orderElementDTO, configuration);
        if (configuration.isCriterionRequirements()) {
            addOrCriterionRequirements(orderElement, orderElementDTO);
        }
    }

    private static final void updateExceptCriterionRequirements(
            OrderVersion orderVersion,
            OrderElement orderElement,
            OrderElementDTO orderElementDTO,
            ConfigurationOrderElementConverter configuration) {

        OrderVersion newOrderVersion = orderVersion;

        if (orderElementDTO instanceof OrderLineDTO) {
            if (!(orderElement instanceof OrderLine)) {
                throw new ValidationException(MessageFormat.format(
                        "Task {0}: Task group is incompatible type with {1}",
                        orderElement.getCode(), orderElement.getClass().getName()));
            }

            if (configuration.isHoursGroups()) {
                for (HoursGroupDTO hoursGroupDTO : ((OrderLineDTO) orderElementDTO).hoursGroups) {
                    if ( ((OrderLine) orderElement).containsHoursGroup(hoursGroupDTO.code) ) {
                        update( ((OrderLine) orderElement).getHoursGroup(hoursGroupDTO.code), hoursGroupDTO);
                    } else {
                        ((OrderLine) orderElement).addHoursGroup(toEntity(hoursGroupDTO));
                    }
                }
            }
        } else { // orderElementDTO instanceof OrderLineGroupDTO
            if (orderElementDTO instanceof OrderDTO) {
                if (!(orderElement instanceof Order)) {
                    throw new ValidationException(MessageFormat.format(
                            "Task {0}: Project is incompatible type with {1}",
                            orderElement.getCode(), orderElement.getClass().getName()));

                }
                Order order = (Order) orderElement;
                newOrderVersion = order.getOrderVersionFor(Registry.getScenarioManager().getCurrent());
                order.useSchedulingDataFor(newOrderVersion);
                Boolean dependenciesConstraintsHavePriority = ((OrderDTO) orderElementDTO).dependenciesConstraintsHavePriority;

                if (dependenciesConstraintsHavePriority != null) {
                    ((Order) orderElement).setDependenciesConstraintsHavePriority(dependenciesConstraintsHavePriority);
                }

                String calendarName = ((OrderDTO) orderElementDTO).calendarName;
                if (calendarName != null && !((Order) orderElement).getCalendar().getName().equals(calendarName)) {

                    List<BaseCalendar> calendars =
                            Registry.getBaseCalendarDAO().findByName(((OrderDTO) orderElementDTO).calendarName);

                    if (calendars.size() == 1) {
                        ((Order) orderElement).setCalendar(calendars.get(0));
                    }
                }
            } else { // orderElementDTO instanceof OrderLineGroupDTO
                if (!(orderElement instanceof OrderLineGroup)) {

                    throw new ValidationException(MessageFormat.format(
                            "Task {0}: Task group is incompatible type with {1}",
                            orderElement.getCode(), orderElement.getClass().getName()));
                }
            }

            for (OrderElementDTO childDTO : ((OrderLineGroupDTO) orderElementDTO).children) {
                if (orderElement.containsOrderElement(childDTO.code)) {
                    update(newOrderVersion, orderElement.getOrderElement(childDTO.code), childDTO, configuration);
                } else {
                    if (checkConstraintUniqueOrderCode(childDTO)) {
                        throw new ValidationException(MessageFormat.format(
                                "Task {0}: Duplicate code in DB", childDTO.code));
                    }

                    if (checkConstraintUniqueHoursGroupCode(childDTO)) {
                        throw new ValidationException(MessageFormat.format(
                                "Hours Group {0}: Duplicate code in DB", childDTO.code));
                    }
                    ((OrderLineGroup) orderElement).add(toEntity(newOrderVersion, childDTO, configuration));
                }
            }

        }

        if (configuration.isLabels()) {
            for (LabelReferenceDTO labelDTO : orderElementDTO.labels) {
                if (!orderElement.containsLabel(labelDTO.code)) {
                    try {
                        orderElement.addLabel(Registry.getLabelDAO().findByCode(labelDTO.code));
                    } catch (InstanceNotFoundException e) {
                        throw new ValidationException("Label " + labelDTO.code + " not found");
                    } catch (IllegalArgumentException e) {
                        throw new ValidationException(e.getMessage());
                    }
                }
            }
        }

        if (configuration.isMaterialAssignments()) {
            for (MaterialAssignmentDTO materialAssignmentDTO : orderElementDTO.materialAssignments) {
                if (orderElement.containsMaterialAssignment(materialAssignmentDTO.materialCode)) {
                    update(orderElement.getMaterialAssignment(materialAssignmentDTO.materialCode), materialAssignmentDTO);
                } else {
                    orderElement.addMaterialAssignment(toEntity(materialAssignmentDTO));
                }
            }
        }

        if (configuration.isAdvanceMeasurements()) {
            addAdvanceMeasurements(orderElement, orderElementDTO);
        }

        if (orderElementDTO.name != null) {
            orderElement.setName(orderElementDTO.name);
        }

        if (orderElementDTO.initDate != null) {
            orderElement.setInitDate(DateConverter.toDate(orderElementDTO.initDate));
        }

        if (orderElementDTO.deadline != null) {
            orderElement.setDeadline(DateConverter.toDate(orderElementDTO.deadline));
        }

        if (orderElementDTO.description != null) {
            orderElement.setDescription(orderElementDTO.description);
        }

    }

    /**
     * Returns true is there's another {@link OrderElement} in DB with the same code.
     *
     * @param orderElement
     * @return boolean
     */
    private static boolean checkConstraintUniqueOrderCode(OrderElementDTO orderElement) {
        try {
            OrderElement existsByCode = Registry.getOrderElementDAO().findByCode(orderElement.code);
            return existsByCode != null;
        } catch (InstanceNotFoundException e) {
            return false;
        }
    }

    /**
     * Returns true if there's another {@link HoursGroup} in DB with the same code.
     *
     * @param orderElement
     * @return boolean
     */
    private static boolean checkConstraintUniqueHoursGroupCode(OrderElementDTO orderElement) {
        return orderElement instanceof OrderLineDTO && checkConstraintUniqueHoursGroupCode((OrderLineDTO) orderElement);
    }

    private static boolean checkConstraintUniqueHoursGroupCode(OrderLineDTO orderLine) {
        try {
            IHoursGroupDAO hoursGroupDAO = Registry.getHoursGroupDAO();
            Set<HoursGroupDTO> hoursGroups = orderLine.hoursGroups;
            for (HoursGroupDTO each: hoursGroups) {
                HoursGroup hoursGroup = hoursGroupDAO.findByCodeAnotherTransaction(each.code);
                if (hoursGroup != null) {
                    return true;
                }
            }
        } catch (InstanceNotFoundException ignored) {
            // Do nothing
        }
        return false;
    }

    public final static void update(HoursGroup hoursGroup, HoursGroupDTO hoursGroupDTO) {
        if (!hoursGroup.getCode().equals(hoursGroupDTO.code)) {
            throw new ValidationException("Not the same hours group, impossible to update");
        }

        if (hoursGroupDTO.workingHours != null) {
            hoursGroup.setWorkingHours(hoursGroupDTO.workingHours);
        }

        if (hoursGroupDTO.resourceType != null) {
            hoursGroup.setResourceType(ResourceEnumConverter.fromDTO(hoursGroupDTO.resourceType));
        }
    }

    public static final void update(MaterialAssignment materialAssignment, MaterialAssignmentDTO materialAssignmentDTO) {
        if (!materialAssignment.getMaterial().getCode().equals(materialAssignmentDTO.materialCode)) {
            throw new ValidationException("Not the same material, impossible to update");
        }

        if (materialAssignmentDTO.units != null) {
            materialAssignment.setUnits(materialAssignmentDTO.units);
        }

        if (materialAssignmentDTO.unitPrice != null) {
            materialAssignment.setUnitPrice(materialAssignmentDTO.unitPrice);
        }

        if (materialAssignmentDTO.estimatedAvailability != null) {
            Date estimatedAvailability = DateConverter.toDate(materialAssignmentDTO.estimatedAvailability);
            materialAssignment.setEstimatedAvailability(estimatedAvailability);
        }
    }

    private static void addAdvanceMeasurements(OrderElement orderElement, OrderElementDTO orderElementDTO) {
        if (!orderElementDTO.advanceMeasurements.isEmpty()) {
            DirectAdvanceAssignment directAdvanceAssignment = getDirectAdvanceAssignmentSubcontractor(orderElement);

            for (AdvanceMeasurementDTO advanceMeasurementDTO : orderElementDTO.advanceMeasurements) {
                AdvanceMeasurement advanceMeasurement = null;
                LocalDate date = null;
                if (advanceMeasurementDTO.date != null) {
                    date = new LocalDate(DateConverter.toLocalDate(advanceMeasurementDTO.date));
                    advanceMeasurement = directAdvanceAssignment.getAdvanceMeasurementAtExactDate(date);
                }

                if (advanceMeasurement == null) {
                    advanceMeasurement = AdvanceMeasurement.create(date, advanceMeasurementDTO.value);
                    directAdvanceAssignment.addAdvanceMeasurements(advanceMeasurement);
                } else {
                    advanceMeasurement.setValue(advanceMeasurementDTO.value);
                }
            }
        }
    }

    private static DirectAdvanceAssignment getDirectAdvanceAssignmentSubcontractor(OrderElement orderElement) {
        DirectAdvanceAssignment directAdvanceAssignment = orderElement.getDirectAdvanceAssignmentSubcontractor();
        if (directAdvanceAssignment == null) {
            try {
                directAdvanceAssignment = orderElement.addSubcontractorAdvanceAssignment();
            } catch (DuplicateValueTrueReportGlobalAdvanceException e) {

                throw new ValidationException(MessageFormat.format(
                        "More than one progress marked as report global for task {0}", orderElement.getCode()));

            } catch (DuplicateAdvanceAssignmentForOrderElementException e) {
                throw new ValidationException(MessageFormat.format(
                        "Duplicate progress assignment for task {0}", orderElement.getCode()));
            }
        }
        return directAdvanceAssignment;
    }

    public static AdvanceMeasurement toEntity(AdvanceMeasurementDTO advanceMeasurementDTO) {
        return AdvanceMeasurement.create(DateConverter.toLocalDate(advanceMeasurementDTO.date), advanceMeasurementDTO.value);
    }

    public static AdvanceMeasurementDTO toDTO(AdvanceMeasurement advanceMeasurement) {
        return new AdvanceMeasurementDTO(
                DateConverter.toXMLGregorianCalendar(advanceMeasurement.getDate()),
                advanceMeasurement.getValue());
    }

    public static EndDateCommunication toEntity(EndDateCommunicationToCustomerDTO endDateCommunicationToCustomerDTO) {
        Date endDate = DateConverter.toDate(endDateCommunicationToCustomerDTO.endDate);
        Date communicationDate = DateConverter.toDate(endDateCommunicationToCustomerDTO.communicationDate);
        Date saveDate = DateConverter.toDate(endDateCommunicationToCustomerDTO.saveDate);

        return EndDateCommunication.create(saveDate, endDate, communicationDate);
    }

    public static EndDateCommunicationToCustomerDTO toDTO(EndDateCommunication endDateCommunicationToCustomer) {
        XMLGregorianCalendar endDate = DateConverter.toXMLGregorianCalendar(endDateCommunicationToCustomer.getEndDate());
        XMLGregorianCalendar saveDate = DateConverter.toXMLGregorianCalendar(endDateCommunicationToCustomer.getSaveDate());

        XMLGregorianCalendar communicationDate =
                DateConverter.toXMLGregorianCalendar(endDateCommunicationToCustomer.getCommunicationDate());

        return new EndDateCommunicationToCustomerDTO(saveDate, endDate, communicationDate);
    }

}

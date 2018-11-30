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

package org.libreplan.ws.resources.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.libreplan.business.calendars.entities.CalendarAvailability;
import org.libreplan.business.calendars.entities.ResourceCalendar;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.common.exceptions.MultipleInstancesException;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.costcategories.entities.ResourcesCostCategoryAssignment;
import org.libreplan.business.resources.entities.CriterionSatisfaction;
import org.libreplan.business.resources.entities.CriterionType;
import org.libreplan.business.resources.entities.Machine;
import org.libreplan.business.resources.entities.Resource;
import org.libreplan.business.resources.entities.Worker;
import org.libreplan.ws.calendars.api.BaseCalendarDTO;
import org.libreplan.ws.calendars.impl.CalendarConverter;
import org.libreplan.ws.common.impl.DateConverter;
import org.libreplan.ws.common.impl.InstanceNotFoundRecoverableErrorException;
import org.libreplan.ws.costcategories.api.CostCategoryDTO;
import org.libreplan.ws.resources.api.CalendarAvailabilityDTO;
import org.libreplan.ws.resources.api.CriterionSatisfactionDTO;
import org.libreplan.ws.resources.api.MachineDTO;
import org.libreplan.ws.resources.api.ResourceCalendarDTO;
import org.libreplan.ws.resources.api.ResourceDTO;
import org.libreplan.ws.resources.api.ResourcesCostCategoryAssignmentDTO;
import org.libreplan.ws.resources.api.WorkerDTO;
import org.libreplan.ws.resources.criterion.api.CriterionDTO;
import org.libreplan.ws.resources.criterion.api.CriterionTypeDTO;

/**
 * Converter from/to resource-related entities to/from DTOs.
 *
 * @author Fernando Bellas Permuy <fbellas@udc.es>
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
public class ResourceConverter {

    private ResourceConverter() {
    }

    public static final Resource toEntity(ResourceDTO resourceDTO) {

        checkResourceDTOType(resourceDTO);

        Resource resource;

        if (resourceDTO instanceof MachineDTO) {
            resource = createResourceWithBasicData((MachineDTO) resourceDTO);
        } else {
            resource = createResourceWithBasicData((WorkerDTO) resourceDTO);
        }

        addCriterionSatisfactions(resource, resourceDTO.criterionSatisfactions);
        setResourceCalendar(resource, resourceDTO.calendar);
        addResourcesCostCategoryAssignments(resource, resourceDTO.resourcesCostCategoryAssignments);

        return resource;
    }

    public static final void updateResource(Resource resource, ResourceDTO resourceDTO) {

        checkResourceDTOType(resourceDTO);

        updateBasicData(resource, resourceDTO);

        updateResourceCalendar(resource, resourceDTO.calendar);

        updateCriterionSatisfactions(resource, resourceDTO.criterionSatisfactions);

        updateResourcesCostCategoryAssignments(resource, resourceDTO.resourcesCostCategoryAssignments);

    }

    private static final Machine createResourceWithBasicData(MachineDTO machineDTO) {

        return Machine.createUnvalidated(
                StringUtils.trim(machineDTO.code),
                StringUtils.trim(machineDTO.name),
                StringUtils.trim(machineDTO.description));

    }

    private static final Worker createResourceWithBasicData(WorkerDTO workerDTO) {

        return Worker.createUnvalidated(
            StringUtils.trim(workerDTO.code),
            StringUtils.trim(workerDTO.firstName),
            StringUtils.trim(workerDTO.surname),
            StringUtils.trim(workerDTO.nif));

    }

    private static void addCriterionSatisfactions(Resource resource, List<CriterionSatisfactionDTO> criterionSatisfactions) {

        for (CriterionSatisfactionDTO criterionSatisfactionDTO : criterionSatisfactions) {

            CriterionSatisfaction criterionSatisfaction = toEntity(criterionSatisfactionDTO, resource);

            resource.addUnvalidatedSatisfaction(criterionSatisfaction);
        }
    }

    private static CriterionSatisfaction toEntity(CriterionSatisfactionDTO criterionSatisfactionDTO, Resource resource) {

        if (StringUtils.isBlank(criterionSatisfactionDTO.criterionTypeName)) {
            throw new ValidationException("criterion type name not specified");
        }

        if (StringUtils.isBlank(criterionSatisfactionDTO.criterionName)) {
            throw new ValidationException("criterion name not specified");
        }

        try {

            return CriterionSatisfaction.createUnvalidated(
                StringUtils.trim(criterionSatisfactionDTO.code),
                StringUtils.trim(criterionSatisfactionDTO.criterionTypeName),
                StringUtils.trim(criterionSatisfactionDTO.criterionName),
                resource,
                DateConverter.toLocalDate(criterionSatisfactionDTO.startDate),
                DateConverter.toLocalDate(criterionSatisfactionDTO.endDate));

        } catch (InstanceNotFoundException e) {

            if (e.getClassName().equals(CriterionType.class.getName())) {
                throw new InstanceNotFoundRecoverableErrorException(CriterionTypeDTO.ENTITY_TYPE, e.getKey().toString());
            } else {
                throw new InstanceNotFoundRecoverableErrorException(CriterionDTO.ENTITY_TYPE, e.getKey().toString());
            }

        }

    }

    private static void setResourceCalendar(Resource resource, ResourceCalendarDTO calendar) {
        String calendarCode = null;
        if (calendar != null) {
            calendarCode = calendar.parent;
        }

        try {
            resource.setResourceCalendar(StringUtils.trim(calendarCode));

            // Copy the data of the resource calendar DTO
            updateBasicPropertiesResourceCalendar(calendar, resource.getCalendar());

        } catch (InstanceNotFoundException e) {
                throw new InstanceNotFoundRecoverableErrorException(ResourceCalendarDTO.ENTITY_TYPE, e.getKey().toString());
        } catch (MultipleInstancesException e) {
            throw new ValidationException(MessageFormat.format(
                    "there exist multiple resource calendars with name {0}", calendarCode));
        }
    }

    private static void updateBasicPropertiesResourceCalendar(ResourceCalendarDTO calendarDTO, ResourceCalendar calendar) {
        if (calendarDTO != null) {

            if (!StringUtils.isBlank(calendarDTO.name)) {
                calendar.setName(calendarDTO.name);
            }

            if (!StringUtils.isBlank(calendarDTO.code)) {
                calendar.setCode(calendarDTO.code);
            } else {
                throw new ValidationException("missing code in the resource calendar");
            }

            if (calendarDTO.capacity != null) {
                calendar.setCapacity(calendarDTO.capacity);
            }

        }
    }

    private static void addResourcesCostCategoryAssignments(
        Resource resource, List<ResourcesCostCategoryAssignmentDTO> resourcesCostCategoryAssignments) {

        for (ResourcesCostCategoryAssignmentDTO assignmentDTO : resourcesCostCategoryAssignments) {
            ResourcesCostCategoryAssignment assignment = toEntity(assignmentDTO, resource);
            resource.addUnvalidatedResourcesCostCategoryAssignment(assignment);
        }
    }

    private static ResourcesCostCategoryAssignment toEntity(
        ResourcesCostCategoryAssignmentDTO assignmentDTO, Resource resource) {

        if (StringUtils.isBlank(assignmentDTO.costCategoryName)) {
            throw new ValidationException("cost category name not specified");
        }

        try {
            return ResourcesCostCategoryAssignment.createUnvalidated(
                assignmentDTO.code,
                StringUtils.trim(assignmentDTO.costCategoryName), resource,
                DateConverter.toLocalDate(assignmentDTO.startDate),
                DateConverter.toLocalDate(assignmentDTO.endDate));

        } catch (InstanceNotFoundException e) {
            throw new InstanceNotFoundRecoverableErrorException(CostCategoryDTO.ENTITY_TYPE, e.getKey().toString());
        }

    }

    private static void updateBasicData(Resource resource,
        ResourceDTO resourceDTO) {

        if (resource instanceof Machine && resourceDTO instanceof MachineDTO) {

            Machine machine = (Machine) resource;
            MachineDTO machineDTO = (MachineDTO) resourceDTO;

            machine.updateUnvalidated(StringUtils.trim(machineDTO.name), StringUtils.trim(machineDTO.description));

        } else if (resource instanceof Worker && resourceDTO instanceof WorkerDTO) {

            Worker worker = (Worker) resource;
            WorkerDTO workerDTO = (WorkerDTO) resourceDTO;

            worker.updateUnvalidated(
                StringUtils.trim(workerDTO.firstName),
                StringUtils.trim(workerDTO.surname),
                StringUtils.trim(workerDTO.nif));

        } else {

            throw new ValidationException(MessageFormat.format(
                    "Incompatible update: stored resource is not of type: {0}", resourceDTO.getEntityType()));
        }

    }

    /** Do not remove parameters */
    private static void updateResourceCalendar(Resource resource, ResourceCalendarDTO calendarDTO) {
        // TODO Decide policy to update calendar
        // (e.g. previous calendar must be removed?, if new calendar is the same as previous, must be reinitialized again?, etc.)
    }

    private static void updateCriterionSatisfactions(
            Resource resource, List<CriterionSatisfactionDTO> criterionSatisfactions) {

        for (CriterionSatisfactionDTO i : criterionSatisfactions) {

            try {
                CriterionSatisfaction criterionSatisfaction = resource.getCriterionSatisfactionByCode(i.code);
                updateCriterionSatisfaction(criterionSatisfaction, i);

            } catch (InstanceNotFoundException e) {
                CriterionSatisfaction criterionSatisfaction = toEntity(i, resource);
                resource.addUnvalidatedSatisfaction(criterionSatisfaction);
            }

        }

    }

    private static void updateCriterionSatisfaction(
        CriterionSatisfaction criterionSatisfaction, CriterionSatisfactionDTO criterionSatisfactionDTO) {

        try {

            criterionSatisfaction.updateUnvalidated(
                StringUtils.trim(criterionSatisfactionDTO.criterionTypeName),
                StringUtils.trim(criterionSatisfactionDTO.criterionName),
                DateConverter.toLocalDate(criterionSatisfactionDTO.startDate),
                DateConverter.toLocalDate(criterionSatisfactionDTO.endDate));

        } catch (InstanceNotFoundException e) {

            if (e.getClassName().equals(CriterionType.class.getName())) {
                throw new InstanceNotFoundRecoverableErrorException(CriterionTypeDTO.ENTITY_TYPE, e.getKey().toString());
            } else {
                throw new InstanceNotFoundRecoverableErrorException(CriterionDTO.ENTITY_TYPE, e.getKey().toString());
            }

        }

    }

    private static void updateResourcesCostCategoryAssignments(
        Resource resource, List<ResourcesCostCategoryAssignmentDTO> resourcesCostCategoryAssignments) {

        for (ResourcesCostCategoryAssignmentDTO i : resourcesCostCategoryAssignments) {

            try {

                ResourcesCostCategoryAssignment assignment = resource.getResourcesCostCategoryAssignmentByCode(i.code);
                updateResourcesCostCategoryAssignment(assignment, i);

            } catch (InstanceNotFoundException e) {

                ResourcesCostCategoryAssignment assignment = toEntity(i, resource);

                resource.addUnvalidatedResourcesCostCategoryAssignment(assignment);

            }
        }
    }

    private static void updateResourcesCostCategoryAssignment(
        ResourcesCostCategoryAssignment assignment, ResourcesCostCategoryAssignmentDTO i) {

        try {
            assignment.updateUnvalidated(
                StringUtils.trim(i.costCategoryName),
                DateConverter.toLocalDate(i.startDate),
                DateConverter.toLocalDate(i.endDate));
        } catch (InstanceNotFoundException e) {
            throw new InstanceNotFoundRecoverableErrorException(CostCategoryDTO.ENTITY_TYPE, e.getKey().toString());
        }
    }

    private static void checkResourceDTOType(ResourceDTO resourceDTO) {

        if (!(resourceDTO instanceof MachineDTO) && !(resourceDTO instanceof WorkerDTO)) {
            throw new ValidationException(MessageFormat.format(
                    "Service does not manage resource of type: {0}", resourceDTO.getEntityType()));
        }
    }

    public static ResourceDTO toDTO(Resource resource) {
        ResourceDTO resourceDTO;
        if (resource instanceof Worker) {
            resourceDTO = toDTO((Worker) resource);
        } else if (resource instanceof Machine) {
            resourceDTO = toDTO((Machine) resource);
        } else {
            return null;
        }

        List<CriterionSatisfactionDTO> criterionSatisfactionDTOs = new ArrayList<>();
        for (CriterionSatisfaction criterionSatisfaction : resource.getCriterionSatisfactions()) {
            criterionSatisfactionDTOs.add(toDTO(criterionSatisfaction));
        }
        resourceDTO.criterionSatisfactions = criterionSatisfactionDTOs;

        List<ResourcesCostCategoryAssignmentDTO> resourcesCostCategoryAssignmentDTOs = new ArrayList<>();
        for (ResourcesCostCategoryAssignment resourcesCostCategoryAssignment : resource.getResourcesCostCategoryAssignments()) {
            resourcesCostCategoryAssignmentDTOs.add(toDTO(resourcesCostCategoryAssignment));
        }
        resourceDTO.resourcesCostCategoryAssignments = resourcesCostCategoryAssignmentDTOs;

        resourceDTO.calendar = toDTO(resource.getCalendar());

        return resourceDTO;
    }

    private static WorkerDTO toDTO(Worker worker) {
        return new WorkerDTO(worker.getCode(), worker.getFirstName(), worker.getSurname(), worker.getNif());
    }

    private static MachineDTO toDTO(Machine machine) {
        return new MachineDTO(machine.getCode(), machine.getName(), machine.getDescription());
    }

    private static CriterionSatisfactionDTO toDTO(CriterionSatisfaction criterionSatisfaction) {
        return new CriterionSatisfactionDTO(
                criterionSatisfaction.getCode(),
                criterionSatisfaction.getCriterion().getType().getName(),
                criterionSatisfaction.getCriterion().getName(),
                DateConverter.toXMLGregorianCalendar(criterionSatisfaction.getStartDate()),
                DateConverter.toXMLGregorianCalendar(criterionSatisfaction.getEndDate()));
    }

    private static ResourcesCostCategoryAssignmentDTO toDTO(ResourcesCostCategoryAssignment resourcesCostCategoryAssignment) {
        Date initDate = (resourcesCostCategoryAssignment.getInitDate() == null)
                ? null
                : resourcesCostCategoryAssignment.getInitDate().toDateTimeAtStartOfDay().toDate();

        Date endDate = (resourcesCostCategoryAssignment.getEndDate() == null)
                ? null
                : resourcesCostCategoryAssignment.getEndDate().toDateTimeAtStartOfDay().toDate();

        return new ResourcesCostCategoryAssignmentDTO(
                resourcesCostCategoryAssignment.getCode(),
                resourcesCostCategoryAssignment.getCostCategory().getName(),
                DateConverter.toXMLGregorianCalendar(initDate),
                DateConverter.toXMLGregorianCalendar(endDate));
    }

    public static ResourceCalendarDTO toDTO(ResourceCalendar calendar) {

        BaseCalendarDTO baseCalendarDTO = CalendarConverter.toDTO(calendar);

        List<CalendarAvailabilityDTO> calendarAvailabilityDTOs = new ArrayList<>();
        for (CalendarAvailability calendarAvailability : calendar.getCalendarAvailabilities()) {
            calendarAvailabilityDTOs.add(toDTO(calendarAvailability));
        }

        return new ResourceCalendarDTO(
                baseCalendarDTO.code, baseCalendarDTO.name, baseCalendarDTO.parent,
                calendar.getCapacity(), baseCalendarDTO.calendarExceptions, baseCalendarDTO.calendarData,
                calendarAvailabilityDTOs);

    }

    private static CalendarAvailabilityDTO toDTO(CalendarAvailability calendarAvailability) {

        Date startDate = calendarAvailability.getStartDate().toDateTimeAtStartOfDay().toDate();

        Date endDate = null;
        if (calendarAvailability.getEndDate() != null) {
            endDate = calendarAvailability.getEndDate().toDateTimeAtStartOfDay().toDate();
        }

        return new CalendarAvailabilityDTO(calendarAvailability.getCode(), startDate, endDate);
    }
}
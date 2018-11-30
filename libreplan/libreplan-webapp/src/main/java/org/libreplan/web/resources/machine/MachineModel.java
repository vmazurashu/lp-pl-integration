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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.libreplan.business.calendars.daos.IBaseCalendarDAO;
import org.libreplan.business.calendars.entities.BaseCalendar;
import org.libreplan.business.calendars.entities.CalendarData;
import org.libreplan.business.calendars.entities.ResourceCalendar;
import org.libreplan.business.common.IntegrationEntity;
import org.libreplan.business.common.daos.IConfigurationDAO;
import org.libreplan.business.common.entities.Configuration;
import org.libreplan.business.common.entities.EntityNameEnum;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.planner.daos.IDayAssignmentDAO;
import org.libreplan.business.planner.daos.IResourceAllocationDAO;
import org.libreplan.business.resources.daos.ICriterionDAO;
import org.libreplan.business.resources.daos.IMachineDAO;
import org.libreplan.business.resources.daos.IResourceDAO;
import org.libreplan.business.resources.daos.IWorkerDAO;
import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.resources.entities.CriterionSatisfaction;
import org.libreplan.business.resources.entities.Machine;
import org.libreplan.business.resources.entities.MachineWorkerAssignment;
import org.libreplan.business.resources.entities.MachineWorkersConfigurationUnit;
import org.libreplan.business.resources.entities.Resource;
import org.libreplan.business.resources.entities.ResourceEnum;
import org.libreplan.business.resources.entities.Worker;
import org.libreplan.business.scenarios.IScenarioManager;
import org.libreplan.business.workreports.daos.IWorkReportLineDAO;
import org.libreplan.web.common.IntegrationEntityModel;
import org.libreplan.web.common.concurrentdetection.OnConcurrentModification;
import org.libreplan.web.resources.search.ResourcePredicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Diego Pino Garcia <dpino@igalia.com>
 * @author Javier Moran Rua <jmoran@igalia.com>
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@OnConcurrentModification(goToPage = "/resources/machine/machines.zul")
public class MachineModel extends IntegrationEntityModel implements IMachineModel {

    private static Log LOG = LogFactory.getLog(MachineModel.class);
    /**
     * State field.
     * Machine is the root of the aggregate for holding the state.
     *
     * Includes:
     * The Machine instance and associated entities.
     * The MachineWorkersConfigurationUnit set of the machine.
     * The MachineWorkerAssignments of each MachineWorkersConfigurationUnit instance.
     * The Criterion set required by each MachineWorkersConfigurationUnit instance.
     * The calendar associated with the Machine instance.
     *
     */
    private Machine machine;

    private ResourceCalendar calendarToRemove = null;

    private Map<Long, Criterion> criterions = new HashMap<>();

    private Map<Long, Worker> workers = new HashMap<>();

    private List<Machine> machineList = new ArrayList<>();

    @Autowired
    private IResourceDAO resourceDAO;

    @Autowired
    private IMachineDAO machineDAO;

    @Autowired
    private IBaseCalendarDAO baseCalendarDAO;

    @Autowired
    private ICriterionDAO criterionDAO;

    @Autowired
    private IWorkerDAO workerDAO;

    @Autowired
    private IConfigurationDAO configurationDAO;

    @Autowired
    private IDayAssignmentDAO dayAssignmentDAO;

    @Autowired
    private IWorkReportLineDAO workReportLineDAO;

    @Autowired
    private IResourceAllocationDAO resourceAllocationDAO;

    @Autowired
    private IScenarioManager scenarioManager;

    private void reattachCriterionsCache() {
        for (Criterion each: criterions.values()) {
            criterionDAO.reattachUnmodifiedEntity(each);
        }
    }

    private void reattachWorkersCache() {
        for (Worker each: workers.values()) {
            workerDAO.reattachUnmodifiedEntity(each);
        }
    }

    private void insertInCriterionsCacheIfNotExist(Criterion criterion) {
        if (!criterions.containsValue(criterion)) {
            criterions.put(criterion.getId(), criterion);
        }
    }

    private void insertInWorkersCacheIfNotExist(Worker worker) {
        if (!workers.containsValue(worker)) {
            workers.put(worker.getId(), worker);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void initCreate() {
        machine = Machine.create("");
        machine.setCodeAutogenerated(configurationDAO.getConfiguration().getGenerateCodeForResources());
        if (machine.isCodeAutogenerated()) {
            setDefaultCode();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void initEdit(Machine machine) {
        Validate.notNull(machine);
        try {
            this.machine = (Machine) resourceDAO.find(machine.getId());
            loadDependentEntities();
            initOldCodes();
        } catch (InstanceNotFoundException e) {
            LOG.error("Machine with id " + machine.getId() + " not found", e);
            throw new RuntimeException();
        }
    }

    private void loadDependentEntities() {
        reattachCriterionsCache();
        reattachWorkersCache();
        loadCriterionSatisfactions();
        loadConfigurationUnits();
        loadCalendar();
    }

    private void loadCalendar() {
        if (machine.getCalendar() != null) {
            forceLoadCalendar(machine.getCalendar());
        }
    }

    private void loadCriterionSatisfactions() {
        for (CriterionSatisfaction each: machine.getCriterionSatisfactions()) {
            each.getStartDate();
            each.getCriterion().getCompleteName();
            insertInCriterionsCacheIfNotExist(each.getCriterion());
        }
    }

    private void loadConfigurationUnits() {
        for (MachineWorkersConfigurationUnit each : machine.getConfigurationUnits()) {
            each.getName();
            loadRequiredCriterionsOf(each);
            loadMachineWorkersAssignmentsOf(each);
        }
    }

    private void loadRequiredCriterionsOf(MachineWorkersConfigurationUnit configurationUnit) {
        for (Criterion each: configurationUnit.getRequiredCriterions()) {
            each.getCompleteName();
            insertInCriterionsCacheIfNotExist(each);
        }
    }

    private void loadMachineWorkersAssignmentsOf(MachineWorkersConfigurationUnit configurationUnit) {
        for (MachineWorkerAssignment each: configurationUnit.getWorkerAssignments()) {
            each.getStartDate();
            each.getWorker().getName();
            insertInWorkersCacheIfNotExist(each.getWorker());
        }
    }

    @Override
    public Machine getMachine() {
        return machine;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MachineWorkersConfigurationUnit> getConfigurationUnitsOfMachine() {
        ArrayList<MachineWorkersConfigurationUnit> elements = new ArrayList<>();
        if (machine != null) {
            elements.addAll(machine.getConfigurationUnits());
        }
        Collections.sort(elements);
        return elements;
    }

    @Override
    public void setCalendarOfMachine(ResourceCalendar resourceCalendar) {
        if (machine != null) {
            machine.setCalendar(resourceCalendar);
        }
    }

    @Override
    public ResourceCalendar getCalendarOfMachine() {
        return (machine != null) ? machine.getCalendar() : null;
    }

    @Transactional(readOnly=true)
    @Override
    public void addWorkerAssignmentToConfigurationUnit(MachineWorkersConfigurationUnit unit, Worker worker) {
        for (MachineWorkersConfigurationUnit each: machine.getConfigurationUnits()) {
            if (each == unit) {
                each.addNewWorkerAssignment(worker);
            }
        }
    }

    @Transactional(readOnly=true)
    @Override
    public void addCriterionRequirementToConfigurationUnit(MachineWorkersConfigurationUnit unit, Criterion criterion) {
        HashSet<ResourceEnum> appliableToMachine = new HashSet<>();
        appliableToMachine.add(ResourceEnum.MACHINE);
        unit.addRequiredCriterion(criterion);
    }

    @Override
    @Transactional
    public void confirmSave() throws ValidationException {
        removeCalendarIfNeeded();
        resourceDAO.save(machine);
    }

    private void removeCalendarIfNeeded() {
        if (calendarToRemove != null) {
            try {
                resourceDAO.reattach(machine);
                baseCalendarDAO.remove(calendarToRemove.getId());
                calendarToRemove = null;
            } catch (InstanceNotFoundException e) {
                LOG.error("Couldn't remove calendar");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BaseCalendar> getBaseCalendars() {
        return baseCalendarDAO.getBaseCalendars();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Machine> getMachines() {
        machineList = machineDAO.getAll();
        return machineList;
    }

    public MachineWorkersConfigurationUnit getConfigurationUnitById(Long id) throws InstanceNotFoundException {
        MachineWorkersConfigurationUnit unit = null;
        for (MachineWorkersConfigurationUnit each : getConfigurationUnitsOfMachine()) {
            if (each.getId().equals(id)) {
                unit = each;
            }
        }
        if (unit == null) {
            throw new InstanceNotFoundException(id, MachineModel.class.getName());
        }
        return unit;
    }

    @Override
    public void removeConfigurationUnit(MachineWorkersConfigurationUnit unit) {
        machine.removeMachineWorkersConfigurationUnit(unit);
    }

    @Override
    public void setCalendar(ResourceCalendar resourceCalendar) {
        if (machine != null) {
            machine.setCalendar(resourceCalendar);
        }
    }

    @Override
    public ResourceCalendar getCalendar() {
        if (machine != null) {
            return machine.getCalendar();
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public BaseCalendar getDefaultCalendar() {
        Configuration configuration = configurationDAO.getConfiguration();
        if (configuration == null) {
            return null;
        }
        BaseCalendar defaultCalendar = configuration.getDefaultCalendar();
        forceLoadCalendar(defaultCalendar);
        return defaultCalendar;
    }

    private void forceLoadCalendar(BaseCalendar baseCalendar) {
        for (CalendarData calendarData : baseCalendar.getCalendarDataVersions()) {
            calendarData.getHoursPerDay().size();
            if (calendarData.getParent() != null) {
                forceLoadCalendar(calendarData.getParent());
            }
        }
        baseCalendar.getExceptions().size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Machine> getFilteredMachines(ResourcePredicate predicate) {
        List<Machine> filteredResourceList = new ArrayList<>();
        for (Machine machine : machineList) {
            machineDAO.reattach(machine);
            if (predicate.accepts(machine)) {
                filteredResourceList.add(machine);
            }
        }
        return filteredResourceList;
    }

    public List<Machine> getAllMachines() {
        return machineList;
    }

    @Override
    @Transactional(readOnly=true)
    public boolean canRemove(Machine machine) {
        List<Resource> resourcesList = new ArrayList<>();
        resourcesList.add(machine);
        return dayAssignmentDAO.findByResources(resourcesList).isEmpty() &&
                workReportLineDAO.findByResources(resourcesList).isEmpty() &&
                resourceAllocationDAO.findAllocationsRelatedToAnyOf(scenarioManager.getCurrent(), resourcesList).isEmpty();
    }

    @Override
    @Transactional
    public void confirmRemove(Machine machine) throws InstanceNotFoundException {
        resourceDAO.remove(machine.getId());
    }

    public EntityNameEnum getEntityName() {
        return EntityNameEnum.MACHINE;
    }

    public Set<IntegrationEntity> getChildren() {
        return new HashSet<>();
    }

    public IntegrationEntity getCurrentEntity() {
        return this.machine;
    }

    @Override
    public void removeCalendar() {
        calendarToRemove = machine.getCalendar();
        machine.setCalendar(null);
    }

    @Override
    public List<Worker> getWorkers() {
        return workerDAO.getWorkers();
    }
}

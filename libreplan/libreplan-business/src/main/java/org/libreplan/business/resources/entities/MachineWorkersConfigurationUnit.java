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

package org.libreplan.business.resources.entities;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.AssertTrue;
import javax.validation.Valid;
import org.libreplan.business.common.BaseEntity;
import org.libreplan.business.util.deepcopy.OnCopy;
import org.libreplan.business.util.deepcopy.Strategy;

/**
 * Machine Workers Configuration Unit.
 * <br />
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 */
public class MachineWorkersConfigurationUnit extends BaseEntity implements Comparable {

    @OnCopy(Strategy.SHARE)
    private Machine machine;

    private BigDecimal alpha;

    private String name;

    private Set<MachineWorkerAssignment> workerAssignments = new HashSet<>();

    @OnCopy(Strategy.SHARE_COLLECTION_ELEMENTS)
    private Set<Criterion> requiredCriterions = new HashSet<>();

    public MachineWorkersConfigurationUnit() {
    }

    protected MachineWorkersConfigurationUnit(Machine machine, String name, BigDecimal alpha) {
        this.machine = machine;
        this.name = name;
        this.alpha = alpha;
    }

    public static MachineWorkersConfigurationUnit create(Machine machine, String name, BigDecimal alpha) {
        return create(new MachineWorkersConfigurationUnit(machine, name, alpha));
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setAlpha(BigDecimal alpha) {
        this.alpha = alpha;
    }

    @Valid
    public BigDecimal getAlpha() {
        return alpha;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Set<MachineWorkerAssignment> getWorkerAssignments() {
        return Collections.unmodifiableSet(workerAssignments);
    }

    public void addWorkerAssignment(MachineWorkerAssignment assignment) {
        workerAssignments.add(assignment);
    }

    public void addNewWorkerAssignment(Worker worker) {
        MachineWorkerAssignment assignment = MachineWorkerAssignment.create(this, worker);
        assignment.setStartDate(new Date());
        workerAssignments.add(assignment);
    }

    public void removeMachineWorkersConfigurationUnit(
            MachineWorkerAssignment assignment) {
        workerAssignments.remove(assignment);
    }

    public void setRequiredCriterions(Set<Criterion> requiredCriterions) {
        this.requiredCriterions = requiredCriterions;
    }

    public Set<Criterion> getRequiredCriterions() {
        return Collections.unmodifiableSet(requiredCriterions);
    }

    public void addRequiredCriterion(Criterion criterion) {
        requiredCriterions.add(criterion);
    }

    public void removeRequiredCriterion(Criterion criterion) {
        requiredCriterions.remove(criterion);
    }

    public boolean existsWorkerAssignmentWithSameWorker(MachineWorkerAssignment assignment) {
        boolean assigned = false;
        for (MachineWorkerAssignment each : workerAssignments) {

            if (!(each.getId().equals(assignment.getId())) &&
                    ((each.getWorker().getId().equals(assignment.getWorker().getId())))) {

                assigned = true;
            }
        }
        return assigned;
    }

    public boolean existsWorkerAssignmentWithSameWorker(MachineWorkerAssignment assignment, Interval interval) {
        boolean assigned = false;
        Worker worker = assignment.getWorker();
        Interval range;

        for (MachineWorkerAssignment each : workerAssignments) {
            if ((each.getWorker().getId().equals(worker.getId())) && (each.getId() != assignment.getId())) {
                if (each.getFinishDate() != null) {
                    range = Interval.range(each.getStart(), each.getFinish());
                } else {
                    range = Interval.from(each.getStart());
                }
                if ((range == null) || (interval.overlapsWith(range))) {
                    assigned = true;
                }
            }
        }
        return assigned;
    }

    @AssertTrue(message = "Alpha must be greater than 0")
    public boolean isAlphaConstraint() {
        return (this.alpha.compareTo(new BigDecimal(0)) > 0);
    }

    @AssertTrue(message = "All machine worker assignments must have a start date earlier than the end date")
    public boolean isWorkerAssignmentsIntervalsProperlyDefinedConstraint() {
        boolean correctIntervals = true;
        for (MachineWorkerAssignment each : workerAssignments) {
            if (each.getStartDate() == null) {
                correctIntervals = false;
            } else if ((each.getFinishDate() != null) && (each.getStartDate().compareTo(each.getFinishDate()) > 0)) {
                correctIntervals = false;
            }
        }
        return correctIntervals;
    }

    @AssertTrue(message = "The same resource is assigned twice inside an interval")
    public boolean isUniqueWorkerAssignmentInIntervalConstraint() {
        boolean unique = true;
        Interval range;
        for (MachineWorkerAssignment each : workerAssignments) {
            if (each.getStartDate() != null) {
                if (each.getFinishDate() != null) {
                    range = Interval.range(each.getStart(), each.getFinish());
                } else {
                    range = Interval.from(each.getStart());
                }
                if (existsWorkerAssignmentWithSameWorker(each, range)) {
                    unique = false;
                }
            }
        }
        return unique;
    }

    @Override
    public int compareTo(Object configurationUnit) {
        return this.name.compareToIgnoreCase(((MachineWorkersConfigurationUnit) configurationUnit).getName());
    }

}

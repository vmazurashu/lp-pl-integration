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
package org.libreplan.business.test.planner.entities;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.libreplan.business.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_FILE;
import static org.libreplan.business.test.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_TEST_FILE;
import static org.libreplan.business.test.planner.entities.DayAssignmentMatchers.haveHours;
import static org.libreplan.business.workingday.EffortDuration.hours;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libreplan.business.planner.entities.DayAssignment;
import org.libreplan.business.planner.entities.DerivedAllocation;
import org.libreplan.business.planner.entities.DerivedAllocationGenerator;
import org.libreplan.business.planner.entities.DerivedAllocationGenerator.IWorkerFinder;
import org.libreplan.business.planner.entities.DerivedDayAssignment;
import org.libreplan.business.planner.entities.ResourceAllocation;
import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.resources.entities.Machine;
import org.libreplan.business.resources.entities.MachineWorkerAssignment;
import org.libreplan.business.resources.entities.MachineWorkersConfigurationUnit;
import org.libreplan.business.resources.entities.Resource;
import org.libreplan.business.resources.entities.Worker;
import org.libreplan.business.scenarios.bootstrap.IScenariosBootstrap;
import org.libreplan.business.workingday.EffortDuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { BUSINESS_SPRING_CONFIG_FILE, BUSINESS_SPRING_CONFIG_TEST_FILE })
public class DerivedAllocationGeneratorTest {

    @Autowired
    private IScenariosBootstrap scenariosBootstrap;

    @Before
    public void loadRequiredData() {
        scenariosBootstrap.loadRequiredData();
    }

    private final Machine machine = null;

    private ResourceAllocation<?> derivedFrom;
    private IWorkerFinder finder;
    private MachineWorkersConfigurationUnit configurationUnit;
    private List<DayAssignment> dayAssignments;

    private void givenDerivedFrom() {
        ResourceAllocation<?> result = createNiceMock(ResourceAllocation.class);
        replay(result);
        derivedFrom = result;
    }

    @SuppressWarnings("unchecked")
    private void givenFinder(Worker... workers) {
        IWorkerFinder result = createNiceMock(IWorkerFinder.class);
        Collection<? extends Criterion> argument = anyObject();
        expect(result.findWorkersMatching(argument)).andReturn(
                Arrays.asList(workers)).anyTimes();
        replay(result);
        finder = result;
    }

    private void givenConfigurationUnit() {
        MachineWorkersConfigurationUnit result = createNiceMock(MachineWorkersConfigurationUnit.class);
        replay(result);
        configurationUnit = result;
    }

    private void givenConfigurationUnit(BigDecimal alpha, Worker... workers) {
        MachineWorkersConfigurationUnit result = createNiceMock(MachineWorkersConfigurationUnit.class);
        expect(result.getAlpha()).andReturn(alpha).anyTimes();
        expect(result.getWorkerAssignments()).andReturn(
                assignmentsFor(result, workers)).anyTimes();
        expect(result.getMachine()).andReturn(machine).anyTimes();
        replay(result);
        configurationUnit = result;
    }

    private Set<MachineWorkerAssignment> assignmentsFor(MachineWorkersConfigurationUnit unit, Worker[] workers) {
        Set<MachineWorkerAssignment> result = new HashSet<>();
        for (Worker each : workers) {
            MachineWorkerAssignment assignment = workerAssignment(unit,each);
            result.add(assignment);
        }

        return result;
    }

    private MachineWorkerAssignment workerAssignment(MachineWorkersConfigurationUnit unit, Worker each) {
        MachineWorkerAssignment result = createNiceMock(MachineWorkerAssignment.class);
        expect(result.getMachineWorkersConfigurationUnit()).andReturn(unit).anyTimes();
        expect(result.getWorker()).andReturn(each);
        expect(result.getStartDate()).andReturn(asDate(new LocalDate(2000, 1, 1))).anyTimes();
        expect(result.getFinishDate()).andReturn(null).anyTimes();
        replay(result);

        return result;
    }

    private Worker workerWithAlwaysAssignedHours(int assignedHours){
        Worker result = createNiceMock(Worker.class);
        expect(result.getAssignedDurationDiscounting(isA(Map.class), isA(LocalDate.class)))
                .andReturn(hours(assignedHours))
                .anyTimes();
        replay(result);

        return result;
    }

    private Date asDate(LocalDate localDate) {
        return localDate.toDateTimeAtStartOfDay().toDate();
    }

    private void givenDayAssignments() {
        dayAssignments = new ArrayList<>();
    }

    private void givenDayAssignments(LocalDate start, int... hours) {
        dayAssignments = new ArrayList<>();
        for (int i = 0; i < hours.length; i++) {
            dayAssignments.add(createAssignment(start.plusDays(i), machine,
                    hours[i]));
        }
    }

    private DayAssignment createAssignment(LocalDate day, Machine machine, int hours) {
        DayAssignment dayAssignment = createNiceMock(DayAssignment.class);

    //    expect(dayAssignment.getHours()).andReturn(hours).anyTimes();
        expect(dayAssignment.getDuration()).andReturn(hours(hours)).anyTimes();
        expect(dayAssignment.getResource()).andReturn(machine).anyTimes();
        expect(dayAssignment.getDay()).andReturn(day).anyTimes();
        expect(dayAssignment.isAssignedTo(machine)).andReturn(true).anyTimes();

        replay(dayAssignment);

        return dayAssignment;
    }

    @Test(expected = NullPointerException.class)
    public void derivedFromMustBeNotNull() {
        givenFinder();
        givenConfigurationUnit();
        givenDayAssignments();
        DerivedAllocationGenerator.generate(derivedFrom, finder, configurationUnit, dayAssignments);
    }

    @Test(expected = NullPointerException.class)
    public void finderMustBeNotNull() {
        givenDerivedFrom();
        givenConfigurationUnit();
        givenDayAssignments();
        DerivedAllocationGenerator.generate(derivedFrom, finder, configurationUnit, dayAssignments);
    }

    @Test(expected = NullPointerException.class)
    public void configurationUnitMustBeNotNull() {
        givenDerivedFrom();
        givenFinder();
        givenDayAssignments();
        DerivedAllocationGenerator.generate(derivedFrom, finder, configurationUnit, dayAssignments);
    }

    @Test(expected = NullPointerException.class)
    public void dayAssignmentsMustBeNotNull() {
        givenDerivedFrom();
        givenFinder();
        givenConfigurationUnit();
        DerivedAllocationGenerator.generate(derivedFrom, finder, configurationUnit, dayAssignments);
    }

    @Test
    public void forOneResourceTheHoursGeneratedAreGotFromAlpha() {
        givenDerivedFrom();
        givenFinder();
        givenConfigurationUnit(new BigDecimal(1.5), new Worker());
        givenDayAssignments(new LocalDate(2009, 10, 20), 8, 8, 8, 4);
        DerivedAllocation derivedAllocation = DerivedAllocationGenerator
                .generate(derivedFrom, finder, configurationUnit, dayAssignments);
        List<DerivedDayAssignment> assignments = derivedAllocation.getAssignments();
        assertThat(assignments, haveHours(12, 12, 12, 6));
    }

    @Test
    public void onlyDayAssignmentsForTheMachineOfTheConfigurationUnitAreUsed() {
        givenDerivedFrom();
        givenFinder();
        givenConfigurationUnit(new BigDecimal(1.5), new Worker());
        LocalDate start = new LocalDate(2009, 10, 20);
        givenDayAssignments(start, 8, 8, 8, 4);

        Machine otherMachine = Machine.create();
        dayAssignments.add(createAssignment(start.plusDays(5), otherMachine, 8));

        DerivedAllocation derivedAllocation = DerivedAllocationGenerator
                .generate(derivedFrom, finder, configurationUnit, dayAssignments);
        List<DerivedDayAssignment> assignments = derivedAllocation.getAssignments();
        assertThat(assignments.size(), equalTo(4));
    }

    @Test
    public void forSeveralResourcesTheHoursAreDistributedTakingIntoAccountTheFreeHours() {
        givenDerivedFrom();
        Worker worker1 = workerWithAlwaysAssignedHours(4);
        Worker worker2 = workerWithAlwaysAssignedHours(6);
        givenFinder(worker1, worker2);
        givenConfigurationUnit(new BigDecimal(1.5));
        givenDayAssignments(new LocalDate(2009, 10, 20), 8, 8, 8, 4);
        DerivedAllocation derivedAllocation = DerivedAllocationGenerator
                .generate(derivedFrom, finder, configurationUnit, dayAssignments);
        List<DerivedDayAssignment> assignments = derivedAllocation.getAssignments();
        Map<Resource, List<DerivedDayAssignment>> byResource = DayAssignment.byResourceAndOrdered(assignments);
        assertThat(byResource.get(worker1), haveHours(7, 7, 7, 4));
        assertThat(byResource.get(worker2), haveHours(5, 5, 5, 2));
    }

}

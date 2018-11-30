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

package org.libreplan.business.test.orders.entities;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.libreplan.business.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_FILE;
import static org.libreplan.business.test.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_TEST_FILE;

import java.math.BigDecimal;

import javax.annotation.Resource;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libreplan.business.IDataBootstrap;
import org.libreplan.business.advance.daos.IAdvanceAssignmentDAO;
import org.libreplan.business.advance.daos.IAdvanceTypeDAO;
import org.libreplan.business.advance.entities.AdvanceAssignment;
import org.libreplan.business.advance.entities.AdvanceMeasurement;
import org.libreplan.business.advance.entities.AdvanceType;
import org.libreplan.business.advance.entities.DirectAdvanceAssignment;
import org.libreplan.business.advance.exceptions.DuplicateAdvanceAssignmentForOrderElementException;
import org.libreplan.business.advance.exceptions.DuplicateValueTrueReportGlobalAdvanceException;
import org.libreplan.business.common.daos.IConfigurationDAO;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.orders.daos.IOrderDAO;
import org.libreplan.business.orders.entities.HoursGroup;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.orders.entities.OrderLine;
import org.libreplan.business.orders.entities.OrderLineGroup;
import org.libreplan.business.scenarios.IScenarioManager;
import org.libreplan.business.scenarios.entities.OrderVersion;
import org.libreplan.business.test.planner.daos.ResourceAllocationDAOTest;
import org.libreplan.business.test.resources.daos.CriterionSatisfactionDAOTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests for {@link AdvanceAssignment of OrderElement}.
 * <br />
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { BUSINESS_SPRING_CONFIG_FILE, BUSINESS_SPRING_CONFIG_TEST_FILE })
public class AddAdvanceAssignmentsToOrderElementTest {

    @Resource
    private IDataBootstrap defaultAdvanceTypesBootstrapListener;

    @Resource
    private IDataBootstrap configurationBootstrap;

    @Before
    public void loadRequiredData() {
        defaultAdvanceTypesBootstrapListener.loadRequiredData();
        configurationBootstrap.loadRequiredData();
    }

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private IOrderDAO orderDao;

    @Autowired
    private IAdvanceAssignmentDAO advanceAssignmentDao;

    @Autowired
    private IAdvanceTypeDAO advanceTypeDao;

    @Autowired
    private IConfigurationDAO configurationDAO;

    @Autowired
    private IScenarioManager scenarioManager;

    private Session getSession() {
        return sessionFactory.getCurrentSession();
    }

    private Order createValidOrder() {
        Order order = Order.create();
        OrderVersion orderVersion = ResourceAllocationDAOTest.setupVersionUsing(scenarioManager, order);
        order.useSchedulingDataFor(orderVersion);
        order.setDescription("description");
        order.setInitDate(CriterionSatisfactionDAOTest.year(2000).toDateTimeAtStartOfDay().toDate());
        order.setName("name");
        order.setResponsible("responsible");
        order.setCode("code");
        order.setCalendar(configurationDAO.getConfiguration().getDefaultCalendar());

        return order;
    }

    private OrderLine createValidLeaf(String name, String code) {
        OrderLine result = OrderLine.create();
        result.setName(name);
        result.setCode(code);
        HoursGroup hoursGroup = HoursGroup.create(result);
        hoursGroup.setWorkingHours(0);
        hoursGroup.setCode("hoursGroupName");
        result.addHoursGroup(hoursGroup);

        return result;
    }

    private AdvanceType createValidAdvanceType(String name) {
        BigDecimal value = new BigDecimal(120).setScale(2);
        BigDecimal precision = new BigDecimal(10).setScale(4);
        AdvanceType advanceType = AdvanceType.create(name, value, true, precision, true, false);
        return advanceType;
    }

    private AdvanceMeasurement createValidAdvanceMeasurement() {
        AdvanceMeasurement advanceMeasurement = AdvanceMeasurement.create(new LocalDate(), new BigDecimal(0));
        return advanceMeasurement;
    }

    private DirectAdvanceAssignment createValidAdvanceAssignment(boolean reportGlobalAdvance) {
        return DirectAdvanceAssignment.create(reportGlobalAdvance, BigDecimal.TEN);
    }

    @Test
    @Transactional
    public void savingTheOrderSavesAlsoTheAddedAssignments()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {

        Order order = createValidOrder();
        OrderElement orderLine = createValidLeaf("OrderLineA", "1k1k1k1k");

        AdvanceType advanceType = createAndSaveType("tipoA");

        DirectAdvanceAssignment advanceAssignment = createValidAdvanceAssignment(true);
        assertTrue(orderLine.getDirectAdvanceAssignments().isEmpty());
        advanceAssignment.setAdvanceType(advanceType);

        order.add(orderLine);
        orderDao.save(order);

        orderLine.addAdvanceAssignment(advanceAssignment);

        orderDao.save(order);
        this.sessionFactory.getCurrentSession().flush();

        assertFalse(orderLine.getDirectAdvanceAssignments().isEmpty());
        assertTrue(advanceAssignmentDao.exists(advanceAssignment.getId()));

    }

    private AdvanceType createAndSaveType(String typeName) {
        AdvanceType advanceType = createValidAdvanceType(typeName);
        advanceTypeDao.save(advanceType);
        return advanceType;
    }

    @Test
    @Transactional
    public void addingSeveralAssignmentsOfDifferentTypes()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {

        Order order = createValidOrder();
        OrderLine orderLine = createValidLeaf("OrderLineA", "1111111");

        AdvanceType advanceTypeA = createAndSaveType("tipoA");
        AdvanceType advanceTypeB = createAndSaveType("tipoB");

        DirectAdvanceAssignment advanceAssignmentA = createValidAdvanceAssignment(true);
        advanceAssignmentA.setAdvanceType(advanceTypeA);

        order.add(orderLine);
        orderDao.save(order);

        orderLine.addAdvanceAssignment(advanceAssignmentA);

        DirectAdvanceAssignment advanceAssignmentB = createValidAdvanceAssignment(false);
        advanceAssignmentB.setAdvanceType(advanceTypeB);
        orderLine.addAdvanceAssignment(advanceAssignmentB);
    }

    @Test
    @Transactional
    public void cannotAddDuplicatedAssignment()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {

        OrderLine orderLine = createValidLeaf("OrderLineA", "22222222");

        AdvanceType advanceTypeA = createAndSaveType("tipoA");

        DirectAdvanceAssignment advanceAssignmentA = createValidAdvanceAssignment(true);
        advanceAssignmentA.setAdvanceType(advanceTypeA);

        orderLine.addAdvanceAssignment(advanceAssignmentA);

        DirectAdvanceAssignment advanceAssignmentB = createValidAdvanceAssignment(false);
        advanceAssignmentB.setAdvanceType(advanceTypeA);

        try {
            orderLine.addAdvanceAssignment(advanceAssignmentB);
            fail("It should throw an exception");
        } catch (DuplicateAdvanceAssignmentForOrderElementException ignored) {
            // Ok
        }
    }

    @Test
    @Transactional
    public void cannotAddTwoAssignmentsWithGlobalReportValue()
            throws DuplicateAdvanceAssignmentForOrderElementException,
            DuplicateValueTrueReportGlobalAdvanceException {

        OrderLine orderLine = createValidLeaf("OrderLineA", "101010101");

        AdvanceType advanceTypeA = createAndSaveType("tipoA");
        AdvanceType advanceTypeB = createAndSaveType("tipoB");

        DirectAdvanceAssignment advanceAssignmentA = createValidAdvanceAssignment(true);
        advanceAssignmentA.setAdvanceType(advanceTypeA);


        orderLine.addAdvanceAssignment(advanceAssignmentA);

        DirectAdvanceAssignment advanceAssignmentB = createValidAdvanceAssignment(true);
        advanceAssignmentB.setAdvanceType(advanceTypeB);
        try {
            orderLine.addAdvanceAssignment(advanceAssignmentB);
            fail("It should throw an exception  ");
        } catch (DuplicateValueTrueReportGlobalAdvanceException ignored) {
            // Ok
        }
    }

    @Test
    @Transactional
    public void addingAssignmentsOfAnotherTypeToSon()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {

        Order validOrder = createValidOrder();
        OrderLineGroup container = OrderLineGroup.create();
        validOrder.add(container);
        container.setName("bla");
        container.setCode("000000000");
        OrderLine son = createValidLeaf("bla", "132");
        container.add(son);

        AdvanceMeasurement advanceMeasurement = createValidAdvanceMeasurement();

        AdvanceType advanceTypeA = createAndSaveType("tipoA");
        AdvanceType advanceTypeB = createAndSaveType("tipoB");

        DirectAdvanceAssignment advanceAssignmentA = createValidAdvanceAssignment(true);
        advanceAssignmentA.setAdvanceType(advanceTypeA);

        DirectAdvanceAssignment advanceAssignmentB = createValidAdvanceAssignment(false);
        advanceAssignmentB.setAdvanceType(advanceTypeB);

        OrderElementTest
                .removeReportGlobalAdvanceFromChildrenAdvance(container);
        container.addAdvanceAssignment(advanceAssignmentA);
        son.addAdvanceAssignment(advanceAssignmentB);

        advanceAssignmentA.addAdvanceMeasurements(advanceMeasurement);
        advanceAssignmentB.addAdvanceMeasurements(advanceMeasurement);
    }

    @Test
    @Transactional
    public void addingAnAdvanceAssignmentIncreasesTheNumberOfAdvanceAssignments()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {

        Order validOrder = createValidOrder();
        final OrderLineGroup container = OrderLineGroup.create();
        validOrder.add(container);
        container.setName("bla");
        container.setCode("000000000");
        container.add(createValidLeaf("bla", "979"));

        AdvanceType advanceTypeA = createAndSaveType("tipoA");
        DirectAdvanceAssignment advanceAssignmentA = createValidAdvanceAssignment(true);
        advanceAssignmentA.setAdvanceType(advanceTypeA);

        OrderElementTest.removeReportGlobalAdvanceFromChildrenAdvance(container);
        container.addAdvanceAssignment(advanceAssignmentA);

        assertThat(container.getDirectAdvanceAssignments().size(), equalTo(1));
    }

    @Test
    @Transactional
    public void cannotAddDuplicatedAssignmentToSon()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {

        Order validOrder = createValidOrder();
        final OrderLineGroup father = OrderLineGroup.create();
        validOrder.add(father);
        father.setName("bla");
        father.setCode("000000000");
        father.add(createValidLeaf("bla", "979"));

        AdvanceType advanceTypeA = createAndSaveType("tipoA");

        DirectAdvanceAssignment advanceAssignmentA = createValidAdvanceAssignment(true);
        advanceAssignmentA.setAdvanceType(advanceTypeA);
        DirectAdvanceAssignment anotherAssignmentWithSameType = createValidAdvanceAssignment(false);
        anotherAssignmentWithSameType.setAdvanceType(advanceTypeA);

        OrderElementTest.removeReportGlobalAdvanceFromChildrenAdvance(father);
        father.addAdvanceAssignment(advanceAssignmentA);

        try {
            OrderElement child = father.getChildren().get(0);
            child.addAdvanceAssignment(anotherAssignmentWithSameType);
            fail("It should throw an exception  ");
        } catch (DuplicateAdvanceAssignmentForOrderElementException ignored) {
            // Ok
        }
    }

    @Test
    @Transactional
    public void cannotAddDuplicateAssignmentToGrandParent()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {

        Order validOrder = createValidOrder();
        OrderLineGroup parent = OrderLineGroup.create();
        validOrder.add(parent);
        parent.setName("bla_");
        parent.setCode("000000000");
        OrderLineGroup son = OrderLineGroup.create();
        son.setName("Son");
        son.setCode("11111111");
        parent.add(son);
        OrderLine grandSon = createValidLeaf("GranSon", "75757");
        son.add(grandSon);

        AdvanceMeasurement advanceMeasurement = createValidAdvanceMeasurement();
        AdvanceType advanceTypeA = createAndSaveType("tipoA");

        DirectAdvanceAssignment advanceAssignmentA = createValidAdvanceAssignment(true);
        advanceAssignmentA.setAdvanceType(advanceTypeA);
        DirectAdvanceAssignment advanceAssignmentB = createValidAdvanceAssignment(false);
        advanceAssignmentB.setAdvanceType(advanceTypeA);

        grandSon.addAdvanceAssignment(advanceAssignmentA);
        advanceAssignmentA.addAdvanceMeasurements(advanceMeasurement);

        try {
            parent.addAdvanceAssignment(advanceAssignmentB);
            advanceAssignmentB.addAdvanceMeasurements(advanceMeasurement);
            fail("It should throw an exception  ");
        } catch (DuplicateAdvanceAssignmentForOrderElementException ignored) {
            // Ok
        }
    }

    @Test(expected = DuplicateAdvanceAssignmentForOrderElementException.class)
    @Transactional
    public void addingAnotherAdvanceAssignmentWithAnEquivalentTypeButDifferentInstance()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {

        final Order order = createValidOrder();
        final OrderLine line = createValidLeaf("GranSon", "75757");
        order.add(line);
        orderDao.save(order);

        AdvanceType type = createAndSaveType("tipoA");
        getSession().flush();

        final DirectAdvanceAssignment assignment = createValidAdvanceAssignment(false);
        assignment.setAdvanceType(type);

        line.addAdvanceAssignment(assignment);

        getSession().evict(type);
        AdvanceType typeReloaded = reloadType(type);

        final DirectAdvanceAssignment assignmentWithSameType = createValidAdvanceAssignment(false);
        assignmentWithSameType.setAdvanceType(typeReloaded);

        line.addAdvanceAssignment(assignmentWithSameType);
    }

    private AdvanceType reloadType(AdvanceType type) {
        try {
            // New instance of id is created to avoid both types have the same id object
            Long newLong = new Long(type.getId());
            return advanceTypeDao.find(newLong);
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(expected = DuplicateValueTrueReportGlobalAdvanceException.class)
    @Transactional
    public void cannotAddTwoAssignmentsDirectAndIndirectWithGlobalReportValue()
            throws DuplicateValueTrueReportGlobalAdvanceException,
            DuplicateAdvanceAssignmentForOrderElementException {

        OrderLineGroup orderLineGroup = OrderLineGroup.create();
        orderLineGroup.setName("test");
        orderLineGroup.setCode("1");

        AdvanceType advanceType = createAndSaveType("test");

        DirectAdvanceAssignment advanceAssignmentA = createValidAdvanceAssignment(true);
        advanceAssignmentA.setAdvanceType(advanceType);

        DirectAdvanceAssignment advanceAssignmentB = createValidAdvanceAssignment(true);
        advanceAssignmentB.setAdvanceType(advanceType);

        orderLineGroup.addAdvanceAssignment(advanceAssignmentA);
        orderLineGroup.addAdvanceAssignment(advanceAssignmentB);
    }

}

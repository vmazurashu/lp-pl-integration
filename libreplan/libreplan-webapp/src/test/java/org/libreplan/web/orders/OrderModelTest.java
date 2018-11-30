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

package org.libreplan.web.orders;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.libreplan.business.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_FILE;
import static org.libreplan.web.WebappGlobalNames.WEBAPP_SPRING_CONFIG_FILE;
import static org.libreplan.web.WebappGlobalNames.WEBAPP_SPRING_SECURITY_CONFIG_FILE;
import static org.libreplan.web.test.WebappGlobalNames.WEBAPP_SPRING_CONFIG_TEST_FILE;
import static org.libreplan.web.test.WebappGlobalNames.WEBAPP_SPRING_SECURITY_CONFIG_TEST_FILE;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;

import org.easymock.EasyMock;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libreplan.business.IDataBootstrap;
import org.libreplan.business.calendars.entities.BaseCalendar;
import org.libreplan.business.common.IAdHocTransactionService;
import org.libreplan.business.common.IOnTransaction;
import org.libreplan.business.common.daos.IConfigurationDAO;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.externalcompanies.daos.IExternalCompanyDAO;
import org.libreplan.business.externalcompanies.entities.ExternalCompany;
import org.libreplan.business.orders.daos.IOrderDAO;
import org.libreplan.business.orders.entities.HoursGroup;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.orders.entities.OrderLine;
import org.libreplan.business.orders.entities.OrderLineGroup;
import org.libreplan.business.orders.entities.TaskSource;
import org.libreplan.business.planner.daos.ITaskSourceDAO;
import org.libreplan.business.requirements.entities.CriterionRequirement;
import org.libreplan.business.requirements.entities.DirectCriterionRequirement;
import org.libreplan.business.resources.daos.ICriterionDAO;
import org.libreplan.business.resources.daos.ICriterionTypeDAO;
import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.resources.entities.CriterionType;
import org.libreplan.business.resources.entities.ResourceEnum;
import org.libreplan.business.scenarios.IScenarioManager;
import org.libreplan.business.scenarios.entities.OrderVersion;
import org.libreplan.business.scenarios.entities.Scenario;
import org.libreplan.web.calendars.BaseCalendarModel;
import org.libreplan.web.planner.order.PlanningStateCreator;
import org.libreplan.web.planner.order.PlanningStateCreator.PlanningState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.zkoss.zk.ui.Desktop;

/**
 * Tests for {@link OrderModel}.
 * <br />
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { BUSINESS_SPRING_CONFIG_FILE,
        WEBAPP_SPRING_CONFIG_FILE, WEBAPP_SPRING_CONFIG_TEST_FILE,
        WEBAPP_SPRING_SECURITY_CONFIG_FILE,
        WEBAPP_SPRING_SECURITY_CONFIG_TEST_FILE })
/**
 * This annotation drops context and force it to reload.
 * Action described above prevents tests from falling
 * due to "Row was updated or deleted by another transaction" exception.
 * Also this trick clears cache and that's why there is no troubles with commands caching
 * in the PlanningState.getSaveCommand() method.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class OrderModelTest {

    public static OrderVersion setupVersionUsing(IScenarioManager scenarioManager, Order order) {
        Scenario current = scenarioManager.getCurrent();
        OrderVersion result = OrderVersion.createInitialVersion(current);
        order.setVersionForScenario(current, result);
        return result;
    }

    @Resource
    private IDataBootstrap defaultAdvanceTypesBootstrapListener;

    @Resource
    private IDataBootstrap configurationBootstrap;

    @Resource
    private IDataBootstrap scenariosBootstrap;

    @BeforeTransaction
    public void loadRequiredData() {
        defaultAdvanceTypesBootstrapListener.loadRequiredData();
        configurationBootstrap.loadRequiredData();
        scenariosBootstrap.loadRequiredData();
    }

    public static Date year(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        return calendar.getTime();
    }

    @Autowired
    private IOrderModel orderModel;

    @Autowired
    private IOrderDAO orderDAO;

    @Autowired
    private ITaskSourceDAO taskSourceDAO;

    @Autowired
    private ICriterionTypeDAO criterionTypeDAO;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private IAdHocTransactionService adHocTransaction;

    @Autowired
    private ICriterionDAO criterionDAO;

    @Autowired
    private IConfigurationDAO configurationDAO;

    @Autowired
    private IExternalCompanyDAO externalCompanyDAO;

    @Autowired
    private PlanningStateCreator planningStateCreator;

    private Criterion criterion;

    private Desktop mockDesktop() {
        return EasyMock.createNiceMock(Desktop.class);
    }

    private PlanningState createPlanningStateFor(final Order newOrder) {
        return adHocTransaction.runOnAnotherReadOnlyTransaction(new IOnTransaction<PlanningState>() {

            @Override
            public PlanningState execute() {
                return planningStateCreator.createOn(mockDesktop(), newOrder);
            }
        });
    }

    private Order createValidOrder() {
        Order order = Order.create();
        order.setDescription("description");
        order.setInitDate(year(2000));
        order.setName("name");
        order.setResponsible("responsible");
        order.setCode("code-" + UUID.randomUUID());

        BaseCalendar calendar = adHocTransaction.runOnReadOnlyTransaction(new IOnTransaction<BaseCalendar>() {

            @Override
            public BaseCalendar execute() {

                BaseCalendar result =
                        configurationDAO.getConfigurationWithReadOnlyTransaction().getDefaultCalendar();

                BaseCalendarModel.forceLoadBaseCalendar(result);
                return result;
            }
        });

        order.setCalendar(calendar);
        return order;
    }

    private ExternalCompany createValidExternalCompany() {
        ExternalCompany externalCompany = ExternalCompany.create(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString());

        externalCompanyDAO.save(externalCompany);
        return externalCompany;
    }

    @Test
    @Transactional
    @Rollback(false)
    public void testNotRollback() {
        // Just to do not make rollback in order to have the default
        // configuration, needed for prepareForCreate in order to autogenerate the order code
    }

    @Test
    @Transactional
    public void testCreation() throws ValidationException {
        Order order = createValidOrder();
        order.setCustomer(createValidExternalCompany());
        orderModel.setPlanningState(createPlanningStateFor(order));
        orderModel.save();
        assertTrue(orderDAO.exists(order.getId()));
    }

    private Order givenOrderFromPrepareForCreate() {
        adHocTransaction.runOnAnotherReadOnlyTransaction(new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                orderModel.prepareForCreate(mockDesktop());
                return null;
            }
        });

        Order order = orderModel.getOrder();
        order.setName("name");
        order.setCode("code");
        order.setInitDate(new Date());
        order.setCustomer(createValidExternalCompany());

        return order;
    }

    @Test
    @Transactional
    public void testCreationUsingPrepareForCreate() {
        Order order = givenOrderFromPrepareForCreate();
        orderModel.save();
        assertTrue(orderDAO.exists(order.getId()));
    }

    @Test
    @Transactional
    public void createOrderWithScheduledOrderLine() {
        Order order = givenOrderFromPrepareForCreate();
        OrderElement line = OrderLine.createOrderLineWithUnfixedPercentage(20);
        order.add(line);
        line.setName(UUID.randomUUID().toString());
        line.setCode(UUID.randomUUID().toString());
        assert line.getSchedulingState().isSomewhatScheduled();
        orderModel.save();
        assertTrue(orderDAO.exists(order.getId()));
        TaskSource lineTaskSource = line.getTaskSource();
        assertTrue(taskSourceDAO.exists(lineTaskSource.getId()));
    }

    @Test
    @Transactional
    public void ifAnOrderLineIsScheduledItsTypeChanges() {
        Order order = givenOrderFromPrepareForCreate();
        OrderElement line = OrderLine.createOrderLineWithUnfixedPercentage(20);
        line.useSchedulingDataFor(order.getCurrentOrderVersion());
        line.getSchedulingState().unschedule();
        order.add(line);
        assertFalse(order.getSchedulingState().isSomewhatScheduled());
        line.getSchedulingState().schedule();
        assertTrue(order.getSchedulingState().isSomewhatScheduled());
    }

    @Test
    @Transactional
    public void testListing() {
        List<Order> orderList = orderDAO.getOrders();
        Order newOrder = createValidOrder();
        orderDAO.save(newOrder);
        assertThat(orderDAO.getOrders().size(), equalTo(orderList.size() + 1));
    }

    @Test
    @Transactional
    public void testRemove() {
        Order order = createValidOrder();
        orderModel.setPlanningState(createPlanningStateFor(order));
        orderModel.save();
        assertTrue(orderDAO.exists(order.getId()));
        orderModel.remove(order);
        assertFalse(orderDAO.exists(order.getId()));
    }

    @Test(expected = ValidationException.class)
    @Transactional
    public void shouldSendValidationExceptionIfEndDateIsBeforeThanStartingDate() throws ValidationException {
        Order order = createValidOrder();
        order.setDeadline(year(0));
        orderModel.setPlanningState(createPlanningStateFor(order));
        orderModel.save();
    }

    @Test
    @Transactional
    public void testFind() throws InstanceNotFoundException {
        Order order = createValidOrder();
        orderModel.setPlanningState(createPlanningStateFor(order));
        orderModel.save();
        assertThat(orderDAO.find(order.getId()), notNullValue());
    }

    @Test
    @Transactional
    public void testOrderPreserved() throws ValidationException, InstanceNotFoundException {
        final Order order = createValidOrder();
        orderModel.setPlanningState(createPlanningStateFor(order));
        final OrderElement[] containers = new OrderLineGroup[10];
        for (int i = 0; i < containers.length; i++) {
            containers[i] = adHocTransaction.runOnTransaction(new IOnTransaction<OrderLineGroup>() {
                @Override
                public OrderLineGroup execute() {
                    return OrderLineGroup.create();
                }
            });
            containers[i].setName("bla");
            containers[i].setCode("code-" + UUID.randomUUID());
            order.add(containers[i]);
        }
        OrderLineGroup container = (OrderLineGroup) containers[0];

        final OrderElement[] orderElements = new OrderElement[10];
        for (int i = 0; i < orderElements.length; i++) {
            OrderLine leaf = createValidLeaf("bla");
            orderElements[i] = leaf;
            container.add(leaf);
        }

        for (int i = 1; i < containers.length; i++) {
            OrderLineGroup orderLineGroup = (OrderLineGroup) containers[i];
            OrderLine leaf = createValidLeaf("foo");
            orderLineGroup.add(leaf);
        }

        orderModel.save();
        adHocTransaction.runOnTransaction(new IOnTransaction<Void>() {

            @Override
            public Void execute() {
                try {
                    Order reloaded = orderDAO.find(order.getId());
                    List<OrderElement> elements = reloaded.getOrderElements();

                    for (OrderElement orderElement : elements) {
                        assertThat(orderElement.getIndirectAdvanceAssignments().size(), equalTo(2));
                    }

                    for (int i = 0; i < containers.length; i++) {
                        assertThat(elements.get(i).getId(), equalTo(containers[i].getId()));
                    }
                    OrderLineGroup container = (OrderLineGroup) reloaded.getOrderElements().iterator().next();
                    List<OrderElement> children = container.getChildren();

                    for (int i = 0; i < orderElements.length; i++) {
                        assertThat(children.get(i).getId(), equalTo(orderElements[i].getId()));
                    }

                    for (int i = 1; i < containers.length; i++) {
                        OrderLineGroup orderLineGroup = (OrderLineGroup) containers[i];
                        assertThat(orderLineGroup.getChildren().size(), equalTo(1));
                    }
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

        });

        orderModel.remove(order);
    }

    private OrderLine createValidLeaf(String parameter) {
        OrderLine result = OrderLine.create();
        result.setName(parameter);
        result.setCode("code-" + UUID.randomUUID());

        HoursGroup hoursGroup = HoursGroup.create(result);
        hoursGroup.setCode("hoursGroupCode-" + UUID.randomUUID());
        hoursGroup.setWorkingHours(0);
        result.addHoursGroup(hoursGroup);

        return result;
    }

    @Test
    public void testAddingOrderElement() {

        defaultAdvanceTypesBootstrapListener.loadRequiredData();
        configurationBootstrap.loadRequiredData();
        scenariosBootstrap.loadRequiredData();

        final Order order = createValidOrder();
        orderModel.setPlanningState(createPlanningStateFor(order));

        OrderLineGroup container = adHocTransaction.runOnTransaction(new IOnTransaction<OrderLineGroup>() {
            @Override
            public OrderLineGroup execute() {
                return OrderLineGroup.create();
            }
        });

        order.add(container);
        container.setName("bla");
        container.setCode("code-" + UUID.randomUUID());
        OrderLine leaf = OrderLine.create();
        leaf.setName("leaf");
        leaf.setCode("code-" + UUID.randomUUID());
        container.add(leaf);
        HoursGroup hoursGroup = HoursGroup.create(leaf);
        hoursGroup.setCode("hoursGroupName");
        hoursGroup.setWorkingHours(3);
        leaf.addHoursGroup(hoursGroup);
        orderModel.save();

        adHocTransaction.runOnTransaction(new IOnTransaction<Void>() {

            @Override
            public Void execute() {
                try {
                    Order reloaded = orderDAO.find(order.getId());
                    assertFalse(order == reloaded);
                    assertThat(reloaded.getOrderElements().size(), equalTo(1));
                    OrderLineGroup containerReloaded = (OrderLineGroup) reloaded
                            .getOrderElements().get(0);
                    assertThat(containerReloaded.getHoursGroups().size(),
                            equalTo(1));
                    assertThat(containerReloaded.getChildren().size(),
                            equalTo(1));
                    OrderElement leaf = containerReloaded.getChildren().get(0);
                    assertThat(leaf.getHoursGroups().size(), equalTo(1));
                    orderModel.remove(order);
                } catch (InstanceNotFoundException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        });
    }

    @Test
    @Transactional
    public void testManyToManyHoursGroupCriterionMapping() {
        givenCriterion();
        final Order order = createValidOrder();
        orderModel.setPlanningState(createPlanningStateFor(order));

        OrderLine orderLine = OrderLine.create();
        orderLine.setName("Order element");
        orderLine.setCode("000000000");
        order.add(orderLine);

        HoursGroup hoursGroup = HoursGroup.create(orderLine);
        hoursGroup.setCode("hoursGroupName");
        hoursGroup.setWorkingHours(10);
        HoursGroup hoursGroup2 = HoursGroup.create(orderLine);
        hoursGroup2.setCode("hoursGroupName2");
        hoursGroup2.setWorkingHours(5);

        orderLine.addHoursGroup(hoursGroup);

        CriterionRequirement criterionRequirement = DirectCriterionRequirement.create(criterion);

        hoursGroup.addCriterionRequirement(criterionRequirement);

        orderModel.save();

        adHocTransaction.runOnTransaction(new IOnTransaction<Void>() {

            @Override
            public Void execute() {
                try {
                    sessionFactory.getCurrentSession().flush();
                    Order reloaded = orderDAO.find(order.getId());
                    List<OrderElement> orderElements = reloaded.getOrderElements();
                    assertThat(orderElements.size(), equalTo(1));

                    List<HoursGroup> hoursGroups = orderElements.get(0).getHoursGroups();
                    assertThat(hoursGroups.size(), equalTo(1));

                    Set<CriterionRequirement> criterionRequirements = hoursGroups.get(0).getCriterionRequirements();
                    assertThat(criterionRequirements.size(), equalTo(1));

                    Set<Criterion> criterions = hoursGroups.get(0).getValidCriterions();
                    assertThat(criterions.size(), equalTo(1));

                } catch (InstanceNotFoundException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        });

    }

    private void givenCriterion() throws ValidationException {
        this.criterion = adHocTransaction.runOnTransaction(new IOnTransaction<Criterion>() {
            @Override
            public Criterion execute() {
                CriterionType criterionType = CriterionType.create(
                        "test" + UUID.randomUUID(), "");
                criterionType.setResource(ResourceEnum.WORKER);
                criterionTypeDAO.save(criterionType);
                Criterion criterion = Criterion.create("Test"
                        + UUID.randomUUID(), criterionType);

                try {
                    criterionDAO.save(criterion);
                } catch (ValidationException e) {
                    throw new RuntimeException(e);
                }
                return criterion;
            }
        });

        this.criterion.dontPoseAsTransientObjectAnymore();
        this.criterion.getType().dontPoseAsTransientObjectAnymore();
    }

    @Test(expected = ValidationException.class)
    @Transactional
    public void testAtLeastOneHoursGroup() {
        Order order = createValidOrder();
        orderModel.setPlanningState(createPlanningStateFor(order));

        OrderLine orderLine = OrderLine.create();
        orderLine.setName(randomize("foo" + new Random().nextInt()));
        orderLine.setCode(randomize("000000000"));
        order.add(orderLine);

        orderModel.save();
    }

    private static String randomize(String original) {
        return original + new Random().nextInt();
    }

}

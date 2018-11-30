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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.libreplan.business.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_FILE;
import static org.libreplan.web.WebappGlobalNames.WEBAPP_SPRING_CONFIG_FILE;
import static org.libreplan.web.WebappGlobalNames.WEBAPP_SPRING_SECURITY_CONFIG_FILE;
import static org.libreplan.web.test.WebappGlobalNames.WEBAPP_SPRING_CONFIG_TEST_FILE;
import static org.libreplan.web.test.WebappGlobalNames.WEBAPP_SPRING_SECURITY_CONFIG_TEST_FILE;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libreplan.business.IDataBootstrap;
import org.libreplan.business.advance.bootstrap.PredefinedAdvancedTypes;
import org.libreplan.business.advance.entities.AdvanceType;
import org.libreplan.business.advance.entities.DirectAdvanceAssignment;
import org.libreplan.business.advance.exceptions.DuplicateAdvanceAssignmentForOrderElementException;
import org.libreplan.business.advance.exceptions.DuplicateValueTrueReportGlobalAdvanceException;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.labels.entities.Label;
import org.libreplan.business.labels.entities.LabelType;
import org.libreplan.business.materials.entities.Material;
import org.libreplan.business.materials.entities.MaterialAssignment;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.orders.entities.OrderLine;
import org.libreplan.business.orders.entities.OrderLineGroup;
import org.libreplan.business.qualityforms.entities.QualityForm;
import org.libreplan.business.requirements.entities.CriterionRequirement;
import org.libreplan.business.requirements.entities.DirectCriterionRequirement;
import org.libreplan.business.requirements.entities.IndirectCriterionRequirement;
import org.libreplan.business.resources.bootstrap.ICriterionsBootstrap;
import org.libreplan.business.resources.daos.ICriterionDAO;
import org.libreplan.business.resources.daos.ICriterionTypeDAO;
import org.libreplan.business.resources.daos.IResourceDAO;
import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.resources.entities.CriterionType;
import org.libreplan.business.scenarios.bootstrap.PredefinedScenarios;
import org.libreplan.business.scenarios.entities.OrderVersion;
import org.libreplan.business.scenarios.entities.Scenario;
import org.libreplan.business.templates.entities.OrderElementTemplate;
import org.libreplan.business.workreports.daos.IWorkReportDAO;
import org.libreplan.business.workreports.entities.WorkReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests for {@link OrderElementTreeModel}.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        BUSINESS_SPRING_CONFIG_FILE,
        WEBAPP_SPRING_CONFIG_FILE, WEBAPP_SPRING_CONFIG_TEST_FILE,
        WEBAPP_SPRING_SECURITY_CONFIG_FILE, WEBAPP_SPRING_SECURITY_CONFIG_TEST_FILE })
public class OrderElementTreeModelTest {

    private static final BigDecimal HUNDRED = new BigDecimal(100);

    @Resource
    private IDataBootstrap defaultAdvanceTypesBootstrapListener;

    @Resource
    private IDataBootstrap scenariosBootstrap;

    @Resource
    private ICriterionsBootstrap criterionsBootstrap;

    @Resource
    private IDataBootstrap configurationBootstrap;

    @Autowired
    private ICriterionDAO criterionDAO;

    @Autowired
    private IWorkReportDAO workReportDAO;

    @Autowired
    private IResourceDAO resourceDAO;

    @Autowired
    private ICriterionTypeDAO criterionTypeDAO;

    private Order order;

    private OrderElementTreeModel model;

    private Criterion criterion, criterion2, criterion3;

    private AdvanceType advanceType, advanceType2, advanceType3;

    private MaterialAssignment materialAssignment;

    private Label label, label2;

    private QualityForm qualityForm;

    private OrderElementTemplate template;

    private OrderLineGroup container, container2;

    private OrderLine element, element2, element3;

    @Before
    public void loadRequiredData() {
        cleanCriteria(workReportDAO, resourceDAO, criterionTypeDAO);

        // Load data
        configurationBootstrap.loadRequiredData();
        defaultAdvanceTypesBootstrapListener.loadRequiredData();
        scenariosBootstrap.loadRequiredData();
        criterionsBootstrap.loadRequiredData();

        givenOrder();
        givenModel();
    }

    public static void cleanCriteria(IWorkReportDAO workReportDAO, IResourceDAO resourceDAO,
                                     ICriterionTypeDAO criterionTypeDAO) {
        try {
            List<WorkReport> reports = workReportDAO.findAll();
            for (WorkReport each : reports) {
                workReportDAO.remove(each.getId());
            }

            List<org.libreplan.business.resources.entities.Resource> resources = resourceDAO.findAll();
            for (org.libreplan.business.resources.entities.Resource each : resources) {
                resourceDAO.remove(each.getId());
            }

            List<CriterionType> types = criterionTypeDAO.findAll();
            for (CriterionType each : types) {
                criterionTypeDAO.remove(each.getId());
            }

        } catch (InstanceNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void givenOrder() {
        order = Order.create();
        order.setName("order");
        Scenario scenario = PredefinedScenarios.MASTER.getScenario();
        OrderVersion result = OrderVersion.createInitialVersion(scenario);
        order.setVersionForScenario(scenario, result);
        order.useSchedulingDataFor(scenario);
    }

    private void givenModel() {
        model = new OrderElementTreeModel(order);
    }

    private void addCriterionRequirement(OrderElement orderElement) {
        criterion = criterionDAO.findByNameAndType("Europe", "LOCATION").get(0);
        DirectCriterionRequirement directCriterionRequirement = DirectCriterionRequirement.create(criterion);
        orderElement.addCriterionRequirement(directCriterionRequirement);
    }

    private void addAnotherCriterionRequirement(OrderElement orderElement) {
        criterion2 = criterionDAO.findByNameAndType("Manager", "CATEGORY").get(0);
        DirectCriterionRequirement directCriterionRequirement = DirectCriterionRequirement.create(criterion2);
        orderElement.addCriterionRequirement(directCriterionRequirement);
    }

    private void addAnotherDifferentCriterionRequirement(OrderElement orderElement) {
        criterion3 = criterionDAO.findByNameAndType("Asia", "LOCATION").get(0);
        DirectCriterionRequirement directCriterionRequirement = DirectCriterionRequirement.create(criterion3);
        orderElement.addCriterionRequirement(directCriterionRequirement);
    }

    private void addDirectAdvanceAssignment(OrderElement orderElement)
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {
        addDirectAdvanceAssignment(orderElement, false);
    }

    private void addDirectAdvanceAssignment(OrderElement orderElement, boolean spread)
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        DirectAdvanceAssignment directAdvanceAssignment = DirectAdvanceAssignment.create(spread, HUNDRED);
        advanceType = PredefinedAdvancedTypes.PERCENTAGE.getType();
        directAdvanceAssignment.setAdvanceType(advanceType);
        orderElement.addAdvanceAssignment(directAdvanceAssignment);
    }

    private void addAnotherDirectAdvanceAssignment(OrderElement orderElement)
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        DirectAdvanceAssignment directAdvanceAssignment = DirectAdvanceAssignment.create(false, HUNDRED);
        advanceType2 = PredefinedAdvancedTypes.UNITS.getType();
        directAdvanceAssignment.setAdvanceType(advanceType2);
        orderElement.addAdvanceAssignment(directAdvanceAssignment);
    }

    private void addAnotherDifferentDirectAdvanceAssignment(OrderElement orderElement)
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        DirectAdvanceAssignment directAdvanceAssignment = DirectAdvanceAssignment.create(false, HUNDRED);
        advanceType3 = PredefinedAdvancedTypes.SUBCONTRACTOR.getType();
        directAdvanceAssignment.setAdvanceType(advanceType3);
        orderElement.addAdvanceAssignment(directAdvanceAssignment);
    }

    private void addLabel(OrderElement orderElement) {
        label = Label.create("label");
        LabelType.create("label-type").addLabel(label);
        orderElement.addLabel(label);
    }

    private void addSameLabel(OrderElement orderElement) {
        orderElement.addLabel(label);
    }

    private void addAnotherLabel(OrderElement orderElement) {
        label2 = Label.create("label2");
        LabelType.create("label-type-2").addLabel(label2);
        orderElement.addLabel(label2);
    }

    private void addMaterialAssignment(OrderElement orderElement) {
        materialAssignment = MaterialAssignment.create(
                Material.createUnvalidated("material-code", "material-description", HUNDRED, false));

        orderElement.addMaterialAssignment(materialAssignment);
    }

    private void addQualityForm(OrderElement element) {
        qualityForm = QualityForm.create("quality-form-name", "quality-form-description");
        element.addTaskQualityForm(qualityForm);
    }

    private void addTemplate(OrderElement element) {
        template = createNiceMock(OrderElementTemplate.class);
        expect(template.getName()).andReturn("order-element-template-name").anyTimes();
        replay(template);
        element.initializeTemplate(template);
    }

    @Test
    @Transactional
    public void checkAddElementWithCriteriaAndAdvancesOnParent()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        addCriterionRequirement(order);
        addDirectAdvanceAssignment(order);

        model.addElement("element", 100);

        assertThat(order.getDirectAdvanceAssignments().size(), equalTo(1));
        assertTrue(order.getIndirectAdvanceAssignments().isEmpty());

        OrderLine element = (OrderLine) model.getRoot().getChildren().get(0);
        assertTrue(element.getDirectAdvanceAssignments().isEmpty());

        assertThat(element.getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(element.getCriterionRequirements().iterator().next(), criterion);

        assertThat(element.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(element.getHoursGroups().get(0).getCriterionRequirements().iterator().next(), criterion);
    }

    private static void assertDirectCriterion(CriterionRequirement criterionRequirement, Criterion criterion) {
        assertCriterion(criterionRequirement, criterion, true);
    }

    private static void assertIndirectCriterion(CriterionRequirement criterionRequirement, Criterion criterion) {
        assertCriterion(criterionRequirement, criterion, false);
    }

    private static void assertCriterion(CriterionRequirement criterionRequirement,
                                        Criterion criterion, boolean direct) {
        if ( direct ) {
            assertTrue(criterionRequirement instanceof DirectCriterionRequirement);
        } else {
            assertTrue(criterionRequirement instanceof IndirectCriterionRequirement);
        }

        assertTrue(criterionRequirement.getCriterion().isEquivalent(criterion));
    }

    @Test
    @Transactional
    public void checkRemoveElementWithCriteriaAndAdvancesOnParent()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        model.addElement("element", 100);

        addCriterionRequirement(order);
        addDirectAdvanceAssignment(order);

        OrderLine element = (OrderLine) order.getChildren().get(0);

        model.removeNode(element);
        assertTrue(order.getChildren().isEmpty());
        assertThat(order.getDirectAdvanceAssignments().size(), equalTo(1));
        assertTrue(order.getIndirectAdvanceAssignments().isEmpty());
        assertFalse(order.getCriterionRequirements().isEmpty());
    }

    @Test
    @Transactional
    public void checkRemoveElementWithCriteriaAndAdvancesOnChild()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        model.addElement("element", 100);
        OrderLine element = (OrderLine) order.getChildren().get(0);

        addCriterionRequirement(element);
        addDirectAdvanceAssignment(element);

        assertNotNull(order.getIndirectAdvanceAssignment(advanceType));

        model.removeNode(element);
        assertTrue(order.getChildren().isEmpty());
        assertTrue(order.getDirectAdvanceAssignments().isEmpty());
        assertTrue(order.getIndirectAdvanceAssignments().isEmpty());
        assertTrue(order.getCriterionRequirements().isEmpty());
    }

    @Test
    @Transactional
    public void checkAddCriterionOnChild()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        model.addElement("element", 100);
        OrderLine element = (OrderLine) model.getRoot().getChildren().get(0);

        addCriterionRequirement(element);

        assertTrue(order.getCriterionRequirements().isEmpty());

        assertThat(element.getCriterionRequirements().size(), equalTo(1));
        assertDirectCriterion(element.getCriterionRequirements().iterator().next(), criterion);

        assertThat(element.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(element.getHoursGroups().get(0).getCriterionRequirements().iterator().next(), criterion);
    }

    @Test
    @Transactional
    public void checkAddCriterionOnParent()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        model.addElement("element", 100);
        OrderLine element = (OrderLine) model.getRoot().getChildren().get(0);

        addCriterionRequirement(order);

        assertThat(order.getCriterionRequirements().size(), equalTo(1));
        assertDirectCriterion(order.getCriterionRequirements().iterator().next(), criterion);

        assertThat(element.getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(element.getCriterionRequirements().iterator().next(), criterion);
        assertThat(element.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(element.getHoursGroups().get(0).getCriterionRequirements().iterator().next(), criterion);
    }

    @Test
    @Transactional
    public void checkAddAssignmentOnChild()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        model.addElement("element", 100);
        OrderLine element = (OrderLine) model.getRoot().getChildren().get(0);

        addDirectAdvanceAssignment(element);
        assertTrue(order.getDirectAdvanceAssignments().isEmpty());
        assertFalse(order.getIndirectAdvanceAssignments().isEmpty());
        assertNotNull(order.getIndirectAdvanceAssignment(advanceType));
        assertThat(element.getDirectAdvanceAssignments().size(), equalTo(1));
    }

    @Test
    @Transactional
    public void checkAddAdvanceOnParent()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        model.addElement("element", 100);
        OrderLine element = (OrderLine) model.getRoot().getChildren().get(0);

        addDirectAdvanceAssignment(order);

        assertThat(order.getDirectAdvanceAssignments().size(), equalTo(1));
        assertNotNull(order.getDirectAdvanceAssignmentByType(advanceType));
        assertTrue(element.getDirectAdvanceAssignments().isEmpty());
    }

    @Test
    @Transactional
    public void checkAddElementOnOrderLineWithCriteriaAndAdvances()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        addCriterionRequirement(order);

        model.addElement("element", 100);
        OrderLine element = (OrderLine) model.getRoot().getChildren().get(0);

        addAnotherCriterionRequirement(element);
        addDirectAdvanceAssignment(element);

        model.addElementAt(element, "element2", 50);

        assertNotNull(order.getIndirectAdvanceAssignment(advanceType));
        assertThat(order.getCriterionRequirements().size(), equalTo(1));
        assertDirectCriterion(order.getCriterionRequirements().iterator().next(), criterion);
        assertThat(order.getWorkHours(), equalTo(150));

        OrderLineGroup container = (OrderLineGroup) order.getChildren().get(0);
        assertThat(container.getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(container.getCriterionRequirements().iterator().next(), criterion);
        assertNotNull(container.getIndirectAdvanceAssignment(advanceType));
        assertThat(container.getWorkHours(), equalTo(150));

        assertThat(container.getChildren().size(), equalTo(2));

        for (OrderElement each : container.getChildren()) {

            if ( each.getName().equals("") ) {
                assertThat(each.getCriterionRequirements().size(), equalTo(2));
                assertThat(each.getDirectAdvanceAssignments().size(), equalTo(1));
                assertNotNull(each.getDirectAdvanceAssignmentByType(advanceType));
                assertThat(each.getWorkHours(), equalTo(100));
                assertThat(element.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(2));

            } else if ( each.getName().equals("element2") ) {
                assertThat(each.getCriterionRequirements().size(), equalTo(1));
                assertIndirectCriterion(each.getCriterionRequirements().iterator().next(), criterion);
                assertTrue(each.getDirectAdvanceAssignments().isEmpty());
                assertThat(each.getWorkHours(), equalTo(50));
                assertThat(each.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(1));
                assertIndirectCriterion(
                        each.getHoursGroups().get(0).getCriterionRequirements().iterator().next(),
                        criterion);
            } else {
                fail("Unexpected OrderElement name: " + each.getName());
            }
        }
    }

    @Test
    @Transactional
    public void checkAddElementOnOrderLineGroupWithCriteriaAndAdvances()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        addCriterionRequirement(order);

        model.addElement("element", 100);
        OrderLine element = (OrderLine) order.getChildren().get(0);

        model.addElementAt(element, "element2", 50);
        OrderLineGroup container = (OrderLineGroup) order.getChildren().get(0);
        container.setName("container");

        addAnotherCriterionRequirement(container);
        addDirectAdvanceAssignment(container);

        model.addElementAt(container, "element3", 150);

        assertThat(order.getWorkHours(), equalTo(300));

        assertThat(container.getChildren().size(), equalTo(3));

        for (OrderElement each : container.getChildren()) {
            if ( each.getName().equals("element3") ) {
                assertThat(each.getCriterionRequirements().size(), equalTo(2));

                for (CriterionRequirement criterionRequirement : each.getCriterionRequirements()) {
                    assertTrue(criterionRequirement instanceof IndirectCriterionRequirement);
                }
                assertTrue(each.getDirectAdvanceAssignments().isEmpty());
                assertThat(each.getWorkHours(), equalTo(150));
                assertThat(element.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(2));
            }
        }
    }

    @Test
    @Transactional
    public void checkRemoveElementOnOnlyOrderLineWithCriteriaAndAdvances()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        model.addElement("element", 100);
        OrderLine element = (OrderLine) order.getChildren().get(0);
        model.addElementAt(element, "element2", 50);

        OrderLineGroup container = (OrderLineGroup) order.getChildren().get(0);
        model.removeNode(container.getChildren().iterator().next());

        element = (OrderLine) container.getChildren().get(0);

        addCriterionRequirement(element);
        addDirectAdvanceAssignment(element);

        // * infoComponent (code, name, description)
        String name = "container";
        container.setName(name);
        String code = "code";
        container.setCode(code);
        String description = "description";
        container.setDescription(description);

        // * initDate
        Date date = new Date();
        container.setInitDate(date);
        // * deadline
        container.setDeadline(date);

        // * directAdvanceAssignments
        addAnotherDirectAdvanceAssignment(container);

        // * materialAssignments
        addMaterialAssignment(container);

        // * labels
        addLabel(container);

        // * taskQualityForms
        addQualityForm(container);

        // * criterionRequirements
        addAnotherCriterionRequirement(container);

        // * template
        addTemplate(container);

        // * externalCode
        String externalCode = "external-code";
        container.setExternalCode(externalCode);

        model.removeNode(element);

        assertTrue(order.getDirectAdvanceAssignments().isEmpty());
        assertNull(order.getIndirectAdvanceAssignment(advanceType));
        assertNotNull(order.getIndirectAdvanceAssignment(advanceType2));
        assertTrue(order.getCriterionRequirements().isEmpty());
        assertThat(order.getWorkHours(), equalTo(0));

        element = (OrderLine) order.getChildren().get(0);
        assertThat(element.getWorkHours(), equalTo(0));

        // * infoComponent (code, name, description)
        assertThat(element.getName(), equalTo(name));
        assertThat(element.getCode(), equalTo(code));
        assertThat(element.getDescription(), equalTo(description));

        // * initDate
        assertThat(element.getInitDate(), equalTo(date));
        // * deadline
        assertThat(element.getDeadline(), equalTo(date));

        // * directAdvanceAssignments
        assertThat(element.getDirectAdvanceAssignments().size(), equalTo(1));
        assertNotNull(element.getDirectAdvanceAssignmentByType(advanceType2));

        assertThat(element.getDirectAdvanceAssignmentByType(advanceType2).getOrderElement(),
                equalTo((OrderElement) element));

        // * materialAssignments
        assertThat(element.getMaterialAssignments().size(), equalTo(1));

        assertThat(element.getMaterialAssignments().iterator().next().getMaterial(),
                equalTo(materialAssignment.getMaterial()));

        assertThat(element.getMaterialAssignments().iterator().next().getOrderElement(),
                equalTo((OrderElement) element));

        // * labels
        assertThat(element.getLabels().size(), equalTo(1));
        assertThat(element.getLabels().iterator().next(), equalTo(label));
        assertThat(element.getLabels().iterator().next().getType(), equalTo(label.getType()));

        // * taskQualityForms
        assertThat(element.getQualityForms().size(), equalTo(1));
        assertThat(element.getQualityForms().iterator().next(), equalTo(qualityForm));
        assertThat(element.getTaskQualityForms().iterator().next().getOrderElement(), equalTo((OrderElement) element));

        // * criterionRequirements
        assertThat(element.getCriterionRequirements().size(), equalTo(1));
        assertDirectCriterion(element.getCriterionRequirements().iterator().next(), criterion2);

        assertThat(element.getCriterionRequirements().iterator().next().getOrderElement(),
                equalTo((OrderElement) element));

        assertThat(element.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(1));

        assertIndirectCriterion(
                element.getHoursGroups().get(0).getCriterionRequirements().iterator().next(),
                criterion2);

        // * template
        assertNotNull(element.getTemplate());
        assertThat(element.getTemplate().getName(), equalTo(template.getName()));

        // * externalCode
        assertThat(element.getExternalCode(), equalTo(externalCode));
    }

    @Test
    @Transactional
    public void checkPreservationOfInvalidatedIndirectCriterionRequirementInToLeaf()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        addCriterionRequirement(order);

        model.addElement("element", 100);
        OrderLine element = (OrderLine) order.getChildren().get(0);
        model.addElementAt(element, "element2", 50);

        OrderLineGroup container = (OrderLineGroup) order.getChildren().get(0);
        model.removeNode(container.getChildren().iterator().next());

        IndirectCriterionRequirement indirectCriterionRequirement =
                (IndirectCriterionRequirement) container.getCriterionRequirements().iterator().next();

        assertTrue(indirectCriterionRequirement.getCriterion().isEquivalent(criterion));
        indirectCriterionRequirement.setValid(false);

        addAnotherCriterionRequirement(container);

        // This calls toLeaf in the container
        model.removeNode(container.getChildren().get(0));

        element = (OrderLine) order.getChildren().get(0);

        assertThat(element.getCriterionRequirements().size(), equalTo(2));

        for (CriterionRequirement each : element.getCriterionRequirements()) {
            if ( each.getCriterion().isEquivalent(criterion) ) {
                assertTrue(each instanceof IndirectCriterionRequirement);
                assertFalse(each.isValid());
            } else if ( each.getCriterion().isEquivalent(criterion2) ) {
                assertTrue(each instanceof DirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }

        assertThat(element.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(2));

        for (CriterionRequirement each : element.getHoursGroups().get(0).getCriterionRequirements()) {
            if ( each.getCriterion().isEquivalent(criterion) ) {
                assertTrue(each instanceof IndirectCriterionRequirement);
                assertFalse(each.isValid());
            } else if ( each.getCriterion().isEquivalent(criterion2) ) {
                assertTrue(each instanceof IndirectCriterionRequirement);
                assertTrue(each.isValid());
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }
    }

    @Test
    @Transactional
    public void checkIndentOrderLineWithCriteriaAndAdvances()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        model.addElement("element", 100);
        model.addElement("element2", 50);

        OrderLine element = null;
        OrderLine element2 = null;

        for (OrderElement each : order.getChildren()) {
            if ( each.getName().equals("element") ) {
                element = (OrderLine) each;
            } else if ( each.getName().equals("element2") ) {
                element2 = (OrderLine) each;
            }
        }

        addCriterionRequirement(element2);
        addDirectAdvanceAssignment(element2);

        model.indent(element2);

        assertTrue(order.getDirectAdvanceAssignments().isEmpty());
        assertThat(order.getIndirectAdvanceAssignments().size(), equalTo(2));
        assertNotNull(order.getIndirectAdvanceAssignment(advanceType));
        assertTrue(order.getCriterionRequirements().isEmpty());

        OrderLineGroup container = (OrderLineGroup) order.getChildren().get(0);
        assertTrue(container.getDirectAdvanceAssignments().isEmpty());
        assertThat(container.getIndirectAdvanceAssignments().size(), equalTo(2));
        assertNotNull(container.getIndirectAdvanceAssignment(advanceType));
        assertTrue(container.getCriterionRequirements().isEmpty());

        assertTrue(element.getDirectAdvanceAssignments().isEmpty());
        assertTrue(element.getCriterionRequirements().isEmpty());
        assertTrue(element.getHoursGroups().get(0).getCriterionRequirements().isEmpty());

        assertNotNull(element2.getAdvanceAssignmentByType(advanceType));
        assertThat(element2.getCriterionRequirements().size(), equalTo(1));
        assertDirectCriterion(element2.getCriterionRequirements().iterator().next(), criterion);
        assertThat(element2.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(1));

        assertIndirectCriterion(
                element2.getHoursGroups().get(0).getCriterionRequirements().iterator().next(),
                criterion);
    }

    @Test
    @Transactional
    public void checkIndentOnOrderLineWithCriteriaAndAdvances()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        model.addElement("element", 100);
        model.addElement("element2", 50);

        OrderLine element = null;
        OrderLine element2 = null;

        for (OrderElement each : order.getChildren()) {
            if ( each.getName().equals("element") ) {
                element = (OrderLine) each;
            } else if ( each.getName().equals("element2") ) {
                element2 = (OrderLine) each;
            }
        }

        addCriterionRequirement(element);
        addDirectAdvanceAssignment(element);

        model.indent(element2);

        assertTrue(order.getDirectAdvanceAssignments().isEmpty());
        assertNotNull(order.getIndirectAdvanceAssignment(advanceType));
        assertTrue(order.getCriterionRequirements().isEmpty());

        OrderLineGroup container = (OrderLineGroup) order.getChildren().get(0);
        assertTrue(container.getDirectAdvanceAssignments().isEmpty());
        assertNotNull(container.getIndirectAdvanceAssignment(advanceType));
        assertTrue(container.getCriterionRequirements().isEmpty());

        assertNotNull(element.getAdvanceAssignmentByType(advanceType));
        assertThat(element.getCriterionRequirements().size(), equalTo(1));
        assertDirectCriterion(element.getCriterionRequirements().iterator().next(), criterion);
        assertThat(element.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(1));

        assertIndirectCriterion(
                element.getHoursGroups().get(0).getCriterionRequirements().iterator().next(),
                criterion);

        assertTrue(element2.getDirectAdvanceAssignments().isEmpty());
        assertTrue(element2.getCriterionRequirements().isEmpty());
        assertTrue(element2.getHoursGroups().get(0).getCriterionRequirements().isEmpty());
    }

    @Test
    @Transactional
    public void checkIndentOnOrderLineGroupWithCriteriaAndAdvances()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        model.addElement("element", 100);
        model.addElementAt(order.getChildren().get(0), "element2", 50);
        model.addElement("element3", 30);

        OrderLineGroup container = null;
        OrderLine element3 = null;

        for (OrderElement each : order.getChildren()) {
            if ( each.getName().equals("element") ) {
                container = (OrderLineGroup) each;
            } else if ( each.getName().equals("element3") ) {
                element3 = (OrderLine) each;
            }
        }

        addCriterionRequirement(container);
        addDirectAdvanceAssignment(container);

        model.indent(element3);

        assertTrue(order.getDirectAdvanceAssignments().isEmpty());
        assertNotNull(order.getIndirectAdvanceAssignment(advanceType));
        assertTrue(order.getCriterionRequirements().isEmpty());

        assertNotNull(container.getAdvanceAssignmentByType(advanceType));
        assertThat(container.getCriterionRequirements().size(), equalTo(1));
        assertDirectCriterion(container.getCriterionRequirements().iterator().next(), criterion);

        assertTrue(element3.getDirectAdvanceAssignments().isEmpty());
        assertThat(element3.getCriterionRequirements().size(), equalTo(1));
        assertDirectCriterion(container.getCriterionRequirements().iterator().next(), criterion);
        assertThat(element3.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(1));

        assertIndirectCriterion(
                container.getHoursGroups().get(0).getCriterionRequirements().iterator().next(),
                criterion);
    }

    @Test
    @Transactional
    public void checkUnindentOrderLineWithCriteriaAndAdvances()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        model.addElement("element", 100);
        model.addElement("element2", 50);

        OrderLine element = null;
        OrderLine element2 = null;

        for (OrderElement each : order.getChildren()) {
            if ( each.getName().equals("element") ) {
                element = (OrderLine) each;
            } else if ( each.getName().equals("element2") ) {
                element2 = (OrderLine) each;
            }
        }

        model.indent(element2);

        addCriterionRequirement(element2);
        addDirectAdvanceAssignment(element2);
        addAnotherDirectAdvanceAssignment(element2);

        addAnotherDirectAdvanceAssignment(element);

        addAnotherCriterionRequirement(order);

        model.unindent(element2);

        assertTrue(order.getDirectAdvanceAssignments().isEmpty());
        assertNotNull(order.getIndirectAdvanceAssignment(advanceType));
        assertNotNull(order.getIndirectAdvanceAssignment(advanceType2));
        assertThat(order.getCriterionRequirements().size(), equalTo(1));

        OrderLineGroup container = (OrderLineGroup) order.getChildren().get(0);
        assertTrue(container.getDirectAdvanceAssignments().isEmpty());
        assertNull(container.getIndirectAdvanceAssignment(advanceType));
        assertNotNull(container.getIndirectAdvanceAssignment(advanceType2));
        assertThat(container.getCriterionRequirements().size(), equalTo(1));

        assertThat(element.getDirectAdvanceAssignments().size(), equalTo(1));
        assertNotNull(element.getAdvanceAssignmentByType(advanceType2));
        assertThat(element.getCriterionRequirements().size(), equalTo(1));
        assertThat(element.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(1));

        assertNotNull(element2.getAdvanceAssignmentByType(advanceType));
        assertNotNull(element2.getAdvanceAssignmentByType(advanceType2));
        assertThat(element2.getCriterionRequirements().size(), equalTo(2));

        for (CriterionRequirement each : element2.getCriterionRequirements()) {
            if ( each.getCriterion().isEquivalent(criterion) ) {
                assertTrue(each instanceof DirectCriterionRequirement);
            } else if ( each.getCriterion().isEquivalent(criterion2) ) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }

        for (CriterionRequirement each : element2.getHoursGroups().get(0).getCriterionRequirements()) {
            if ( (each.getCriterion().isEquivalent(criterion) || each.getCriterion().isEquivalent(criterion2)) ) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }
    }

    @Test
    @Transactional
    public void checkMoveOrderLineWithCriteriaAndAdvancesToOrderLineWithCriteriaAndAdvances()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        model.addElement("element", 100);
        model.addElement("element2", 50);

        OrderLine element = null;
        OrderLine element2 = null;

        for (OrderElement each : order.getChildren()) {
            if ( each.getName().equals("element") ) {
                element = (OrderLine) each;
            } else if ( each.getName().equals("element2") ) {
                element2 = (OrderLine) each;
            }
        }

        addCriterionRequirement(element);
        addDirectAdvanceAssignment(element);

        addAnotherCriterionRequirement(element2);
        addAnotherDirectAdvanceAssignment(element2);

        addAnotherDifferentCriterionRequirement(order);
        addAnotherDifferentDirectAdvanceAssignment(order);

        model.move(element2, element);

        assertThat(order.getDirectAdvanceAssignments().size(), equalTo(1));
        assertNotNull(order.getDirectAdvanceAssignmentByType(advanceType3));
        assertNotNull(order.getIndirectAdvanceAssignment(advanceType));
        assertNotNull(order.getIndirectAdvanceAssignment(advanceType2));
        assertThat(order.getCriterionRequirements().size(), equalTo(1));
        assertDirectCriterion(order.getCriterionRequirements().iterator().next(), criterion3);

        OrderLineGroup container = (OrderLineGroup) order.getChildren().get(0);
        assertTrue(container.getDirectAdvanceAssignments().isEmpty());
        assertNotNull(container.getIndirectAdvanceAssignment(advanceType));
        assertNotNull(container.getIndirectAdvanceAssignment(advanceType2));
        assertNull(container.getIndirectAdvanceAssignment(advanceType3));
        assertThat(container.getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(container.getCriterionRequirements().iterator().next(), criterion3);

        assertNotNull(element.getAdvanceAssignmentByType(advanceType));
        assertThat(element.getCriterionRequirements().size(), equalTo(2));

        for (CriterionRequirement each : element.getCriterionRequirements()) {
            if ( each.getCriterion().isEquivalent(criterion) ) {
                assertTrue(each instanceof DirectCriterionRequirement);
            } else if ( each.getCriterion().isEquivalent(criterion3) ) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }
        assertThat(element.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(2));

        for (CriterionRequirement each : element.getHoursGroups().get(0).getCriterionRequirements()) {
            if ( each.getCriterion().isEquivalent(criterion) || each.getCriterion().isEquivalent(criterion3) ) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }

        assertNotNull(element2.getAdvanceAssignmentByType(advanceType2));
        assertThat(element2.getCriterionRequirements().size(), equalTo(2));

        for (CriterionRequirement each : element2.getCriterionRequirements()) {
            if ( each.getCriterion().isEquivalent(criterion2) ) {
                assertTrue(each instanceof DirectCriterionRequirement);
            } else if ( each.getCriterion().isEquivalent(criterion3) ) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }
        assertThat(element2.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(2));

        for (CriterionRequirement each : element2.getHoursGroups().get(0).getCriterionRequirements()) {
            if ( each.getCriterion().isEquivalent(criterion2) || each.getCriterion().isEquivalent(criterion3) ) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }
    }

    @Test
    @Transactional
    public void checkMoveOrderLineWithCriteriaAndAdvancesToOrderLineGroupWithCriteriaAndAdvances()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        model.addElement("element", 100);
        model.addElementAt(order.getChildren().get(0), "element2", 50);

        OrderLineGroup container = (OrderLineGroup) order.getChildren().get(0);

        OrderLine element = null;
        OrderLine element2 = null;

        for (OrderElement each : container.getChildren()) {
            if ( each.getName().equals("") ) {
                element = (OrderLine) each;
            } else if ( each.getName().equals("element2") ) {
                element2 = (OrderLine) each;
            }
        }

        model.unindent(element2);

        addCriterionRequirement(container);
        addDirectAdvanceAssignment(container);

        addAnotherCriterionRequirement(element2);
        addAnotherDirectAdvanceAssignment(element2);

        addAnotherDifferentCriterionRequirement(order);
        addAnotherDifferentDirectAdvanceAssignment(order);

        model.move(element2, container);

        assertThat(order.getDirectAdvanceAssignments().size(), equalTo(1));
        assertNotNull(order.getDirectAdvanceAssignmentByType(advanceType3));
        assertNotNull(order.getIndirectAdvanceAssignment(advanceType));
        assertNotNull(order.getIndirectAdvanceAssignment(advanceType2));
        assertThat(order.getCriterionRequirements().size(), equalTo(1));
        assertDirectCriterion(order.getCriterionRequirements().iterator().next(), criterion3);

        assertThat(container.getDirectAdvanceAssignments().size(), equalTo(1));
        assertNotNull(container.getDirectAdvanceAssignmentByType(advanceType));
        assertNotNull(container.getIndirectAdvanceAssignment(advanceType2));
        assertNull(container.getIndirectAdvanceAssignment(advanceType3));
        assertThat(container.getCriterionRequirements().size(), equalTo(2));

        for (CriterionRequirement each : container.getCriterionRequirements()) {
            if ( each.getCriterion().isEquivalent(criterion3) ) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else if ( each.getCriterion().isEquivalent(criterion) ) {
                assertTrue(each instanceof DirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }

        assertTrue(element.getDirectAdvanceAssignments().isEmpty());
        assertThat(element.getCriterionRequirements().size(), equalTo(2));

        for (CriterionRequirement each : element.getCriterionRequirements()) {
            if ( each.getCriterion().isEquivalent(criterion) || each.getCriterion().isEquivalent(criterion3) ) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }
        assertThat(element.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(2));

        for (CriterionRequirement each : element.getHoursGroups().get(0).getCriterionRequirements()) {
            if ( each.getCriterion().isEquivalent(criterion) || each.getCriterion().isEquivalent(criterion3) ) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }

        assertNotNull(element2.getAdvanceAssignmentByType(advanceType2));
        assertThat(element2.getCriterionRequirements().size(), equalTo(3));

        for (CriterionRequirement each : element2.getCriterionRequirements()) {
            if ( each.getCriterion().isEquivalent(criterion2) ) {
                assertTrue(each instanceof DirectCriterionRequirement);
            } else if ( each.getCriterion().isEquivalent(criterion) || each.getCriterion().isEquivalent(criterion3) ) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }
        assertThat(element2.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(3));

        for (CriterionRequirement each : element2.getHoursGroups().get(0).getCriterionRequirements()) {
            if ( each.getCriterion().isEquivalent(criterion) ||
                    each.getCriterion().isEquivalent(criterion2) ||
                    each.getCriterion().isEquivalent(criterion3) ) {

                assertTrue(each instanceof IndirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }
    }

    /**
     * This will create the following tree:
     *
     * <pre>
     *     order
     *       |-- container
     *             |-- element
     *       |-- element2
     * </pre>
     */
    private void createTreeWithContainerAndTask() {
        model.addElement("element", 100);
        model.addElementAt(order.getChildren().get(0), "element2", 50);

        container = (OrderLineGroup) order.getChildren().get(0);

        element = null;
        element2 = null;
        for (OrderElement each : container.getChildren()) {
            if (each.getName().equals("")) {
                element = (OrderLine) each;
            } else if (each.getName().equals("element2")) {
                element2 = (OrderLine) each;
            }
        }

        model.unindent(element2);
    }

    @Test
    @Transactional
    public void checkMoveOrderLineWithLabelToOrderLineGroupWithSameLabel() {
        createTreeWithContainerAndTask();

        addLabel(container);
        addSameLabel(element2);

        addAnotherLabel(element2);

        model.move(element2, container);

        assertTrue(order.getLabels().isEmpty());

        assertThat(container.getLabels().size(), equalTo(1));
        assertThat(container.getLabels().iterator().next(), equalTo(label));

        assertTrue(element.getLabels().isEmpty());
        assertThat(element2.getLabels().size(), equalTo(1));
        assertThat(element2.getLabels().iterator().next(), equalTo(label2));
    }

    @Test
    @Transactional
    public void checkMoveOrderLineWithLabelToOrderLineInGroupWithSameLabel() {
        createTreeWithContainerAndTask();

        addLabel(container);
        addSameLabel(element2);

        addAnotherLabel(element2);

        model.move(element2, element);

        assertTrue(order.getLabels().isEmpty());

        assertThat(container.getLabels().size(), equalTo(1));
        assertThat(container.getLabels().iterator().next(), equalTo(label));

        assertTrue(element.getLabels().isEmpty());
        assertThat(element2.getLabels().size(), equalTo(1));
        assertThat(element2.getLabels().iterator().next(), equalTo(label2));
    }

    /**
     * This will create the following tree:
     *
     * <pre>
     *     order
     *       |-- container
     *             |-- element
     *       |-- container2
     *             |-- element2
     *             |-- element3
     * </pre>
     */
    private void createTreeWithContainerAndContainer() {
        model.addElement("element", 100);
        model.addElementAt(order.getChildren().get(0), "element2", 50);

        container = (OrderLineGroup) order.getChildren().get(0);
        container.setName("container");

        element = null;
        element2 = null;
        for (OrderElement each : container.getChildren()) {
            if (each.getName().equals("element")) {
                element = (OrderLine) each;
            } else if (each.getName().equals("element2")) {
                element2 = (OrderLine) each;
            }
        }

        model.unindent(element2);

        model.addElementAt(element2, "element3", 200);

        container2 = null;
        for (OrderElement each : order.getChildren()) {
            if (each.getName().equals("container")) {
                container = (OrderLineGroup) each;
            } else {
                container2 = (OrderLineGroup) each;
            }
        }

        element = (OrderLine) container.getChildren().get(0);

        element3 = null;
        for (OrderElement each : container2.getChildren()) {
            if (each.getName().equals("element2")) {
                element2 = (OrderLine) each;
            } else if (each.getName().equals("element3")) {
                element3 = (OrderLine) each;
            }
        }
    }

    @Test
    @Transactional
    public void checkMoveOrderGroupLineWithLabelToOrderLineGroupWithSameLabel() {
        createTreeWithContainerAndContainer();

        addLabel(container);
        addSameLabel(container2);

        addAnotherLabel(container2);

        model.move(container2, container);

        assertTrue(order.getLabels().isEmpty());

        assertThat(container.getLabels().size(), equalTo(1));
        assertThat(container.getLabels().iterator().next(), equalTo(label));

        assertTrue(element.getLabels().isEmpty());

        assertThat(container2.getLabels().size(), equalTo(1));
        assertThat(container2.getLabels().iterator().next(), equalTo(label2));
        assertTrue(element2.getLabels().isEmpty());
        assertTrue(element3.getLabels().isEmpty());
    }

    @Test
    @Transactional
    public void checkMoveOrderGroupLineWithLabelToOrderLineInGroupWithSameLabel() {
        createTreeWithContainerAndContainer();

        addLabel(container);
        addSameLabel(container2);

        addAnotherLabel(container2);

        model.move(container2, element);

        assertTrue(order.getLabels().isEmpty());

        assertThat(container.getLabels().size(), equalTo(1));
        assertThat(container.getLabels().iterator().next(), equalTo(label));

        assertTrue(element.getLabels().isEmpty());

        assertThat(container2.getLabels().size(), equalTo(1));
        assertThat(container2.getLabels().iterator().next(), equalTo(label2));
        assertTrue(element2.getLabels().isEmpty());
        assertTrue(element3.getLabels().isEmpty());
    }

    @Test
    @Transactional
    public void checkMoveOrderLineGroupWithLabelOnChildToOrderLineGroupWithSameLabel() {
        createTreeWithContainerAndContainer();

        addLabel(container);
        addSameLabel(element2);

        addAnotherLabel(element2);

        model.move(container2, container);

        assertTrue(order.getLabels().isEmpty());

        assertThat(container.getLabels().size(), equalTo(1));
        assertThat(container.getLabels().iterator().next(), equalTo(label));

        assertTrue(element.getLabels().isEmpty());

        assertTrue(container2.getLabels().isEmpty());
        assertThat(element2.getLabels().size(), equalTo(1));
        assertThat(element2.getLabels().iterator().next(), equalTo(label2));
        assertTrue(element3.getLabels().isEmpty());
    }

    @Test
    @Transactional
    public void checkMoveOrderLineGroupWithLabelOnChildToOrderLineInGroupWithSameLabel() {
        createTreeWithContainerAndContainer();

        addLabel(container);
        addSameLabel(element2);

        addAnotherLabel(element2);

        model.move(container2, element);

        assertTrue(order.getLabels().isEmpty());

        assertThat(container.getLabels().size(), equalTo(1));
        assertThat(container.getLabels().iterator().next(), equalTo(label));

        assertTrue(element.getLabels().isEmpty());

        assertTrue(container2.getLabels().isEmpty());
        assertThat(element2.getLabels().size(), equalTo(1));
        assertThat(element2.getLabels().iterator().next(), equalTo(label2));
        assertTrue(element3.getLabels().isEmpty());
    }

    @Test
    @Transactional
    public void checkMoveOrderLineWithAdvanceToOrderLineGroupWithSameAdvanceType()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        createTreeWithContainerAndTask();

        addDirectAdvanceAssignment(container);
        addDirectAdvanceAssignment(element2);

        addAnotherDirectAdvanceAssignment(element2);

        model.move(element2, container);

        assertTrue(order.getDirectAdvanceAssignments().isEmpty());
        assertThat(order.getIndirectAdvanceAssignments().size(), equalTo(3));
        assertNotNull(order.getAdvanceAssignmentByType(advanceType));
        assertNotNull(order.getAdvanceAssignmentByType(advanceType2));

        assertThat(container.getDirectAdvanceAssignments().size(), equalTo(1));
        assertThat(container.getIndirectAdvanceAssignments().size(), equalTo(2));
        assertNotNull(container.getAdvanceAssignmentByType(advanceType));
        assertNull(container.getIndirectAdvanceAssignment(advanceType));
        assertNotNull(container.getIndirectAdvanceAssignment(advanceType2));

        assertTrue(element.getDirectAdvanceAssignments().isEmpty());
        assertNull(element.getAdvanceAssignmentByType(advanceType));
        assertNull(element.getAdvanceAssignmentByType(advanceType2));

        assertFalse(element2.getDirectAdvanceAssignments().isEmpty());
        assertNull(element2.getAdvanceAssignmentByType(advanceType));
        assertNotNull(element2.getAdvanceAssignmentByType(advanceType2));
    }

    @Test
    @Transactional
    public void checkMoveOrderLineWithAdvanceToOrderLineInGroupWithSameAdvanceType()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        createTreeWithContainerAndTask();

        addDirectAdvanceAssignment(container);
        addDirectAdvanceAssignment(element2);

        addAnotherDirectAdvanceAssignment(element2);

        model.move(element2, element);

        assertTrue(order.getDirectAdvanceAssignments().isEmpty());
        assertThat(order.getIndirectAdvanceAssignments().size(), equalTo(3));
        assertNotNull(order.getAdvanceAssignmentByType(advanceType));
        assertNotNull(order.getAdvanceAssignmentByType(advanceType2));

        assertThat(container.getDirectAdvanceAssignments().size(), equalTo(1));
        assertThat(container.getIndirectAdvanceAssignments().size(), equalTo(2));
        assertNotNull(container.getAdvanceAssignmentByType(advanceType));
        assertNull(container.getIndirectAdvanceAssignment(advanceType));
        assertNotNull(container.getIndirectAdvanceAssignment(advanceType2));

        assertTrue(element.getDirectAdvanceAssignments().isEmpty());
        assertNull(element.getAdvanceAssignmentByType(advanceType));
        assertNull(element.getAdvanceAssignmentByType(advanceType2));

        assertFalse(element2.getDirectAdvanceAssignments().isEmpty());
        assertNull(element2.getAdvanceAssignmentByType(advanceType));
        assertNotNull(element2.getAdvanceAssignmentByType(advanceType2));
    }

    @Test
    @Transactional
    public void checkMoveOrderLineGroupWithAdvanceToOrderLineGroupWithSameAdvance()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        createTreeWithContainerAndContainer();

        addDirectAdvanceAssignment(container);
        addDirectAdvanceAssignment(container2);

        addAnotherDirectAdvanceAssignment(container2);

        model.move(container2, container);

        assertTrue(order.getDirectAdvanceAssignments().isEmpty());
        assertThat(order.getIndirectAdvanceAssignments().size(), equalTo(3));
        assertNotNull(order.getAdvanceAssignmentByType(advanceType));
        assertNotNull(order.getAdvanceAssignmentByType(advanceType2));

        assertThat(container.getDirectAdvanceAssignments().size(), equalTo(1));
        assertThat(container.getIndirectAdvanceAssignments().size(), equalTo(2));
        assertNotNull(container.getAdvanceAssignmentByType(advanceType));
        assertNull(container.getIndirectAdvanceAssignment(advanceType));
        assertNotNull(container.getIndirectAdvanceAssignment(advanceType2));

        assertTrue(element.getDirectAdvanceAssignments().isEmpty());
        assertNull(element.getAdvanceAssignmentByType(advanceType));
        assertNull(element.getAdvanceAssignmentByType(advanceType2));

        assertFalse(container2.getDirectAdvanceAssignments().isEmpty());
        assertTrue(container2.getIndirectAdvanceAssignments().isEmpty());
        assertNull(container2.getAdvanceAssignmentByType(advanceType));
        assertNotNull(container2.getAdvanceAssignmentByType(advanceType2));
        assertNull(container2.getIndirectAdvanceAssignment(advanceType2));

        assertTrue(element2.getDirectAdvanceAssignments().isEmpty());
        assertNull(element2.getAdvanceAssignmentByType(advanceType));
        assertNull(element2.getAdvanceAssignmentByType(advanceType2));

        assertTrue(element3.getDirectAdvanceAssignments().isEmpty());
        assertNull(element3.getAdvanceAssignmentByType(advanceType));
        assertNull(element3.getAdvanceAssignmentByType(advanceType2));
    }

    @Test
    @Transactional
    public void checkMoveOrderLineGroupWithAdvanceToOrderLineInGroupWithSameAdvance()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        createTreeWithContainerAndContainer();

        addDirectAdvanceAssignment(container);
        addDirectAdvanceAssignment(container2);

        addAnotherDirectAdvanceAssignment(container2);

        model.move(container2, element);

        assertTrue(order.getDirectAdvanceAssignments().isEmpty());
        assertThat(order.getIndirectAdvanceAssignments().size(), equalTo(3));
        assertNotNull(order.getAdvanceAssignmentByType(advanceType));
        assertNotNull(order.getAdvanceAssignmentByType(advanceType2));

        assertThat(container.getDirectAdvanceAssignments().size(), equalTo(1));
        assertThat(container.getIndirectAdvanceAssignments().size(), equalTo(2));
        assertNotNull(container.getAdvanceAssignmentByType(advanceType));
        assertNull(container.getIndirectAdvanceAssignment(advanceType));
        assertNotNull(container.getIndirectAdvanceAssignment(advanceType2));

        assertTrue(element.getDirectAdvanceAssignments().isEmpty());
        assertNull(element.getAdvanceAssignmentByType(advanceType));
        assertNull(element.getAdvanceAssignmentByType(advanceType2));

        assertFalse(container2.getDirectAdvanceAssignments().isEmpty());
        assertTrue(container2.getIndirectAdvanceAssignments().isEmpty());
        assertNull(container2.getAdvanceAssignmentByType(advanceType));
        assertNotNull(container2.getAdvanceAssignmentByType(advanceType2));
        assertNull(container2.getIndirectAdvanceAssignment(advanceType2));

        assertTrue(element2.getDirectAdvanceAssignments().isEmpty());
        assertNull(element2.getAdvanceAssignmentByType(advanceType));
        assertNull(element2.getAdvanceAssignmentByType(advanceType2));

        assertTrue(element3.getDirectAdvanceAssignments().isEmpty());
        assertNull(element3.getAdvanceAssignmentByType(advanceType));
        assertNull(element3.getAdvanceAssignmentByType(advanceType2));
    }

    @Test
    @Transactional
    public void checkMoveOrderLineGroupWithAdvanceOnChildToOrderLineGroupWithSameAdvance()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        createTreeWithContainerAndContainer();

        addDirectAdvanceAssignment(container);
        addDirectAdvanceAssignment(element2);

        addAnotherDirectAdvanceAssignment(element2);

        model.move(container2, container);

        assertTrue(order.getDirectAdvanceAssignments().isEmpty());
        assertThat(order.getIndirectAdvanceAssignments().size(), equalTo(3));
        assertNotNull(order.getAdvanceAssignmentByType(advanceType));
        assertNotNull(order.getAdvanceAssignmentByType(advanceType2));

        assertThat(container.getDirectAdvanceAssignments().size(), equalTo(1));
        assertThat(container.getIndirectAdvanceAssignments().size(), equalTo(2));
        assertNotNull(container.getAdvanceAssignmentByType(advanceType));
        assertNull(container.getIndirectAdvanceAssignment(advanceType));
        assertNotNull(container.getIndirectAdvanceAssignment(advanceType2));

        assertTrue(element.getDirectAdvanceAssignments().isEmpty());
        assertNull(element.getAdvanceAssignmentByType(advanceType));
        assertNull(element.getAdvanceAssignmentByType(advanceType2));

        assertTrue(container2.getDirectAdvanceAssignments().isEmpty());
        assertThat(container2.getIndirectAdvanceAssignments().size(),
                equalTo(2));
        assertNull(container2.getAdvanceAssignmentByType(advanceType));
        assertNotNull(container2.getAdvanceAssignmentByType(advanceType2));
        assertNotNull(container2.getIndirectAdvanceAssignment(advanceType2));

        assertThat(element2.getDirectAdvanceAssignments().size(), equalTo(1));
        assertNull(element2.getAdvanceAssignmentByType(advanceType));
        assertNotNull(element2.getAdvanceAssignmentByType(advanceType2));

        assertTrue(element3.getDirectAdvanceAssignments().isEmpty());
        assertNull(element3.getAdvanceAssignmentByType(advanceType));
        assertNull(element3.getAdvanceAssignmentByType(advanceType2));
    }

    @Test
    @Transactional
    public void checkMoveOrderLineGroupWithAdvanceOnChildToOrderLineInGroupWithSameAdvance()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        createTreeWithContainerAndContainer();

        addDirectAdvanceAssignment(container);
        addDirectAdvanceAssignment(element2);

        addAnotherDirectAdvanceAssignment(element2);

        model.move(container2, element);

        assertTrue(order.getDirectAdvanceAssignments().isEmpty());
        assertThat(order.getIndirectAdvanceAssignments().size(), equalTo(3));
        assertNotNull(order.getAdvanceAssignmentByType(advanceType));
        assertNotNull(order.getAdvanceAssignmentByType(advanceType2));

        assertThat(container.getDirectAdvanceAssignments().size(), equalTo(1));
        assertThat(container.getIndirectAdvanceAssignments().size(), equalTo(2));
        assertNotNull(container.getAdvanceAssignmentByType(advanceType));
        assertNull(container.getIndirectAdvanceAssignment(advanceType));
        assertNotNull(container.getIndirectAdvanceAssignment(advanceType2));

        assertTrue(element.getDirectAdvanceAssignments().isEmpty());
        assertNull(element.getAdvanceAssignmentByType(advanceType));
        assertNull(element.getAdvanceAssignmentByType(advanceType2));

        assertTrue(container2.getDirectAdvanceAssignments().isEmpty());
        assertThat(container2.getIndirectAdvanceAssignments().size(), equalTo(2));
        assertNull(container2.getAdvanceAssignmentByType(advanceType));
        assertNotNull(container2.getAdvanceAssignmentByType(advanceType2));
        assertNotNull(container2.getIndirectAdvanceAssignment(advanceType2));

        assertThat(element2.getDirectAdvanceAssignments().size(), equalTo(1));
        assertNull(element2.getAdvanceAssignmentByType(advanceType));
        assertNotNull(element2.getAdvanceAssignmentByType(advanceType2));

        assertTrue(element3.getDirectAdvanceAssignments().isEmpty());
        assertNull(element3.getAdvanceAssignmentByType(advanceType));
        assertNull(element3.getAdvanceAssignmentByType(advanceType2));
    }

    @Test
    @Transactional
    public void checkMoveOrderLineWithCriterionToOrderLineGroupWithSameCriterion() {
        createTreeWithContainerAndTask();

        addCriterionRequirement(container);
        addCriterionRequirement(element2);

        addAnotherCriterionRequirement(element2);

        model.move(element2, container);

        assertTrue(order.getCriterionRequirements().isEmpty());

        assertThat(container.getCriterionRequirements().size(), equalTo(1));
        assertDirectCriterion(container.getCriterionRequirements().iterator().next(), criterion);

        assertThat(element.getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(element.getCriterionRequirements().iterator().next(), criterion);
        assertThat(element.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(element.getHoursGroups().get(0).getCriterionRequirements().iterator().next(), criterion);

        assertThat(element2.getCriterionRequirements().size(), equalTo(2));
        for (CriterionRequirement each : element2.getCriterionRequirements()) {
            if ( each.getCriterion().isEquivalent(criterion) ) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else if ( each.getCriterion().isEquivalent(criterion2) ) {
                assertTrue(each instanceof DirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }

        assertThat(element2.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(2));
        for (CriterionRequirement each : element2.getHoursGroups().get(0).getCriterionRequirements()) {
            if ( each.getCriterion().isEquivalent(criterion) || each.getCriterion().isEquivalent(criterion2) ) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }
    }

    @Test
    @Transactional
    public void checkMoveOrderLineWithCriterionToOrderLineInGroupWithSameCriterion() {
        createTreeWithContainerAndTask();

        addCriterionRequirement(container);
        addCriterionRequirement(element2);

        addAnotherCriterionRequirement(element2);

        model.move(element2, element);

        assertTrue(order.getCriterionRequirements().isEmpty());

        assertThat(container.getCriterionRequirements().size(), equalTo(1));
        assertDirectCriterion(container.getCriterionRequirements().iterator().next(), criterion);

        assertThat(element.getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(element.getCriterionRequirements().iterator().next(), criterion);
        assertThat(element.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(element.getHoursGroups().get(0).getCriterionRequirements().iterator().next(), criterion);

        assertThat(element2.getCriterionRequirements().size(), equalTo(2));
        for (CriterionRequirement each : element2.getCriterionRequirements()) {
            if ( each.getCriterion().isEquivalent(criterion) ) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else if ( each.getCriterion().isEquivalent(criterion2) ) {
                assertTrue(each instanceof DirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }

        assertThat(element2.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(2));
        for (CriterionRequirement each : element2.getHoursGroups().get(0).getCriterionRequirements()) {
            if ( each.getCriterion().isEquivalent(criterion) || each.getCriterion().isEquivalent(criterion2) ) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }
    }

    @Test
    @Transactional
    public void checkMoveOrderGroupLineWithCriterionToOrderLineGroupWithSameCriterion() {
        createTreeWithContainerAndContainer();

        addCriterionRequirement(container);
        addCriterionRequirement(container2);

        addAnotherCriterionRequirement(container2);

        model.move(container2, container);

        assertTrue(order.getCriterionRequirements().isEmpty());

        assertThat(container.getCriterionRequirements().size(), equalTo(1));
        assertDirectCriterion(container.getCriterionRequirements().iterator().next(), criterion);

        assertThat(element.getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(element.getCriterionRequirements().iterator().next(), criterion);
        assertThat(element.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(element.getHoursGroups().get(0).getCriterionRequirements().iterator().next(), criterion);

        assertThat(container2.getCriterionRequirements().size(), equalTo(2));
        for (CriterionRequirement each : container2.getCriterionRequirements()) {
            if (each.getCriterion().isEquivalent(criterion)) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else if (each.getCriterion().isEquivalent(criterion2)) {
                assertTrue(each instanceof DirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }

        assertThat(element2.getCriterionRequirements().size(), equalTo(2));
        for (CriterionRequirement each : element2.getCriterionRequirements()) {
            if (each.getCriterion().isEquivalent(criterion) || each.getCriterion().isEquivalent(criterion2)) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }

        assertThat(element2.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(2));
        for (CriterionRequirement each : element2.getHoursGroups().get(0).getCriterionRequirements()) {
            if (each.getCriterion().isEquivalent(criterion) || each.getCriterion().isEquivalent(criterion2)) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }

        assertThat(element3.getCriterionRequirements().size(), equalTo(2));
        for (CriterionRequirement each : element3.getCriterionRequirements()) {
            if (each.getCriterion().isEquivalent(criterion) || each.getCriterion().isEquivalent(criterion2)) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }

        assertThat(element3.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(2));
        for (CriterionRequirement each : element3.getHoursGroups().get(0).getCriterionRequirements()) {
            if (each.getCriterion().isEquivalent(criterion) || each.getCriterion().isEquivalent(criterion2)) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }

    }

    @Test
    @Transactional
    public void checkMoveOrderGroupLineWithCriterionToOrderLineInGroupWithSameCriterion() {
        createTreeWithContainerAndContainer();

        addCriterionRequirement(container);
        addCriterionRequirement(container2);

        addAnotherCriterionRequirement(container2);

        model.move(container2, element);

        assertTrue(order.getCriterionRequirements().isEmpty());

        assertThat(container.getCriterionRequirements().size(), equalTo(1));
        assertDirectCriterion(container.getCriterionRequirements().iterator().next(), criterion);

        assertThat(element.getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(element.getCriterionRequirements().iterator().next(), criterion);
        assertThat(element.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(element.getHoursGroups().get(0).getCriterionRequirements().iterator().next(), criterion);

        assertThat(container2.getCriterionRequirements().size(), equalTo(2));
        for (CriterionRequirement each : container2.getCriterionRequirements()) {
            if (each.getCriterion().isEquivalent(criterion)) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else if (each.getCriterion().isEquivalent(criterion2)) {
                assertTrue(each instanceof DirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }

        assertThat(element2.getCriterionRequirements().size(), equalTo(2));
        for (CriterionRequirement each : element2.getCriterionRequirements()) {
            if (each.getCriterion().isEquivalent(criterion) || each.getCriterion().isEquivalent(criterion2)) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }

        assertThat(element2.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(2));
        for (CriterionRequirement each : element2.getHoursGroups().get(0).getCriterionRequirements()) {
            if (each.getCriterion().isEquivalent(criterion) || each.getCriterion().isEquivalent(criterion2)) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }

        assertThat(element3.getCriterionRequirements().size(), equalTo(2));
        for (CriterionRequirement each : element3.getCriterionRequirements()) {
            if (each.getCriterion().isEquivalent(criterion) || each.getCriterion().isEquivalent(criterion2)) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }

        assertThat(element3.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(2));
        for (CriterionRequirement each : element3.getHoursGroups().get(0).getCriterionRequirements()) {
            if (each.getCriterion().isEquivalent(criterion) || each.getCriterion().isEquivalent(criterion2)) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }

    }

    @Test
    @Transactional
    public void checkMoveOrderLineGroupWithCriterionOnChildToOrderLineGroupWithSameCriterion() {
        createTreeWithContainerAndContainer();

        addCriterionRequirement(container);
        addCriterionRequirement(element2);

        addAnotherCriterionRequirement(element2);

        model.move(container2, container);

        assertTrue(order.getCriterionRequirements().isEmpty());

        assertThat(container.getCriterionRequirements().size(), equalTo(1));
        assertDirectCriterion(container.getCriterionRequirements().iterator().next(), criterion);

        assertThat(element.getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(element.getCriterionRequirements().iterator().next(), criterion);
        assertThat(element.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(element.getHoursGroups().get(0).getCriterionRequirements().iterator().next(), criterion);

        assertThat(container2.getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(container2.getCriterionRequirements().iterator().next(), criterion);

        assertThat(element2.getCriterionRequirements().size(), equalTo(2));
        for (CriterionRequirement each : element2.getCriterionRequirements()) {
            if (each.getCriterion().isEquivalent(criterion)) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else if (each.getCriterion().isEquivalent(criterion2)) {
                assertTrue(each instanceof DirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }

        assertThat(element2.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(2));
        for (CriterionRequirement each : element2.getHoursGroups().get(0).getCriterionRequirements()) {
            if (each.getCriterion().isEquivalent(criterion) || each.getCriterion().isEquivalent(criterion2)) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }

        assertThat(element3.getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(element3.getCriterionRequirements().iterator().next(), criterion);

        assertThat(element3.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(element3.getHoursGroups().get(0).getCriterionRequirements().iterator().next(), criterion);

    }

    @Test
    @Transactional
    public void checkMoveOrderLineGroupWithCriterionOnChildToOrderLineInGroupWithSameCriterion() {
        createTreeWithContainerAndContainer();

        addCriterionRequirement(container);
        addCriterionRequirement(element2);

        addAnotherCriterionRequirement(element2);

        model.move(container2, element);

        assertTrue(order.getCriterionRequirements().isEmpty());

        assertThat(container.getCriterionRequirements().size(), equalTo(1));
        assertDirectCriterion(container.getCriterionRequirements().iterator().next(), criterion);

        assertThat(element.getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(element.getCriterionRequirements().iterator().next(), criterion);
        assertThat(element.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(element.getHoursGroups().get(0).getCriterionRequirements().iterator().next(), criterion);

        assertThat(container2.getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(container2.getCriterionRequirements().iterator().next(), criterion);

        assertThat(element2.getCriterionRequirements().size(), equalTo(2));
        for (CriterionRequirement each : element2.getCriterionRequirements()) {
            if (each.getCriterion().isEquivalent(criterion)) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else if (each.getCriterion().isEquivalent(criterion2)) {
                assertTrue(each instanceof DirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }

        assertThat(element2.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(2));
        for (CriterionRequirement each : element2.getHoursGroups().get(0).getCriterionRequirements()) {
            if (each.getCriterion().isEquivalent(criterion) || each.getCriterion().isEquivalent(criterion2)) {
                assertTrue(each instanceof IndirectCriterionRequirement);
            } else {
                fail("Unexpected criterion: " + each.getCriterion());
            }
        }

        assertThat(element3.getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(element3.getCriterionRequirements().iterator().next(), criterion);

        assertThat(element3.getHoursGroups().get(0).getCriterionRequirements().size(), equalTo(1));
        assertIndirectCriterion(element3.getHoursGroups().get(0).getCriterionRequirements().iterator().next(), criterion);

    }

    /**
     * This will create the following tree:
     *
     * <pre>
     *     order
     *       |-- element1
     *       |-- element2
     * </pre>
     */
    private void createTreeWith2Task() {
        model.addElement("element", 100);
        model.addElement("element2", 50);

        element = null;
        element2 = null;
        for (OrderElement each : order.getChildren()) {
            if ( each.getName().equals("element") ) {
                element = (OrderLine) each;
            } else if ( each.getName().equals("element2") ) {
                element2 = (OrderLine) each;
            } else {
                fail("Unexpected OrderElement name: " + each.getName());
            }
        }

        model.unindent(element2);
    }

    @Test
    @Transactional
    public void checkRemoveElementWithAdvanceOnChildWhichSpread()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        createTreeWith2Task();

        assertTrue(order.getIndirectAdvanceAssignments().isEmpty());
        assertNull(order.getReportGlobalAdvanceAssignment());

        addDirectAdvanceAssignment(element, true);
        addAnotherDirectAdvanceAssignment(element);
        addAnotherDirectAdvanceAssignment(element2);

        assertThat(order.getIndirectAdvanceAssignments().size(), equalTo(3));
        assertNotNull(order.getIndirectAdvanceAssignment(advanceType));
        assertNotNull(order.getIndirectAdvanceAssignment(advanceType2));
        assertNotNull(order.getReportGlobalAdvanceAssignment());
        assertTrue(order.getChildrenAdvance().getReportGlobalAdvance());

        model.removeNode(element);

        assertTrue(order.getDirectAdvanceAssignments().isEmpty());
        assertThat(order.getIndirectAdvanceAssignments().size(), equalTo(2));
        assertNull(order.getIndirectAdvanceAssignment(advanceType));
        assertNotNull(order.getIndirectAdvanceAssignment(advanceType2));
        assertNotNull(order.getReportGlobalAdvanceAssignment());

        assertThat(element2.getDirectAdvanceAssignments().size(), equalTo(1));
        assertNotNull(element2.getDirectAdvanceAssignmentByType(advanceType2));
    }

    @Test
    @Transactional
    public void checkChildrenAdvanceIsCreatedAndMarkedAsSpreadInOrder()
            throws DuplicateValueTrueReportGlobalAdvanceException, DuplicateAdvanceAssignmentForOrderElementException {

        model.addElement("element", 100);

        OrderLine element = (OrderLine) order.getChildren().get(0);

        addDirectAdvanceAssignment(element);

        assertThat(order.getIndirectAdvanceAssignments().size(), equalTo(2));
        assertNotNull(order.getAdvanceAssignmentByType(advanceType));
        assertNotNull(order.getChildrenAdvance());
        assertNotNull(order.getReportGlobalAdvanceAssignment());
        assertTrue(order.getChildrenAdvance().getReportGlobalAdvance());

        assertThat(element.getDirectAdvanceAssignments().size(), equalTo(1));
        assertNotNull(element.getAdvanceAssignmentByType(advanceType));
        assertNotNull(element.getReportGlobalAdvanceAssignment());
        assertTrue(element.getAdvanceAssignmentByType(advanceType).getReportGlobalAdvance());
    }

}

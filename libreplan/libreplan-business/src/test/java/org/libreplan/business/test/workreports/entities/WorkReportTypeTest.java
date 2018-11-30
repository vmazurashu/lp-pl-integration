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

package org.libreplan.business.test.workreports.entities;

import static org.junit.Assert.fail;
import static org.libreplan.business.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_FILE;
import static org.libreplan.business.test.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_TEST_FILE;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.test.workreports.daos.AbstractWorkReportTest;
import org.libreplan.business.workreports.daos.IWorkReportTypeDAO;
import org.libreplan.business.workreports.entities.WorkReportLabelTypeAssignment;
import org.libreplan.business.workreports.entities.WorkReportType;
import org.libreplan.business.workreports.valueobjects.DescriptionField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Diego Pino García <dpino@igalia.com>
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { BUSINESS_SPRING_CONFIG_FILE, BUSINESS_SPRING_CONFIG_TEST_FILE })
public class WorkReportTypeTest extends AbstractWorkReportTest {

    @Autowired
    IWorkReportTypeDAO workReportTypeDAO;

    @Test
    @Transactional
    public void checkInvalidNameWorkReportType() throws ValidationException {
        WorkReportType workReportType = createValidWorkReportType();
        workReportType.setName("");
        try {
            workReportTypeDAO.save(workReportType);
            fail("It should throw an exception");
        } catch (ValidationException e) {
            // It should throw an exception
        }

        workReportType = createValidWorkReportType();
        workReportType.setName(null);
        try {
            workReportTypeDAO.save(workReportType);
            fail("It should throw an exception");
        } catch (ValidationException e) {
            // It should throw an exception
        }
    }

    @Test
    @Transactional
    public void checkInvalidCodeWorkReportType() throws ValidationException {
        WorkReportType workReportType = createValidWorkReportType();
        workReportType.setCode("");
        try {
            workReportTypeDAO.save(workReportType);
            fail("It should throw an exception");
        } catch (ValidationException e) {
            // It should throw an exception
        }

        workReportType = createValidWorkReportType();
        workReportType.setCode(null);
        try {
            workReportTypeDAO.save(workReportType);
            fail("It should throw an exception");
        } catch (ValidationException e) {
            // It should throw an exception
        }

        workReportType.setCode("invalid_code");
        try {
            workReportTypeDAO.save(workReportType);
            fail("It should throw an exception");
        } catch (ValidationException e) {
            // It should throw an exception
        }

    }

    @Test
    @Transactional
    public void checkIfCodeWorkReportTypeIsUnique() throws ValidationException {
        String code = "A";
        WorkReportType workReportTypeA = createValidWorkReportType();
        workReportTypeA.setCode(code);
        workReportTypeDAO.save(workReportTypeA);
        workReportTypeDAO.flush();

        WorkReportType workReportTypeACopy = createValidWorkReportType();
        workReportTypeACopy.setCode(code);

        try {
            workReportTypeDAO.save(workReportTypeACopy);
            workReportTypeDAO.flush();
            fail("It should throw an Exception");
        } catch (DataIntegrityViolationException e) {
            // It should throw an exception
        }
    }

    @Test
    @Transactional
    public void checkIfNameWorkReportTypeIsUnique() throws ValidationException {
        WorkReportType workReportTypeA = createValidWorkReportType();
        workReportTypeA.setName("A");
        workReportTypeDAO.save(workReportTypeA);
        workReportTypeDAO.flush();

        WorkReportType workReportTypeACopy = createValidWorkReportType();
        workReportTypeACopy.setName("A");

        try {
            workReportTypeDAO.save(workReportTypeACopy);
            workReportTypeDAO.flush();
            fail("It should throw an Exception");
        } catch (DataIntegrityViolationException e) {
            // It should throw an exception
        }
    }

    @Test
    @Transactional
    public void checkSaveDescriptionFieldsWorkReportType() {
        WorkReportType workReportType = createValidWorkReportType();

        DescriptionField descriptionFieldHead = createValidDescriptionField();
        workReportType.addDescriptionFieldToEndHead(descriptionFieldHead);

        DescriptionField descriptionFieldLine = createValidDescriptionField();
        workReportType.addDescriptionFieldToEndLine(descriptionFieldLine);

        try {
            workReportTypeDAO.save(workReportType);
        } catch (ValidationException e) {
            fail("It should throw an exception");
        }
    }

    @Test
    @Transactional
    public void checkIfFieldNameDescriptionFieldsIsUnique() {
        WorkReportType workReportType = createValidWorkReportType();

        DescriptionField descriptionFieldHead = createValidDescriptionField();
        descriptionFieldHead.setFieldName("A");
        workReportType.addDescriptionFieldToEndHead(descriptionFieldHead);

        DescriptionField descriptionFieldLine = createValidDescriptionField();
        descriptionFieldLine.setFieldName("A");
        workReportType.addDescriptionFieldToEndLine(descriptionFieldLine);

        try {
            workReportTypeDAO.save(workReportType);
            fail("It should throw an exception");
        } catch (ValidationException ignored) {
        }
    }

    @Test
    @Transactional
    public void checkInvalidFieldNameDescriptionFields() {
        WorkReportType workReportType = createValidWorkReportType();

        DescriptionField descriptionFieldHead = createValidDescriptionField();
        descriptionFieldHead.setFieldName("");
        workReportType.addDescriptionFieldToEndHead(descriptionFieldHead);

        try {
            workReportTypeDAO.save(workReportType);
            fail("It should throw an exception");
        } catch (ValidationException ignored) {
        }

        descriptionFieldHead.setFieldName(null);
        try {
            workReportTypeDAO.save(workReportType);
            fail("It should throw an exception");
        } catch (ValidationException ignored) {
        }

        descriptionFieldHead.setFieldName("XXX");
        try {
            workReportTypeDAO.save(workReportType);
        } catch (ValidationException e) {
            fail("It should throw an exception");
        }
    }

    @Test
    @Transactional
    public void checkInvalidLengthDescriptionFields() {
        WorkReportType workReportType = createValidWorkReportType();

        DescriptionField descriptionFieldHead = createValidDescriptionField();
        descriptionFieldHead.setLength(null);
        workReportType.addDescriptionFieldToEndHead(descriptionFieldHead);

        try {
            workReportTypeDAO.save(workReportType);
            fail("It should throw an exception");
        } catch (ValidationException ignored) {
        }

        descriptionFieldHead.setLength(0);
        try {
            workReportTypeDAO.save(workReportType);
            fail("It should throw an exception");
        } catch (ValidationException ignored) {
        }

        descriptionFieldHead.setLength(1);
        try {
            workReportTypeDAO.save(workReportType);
        } catch (ValidationException e) {
            fail("It should throw an exception");
        }
    }

    public void checkSaveWorkReportLabelTypeAssignment() {
        WorkReportType workReportType = createValidWorkReportType();

        WorkReportLabelTypeAssignment labelAssignmentHead = createValidWorkReportLabelTypeAssignment();
        workReportType.addLabelAssignmentToEndHead(labelAssignmentHead);

        WorkReportLabelTypeAssignment labelAssignmentLine = createValidWorkReportLabelTypeAssignment();
        workReportType.addLabelAssignmentToEndLine(labelAssignmentLine);

        try {
            workReportTypeDAO.save(workReportType);
        } catch (ValidationException e) {
            fail("It should throw an exception");
        }
    }

    public void checkIfLabelTypeWorkReportLabelTypeAssignmentIsNull() {
        WorkReportType workReportType = createValidWorkReportType();
        WorkReportLabelTypeAssignment labelAssignment = createValidWorkReportLabelTypeAssignment();
        labelAssignment.setLabelType(null);
        workReportType.addLabelAssignmentToEndLine(labelAssignment);

        try {
            workReportTypeDAO.save(workReportType);
            fail("It should throw an exception");
        } catch (ValidationException ignored) {
        }
    }

    @Test
    @Transactional
    public void checkIfLabelWorkReportLabelTypeAssignmentIsNull() {
        WorkReportType workReportType = createValidWorkReportType();
        WorkReportLabelTypeAssignment labelAssignment = createValidWorkReportLabelTypeAssignment();
        labelAssignment.setDefaultLabel(null);
        workReportType.addLabelAssignmentToEndLine(labelAssignment);

        try {
            workReportTypeDAO.save(workReportType);
            fail("It should throw an exception");
        } catch (ValidationException ignored) {
        }
    }

    @Test
    @Transactional
    public void checkIfIndexLabelsAndFieldsAreConsecutive() {
        WorkReportType workReportType = createValidWorkReportType();

        WorkReportLabelTypeAssignment labelAssignment_1 = createValidWorkReportLabelTypeAssignment();
        workReportType.addLabelAssignmentToEndLine(labelAssignment_1);

        WorkReportLabelTypeAssignment labelAssignment_2 = createValidWorkReportLabelTypeAssignment();
        workReportType.addLabelAssignmentToEndLine(labelAssignment_2);

        WorkReportLabelTypeAssignment labelAssignment_3 = createValidWorkReportLabelTypeAssignment();
        workReportType.addLabelAssignmentToEndLine(labelAssignment_3);

        // Set not consecutives index labels
        labelAssignment_1.setPositionNumber(3);
        labelAssignment_2.setPositionNumber(0);
        labelAssignment_3.setPositionNumber(2);

        try {
            workReportTypeDAO.save(workReportType);
            fail("It should throw an exception");
        } catch (ValidationException ignored) {
        }
    }

    @Test
    @Transactional
    public void checkIfIndexLabelsAndFieldsInitInZero() {
        WorkReportType workReportType = createValidWorkReportType();

        WorkReportLabelTypeAssignment labelAssignment_1 = createValidWorkReportLabelTypeAssignment();
        workReportType.addLabelAssignmentToEndLine(labelAssignment_1);

        WorkReportLabelTypeAssignment labelAssignment_2 = createValidWorkReportLabelTypeAssignment();
        workReportType.addLabelAssignmentToEndLine(labelAssignment_2);

        WorkReportLabelTypeAssignment labelAssignment_3 = createValidWorkReportLabelTypeAssignment();
        workReportType.addLabelAssignmentToEndLine(labelAssignment_3);

        // Set repeat indexes labels
        labelAssignment_1.setPositionNumber(1);
        labelAssignment_2.setPositionNumber(2);
        labelAssignment_3.setPositionNumber(3);

        try {
            workReportTypeDAO.save(workReportType);
            fail("It should throw an exception");
        } catch (ValidationException ignored) {
        }
    }

    @Test
    @Transactional
    public void checkIfIndexLabelsAndFieldsAreUniques() {
        WorkReportType workReportType = createValidWorkReportType();

        WorkReportLabelTypeAssignment labelAssignment_1 = createValidWorkReportLabelTypeAssignment();
        workReportType.addLabelAssignmentToEndLine(labelAssignment_1);

        WorkReportLabelTypeAssignment labelAssignment_2 = createValidWorkReportLabelTypeAssignment();
        workReportType.addLabelAssignmentToEndLine(labelAssignment_2);

        WorkReportLabelTypeAssignment labelAssignment_3 = createValidWorkReportLabelTypeAssignment();
        workReportType.addLabelAssignmentToEndLine(labelAssignment_3);

        // Set repeat indexes labels
        labelAssignment_1.setPositionNumber(1);
        labelAssignment_2.setPositionNumber(0);
        labelAssignment_3.setPositionNumber(1);

        try {
            workReportTypeDAO.save(workReportType);
            fail("It should throw an exception");
        } catch (ValidationException ignored) {
        }
    }
}

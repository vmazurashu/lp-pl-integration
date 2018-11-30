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

package org.libreplan.business.test.resources.daos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.libreplan.business.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_FILE;
import static org.libreplan.business.test.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_TEST_FILE;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.libreplan.business.common.IAdHocTransactionService;
import org.libreplan.business.common.IOnTransaction;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.resources.daos.ICriterionTypeDAO;
import org.libreplan.business.resources.entities.CriterionType;
import org.libreplan.business.resources.entities.ResourceEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Diego Pino García <dpino@igalia.com>
 */

/**
 * Test cases for CriterionTypeDAO <br />
 * @author Diego Pino García <dpino@igalia.com>
 * @author Fernando Bellas Permuy <fbellas@udc.es>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { BUSINESS_SPRING_CONFIG_FILE,
        BUSINESS_SPRING_CONFIG_TEST_FILE })
public class CriterionTypeDAOTest {

    @Autowired
    private ICriterionTypeDAO criterionTypeDAO;

    @Autowired
    private IAdHocTransactionService transactionService;

    public static final String DEFAULT_CRITERION_TYPE = "TEST_DEFAULT";

    public static CriterionType createValidCriterionType(String name,String description) {
        return CriterionType.create(name,description);
    }

    public static CriterionType createValidCriterionType() {
        String unique = UUID.randomUUID().toString();
        String description = "";
        return createValidCriterionType(unique,description);
    }

    @Test
    @Transactional
    public void testSaveCriterionType() {
        CriterionType criterionType = createValidCriterionType();
        criterionTypeDAO.save(criterionType);
        assertTrue(criterionTypeDAO.exists(criterionType.getId()));
    }

    @Test
    @Transactional
    public void testCriterionTypeCanBeSavedTwice() throws ValidationException {
        CriterionType criterionType = createValidCriterionType();
        criterionTypeDAO.save(criterionType);
        criterionTypeDAO.save(criterionType);
        assertTrue(criterionTypeDAO.exists(criterionType.getId())
                || criterionTypeDAO
                        .existsOtherCriterionTypeByName(criterionType));
    }

    @Test(expected = ValidationException.class)
    public void testCannotSaveTwoDifferentCriterionTypesWithTheSameName() {
        IOnTransaction<Void> createTypeWithRepeatedName = new IOnTransaction<Void>() {

            @Override
            public Void execute() {
                CriterionType criterionType = createValidCriterionType("bla",
                        "");
                criterionTypeDAO.save(criterionType);
                return null;
            }
        };
        transactionService.runOnTransaction(createTypeWithRepeatedName);
        transactionService.runOnTransaction(createTypeWithRepeatedName);
    }

    @Test
    public void testUpdateWithExistingName() {

        final String name1 = getUniqueName();
        final String name2 = getUniqueName();

        IOnTransaction<Void> createCriterionTypes = new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                criterionTypeDAO.save(createValidCriterionType(name1, ""));
                criterionTypeDAO.save(createValidCriterionType(name2, ""));
                return null;
            }
        };

        IOnTransaction<Void> updateCriterionType1 = new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                CriterionType criterionType = null;
                try {
                    criterionType = criterionTypeDAO.findUniqueByName(name1);
                } catch (InstanceNotFoundException e) {
                    fail("InstanceNotFoundException not expected");
                }
                criterionType.setName(name2);
                criterionTypeDAO.save(criterionType);
                return null;
            }
        };

        transactionService.runOnTransaction(createCriterionTypes);

        try {
            transactionService.runOnTransaction(updateCriterionType1);
            fail("ValidationException expected");
        } catch (ValidationException e) {
        }

    }

    @Test
    public void testUpdateWithTheSameName() {

        final String name1 = getUniqueName();
        final String name2 = getUniqueName();

        IOnTransaction<Void> createCriterionTypes = new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                criterionTypeDAO.save(createValidCriterionType(name1, ""));
                criterionTypeDAO.save(createValidCriterionType(name2, ""));
                return null;
            }
        };

        IOnTransaction<Void> updateCriterionType1 = new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                CriterionType criterionType = null;
                try {
                    criterionType = criterionTypeDAO.findUniqueByName(name1);
                } catch (InstanceNotFoundException e) {
                    fail("InstanceNotFoundException not expected");
                }
                criterionType.setDescription("New description");
                criterionTypeDAO.save(criterionType);
                return null;
            }
        };

        transactionService.runOnTransaction(createCriterionTypes);
        transactionService.runOnTransaction(updateCriterionType1);

    }

    @Test
    @Transactional
    public void testRemove() throws InstanceNotFoundException {
        CriterionType criterionType = createValidCriterionType();
        criterionTypeDAO.save(criterionType);
        criterionTypeDAO.remove(criterionType.getId());
        assertFalse(criterionTypeDAO.exists(criterionType.getId()));
    }

    @Test
    @Transactional
    public void testList() {
        int previous = criterionTypeDAO.list(CriterionType.class).size();
        CriterionType criterion1 = createValidCriterionType();
        CriterionType criterion2 = createValidCriterionType();
        criterionTypeDAO.save(criterion1);
        criterionTypeDAO.save(criterion2);
        List<CriterionType> list = criterionTypeDAO.list(CriterionType.class);
        assertEquals(previous + 2, list.size());
    }

    @Test
    @Transactional
    public void testGetCriterionTypes() {
        int previous = criterionTypeDAO.list(CriterionType.class).size();
        CriterionType criterion1 = createValidCriterionType();
        CriterionType criterion2 = createValidCriterionType();
        criterionTypeDAO.save(criterion1);
        criterionTypeDAO.save(criterion2);
        List<CriterionType> list = criterionTypeDAO.getCriterionTypes();
        assertEquals(previous + 2, list.size());
    }

    @Test
    @Transactional
    public void testGetCriterionTypesByResourceType() {
        // Add RESOURCE criterionType
        CriterionType criterionType = createValidCriterionType();
        criterionType.setResource(ResourceEnum.WORKER);
        criterionTypeDAO.save(criterionType);

        // Add WORKER criterionType
        criterionType = createValidCriterionType();
        criterionType.setResource(ResourceEnum.WORKER);
        criterionTypeDAO.save(criterionType);

        // Get number of criterionTypes of type RESOURCE
        List<ResourceEnum> resources = new ArrayList<ResourceEnum>();
        resources.add(ResourceEnum.WORKER);
        List<CriterionType> criterions = criterionTypeDAO.getCriterionTypesByResources(resources);
        int numberOfCriterionsOfTypeResource = criterions.size();

        // Get number of criterionTypes of type WORKER
        resources.add(ResourceEnum.WORKER);
        criterions = criterionTypeDAO
                .getCriterionTypesByResources(resources);
        int numberOfCriterionsOfTypeResourceAndWorker = criterions.size();

        assertTrue(numberOfCriterionsOfTypeResourceAndWorker >= numberOfCriterionsOfTypeResource);
    }

    private String getUniqueName() {
        return UUID.randomUUID().toString();
    }

}

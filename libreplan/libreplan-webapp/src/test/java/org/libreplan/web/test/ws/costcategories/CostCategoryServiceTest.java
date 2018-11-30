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

package org.libreplan.web.test.ws.costcategories;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.libreplan.business.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_FILE;
import static org.libreplan.web.WebappGlobalNames.WEBAPP_SPRING_CONFIG_FILE;
import static org.libreplan.web.WebappGlobalNames.WEBAPP_SPRING_SECURITY_CONFIG_FILE;
import static org.libreplan.web.test.WebappGlobalNames.WEBAPP_SPRING_CONFIG_TEST_FILE;
import static org.libreplan.web.test.WebappGlobalNames.WEBAPP_SPRING_SECURITY_CONFIG_TEST_FILE;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.xml.datatype.XMLGregorianCalendar;

import org.hibernate.SessionFactory;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libreplan.business.common.IAdHocTransactionService;
import org.libreplan.business.common.IOnTransaction;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.costcategories.daos.ICostCategoryDAO;
import org.libreplan.business.costcategories.daos.IHourCostDAO;
import org.libreplan.business.costcategories.daos.ITypeOfWorkHoursDAO;
import org.libreplan.business.costcategories.entities.CostCategory;
import org.libreplan.business.costcategories.entities.HourCost;
import org.libreplan.business.costcategories.entities.TypeOfWorkHours;
import org.libreplan.ws.common.api.InstanceConstraintViolationsDTO;
import org.libreplan.ws.common.impl.DateConverter;
import org.libreplan.ws.costcategories.api.CostCategoryDTO;
import org.libreplan.ws.costcategories.api.CostCategoryListDTO;
import org.libreplan.ws.costcategories.api.HourCostDTO;
import org.libreplan.ws.costcategories.api.ICostCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests for <code>ICostCategoryService</code>.
 *
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        BUSINESS_SPRING_CONFIG_FILE,
        WEBAPP_SPRING_CONFIG_FILE, WEBAPP_SPRING_CONFIG_TEST_FILE,
        WEBAPP_SPRING_SECURITY_CONFIG_FILE, WEBAPP_SPRING_SECURITY_CONFIG_TEST_FILE })
public class CostCategoryServiceTest {

    @Autowired
    private ICostCategoryService costCategoryService;

    @Autowired
    private ICostCategoryDAO costCategoryDAO;

    @Autowired
    private IHourCostDAO hourCostDAO;

    @Autowired
    private ITypeOfWorkHoursDAO typeOfWorkHoursDAO;

    @Autowired
    private SessionFactory sessionFactory;

    private final String typeOfWorkHoursCodeA = "code-A";

    private final String typeOfWorkHoursCodeB = "code-B";

    @Autowired
    private IAdHocTransactionService transactionService;

    @Test
    @Transactional
    @Rollback(false)
    public void createAPairTypeOfWorkHours() {
        givenTypeOfWorkHours(typeOfWorkHoursCodeA);
        givenTypeOfWorkHours(typeOfWorkHoursCodeB);
    }

    private void givenTypeOfWorkHours(String code) {

        TypeOfWorkHours typeOfWorkHours = TypeOfWorkHours.create();

        typeOfWorkHours.setCode(code);
        typeOfWorkHours.setName("name" + UUID.randomUUID());
        typeOfWorkHours.setDefaultPrice(BigDecimal.TEN);

        typeOfWorkHoursDAO.save(typeOfWorkHours);
        typeOfWorkHoursDAO.flush();
        sessionFactory.getCurrentSession().evict(typeOfWorkHours);
        typeOfWorkHours.dontPoseAsTransientObjectAnymore();
    }

    @Test
    @Transactional
    public void testAddAndGetCostCategories() {

        /* Build cost category (5 constraint violations) */

        // Missing cost category name and enabled
        CostCategoryDTO cc1 = new CostCategoryDTO(null, true, new HashSet<>());

        // Valid cost category DTO without hour cost
        CostCategoryDTO cc2 = new CostCategoryDTO("cc2", true, new HashSet<>());

        // Valid cost category DTO with a hour cost
        Set<HourCostDTO> cc3_HourCostDTOs = new HashSet<>();

        XMLGregorianCalendar initDate = DateConverter.toXMLGregorianCalendar(new Date());

        HourCostDTO cc3_1_HourCostDTO = new HourCostDTO(new BigDecimal(3), initDate, initDate, typeOfWorkHoursCodeA);
        cc3_HourCostDTOs.add(cc3_1_HourCostDTO);
        CostCategoryDTO cc3 = new CostCategoryDTO("cc3", true, cc3_HourCostDTOs);

        // Valid cost category DTO with a invalid hour cost missing priceCost, initDate and type
        Set<HourCostDTO> cc4_HourCostDTOs = new HashSet<>();
        HourCostDTO cc4_1_HourCostDTO = new HourCostDTO(null, null, null, null);
        cc4_HourCostDTOs.add(cc4_1_HourCostDTO);
        CostCategoryDTO cc4 = new CostCategoryDTO("cc4", true, cc4_HourCostDTOs);

        /* Cost category type list */
        CostCategoryListDTO costCategoryListDTO = createCostCategoryListDTO(cc1, cc2, cc3, cc4);

        List<InstanceConstraintViolationsDTO> instanceConstraintViolationsList = costCategoryService
                .addCostCategories(costCategoryListDTO).instanceConstraintViolationsList;

        assertTrue(instanceConstraintViolationsList.toString(), instanceConstraintViolationsList.size() == 2);

        assertTrue(
                instanceConstraintViolationsList.get(0).constraintViolations.toString(),
                instanceConstraintViolationsList.get(0).constraintViolations.size() == 1); // cc1 constraint violations

        assertTrue(
                instanceConstraintViolationsList.get(1).constraintViolations.toString(),
                instanceConstraintViolationsList.get(1).constraintViolations.size() == 3); // cc1 constraint violations

        assertFalse(costCategoryDAO.existsByCode(cc1.code));
        assertTrue(costCategoryDAO.existsByCode(cc2.code));
        assertTrue(costCategoryDAO.existsByCode(cc3.code));
        assertFalse(costCategoryDAO.existsByCode(cc4.code));

        try {
            CostCategory costCategory = costCategoryDAO.findByCode(cc3.code);
            assertTrue(costCategory.getHourCosts().size() == 1);
        } catch (InstanceNotFoundException e) {
            assertTrue(false);
        }
    }

    @Test
    public void testUpdateCostCategory() throws InstanceNotFoundException {

        // First one it creates Valid cost category DTO with a hour cost
        final String costCategoryCode = "code-CC";
        final String hourCostCode = "code-HC";

        Set<HourCostDTO> cc1_HourCostDTOs = new HashSet<>();

        XMLGregorianCalendar initDate = DateConverter.toXMLGregorianCalendar(new Date());

        final HourCostDTO cc1_1_HourCostDTO =
                new HourCostDTO(hourCostCode, new BigDecimal(3), initDate, initDate, typeOfWorkHoursCodeA);

        cc1_HourCostDTOs.add(cc1_1_HourCostDTO);
        final CostCategoryDTO cc1 = new CostCategoryDTO(costCategoryCode, "newCC1", true, cc1_HourCostDTOs);

        CostCategoryListDTO costCategoryListDTO = createCostCategoryListDTO(cc1);

        List<InstanceConstraintViolationsDTO> instanceConstraintViolationsList = costCategoryService
                .addCostCategories(costCategoryListDTO).instanceConstraintViolationsList;

        transactionService.runOnTransaction(new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                costCategoryDAO.flush();
                hourCostDAO.flush();
                return null;
            }
        });

        assertTrue(instanceConstraintViolationsList.toString(), instanceConstraintViolationsList.size() == 0);

        transactionService.runOnTransaction(new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                assertTrue(costCategoryDAO.existsByCode(cc1.code));
                return null;
            }
        });

        final CostCategory costCategory = transactionService.runOnTransaction(new IOnTransaction<CostCategory>() {
            @Override
            public CostCategory execute() {
                CostCategory cost;
                try {
                    cost = costCategoryDAO.findByCode(cc1.code);
                    cost.getHourCosts().size();
                    return cost;
                } catch (InstanceNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        assertTrue(costCategory.getHourCosts().size() == 1);

        final HourCost hourCost = transactionService.runOnTransaction(new IOnTransaction<HourCost>() {
            @Override
            public HourCost execute() {
                try {
                    HourCost cost = hourCostDAO
                            .findByCode(hourCostCode);
                    cost.getType().getCode();
                    return cost;
                } catch (InstanceNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        LocalDate currentDate = LocalDate.fromDateFields(new Date());
        assertTrue(hourCost.getInitDate().compareTo(currentDate) == 0);
        assertFalse(hourCost.getEndDate() == null);
        assertTrue(hourCost.getEndDate().compareTo(hourCost.getInitDate()) == 0);
        assertTrue(hourCost.getPriceCost().compareTo(new BigDecimal(3)) == 0);
        assertTrue(hourCost.getType().getCode().equalsIgnoreCase(typeOfWorkHoursCodeA));

        transactionService.runOnTransaction(new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                costCategoryDAO.flush();
                sessionFactory.getCurrentSession().evict(costCategory);
                return null;
            }
        });

        costCategory.dontPoseAsTransientObjectAnymore();

        // Update the previous cost category
        Set<HourCostDTO> cc2_HourCostDTOs = new HashSet<>();

        XMLGregorianCalendar initDate2 = DateConverter.toXMLGregorianCalendar(new Date());
        XMLGregorianCalendar endDate2 = DateConverter.toXMLGregorianCalendar(getNextMonthDate());

        HourCostDTO cc2_1_HourCostDTO =
                new HourCostDTO(hourCostCode, new BigDecimal(100), initDate2, endDate2, typeOfWorkHoursCodeB);

        cc2_HourCostDTOs.add(cc2_1_HourCostDTO);
        CostCategoryDTO cc2 = new CostCategoryDTO(costCategoryCode, "updateCC1", false, cc2_HourCostDTOs);

        /* Cost category type list */
        costCategoryListDTO = createCostCategoryListDTO(cc2);

        instanceConstraintViolationsList = costCategoryService
                .addCostCategories(costCategoryListDTO).instanceConstraintViolationsList;

        assertTrue(instanceConstraintViolationsList.toString(), instanceConstraintViolationsList.size() == 0);

        transactionService.runOnTransaction(new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                assertTrue(costCategoryDAO.existsByCode(cc1.code));
                assertTrue(hourCostDAO.existsByCode(cc1_1_HourCostDTO.code));
                return null;
            }
        });

        final CostCategory costCategory2 = transactionService.runOnTransaction(new IOnTransaction<CostCategory>() {
            @Override
            public CostCategory execute() {
                CostCategory cost;
                try {
                    cost = costCategoryDAO.findByCode(costCategoryCode);
                    cost.getHourCosts().size();
                    return cost;
                } catch (InstanceNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // Check if the changes was updated
        assertTrue(costCategory2.getHourCosts().size() == 1);
        assertTrue(costCategory2.getName().equalsIgnoreCase("updateCC1"));
        assertFalse(costCategory2.getEnabled());

        final HourCost hourCost2 = transactionService.runOnTransaction(new IOnTransaction<HourCost>() {
            @Override
            public HourCost execute() {
                try {
                    HourCost cost = hourCostDAO.findByCode(cc1_1_HourCostDTO.code);
                    cost.getType().getCode();
                    return cost;
                } catch (InstanceNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        LocalDate nextMonthDate = LocalDate.fromDateFields(getNextMonthDate());
        assertTrue(hourCost2.getInitDate().compareTo(LocalDate.fromDateFields(new Date())) == 0);
        assertFalse(hourCost2.getEndDate() == null);
        assertTrue(hourCost2.getEndDate().compareTo(nextMonthDate) == 0);
        assertTrue(hourCost2.getPriceCost().compareTo(new BigDecimal(100)) == 0);
        assertTrue(hourCost2.getType().getCode().equalsIgnoreCase(typeOfWorkHoursCodeB));
    }

    private Date getNextMonthDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(calendar.MONTH, 1);

        int date = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        calendar.set(year, month, date);
        return calendar.getTime();
    }

    private CostCategoryListDTO createCostCategoryListDTO(CostCategoryDTO... costCategories) {

        List<CostCategoryDTO> costCategoryList = new ArrayList<>();

        for (CostCategoryDTO c : costCategories) {
            costCategoryList.add(c);
        }

        return new CostCategoryListDTO(costCategoryList);

    }

}

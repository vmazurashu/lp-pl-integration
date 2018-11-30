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

package org.libreplan.business.test.workreports.daos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.libreplan.business.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_FILE;
import static org.libreplan.business.test.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_TEST_FILE;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.workreports.daos.IWorkReportDAO;
import org.libreplan.business.workreports.entities.WorkReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { BUSINESS_SPRING_CONFIG_FILE,
        BUSINESS_SPRING_CONFIG_TEST_FILE })
/*
 * @author Diego Pino García <dpino@igalia.com>
 */
public class WorkReportDAOTest extends AbstractWorkReportTest {

    @Autowired
    private IWorkReportDAO workReportDAO;

    @Test
    @Transactional
    public void testInSpringContainer() {
        assertNotNull(workReportDAO);
    }

    @Test
    @Transactional
    public void testSaveWorkReport() {
        WorkReport workReport = createValidWorkReport();
        workReportDAO.save(workReport);
        assertTrue(workReportDAO.exists(workReport.getId()));
    }

    @Test
    @Transactional
    public void testRemoveWorkReport() throws InstanceNotFoundException {
        WorkReport workReport = createValidWorkReport();
        workReportDAO.save(workReport);
        workReportDAO.remove(workReport.getId());
        assertFalse(workReportDAO.exists(workReport.getId()));
    }

    @Test
    @Transactional
    public void testListWorkReport() {
        int previous = workReportDAO.list(WorkReport.class).size();

        WorkReport workReport1 = createValidWorkReport();
        workReportDAO.save(workReport1);
        WorkReport workReport2 = createValidWorkReport();
        workReportDAO.save(workReport1);
        workReportDAO.save(workReport2);

        List<WorkReport> list = workReportDAO
                .list(WorkReport.class);
        assertEquals(previous + 2, list.size());
    }
}

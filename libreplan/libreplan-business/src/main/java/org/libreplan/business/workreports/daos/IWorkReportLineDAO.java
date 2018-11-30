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

package org.libreplan.business.workreports.daos;

import java.util.Date;
import java.util.List;

import org.libreplan.business.common.daos.IIntegrationEntityDAO;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.reports.dtos.WorkReportLineDTO;
import org.libreplan.business.resources.entities.Resource;
import org.libreplan.business.util.Pair;
import org.libreplan.business.workreports.entities.WorkReport;
import org.libreplan.business.workreports.entities.WorkReportLine;

/**
 * Dao for {@link WorkReportLine}
 *
 * @author Diego Pino García <dpino@igalia.com>
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
public interface IWorkReportLineDAO extends
        IIntegrationEntityDAO<WorkReportLine> {

    List<WorkReportLine> findByOrderElement(OrderElement orderElement);

    List<WorkReportLine> findByOrderElementAndChildren(OrderElement orderElement);

    List<WorkReportLine> findByOrderElementAndChildren(OrderElement orderElement, boolean sortByDate);

    List<WorkReportLine> findFilteredByDate(Date start, Date end);

    List<WorkReportLine> findByResources(List<Resource> resourcesList);

    List<WorkReportLineDTO> findByOrderElementGroupByResourceAndHourTypeAndDate(
            OrderElement orderElement);

    /**
     * Returns the {@link List} of {@link WorkReportLine WorkReportLines} of the
     * given <code>resource</code> between 2 dates that not belong to the
     * specified <code>workReport</code>.<br />
     *
     * If <code>workReport</code> is <code>null</code>, it returns all the
     * {@link WorkReportLine WorkReportLines} for this <code>resource</code>
     * between the dates.
     */
    List<WorkReportLine> findByResourceFilteredByDateNotInWorkReport(
            Resource resource, Date start, Date end, WorkReport workReport);

    Pair<Date, Date> findMinAndMaxDatesByOrderElement(
            OrderElement orderElement);

    List<WorkReportLine> findFinishedByOrderElementNotInWorkReportAnotherTransaction(
            OrderElement orderElement, WorkReport workReport);

    Boolean isFinished(OrderElement orderElement);

    List<WorkReportLine> findByOrderElementAndWorkReports(
            OrderElement orderElement, List<WorkReport> workReports);
    /**
     * Returns the {@link WorkReportLine}s of the specified
     * <code>orderElement</code> specified between <code>start</code> date and
     * <code>end</code> date
     */
    List<WorkReportLine> findByOrderElementAndChildrenFilteredByDate(
            OrderElement orderElement, Date start, Date end, boolean sortByDate);

}

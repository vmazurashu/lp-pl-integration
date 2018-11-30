/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2012 Igalia, S.L.
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

package org.libreplan.ws.boundusers.impl;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.joda.time.LocalDate;
import org.libreplan.business.common.IAdHocTransactionService;
import org.libreplan.business.common.IOnTransaction;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.orders.daos.IOrderElementDAO;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.workingday.EffortDuration;
import org.libreplan.business.workreports.daos.IWorkReportDAO;
import org.libreplan.business.workreports.daos.IWorkReportLineDAO;
import org.libreplan.business.workreports.entities.WorkReport;
import org.libreplan.business.workreports.entities.WorkReportLine;
import org.libreplan.web.users.dashboard.IMyTasksAreaModel;
import org.libreplan.web.users.dashboard.IPersonalTimesheetModel;
import org.libreplan.web.users.dashboard.UserDashboardUtil;
import org.libreplan.ws.boundusers.api.IBoundUserService;
import org.libreplan.ws.boundusers.api.PersonalTimesheetEntryDTO;
import org.libreplan.ws.boundusers.api.PersonalTimesheetEntryListDTO;
import org.libreplan.ws.boundusers.api.TaskListDTO;
import org.libreplan.ws.common.impl.DateConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * REST-based implementation of {@link IBoundUserService};
 *
 * @author Manuel Rego Casasnovas <rego@igalia.com>
 */
@Path("/bounduser/")
@Produces("application/xml")
@Service("boundUserServiceREST")
public class BoundUserServiceREST implements IBoundUserService {

    @Autowired
    private IMyTasksAreaModel myTasksAreaModel;

    @Autowired
    private IOrderElementDAO orderElementDAO;

    @Autowired
    private IWorkReportDAO workReportDAO;

    @Autowired
    private IWorkReportLineDAO workReportLineDAO;

    @Autowired
    private IPersonalTimesheetModel personalTimesheetModel;

    @Autowired
    private IAdHocTransactionService transactionService;

    @Override
    @GET
    @Transactional(readOnly = true)
    @Path("/mytasks/")
    public TaskListDTO getTasks() {
        return TaskConverter.toDTO(myTasksAreaModel.getTasks());
    }

    @Override
    @GET
    @Transactional(readOnly = true)
    @Path("/timesheets/{task-code}/")
    @SuppressWarnings("unchecked")
    public Response getTimesheetEntriesByTask(
            @PathParam("task-code") String taskCode) {
        try {
            OrderElement orderElement = orderElementDAO.findByCode(taskCode);
            List<WorkReport> workReports = workReportDAO
                    .findPersonalTimesheetsByResourceAndOrderElement(UserDashboardUtil
                            .getBoundResourceFromSession());
            List<WorkReportLine> workReportLines = workReportLineDAO
                    .findByOrderElementAndWorkReports(orderElement, workReports);
            Collections.sort(workReportLines);
            Collections.reverse(workReportLines);
            PersonalTimesheetEntryListDTO dto = PersonalTimesheetEntryConverter
                    .toDTO(workReportLines);
            return Response.ok(dto).build();
        } catch (InstanceNotFoundException e) {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @Override
    @POST
    @Path("/timesheets/")
    public Response importTimesheetEntries(PersonalTimesheetEntryListDTO dto) {
        for (final PersonalTimesheetEntryDTO each : dto.entries) {
            LocalDate date = DateConverter.toLocalDate(each.date);
            OrderElement orderElement = transactionService
                    .runOnReadOnlyTransaction(new IOnTransaction<OrderElement>() {
                        @Override
                        public OrderElement execute() {
                            try {
                                return orderElementDAO.findByCode(each.task);
                            } catch (InstanceNotFoundException e) {
                                return null;
                            }
                        }
                    });
            if (orderElement == null) {
                return Response.status(Status.NOT_FOUND).build();
            }
            EffortDuration effortDuration = EffortDuration
                    .parseFromFormattedString(each.effort);

            personalTimesheetModel.initCreateOrEdit(date);
            personalTimesheetModel.setEffortDuration(orderElement, date,
                    effortDuration);
            personalTimesheetModel.save();
        }

        return Response.ok().build();
    }

}

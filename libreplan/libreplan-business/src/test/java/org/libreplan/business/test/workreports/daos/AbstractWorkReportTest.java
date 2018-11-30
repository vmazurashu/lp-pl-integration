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

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.libreplan.business.costcategories.daos.ITypeOfWorkHoursDAO;
import org.libreplan.business.costcategories.entities.TypeOfWorkHours;
import org.libreplan.business.labels.daos.ILabelDAO;
import org.libreplan.business.labels.daos.ILabelTypeDAO;
import org.libreplan.business.labels.entities.Label;
import org.libreplan.business.labels.entities.LabelType;
import org.libreplan.business.orders.daos.IOrderElementDAO;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.orders.entities.OrderLine;
import org.libreplan.business.resources.daos.IResourceDAO;
import org.libreplan.business.resources.entities.Resource;
import org.libreplan.business.resources.entities.Worker;
import org.libreplan.business.workingday.EffortDuration;
import org.libreplan.business.workreports.daos.IWorkReportDAO;
import org.libreplan.business.workreports.daos.IWorkReportTypeDAO;
import org.libreplan.business.workreports.entities.WorkReport;
import org.libreplan.business.workreports.entities.WorkReportLabelTypeAssignment;
import org.libreplan.business.workreports.entities.WorkReportLine;
import org.libreplan.business.workreports.entities.WorkReportType;
import org.libreplan.business.workreports.valueobjects.DescriptionField;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractWorkReportTest {

    @Autowired
    ITypeOfWorkHoursDAO typeOfWorkHoursDAO;

    @Autowired
    IWorkReportTypeDAO workReportTypeDAO;

    @Autowired
    IWorkReportDAO workReportDAO;

    @Autowired
    IResourceDAO resourceDAO;

    @Autowired
    IOrderElementDAO orderElementDAO;

    @Autowired
    ILabelDAO labelDAO;

    @Autowired
    ILabelTypeDAO labelTypeDAO;

    public WorkReportType createValidWorkReportType() {
        return WorkReportType.create(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    public WorkReportLine createValidWorkReportLine() {
        Resource resource = createValidWorker();

        WorkReport workReport = createValidWorkReport();
        workReportDAO.save(workReport);

        WorkReportLine workReportLine = WorkReportLine.create(workReport);
        workReport.addWorkReportLine(workReportLine);
        workReportLine.setDate(new Date());
        workReportLine.setEffort(EffortDuration.hours(100));
        workReportLine.setResource(resource);
        workReportLine.setOrderElement(createValidOrderElement());
        workReportLine.setTypeOfWorkHours(createValidTypeOfWorkHours());

        return workReportLine;
    }

    private TypeOfWorkHours createValidTypeOfWorkHours() {
        TypeOfWorkHours typeOfWorkHours =
                TypeOfWorkHours.create(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        typeOfWorkHours.setDefaultPrice(BigDecimal.TEN);
        typeOfWorkHoursDAO.save(typeOfWorkHours);

        return typeOfWorkHours;

    }

    private Resource createValidWorker() {
        Worker worker = Worker.create();
        worker.setFirstName(UUID.randomUUID().toString());
        worker.setSurname(UUID.randomUUID().toString());
        worker.setNif(UUID.randomUUID().toString());
        resourceDAO.save(worker);

        return worker;
    }

    private OrderElement createValidOrderElement() {
        OrderLine orderLine = OrderLine.create();
        orderLine.setName(UUID.randomUUID().toString());
        orderLine.setCode(UUID.randomUUID().toString());
        orderElementDAO.save(orderLine);

        return orderLine;
    }

    public Set<WorkReportLine> createValidWorkReportLines() {
        Set<WorkReportLine> workReportLines = new HashSet<>();

        WorkReportLine workReportLine = createValidWorkReportLine();
        workReportLines.add(workReportLine);

        return workReportLines;
    }

    public WorkReport createValidWorkReport() {
        WorkReportType workReportType = createValidWorkReportType();
        workReportTypeDAO.save(workReportType);

        return WorkReport.create(workReportType);
    }

    public DescriptionField createValidDescriptionField() {
        return DescriptionField.create(UUID.randomUUID().toString(), 1);
    }

    public WorkReportLabelTypeAssignment createValidWorkReportLabelTypeAssignment() {
        LabelType labelType = LabelType.create(UUID.randomUUID().toString());
        labelTypeDAO.save(labelType);
        Label label = Label.create(UUID.randomUUID().toString());
        label.setType(labelType);
        labelDAO.save(label);

        WorkReportLabelTypeAssignment labelAssignment = WorkReportLabelTypeAssignment.create();
        labelAssignment.setDefaultLabel(label);
        labelAssignment.setLabelType(labelType);

        return labelAssignment;
    }

}

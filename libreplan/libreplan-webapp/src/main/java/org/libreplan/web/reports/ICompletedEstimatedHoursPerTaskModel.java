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

package org.libreplan.web.reports;

import java.util.Date;
import java.util.List;

import net.sf.jasperreports.engine.JRDataSource;

import org.libreplan.business.labels.entities.Label;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.resources.entities.Criterion;

/**
 *
 * @author Diego Pino Garcia <dpino@igalia.com>
 *
 */
public interface ICompletedEstimatedHoursPerTaskModel {

    JRDataSource getCompletedEstimatedHoursReportPerTask(Order order,
            Date referenceDate, List<Label> labels, List<Criterion> criterions);

    void init();

    List<Order> getOrders();

    List<Label> getAllLabels();

    void removeSelectedLabel(Label label);

    boolean addSelectedLabel(Label label);

    List<Label> getSelectedLabels();

    List<Criterion> getCriterions();

    void removeSelectedCriterion(Criterion criterion);

    boolean addSelectedCriterion(Criterion criterion);

    List<Criterion> getSelectedCriterions();

    String getSelectedCriteria();

    String getSelectedLabel();
}

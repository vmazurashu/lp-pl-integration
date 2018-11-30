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

package org.libreplan.web.limitingresources;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.LocalDate;
import org.libreplan.business.planner.limiting.entities.LimitingResourceQueueElement;
import org.zkoss.zul.Div;

public class QueueTask extends Div {

    private final LocalDate start;

    private final LocalDate end;

    private final LimitingResourceQueueElement element;

    public QueueTask(LimitingResourceQueueElement element) {
        Validate.notNull(element.getStartDate());
        Validate.notNull(element.getEndDate());
        Validate.isTrue(!(element.getStartDate()).isAfter(element.getEndDate()));

        this.start = element.getStartDate();
        this.end = element.getEndDate();
        this.element = element;
    }

    public LocalDate getStart() {
        return start;
    }

    public LocalDate getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public LimitingResourceQueueElement getLimitingResourceQueueElement() {
        return element;
    }

}

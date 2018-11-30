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

package org.libreplan.business.planner.entities.consolidations;

import java.math.BigDecimal;

import javax.validation.constraints.NotNull;
import org.joda.time.LocalDate;
import org.libreplan.business.common.BaseEntity;
import org.libreplan.business.workingday.IntraDayDate;

/**
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */

public abstract class ConsolidatedValue extends BaseEntity {

    private LocalDate date;
    private BigDecimal value;
    private IntraDayDate taskEndDate;

    public abstract boolean isCalculated();

    protected ConsolidatedValue() {

    }

    protected ConsolidatedValue(LocalDate date, BigDecimal value,
            IntraDayDate taskEndDate) {
        this.date = date;
        this.value = value;
        this.taskEndDate = taskEndDate;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalDate getDate() {
        return date;
    }

    @NotNull(message = "task end date not specified")
    public IntraDayDate getTaskEndDate() {
        return taskEndDate;
    }

}

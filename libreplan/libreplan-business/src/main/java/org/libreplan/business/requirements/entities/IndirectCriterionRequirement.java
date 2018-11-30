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

package org.libreplan.business.requirements.entities;

import org.apache.commons.lang3.BooleanUtils;
import javax.validation.constraints.NotNull;
import org.libreplan.business.orders.entities.HoursGroup;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.templates.entities.OrderElementTemplate;


/**
 *
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 * @author Diego Pino Garcia <dpino@igalia.com>
 */
public class IndirectCriterionRequirement extends CriterionRequirement{

    private DirectCriterionRequirement parent;

    private Boolean valid = true;

    public static IndirectCriterionRequirement create(IndirectCriterionRequirement criterionRequirement) {
        return create(criterionRequirement.getParent(), criterionRequirement.getCriterion());
    }

    public static IndirectCriterionRequirement create(
            DirectCriterionRequirement parent, Criterion criterion) {
        IndirectCriterionRequirement result = new IndirectCriterionRequirement(
                criterion);
        result.setNewObject(true);
        result.setParent(parent);
        return result;
    }

    /**
     * Constructor for hibernate. Do not use!
     */
    public IndirectCriterionRequirement() {

    }

    public IndirectCriterionRequirement(Criterion criterion) {
        super(criterion);
    }

    public IndirectCriterionRequirement(DirectCriterionRequirement parent,Criterion criterion,
            OrderElement orderElement,HoursGroup hoursGroup){
        super(criterion, orderElement, hoursGroup);
    }

    public IndirectCriterionRequirement(DirectCriterionRequirement parent, Criterion criterion,
            OrderElementTemplate orderElementTemplate, HoursGroup hoursGroup){
        super(criterion, orderElementTemplate, hoursGroup);
    }

    @NotNull(message = "parent not specified")
    public DirectCriterionRequirement getParent() {
        return parent;
    }

    public void setParent(DirectCriterionRequirement
            directCriterionRequirement) {
        this.parent = directCriterionRequirement;
    }

    @Override
    public boolean isValid() {
        return BooleanUtils.toBoolean(valid);
    }

    public void setValid(Boolean valid) {
        this.valid = BooleanUtils.toBoolean(valid);
    }

    @Override
    protected void ensureSpecificDataLoaded() {
    }
}

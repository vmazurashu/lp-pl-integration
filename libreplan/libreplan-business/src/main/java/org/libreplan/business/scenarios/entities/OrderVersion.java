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

package org.libreplan.business.scenarios.entities;

import org.apache.commons.lang3.Validate;
import javax.validation.constraints.NotNull;
import org.joda.time.DateTime;
import org.libreplan.business.common.BaseEntity;
import org.libreplan.business.orders.entities.Order;

/**
 * Version of an {@link Order} used in some {@link Scenario}.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public class OrderVersion extends BaseEntity {

    private Scenario ownerScenario;

    private DateTime modificationByOwnerTimestamp;

    public static OrderVersion createInitialVersion(Scenario ownerScenario) {
        return create(new OrderVersion(ownerScenario));
    }

    // Default constructor, needed by Hibernate
    protected OrderVersion() {
    }

    private OrderVersion(Scenario ownerScenario) {
        Validate.notNull(ownerScenario);
        this.ownerScenario = ownerScenario;
        this.modificationByOwnerTimestamp = new DateTime();
    }

    @NotNull(message = "owner scenario not specified")
    public Scenario getOwnerScenario() {
        return ownerScenario;
    }

    public boolean isOwnedBy(Scenario scenario) {
        return scenario.getId().equals(ownerScenario.getId());
    }

    public void savingThroughOwner() {
        modificationByOwnerTimestamp = new DateTime();
    }

    public boolean hasBeenModifiedAfter(DateTime time) {
        if (time == null || modificationByOwnerTimestamp == null) {
            return true;
        }
        return modificationByOwnerTimestamp.isAfter(time);
    }

}

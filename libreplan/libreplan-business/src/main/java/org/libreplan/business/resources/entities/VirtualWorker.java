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

package org.libreplan.business.resources.entities;

import java.util.List;

import javax.validation.constraints.AssertTrue;

import org.libreplan.business.common.Registry;


/**
 * This class models a VirtualWorker.
 *
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 * @author Fernando Bellas Permuy <fbellas@udc.es>
 */
public class VirtualWorker extends Worker {

    public static VirtualWorker create() {
        VirtualWorker virtualWorker = new VirtualWorker();
        virtualWorker.setNewObject(true);
        virtualWorker.setNif("(Virtual)");
        virtualWorker.setSurname("---");
        virtualWorker.getCalendar();
        return create(virtualWorker);
    }

    public static VirtualWorker create(String code) {
        VirtualWorker virtualWorker = new VirtualWorker();
        virtualWorker.setNewObject(true);
        virtualWorker.setNif("(Virtual)");
        virtualWorker.setSurname("---");
        virtualWorker.getCalendar();
        return create(virtualWorker, code);
    }

    private String observations;

    /**
     * Constructor for hibernate. Do not use!
     */
    public VirtualWorker() {
    }

    @Override
    public String getDescription() {
        return getFirstName();
    }

    @Override
    public String getShortDescription() {
        return getFirstName() + " " + getNif();
    }

    public String getName() {
        return getFirstName();
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    @AssertTrue
    @Override
    public boolean isUniqueFiscalCodeConstraint() {
        return true;
    }

    @AssertTrue(message = "Virtual worker group name must be unique")
    public boolean isUniqueVirtualGroupNameConstraint() {

        List<Worker> list = Registry.getWorkerDAO().findByFirstNameAnotherTransactionCaseInsensitive(this.getFirstName());

        if ((isNewObject() && list.isEmpty()) || list.isEmpty()) {
            return true;
        } else {
            return list.get(0).getId().equals(getId());
        }
    }

    @Override
    public Boolean isLimitingResource() {
        return false;
    }

    @Override
    public String getHumanId() {
        return getFirstName();
    }

}

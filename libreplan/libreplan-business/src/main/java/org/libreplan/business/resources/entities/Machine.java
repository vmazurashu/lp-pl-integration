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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import javax.validation.Valid;

/**
 * Represents entity. It is another type of work resource.
 *
 * @author Javier Moran Rua <jmoran@igalia.com>
 * @author Fernando Bellas Permuy <fbellas@udc.es>
 */
public class Machine extends Resource {

    private final static ResourceEnum type = ResourceEnum.MACHINE;

    private String name;

    private String description;

    private Set<MachineWorkersConfigurationUnit> configurationUnits = new HashSet<>();

    @Valid
    public Set<MachineWorkersConfigurationUnit> getConfigurationUnits() {
        return Collections.unmodifiableSet(configurationUnits);
    }

    public void addMachineWorkersConfigurationUnit(MachineWorkersConfigurationUnit unit) {
        configurationUnits.add(unit);
    }

    public void removeMachineWorkersConfigurationUnit(MachineWorkersConfigurationUnit unit) {
        configurationUnits.remove(unit);
    }

    public static Machine createUnvalidated(String code, String name, String description) {

        Machine machine = create(new Machine(), code);

        machine.name = name;
        machine.description = description;

        return machine;

    }

    public void updateUnvalidated(String name, String description) {

        if (!StringUtils.isBlank(name)) {
            this.name = name;
        }

        if (!StringUtils.isBlank(description)) {
            this.description = description;
        }

    }

    /**
     * Used by Hibernate. Do not use!
     */
    protected Machine() {}

    public static Machine create() {
        return create(new Machine());
    }

    public static Machine create(String code) {
        return create(new Machine(), code);
    }

    @NotEmpty(message = "machine name not specified")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public String getShortDescription() {
        return name + " (" + getCode() + ")";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    protected boolean isCriterionSatisfactionOfCorrectType(CriterionSatisfaction c) {
        return c.getResourceType().equals(ResourceEnum.MACHINE);
    }

    @Override
    public ResourceEnum getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("MACHINE: %s", name);
    }

    @Override
    public String getHumanId() {
        return name;
    }
}

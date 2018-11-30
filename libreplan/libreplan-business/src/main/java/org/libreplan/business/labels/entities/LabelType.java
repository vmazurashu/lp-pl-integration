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

package org.libreplan.business.labels.entities;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import javax.validation.constraints.AssertTrue;
import org.hibernate.validator.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.libreplan.business.common.IHumanIdentifiable;
import org.libreplan.business.common.IntegrationEntity;
import org.libreplan.business.common.Registry;
import org.libreplan.business.common.entities.EntitySequence;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.labels.daos.ILabelTypeDAO;

/**
 * LabeType entity
 *
 * Note: this class has a natural ordering that is inconsistent with equals.
 *
 * @author Diego Pino Garcia<dpino@igalia.com>
 */

public class LabelType extends IntegrationEntity implements Comparable, IHumanIdentifiable {

    @NotEmpty(message = "name not specified")
    private String name;

    private Set<Label> labels = new HashSet<>();

    private Integer lastLabelSequenceCode = 0;

    // Default constructor, needed by Hibernate; at least package visibility
    protected LabelType() {
    }

    public static LabelType create(String name) {
        return create(new LabelType(name));
    }

    public static LabelType create(String code, String name) {
        return create(new LabelType(name), code);
    }

    protected LabelType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Label> getLabels() {
        return Collections.unmodifiableSet(labels);
    }

    public void addLabel(Label label) {
        Validate.notNull(label);
        labels.add(label);
        label.setType(this);
    }

    public void removeLabel(Label label) {
        labels.remove(label);
    }

    @Override
    public int compareTo(Object arg0) {
        if ( getName() != null ) {
            return getName().compareTo(((LabelType) arg0).getName());
        }
        return -1;
    }

    @Override
    protected ILabelTypeDAO getIntegrationEntityDAO() {
        return Registry.getLabelTypeDAO();
    }

    @AssertTrue(message = "label names must be unique inside a label type")
    public boolean isNonRepeatedLabelNamesConstraint() {
        Set<String> labelNames = new HashSet<>();

        for (Label label : labels) {
            if ( !StringUtils.isBlank(label.getName()) ) {
                if ( labelNames.contains(label.getName().toLowerCase()) ) {
                    return false;
                } else {
                    labelNames.add(label.getName().toLowerCase());
                }
            }
        }

        return true;
    }

    @AssertTrue(message = "label type name is already in use")
    public boolean isUniqueLabelTypeNameConstraint() {
        if ( !firstLevelValidationsPassed() ) {
            return true;
        }

        ILabelTypeDAO labelTypeDAO = Registry.getLabelTypeDAO();

        if ( isNewObject() ) {
            return !labelTypeDAO.existsByNameAnotherTransaction(this);
        } else {
            try {
                LabelType c = labelTypeDAO.findUniqueByNameAnotherTransaction(name);

                return c.getId().equals(getId());
            } catch (InstanceNotFoundException e) {
                return true;
            }
        }
    }

    private boolean firstLevelValidationsPassed() {
        return !StringUtils.isBlank(name);
    }

    public void updateUnvalidated(String name) {
        if ( !StringUtils.isBlank(name) ) {
            this.name = name;
        }
    }

    @AssertTrue(message = "label code is already being used")
    public boolean isNonRepeatedMaterialCodesConstraint() {
        return getFirstRepeatedCode(this.getLabels()) == null;
    }

    public Label getLabelByCode(String code) throws InstanceNotFoundException {

        if ( StringUtils.isBlank(code) ) {
            throw new InstanceNotFoundException(code, Label.class.getName());
        }

        for (Label l : labels) {
            if ( l.getCode().equalsIgnoreCase(StringUtils.trim(code)) ) {
                return l;
            }
        }

        throw new InstanceNotFoundException(code, Label.class.getName());

    }

    public void generateLabelCodes(int numberOfDigits) {
        boolean condition;

        for (Label label : this.getLabels()) {

            condition = (label.getCode() == null) ||
                    (label.getCode().isEmpty()) ||
                    (!label.getCode().startsWith(this.getCode()));

            if ( condition ) {
                this.incrementLastLabelSequenceCode();
                String labelCode = EntitySequence.formatValue(numberOfDigits, this.getLastLabelSequenceCode());
                label.setCode(this.getCode() + EntitySequence.CODE_SEPARATOR_CHILDREN + labelCode);
            }
        }
    }

    public void incrementLastLabelSequenceCode() {
        if ( this.lastLabelSequenceCode == null ) {
            this.lastLabelSequenceCode = 0;
        }
        this.lastLabelSequenceCode++;
    }

    @NotNull(message = "last label sequence code not specified")
    public Integer getLastLabelSequenceCode() {
        return lastLabelSequenceCode;
    }

    @Override
    public String getHumanId() {
        return name;
    }

}

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hibernate.validator.constraints.NotEmpty;
import org.libreplan.business.advance.entities.AdvanceAssignment;
import org.libreplan.business.common.IHumanIdentifiable;
import org.libreplan.business.common.IntegrationEntity;
import org.libreplan.business.common.Registry;
import org.libreplan.business.common.entities.EntitySequence;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.resources.daos.ICriterionTypeDAO;
import org.springframework.stereotype.Component;
/**
 * Base implementation of {@link ICriterionType}.
 * <br />

 * @author Diego Pino García <dpino@igalia.com>
 * @author Fernando Bellas Permuy <fbellas@udc.es>
 */
@Component
public class CriterionType extends IntegrationEntity implements ICriterionType<Criterion>, IHumanIdentifiable {

    public static CriterionType create() {
        return create(new CriterionType());
    }

    public static CriterionType create(String code) {
        return create(new CriterionType(), code);
    }

    public static CriterionType createUnvalidated(String code, String name,
        String description, Boolean allowHierarchy,
        Boolean allowSimultaneousCriterionsPerResource, Boolean enabled,
        ResourceEnum resource) {

        CriterionType criterionType = create(new CriterionType(), code);

        criterionType.name = name;
        criterionType.description = description;

        if (allowHierarchy != null) {
            criterionType.allowHierarchy = allowHierarchy;
        }

        if (allowSimultaneousCriterionsPerResource != null) {
            criterionType.allowSimultaneousCriterionsPerResource =
                allowSimultaneousCriterionsPerResource;
        }

        if (enabled != null) {
            criterionType.enabled = enabled;
        }

        if (resource != null) {
            criterionType.resource = resource;
        }

        return criterionType;

    }

    public void updateUnvalidated(String name, String description, Boolean allowHierarchy,
                                  Boolean allowSimultaneousCriterionsPerResource, Boolean enabled, ResourceEnum resource) {

        if (!StringUtils.isBlank(name)) {
            this.name = name;
        }

        if (!StringUtils.isBlank(description)) {
            this.description = description;
        }

        if (allowHierarchy != null) {
            this.allowHierarchy = allowHierarchy;
        }

        if (allowSimultaneousCriterionsPerResource != null) {
            this.allowSimultaneousCriterionsPerResource =
                allowSimultaneousCriterionsPerResource;
        }

        if (enabled != null) {
            this.enabled = enabled;
        }

        if (resource != null) {
            this.resource = resource;
        }

    }

    public static CriterionType create(String name,String description) {
        return create(new CriterionType(name,description));
    }

    public static CriterionType create(String name, String description, boolean allowHierarchy,
                                       boolean allowSimultaneousCriterionsPerResource, boolean enabled,
                                       ResourceEnum resource) {

        return create(new CriterionType(
                name, description, allowHierarchy, allowSimultaneousCriterionsPerResource, enabled, resource));

    }

    private String name;

    /**
     * The original name of the criterion type.
     * It only exists for CriterionTypes created from {@link PredefinedCriterionTypes}.
     * Important: This value must <strong>not</strong> be editable and should only be set at creation time.
     */
    private String predefinedTypeInternalName;

    private String description;

    private Boolean allowHierarchy = true;

    private Boolean allowSimultaneousCriterionsPerResource = true;

    private Boolean enabled = true;

    private ResourceEnum resource = ResourceEnum.getDefault();

    private Set<Criterion> criterions = new HashSet<>();

    private Integer lastCriterionSequenceCode = 0;

    /**
     * Constructor for hibernate. Do not use!
     */
    public CriterionType() {
    }

    private CriterionType(String name,String description) {
        this.name = name;
        this.description = description;
    }

    private CriterionType(String name, String description, boolean allowHierarchy,
                          boolean allowSimultaneousCriterionsPerResource, boolean enabled, ResourceEnum resource) {

        this.allowHierarchy = allowHierarchy;
        this.allowSimultaneousCriterionsPerResource = allowSimultaneousCriterionsPerResource;
        this.enabled = enabled;
        this.name = name;
        this.description = description;
        this.resource = resource;
    }

    public static CriterionType fromPredefined(PredefinedCriterionTypes predefinedType) {
        CriterionType result = asCriterionType(predefinedType);
        result.predefinedTypeInternalName = predefinedType.getName();
        return result;
    }

    public static CriterionType asCriterionType(ICriterionType<?> criterionType) {
        return create(
                criterionType.getName(),
                criterionType.getDescription(),
                criterionType.allowHierarchy(),
                criterionType.isAllowSimultaneousCriterionsPerResource(),
                criterionType.isEnabled(),
                CriterionType.getResource(criterionType));
    }

    private static ResourceEnum getResource(ICriterionType<?> criterionType) {
        for (ResourceEnum resource : ResourceEnum.values()) {
            if (criterionType.criterionCanBeRelatedTo(resource.asClass())) {
                return resource;
            }
        }

        return ResourceEnum.getDefault();
    }

    @Override
    @NotEmpty(message="criterion type name not specified")
    public String getName() {
        return name;
    }

    public String getPredefinedTypeInternalName() {
        return predefinedTypeInternalName;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Valid
    public Set<Criterion> getCriterions() {
        return criterions;
    }

    public List<Criterion> getSortCriterions() {
        List<Criterion> criterions = new ArrayList<>(getCriterions());

        Collections.sort(criterions, new Comparator<Criterion>() {
            @Override
            public int compare(Criterion o1, Criterion o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        return criterions;
    }

    public void setCriterions(Set<Criterion> criterions) {
        this.criterions = criterions;
    }

    @Override
    public boolean allowHierarchy() {
        return allowHierarchy == null ? false : allowHierarchy;
    }

    public void setAllowHierarchy(boolean allowHierarchy) {
        this.allowHierarchy = allowHierarchy;
    }

    @Override
    public boolean isAllowSimultaneousCriterionsPerResource() {
        return allowSimultaneousCriterionsPerResource == null ? false : allowSimultaneousCriterionsPerResource;
    }

    public void setAllowSimultaneousCriterionsPerResource(boolean allowSimultaneousCriterionsPerResource) {
        this.allowSimultaneousCriterionsPerResource = allowSimultaneousCriterionsPerResource;
    }

    public ResourceEnum getResource() {
        return resource;
    }

    public void setResource(ResourceEnum resource) {
        this.resource = resource;
    }

    @Override
    public Criterion createCriterion(String name) {
        return Criterion.withNameAndType(name, this);
    }

    public static Criterion createCriterion(PredefinedCriterionTypes predefinedCriterionType, String name) {
        return Criterion.withNameAndType(name, CriterionType.fromPredefined(predefinedCriterionType));
    }

    @Override
    public Criterion createCriterionWithoutNameYet() {
        return Criterion.ofType(this);
    }

    @Override
    public boolean contains(ICriterion criterion) {
        if (criterion instanceof Criterion) {
            Criterion c = (Criterion) criterion;
            return this.equals(c.getType());
        } else {
            return false;
        }
    }

    /**
     * A {@link CriterionType} can be related with {@link Resource} matching
     * attribute resource and it's always related with resource of type RESOURCE.
     */
    @Override
    public boolean criterionCanBeRelatedTo(Class<? extends Resource> klass) {
        return getResource().isAssignableFrom(klass);
    }


    /**
     * Two criterion types are equals if they both got the same name.
     *
     * @param o
     * @return boolean
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CriterionType)) {
            return false;
        }

        CriterionType criterionType = (CriterionType) o;

        return new EqualsBuilder().append(criterionType.getName(), this.getName()).isEquals();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled == null ? false : enabled;
    }

    @Override
    public boolean isImmutable() {
        return !isEnabled();
    }


    public int getNumCriterions() {
        return criterions.size();
    }

    public Criterion getCriterion(String criterionName) throws InstanceNotFoundException {

        for (Criterion c : criterions) {
            if (c.getName().equalsIgnoreCase(criterionName)) {
                return c;
            }
        }

        throw new InstanceNotFoundException(criterionName, Criterion.class.getName());

    }

    public Criterion getCriterionByCode(String code) throws InstanceNotFoundException {

        if (StringUtils.isBlank(code)) {
            throw new InstanceNotFoundException(code, Criterion.class.getName());
        }

        for (Criterion c : criterions) {
            if (c.getCode().equalsIgnoreCase(StringUtils.trim(code))) {
                return c;
            }
        }

        throw new InstanceNotFoundException(code, Criterion.class.getName());
    }

    public Criterion getExistingCriterionByCode(String code) {

        try {
            return getCriterionByCode(code);
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    public boolean existsCriterionByCode(String code) {

        try {
            getCriterionByCode(code);
            return true;
        } catch (InstanceNotFoundException e) {
            return false;
        }

    }

    @AssertTrue(message="criterion codes must be unique inside a criterion type")
    public boolean isConstraintNonRepeatedCriterionCodesConstraint() {
        return getFirstRepeatedCode(criterions) == null;
    }

    @AssertTrue(message="criterion names must be unique inside a criterion type")
    public boolean isNonRepeatedCriterionNamesConstraint() {

        Set<String> criterionNames = new HashSet<>();

        for (Criterion c : criterions) {
            if (!StringUtils.isBlank(c.getName())) {
                if (criterionNames.contains(c.getName().toLowerCase())) {
                    return false;
                } else {
                    criterionNames.add(c.getName().toLowerCase());
                }
            }
        }

        return true;

    }

    @AssertTrue(message="Criterion Type name is already being used")
    public boolean isUniqueCriterionTypeNameConstraint() {

        /* Check if it makes sense to check the constraint */
        if (!isNameSpecified()) {
            return true;
        }

        /* Check the constraint */
        ICriterionTypeDAO criterionTypeDAO = Registry.getCriterionTypeDAO();

        if (isNewObject()) {
            return !criterionTypeDAO.existsByNameAnotherTransaction(this);
        } else {
            try {
                CriterionType c = criterionTypeDAO.findUniqueByNameAnotherTransaction(name);
                return c.getId().equals(getId());
            } catch (InstanceNotFoundException e) {
                return true;
            }

        }

    }

    @AssertTrue(message="criterion type does not allow hierarchy")
    public boolean isAllowHierarchyConstraint() {

        if (!allowHierarchy) {
            for (Criterion c : criterions) {
                if (c.getParent() != null) {
                    return false;
                }
            }
        }

        return true;

    }

    @AssertTrue(message="resource type does not allow enabled criteria")
    public boolean isEnabledConstraint() {

        if (!enabled) {
            for (Criterion c : criterions) {
                if (c.isActive()) {
                    return false;
                }
            }
        }

        return true;

    }

    private boolean isNameSpecified() {
        return !StringUtils.isBlank(name);
    }


    @Override
    protected ICriterionTypeDAO getIntegrationEntityDAO() {
        return Registry.getCriterionTypeDAO();
    }

    /**
     * It checks there are no {@link AdvanceAssignment} any criteria of this
     * {@link CriterionType} has been assigned to any {@link Resource}.
     */
    @AssertTrue(message = "Criteria of this criterion type have been assigned to some resource.")
    protected boolean isChangeTypeConstraint() {
        /* Check the constraint */
        ICriterionTypeDAO criterionTypeDAO = Registry.getCriterionTypeDAO();

        if (isNewObject()) {
            return true;
        }

        if (!criterionTypeDAO.hasDifferentTypeSaved(getId(), getResource())) {
            return true;
        }

        return (!(criterionTypeDAO.checkChildrenAssignedToAnyResource(this)));
    }

    @NotNull(message = "last criterion sequence code not specified")
    public Integer getLastCriterionSequenceCode() {
        return lastCriterionSequenceCode;
    }

    public void incrementLastCriterionSequenceCode() {
        if (this.lastCriterionSequenceCode == null) {
            this.lastCriterionSequenceCode = 0;
        }
        this.lastCriterionSequenceCode++;
    }

    public void setGenerateCode(Criterion criterion, int numberOfDigits) {
        if ( (criterion.getCode() == null) ||
                (criterion.getCode().isEmpty()) ||
                (!criterion.getCode().startsWith(getCode())) ) {

            incrementLastCriterionSequenceCode();
            String criterionCode = EntitySequence.formatValue(numberOfDigits, getLastCriterionSequenceCode());
            criterion.setCode(getCode() + EntitySequence.CODE_SEPARATOR_CHILDREN + criterionCode);
        }
    }

    public String toString() {
        return name;
    }

    @Override
    public String getHumanId() {
        return name;
    }

}

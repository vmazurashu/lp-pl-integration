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
package org.libreplan.business.orders.entities;

import static org.libreplan.business.i18n.I18nHelper._;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.Validate;
import org.libreplan.business.common.IntegrationEntity;
import org.libreplan.business.common.Registry;
import org.libreplan.business.common.daos.IIntegrationEntityDAO;
import org.libreplan.business.requirements.entities.CriterionRequirement;
import org.libreplan.business.requirements.entities.DirectCriterionRequirement;
import org.libreplan.business.requirements.entities.IndirectCriterionRequirement;
import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.resources.entities.ResourceEnum;
import org.libreplan.business.templates.entities.OrderLineTemplate;

public class HoursGroup extends IntegrationEntity implements Cloneable, ICriterionRequirable {

    private ResourceEnum resourceType = ResourceEnum.WORKER;

    private Integer workingHours = 0;

    private BigDecimal percentage = new BigDecimal(0).setScale(2);

    private Boolean fixedPercentage = false;

    private Set<CriterionRequirement> criterionRequirements = new HashSet<>();

    private OrderLine parentOrderLine;

    private OrderLineTemplate orderLineTemplate;

    private HoursGroup origin;

    protected CriterionRequirementOrderElementHandler criterionRequirementHandler =
                CriterionRequirementOrderElementHandler.getInstance();

    /**
     * Constructor for hibernate. Do not use!
     */
    public HoursGroup() {}

    private HoursGroup(OrderLine parentOrderLine) {
        this.parentOrderLine = parentOrderLine;
        String code = parentOrderLine.getCode();
        this.setCode(code != null ? code : "");
        this.setOrderLineTemplate(null);
    }

    private HoursGroup(OrderLineTemplate orderLineTemplate) {
        this.orderLineTemplate = orderLineTemplate;
        this.setParentOrderLine(null);
    }

    public static HoursGroup create(OrderLine parentOrderLine) {
        HoursGroup result = new HoursGroup(parentOrderLine);
        result.setNewObject(true);

        return result;
    }

    public static HoursGroup create(OrderLineTemplate orderLineTemplate) {
        HoursGroup result = new HoursGroup(orderLineTemplate);
        result.setNewObject(true);

        return result;
    }

    public static HoursGroup createUnvalidated(String code, ResourceEnum resourceType, Integer workingHours) {
        HoursGroup result = create(new HoursGroup());
        result.setCode(code);
        result.setResourceType(resourceType);
        result.setWorkingHours(workingHours);

        return result;
    }

    /**
     * Returns a copy of hoursGroup, and sets parent as its parent.
     *
     * @param hoursGroup
     * @param parent
     * @return {@link HoursGroup}
     */
    public static HoursGroup copyFrom(HoursGroup hoursGroup, OrderLineTemplate parent) {
        HoursGroup result = copyFrom(hoursGroup);

        result.setCriterionRequirements(
                copyDirectCriterionRequirements(result, hoursGroup.getDirectCriterionRequirement()));

        result.setOrderLineTemplate(parent);
        result.setParentOrderLine(null);

        return result;
    }

    private static Set<CriterionRequirement> copyDirectCriterionRequirements(
            HoursGroup hoursGroup, Collection<DirectCriterionRequirement> criterionRequirements) {

        Set<CriterionRequirement> result = new HashSet<>();

        for (DirectCriterionRequirement each: criterionRequirements) {

            DirectCriterionRequirement newDirectCriterionRequirement =
                    DirectCriterionRequirement.copyFrom(each, hoursGroup);

            newDirectCriterionRequirement.setHoursGroup(hoursGroup);
            result.add(newDirectCriterionRequirement);
        }
        return result;
    }

    public static HoursGroup copyFrom(HoursGroup hoursGroup, OrderLine parent) {
        HoursGroup result = copyFrom(hoursGroup);

        result.setCriterionRequirements(
                copyDirectCriterionRequirements(result, hoursGroup.getDirectCriterionRequirement()));

        result.setOrderLineTemplate(null);
        result.setParentOrderLine(parent);

        return result;
    }

    private static HoursGroup copyFrom(HoursGroup hoursGroup) {
        HoursGroup result = createUnvalidated(
                hoursGroup.getCode(),
                hoursGroup.getResourceType(),
                hoursGroup.getWorkingHours());

        result.setCode(UUID.randomUUID().toString());
        result.percentage = hoursGroup.getPercentage();
        result.fixedPercentage = hoursGroup.isFixedPercentage();
        result.origin = hoursGroup;

        return result;
    }

    public ResourceEnum getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceEnum resource) {
        Validate.notNull(resource);
        this.resourceType = resource;
    }

    public void setWorkingHours(Integer workingHours) throws IllegalArgumentException {
        if ( (workingHours != null) && (workingHours < 0) ) {
            throw new IllegalArgumentException("Working hours should not be negative");
        }

        if ( workingHours == null ) {
            workingHours = 0;
        }
        this.workingHours = workingHours;
    }

    @NotNull(message = "working hours not specified")
    public Integer getWorkingHours() {
        return workingHours;
    }

    /**
     * @param proportion
     *            It's one based, instead of one hundred based
     * @throws IllegalArgumentException
     *             if the new sum of percentages in the parent {@link OrderLine}
     *             surpasses one
     */
    public void setPercentage(BigDecimal proportion) throws IllegalArgumentException {
        BigDecimal oldPercentage = this.percentage;

        this.percentage = proportion;

        if ( !isPercentageValidForParent() ) {
            this.percentage = oldPercentage;
            throw new IllegalArgumentException(_("Total percentage should be less than 100%"));
        }
    }

    private boolean isPercentageValidForParent() {
        return (parentOrderLine != null) ? parentOrderLine.isPercentageValid() : orderLineTemplate.isPercentageValid();
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setFixedPercentage(Boolean fixedPercentage) {
        this.fixedPercentage = fixedPercentage;
    }

    public Boolean isFixedPercentage() {
        return this.fixedPercentage;
    }

    public void setCriterionRequirements(Set<CriterionRequirement> criterionRequirements) {
        this.criterionRequirements = criterionRequirements;
    }

    @Valid
    @Override
    public Set<CriterionRequirement> getCriterionRequirements() {
        return criterionRequirements;
    }

    public Set<Criterion> getValidCriterions() {
        Set<Criterion> criterions = new HashSet<>();
        for (CriterionRequirement criterionRequirement : getDirectCriterionRequirement()) {
            criterions.add(criterionRequirement.getCriterion());
        }

        for (IndirectCriterionRequirement requirement : getIndirectCriterionRequirement()) {
            if ( requirement.isValid() ) {
                criterions.add(requirement.getCriterion());
            }
        }
        return Collections.unmodifiableSet(criterions);
    }

    @Override
    public void addCriterionRequirement(CriterionRequirement requirement) {
        if ( !isValidResourceType(requirement) ) {
            throw new IllegalStateException(
                    "Criterion cannot be assigned to this Hours Group. Criterion Resource Type is of a different type");
        }

        if ( existSameCriterionRequirement(requirement) ) {
            throw new IllegalStateException(
                    "Criterion cannot be assigned to this Hours Group. Criterion already exist within Hours Group");

        }
        requirement.setHoursGroup(this);
        criterionRequirements.add(requirement);
    }

    public boolean canAddCriterionRequirement(CriterionRequirement newRequirement) {
        return !((isValidResourceType(newRequirement)) && (!existSameCriterionRequirement(newRequirement)));
    }

    @Override
    public void removeCriterionRequirement(CriterionRequirement requirement) {
        criterionRequirements.remove(requirement);
        if ( requirement instanceof IndirectCriterionRequirement ) {
            ((IndirectCriterionRequirement) requirement).getParent().getChildren().remove(requirement);
        }
        requirement.setCriterion(null);
        requirement.setHoursGroup(null);
        requirement.setOrderElement(null);
    }

    public void setParentOrderLine(OrderLine parentOrderLine) {
        this.parentOrderLine = parentOrderLine;
    }

    public OrderLine getParentOrderLine() {
        return parentOrderLine;
    }

    public void updateMyCriterionRequirements() {
        Set<CriterionRequirement> requirementsParent = criterionRequirementHandler
                .getRequirementWithSameResourType(getCriterionRequirementsFromParent(), resourceType);

        Set<IndirectCriterionRequirement> currentIndirects = criterionRequirementHandler
                .getCurrentIndirectRequirements(getIndirectCriterionRequirement(), requirementsParent);

        criterionRequirementHandler.removeOldIndirects(this, currentIndirects);
        criterionRequirementHandler.addNewsIndirects(this, currentIndirects);
    }

    public void propagateIndirectCriterionRequirementsKeepingValid() {
        updateMyCriterionRequirements();

        // Set valid value as original value for every indirect
        Map<Criterion, Boolean> mapCriterionToValid =
                createCriterionToValidMap(origin.getIndirectCriterionRequirement());

        for (CriterionRequirement each : criterionRequirements) {
            if ( each instanceof IndirectCriterionRequirement ) {
                IndirectCriterionRequirement indirect = (IndirectCriterionRequirement) each;
                indirect.setValid(mapCriterionToValid.get(each.getCriterion()));
            }
        }
    }

    private Map<Criterion, Boolean> createCriterionToValidMap(Set<IndirectCriterionRequirement> indirects) {
        Map<Criterion, Boolean> result = new HashMap<>();

        for (IndirectCriterionRequirement each : indirects) {
            result.put(each.getCriterion(), each.isValid());
        }
        return result;
    }

    private Set<CriterionRequirement> getCriterionRequirementsFromParent() {
        return (parentOrderLine != null)
                ? parentOrderLine.getCriterionRequirements()
                : orderLineTemplate.getCriterionRequirements();
    }

    public Set<IndirectCriterionRequirement> getIndirectCriterionRequirement() {
        Set<IndirectCriterionRequirement> list = new HashSet<>();
        for (CriterionRequirement criterionRequirement : criterionRequirements ) {
            if ( criterionRequirement instanceof IndirectCriterionRequirement ) {
                list.add((IndirectCriterionRequirement) criterionRequirement);
            }
        }
        return list;
    }

    public Set<DirectCriterionRequirement> getDirectCriterionRequirement() {
        Set<DirectCriterionRequirement> list = new HashSet<>();
        for (CriterionRequirement criterionRequirement : criterionRequirements ) {
            if ( criterionRequirement instanceof DirectCriterionRequirement ) {
                list.add((DirectCriterionRequirement) criterionRequirement);
            }
        }
        return list;
    }

    public boolean isValidResourceType(CriterionRequirement newRequirement) {
        ResourceEnum resourceTypeRequirement = newRequirement.getCriterion().getType().getResource();

        return resourceType == null ||
                (resourceType.equals(resourceTypeRequirement) ||
                        (resourceTypeRequirement.equals(ResourceEnum.getDefault())));
    }

    /**
     * Duplicate of {@link HoursGroup#isValidResourceType(CriterionRequirement)}.
     * Needed because in my case I do not need to check equality with {@link ResourceEnum#getDefault()}.
     */
    public boolean isValidResourceTypeChanged(CriterionRequirement newRequirement) {
        return resourceType == null || resourceType.equals(newRequirement.getCriterion().getType().getResource());
    }

    boolean existSameCriterionRequirement(CriterionRequirement newRequirement) {
        Criterion criterion = newRequirement.getCriterion();
        for (CriterionRequirement requirement : getCriterionRequirements()){
            if ( requirement.getCriterion().equals(criterion) ) {
                return true;
            }
        }
        return false;
    }

    public OrderLineTemplate getOrderLineTemplate() {
        return orderLineTemplate;
    }

    public void setOrderLineTemplate(OrderLineTemplate orderLineTemplate) {
        this.orderLineTemplate = orderLineTemplate;
    }

    public HoursGroup getOrigin() {
        return origin;
    }

    public void setOrigin(HoursGroup origin) {
        this.origin = origin;
    }

    @Override
    protected IIntegrationEntityDAO<? extends IntegrationEntity> getIntegrationEntityDAO() {
        return Registry.getHoursGroupDAO();
    }

    /**
     * The automatic checking of this constraint is avoided because it uses the wrong code property.
     */
    @Override
    public boolean isUniqueCodeConstraint() {
        return true;
    }

}

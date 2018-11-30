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

package org.libreplan.business.costcategories.entities;

import static org.libreplan.business.i18n.I18nHelper._;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.LocalDate;
import org.libreplan.business.common.IHumanIdentifiable;
import org.libreplan.business.common.IntegrationEntity;
import org.libreplan.business.common.Registry;
import org.libreplan.business.common.entities.EntitySequence;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.costcategories.daos.ICostCategoryDAO;

/**
 * @author Jacobo Aragunde Perez <jaragunde@igalia.com>
 * @author Diego Pino García <dpino@igalia.com>
 */
public class CostCategory extends IntegrationEntity implements IHumanIdentifiable {

    private String name;

    private boolean enabled = true;

    private Integer lastHourCostSequenceCode = 0;

    @Valid
    private Set<HourCost> hourCosts = new HashSet<HourCost>();

    private ICostCategoryDAO costCategoryDAO = Registry.getCostCategoryDAO();

    // Default constructor, needed by Hibernate
    protected CostCategory() {

    }

    public static CostCategory createUnvalidated(String code, String name,
            Boolean enabled) {
        CostCategory costCategory = create(new CostCategory(), code);
        costCategory.name = name;
        if (enabled != null) {
            costCategory.enabled = enabled;
        }
        return costCategory;

    }

    public void updateUnvalidated(String name, Boolean enabled) {
        if (!StringUtils.isBlank(name)) {
            this.name = name;
        }
        if (enabled != null) {
            this.enabled = enabled;
        }
    }

    public static CostCategory create() {
        return (CostCategory) create(new CostCategory());
    }

    public static CostCategory create(String name) {
        return (CostCategory) create(new CostCategory(name));
    }

    public static void validateHourCostsOverlap(Set<HourCost> hoursCosts)
            throws ValidationException {
        List<HourCost> listHourCosts = new ArrayList<HourCost>(hoursCosts);
        for (int i = 0; i < listHourCosts.size(); i++) {
            LocalDate initDate = listHourCosts.get(i).getInitDate();
            LocalDate endDate = listHourCosts.get(i).getEndDate();
            for (int j = i + 1; j < listHourCosts.size(); j++) {
                HourCost listElement = listHourCosts.get(j);
                if (listElement.getType() == null
                        || listHourCosts.get(i).getType() == null) {
                    // this is not exactly an overlapping but a
                    // problem with missing compulsory fields
                    throw ValidationException.invalidValueException(
                            "Hours cost type cannot be empty",
                            listElement);
                }
                if (listElement.getType().getId()
                        .equals(listHourCosts.get(i).getType().getId())) {
                    if (initDate == null || listElement.getInitDate() == null) {
                        // this is not exactly an overlapping but a
                        // problem with missing compulsory fields
                        throw ValidationException.invalidValueException(
                                "Init date cannot be empty",
                                listElement);
                    }
                    if (endDate == null && listElement.getEndDate() == null) {
                        throw ValidationException.invalidValueException(
                                "End date cannot be empty",
                                listElement);
                    } else if ((endDate == null && listElement.getEndDate()
                            .compareTo(initDate) >= 0)
                            || (listElement.getEndDate() == null && listElement
                                    .getInitDate().compareTo(endDate) <= 0)) {
                        throw ValidationException
                                .invalidValueException(
                                        _("Two Hour Cost of the same type overlap in time"),
                                        listElement);
                    } else if ((endDate != null && listElement.getEndDate() != null)
                            && ((listElement.getEndDate().compareTo(initDate) >= 0 && listElement
                                    .getEndDate().compareTo(endDate) <= 0) || (listElement
                                    .getInitDate().compareTo(initDate) >= 0 && listElement
                                    .getInitDate().compareTo(endDate) <= 0))) {
                        throw ValidationException
                                .invalidValueException(
                                        _("Two Hour Cost of the same type overlap in time"),
                                        listElement);
                    }
                }
            }
        }
    }

    public static void validateCostCategoryOverlapping(
            List<ResourcesCostCategoryAssignment> costCategoryAssignments) {

        for (int i = 0; i < costCategoryAssignments.size(); i++) {
            LocalDate initDate = costCategoryAssignments.get(i).getInitDate();
            LocalDate endDate = costCategoryAssignments.get(i).getEndDate();
            for (int j = i + 1; j < costCategoryAssignments.size(); j++) {
                ResourcesCostCategoryAssignment costCategory = costCategoryAssignments
                        .get(j);
                if (endDate == null && costCategory.getEndDate() == null) {
                    throw ValidationException
                            .invalidValueException(
                                    _("Some cost category assignments overlap in time"),
                                    costCategory);
                } else if ((endDate == null && costCategory.getEndDate()
                        .compareTo(initDate) >= 0)
                        || (costCategory.getEndDate() == null && costCategory
                                .getInitDate().compareTo(endDate) <= 0)) {
                    throw ValidationException
                            .invalidValueException(
                                    _("Some cost category assignments overlap in time"),
                                    costCategory);
                } else if ((endDate != null && costCategory.getEndDate() != null)
                        && ((costCategory.getEndDate().compareTo(initDate) >= 0 && // (1)
                                                                                   // listElement.getEndDate()
                                                                                   // inside
                                                                                   // [initDate,
                                                                                   // endDate]
                        costCategory.getEndDate().compareTo(endDate) <= 0)
                                || (costCategory.getInitDate().compareTo(
                                        initDate) >= 0 && // (2)
                                                          // listElement.getInitDate()
                                                          // inside [initDate,
                                                          // endDate]
                                costCategory.getInitDate().compareTo(endDate) <= 0) || (costCategory
                                .getInitDate().compareTo(initDate) <= 0 && // (3)
                                                                           // [listElement.getInitDate(),
                                                                           // listElement.getEndDate()]
                        costCategory.getEndDate().compareTo(endDate) >= 0))) { // contains
                                                                               // [initDate,
                                                                               // endDate]
                    throw ValidationException
                            .invalidValueException(
                                    _("Some cost category assignments overlap in time"),
                                    costCategory);
                }
            }
        }
    }

    protected CostCategory(String name) {
        this.name = name;
    }

    @NotEmpty(message = "name not specified")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Valid
    public Set<HourCost> getHourCosts() {
        return hourCosts;
    }

    public void addHourCost(HourCost hourCost) {
        hourCosts.add(hourCost);
        if (hourCost.getCategory() != this) {
            hourCost.setCategory(this);
        }
    }

    public void removeHourCost(HourCost hourCost) {
        hourCosts.remove(hourCost);
        if (hourCost.getCategory() == this) {
            hourCost.setCategory(null);
        }
    }

    public boolean canAddHourCost(HourCost hourCost) {
        boolean overlap = false;
        LocalDate initDate = hourCost.getInitDate();
        LocalDate endDate = hourCost.getEndDate();
        for(HourCost listElement:hourCosts) {
            if(listElement.getType().getId().equals(hourCost.getType().getId())) {
                if (endDate == null && listElement.getEndDate() == null) {
                    overlap = true;
                }
                else if((endDate == null && listElement.getEndDate().compareTo(initDate)>=0) ||
                        (listElement.getEndDate() == null && listElement.getInitDate().compareTo(endDate)<=0)) {
                    overlap = true;
                }
                else if((endDate != null && listElement.getEndDate() != null) &&
                        ((listElement.getEndDate().compareTo(initDate)>=0 &&
                        listElement.getEndDate().compareTo(endDate)<=0) ||
                        (listElement.getInitDate().compareTo(initDate)>=0 &&
                                listElement.getInitDate().compareTo(endDate)<=0))) {
                    overlap = true;
                }
            }
        }
        return !overlap;
    }

    public HourCost getHourCostByCode(String code)
            throws InstanceNotFoundException {

        if (StringUtils.isBlank(code)) {
            throw new InstanceNotFoundException(code, HourCost.class.getName());
        }

        for (HourCost c : this.hourCosts) {
            if (c.getCode().equalsIgnoreCase(StringUtils.trim(code))) {
                return c;
            }
        }

        throw new InstanceNotFoundException(code, HourCost.class.getName());
    }

    @AssertFalse(message="Two Hour Cost of the same type overlap in time")
    public boolean isHourCostsOverlapConstraint() {
        try {
            validateHourCostsOverlap(getHourCosts());
            return false;
        } catch (ValidationException e) {
            return true;
        }
    }

    @Override
    protected ICostCategoryDAO getIntegrationEntityDAO() {
        return Registry.getCostCategoryDAO();
    }

    @AssertTrue(message = "The hour cost codes must be unique.")
    public boolean isNonRepeatedHourCostCodesConstraint() {
        return getFirstRepeatedCode(this.hourCosts) == null;
    }

    public void generateHourCostCodes(int numberOfDigits) {
        for (HourCost hourCost : this.hourCosts) {
            if ((hourCost.getCode() == null) || (hourCost.getCode().isEmpty())
                    || (!hourCost.getCode().startsWith(this.getCode()))) {
                this.incrementLastHourCostSequenceCode();
                String hourCostCode = EntitySequence.formatValue(
                        numberOfDigits, this.getLastHourCostSequenceCode());
                hourCost
                        .setCode(this.getCode()
                                + EntitySequence.CODE_SEPARATOR_CHILDREN
                                + hourCostCode);
            }
        }
    }

    public BigDecimal getPriceCostByTypeOfWorkHour(String code)
            throws InstanceNotFoundException {

        if (StringUtils.isBlank(code)) {
            throw new InstanceNotFoundException(code, HourCost.class.getName());
        }

        for (HourCost c : this.hourCosts) {
            if (c.getType().getCode().equalsIgnoreCase(StringUtils.trim(code))) {
                return c.getPriceCost();
            }
        }

        throw new InstanceNotFoundException(code, HourCost.class.getName());
    }

    public void incrementLastHourCostSequenceCode() {
        if (lastHourCostSequenceCode == null) {
            lastHourCostSequenceCode = 0;
        }
        lastHourCostSequenceCode++;
    }

    @NotNull(message = "last hours cost sequence code not specified")
    public Integer getLastHourCostSequenceCode() {
        return lastHourCostSequenceCode;
    }

    @Override
    public String getHumanId() {
        return name;
    }

    @AssertTrue(message = "the cost category name has to be unique and it is already in use")
    public boolean isUniqueNameConstraint() {
        if (StringUtils.isBlank(name)) {
            return true;
        }
        if (isNewObject()) {
            return !costCategoryDAO.existsByNameInAnotherTransaction(name);
        } else {
            return checkNotExistsOrIsTheSame();
        }
    }

    private boolean checkNotExistsOrIsTheSame() {
        try {
            CostCategory costCategory = costCategoryDAO
                    .findUniqueByNameInAnotherTransaction(name);
            return costCategory.getId().equals(getId());
        } catch (InstanceNotFoundException e) {
            return true;
        }
    }
}

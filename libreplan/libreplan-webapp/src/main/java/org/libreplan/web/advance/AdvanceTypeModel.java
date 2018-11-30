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

package org.libreplan.web.advance;

import static org.libreplan.web.I18nHelper._;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.libreplan.business.advance.daos.IAdvanceTypeDAO;
import org.libreplan.business.advance.entities.AdvanceType;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.web.common.concurrentdetection.OnConcurrentModification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Model for UI operations related to {@link AdvanceType}.
 *
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */

@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@OnConcurrentModification(goToPage = "/advance/advanceTypes.zul")
public class AdvanceTypeModel implements IAdvanceTypeModel {

    private AdvanceType advanceType;

    @Autowired
    private IAdvanceTypeDAO advanceTypeDAO;

    @Override
    public AdvanceType getAdvanceType() {
        return this.advanceType;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdvanceType> getAdvanceTypes() {
        return advanceTypeDAO.list(AdvanceType.class);
    }

    @Override
    public void prepareForCreate() {
        this.advanceType = AdvanceType.create();
    }

    private AdvanceType getFromDB(AdvanceType advanceType) {
        try {
            return advanceTypeDAO.find(advanceType.getId());
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void prepareForEdit(AdvanceType advanceType) {
        Validate.notNull(advanceType);
        this.advanceType = getFromDB(advanceType);
    }

    private void checkCanBeModified(AdvanceType advanceType) {
        if (!canBeModified(advanceType)) {
            throw new IllegalArgumentException(_("Progress type cannot be modified"));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void prepareForRemove(AdvanceType advanceType) {
        checkCanBeModified(advanceType);
        this.advanceType = advanceType;
    }

    @Override
    @Transactional
    public void save() {
        advanceTypeDAO.save(advanceType);
        checkCanBeModified(advanceType);
    }

    @Override
    @Transactional
    public void remove(AdvanceType advanceType) {
        try {
            advanceTypeDAO.remove(advanceType.getId());
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canBeModified(AdvanceType advanceType) {
        return advanceType.isUpdatable();
    }

    @Override
    public boolean isPrecisionValid(BigDecimal precision) {
        return this.advanceType.isPrecisionValid(precision);
    }

    @Override
    public boolean isDefaultMaxValueValid(BigDecimal defaultMaxValue) {
        return this.advanceType.isDefaultMaxValueValid(defaultMaxValue);
    }

    @Override
    @Transactional
    public boolean distinctNames(String name) {
        if (name.isEmpty()) {
            return true;
        }

        for (AdvanceType itemAdvanceType : advanceTypeDAO.list(AdvanceType.class)) {
            if (!itemAdvanceType.getId().equals(this.advanceType.getId()) &&
                    itemAdvanceType.getUnitName().equalsIgnoreCase(name)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public BigDecimal getDefaultMaxValue() {
        if (advanceType == null) {
            return null;
        }

        return advanceType.getDefaultMaxValue();
    }

    @Override
    public void setDefaultMaxValue(BigDecimal defaultMaxValue) {
        if (advanceType != null) {
            advanceType.setDefaultMaxValue(defaultMaxValue);
        }
    }

    @Override
    public Boolean getPercentage() {
        return advanceType != null && advanceType.getPercentage();

    }

    @Override
    public void setPercentage(Boolean percentage) {
        if (advanceType != null) {
            advanceType.setPercentage(percentage);
        }
    }

    @Override
    public boolean isImmutable() {
        return advanceType != null && advanceType.isImmutable();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isImmutableOrAlreadyInUse(AdvanceType advanceType) {
        return advanceType != null && (advanceType.isImmutable() || advanceTypeDAO.isAlreadyInUse(advanceType));
    }

}

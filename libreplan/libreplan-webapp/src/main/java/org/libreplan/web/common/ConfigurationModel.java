/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
 * Copyright (C) 2010-2012 Igalia, S.L.
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

package org.libreplan.web.common;

import static org.libreplan.web.I18nHelper._;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.libreplan.business.calendars.daos.IBaseCalendarDAO;
import org.libreplan.business.calendars.entities.BaseCalendar;
import org.libreplan.business.common.daos.IConfigurationDAO;
import org.libreplan.business.common.daos.IConnectorDAO;
import org.libreplan.business.common.daos.IEntitySequenceDAO;
import org.libreplan.business.common.entities.Configuration;
import org.libreplan.business.common.entities.Connector;
import org.libreplan.business.common.entities.EntityNameEnum;
import org.libreplan.business.common.entities.EntitySequence;
import org.libreplan.business.common.entities.LDAPConfiguration;
import org.libreplan.business.common.entities.PersonalTimesheetsPeriodicityEnum;
import org.libreplan.business.common.entities.ProgressType;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.costcategories.entities.TypeOfWorkHours;
import org.libreplan.business.workreports.daos.IWorkReportDAO;
import org.libreplan.web.common.concurrentdetection.OnConcurrentModification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 * @author Cristina Alvarino Perez <cristina.alvarino@comtecsf.es>
 * @author Vova Perebykivskyi <vova@libreplan-enterprise.com>
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@OnConcurrentModification(goToPage = "/common/configuration.zul")
public class ConfigurationModel implements IConfigurationModel {

    /**
     * Conversation state.
     */
    private Configuration configuration;

    private Map<EntityNameEnum, List<EntitySequence>> entitySequences = new HashMap<>();

    private static Map<String, String> currencies = getAllCurrencies();

    private List<Connector> connectors;

    @Autowired
    private IConfigurationDAO configurationDAO;

    @Autowired
    private IBaseCalendarDAO baseCalendarDAO;

    @Autowired
    private IEntitySequenceDAO entitySequenceDAO;

    @Autowired
    private IWorkReportDAO workReportDAO;

    @Autowired
    private IConnectorDAO connectorDAO;

    @Autowired
    private IJobSchedulerModel jobSchedulerModel;

    @Override
    @Transactional(readOnly = true)
    public List<BaseCalendar> getCalendars() {
        return baseCalendarDAO.getBaseCalendars();
    }

    @Override
    public BaseCalendar getDefaultCalendar() {
        return configuration == null ? null : configuration.getDefaultCalendar();
    }

    @Override
    @Transactional(readOnly = true)
    public void init() {
        this.configuration = getCurrentConfiguration();
        initEntitySequences();
        initLdapConfiguration();
        initConnectorConfiguration();
    }

    private void initEntitySequences() {
        this.entitySequences.clear();
        for (EntityNameEnum entityName : EntityNameEnum.values()) {
            entitySequences.put(entityName, new ArrayList<>());
        }
        for (EntitySequence entitySequence : entitySequenceDAO.getAll()) {
            entitySequences.get(entitySequence.getEntityName()).add(entitySequence);
        }
    }

    private void initLdapConfiguration() {
        if (null == configuration.getLdapConfiguration()) {
            configuration.setLdapConfiguration(LDAPConfiguration.create());
        }
    }

    private void initConnectorConfiguration() {
        connectors = connectorDAO.getAll();
        forceLoadConnectors();
    }

    private void forceLoadConnectors() {
        for (Connector connector : connectors) {
            connector.getProperties().size();
        }
    }

    private Configuration getCurrentConfiguration() {
        Configuration configuration = configurationDAO.getConfiguration();
        if (configuration == null) {
            configuration = Configuration.create();
        }
        forceLoad(configuration);
        return configuration;
    }

    private void forceLoad(Configuration configuration) {
        forceLoad(configuration.getDefaultCalendar());
        forceLoad(configuration.getPersonalTimesheetsTypeOfWorkHours());
        forceLoad(configuration.getBudgetDefaultTypeOfWorkHours());
    }

    private void forceLoad(BaseCalendar calendar) {
        if (calendar != null) {
            calendar.getName();
        }
    }

    private void forceLoad(TypeOfWorkHours typeOfWorkHours) {
        if (typeOfWorkHours != null) {
            typeOfWorkHours.getName();
        }
    }

    @Override
    public void setDefaultCalendar(BaseCalendar calendar) {
        if (configuration != null) {
            configuration.setDefaultCalendar(calendar);
        }
    }

    @Override
    @Transactional
    public void confirm() {
        checkEntitySequences();
        configurationDAO.save(configuration);
        saveConnectors();
        try {
            storeAndRemoveEntitySequences();
        } catch (IllegalStateException e) {
            throw new OptimisticLockingFailureException("concurrency problem in entity sequences");
        }
    }

    private void checkEntitySequences() {
        // Check if exist at least one sequence for each entity
        for (EntityNameEnum entityName : EntityNameEnum.values()) {
            String entity = entityName.getDescription();
            List<EntitySequence> sequences = entitySequences.get(entityName);
            if (sequences.isEmpty()) {
                throw new ValidationException(_("At least one {0} sequence is needed", entity));
            }

            if (!isAnyActive(sequences)) {
                throw new ValidationException(_("At least one {0} sequence must be active", entity));
            }

            if (!checkConstraintPrefixNotRepeated(sequences)) {
                throw new ValidationException(_(
                        "The {0} sequence prefixes cannot be repeated", entityName.getDescription()));
            }
        }
    }

    private boolean checkConstraintPrefixNotRepeated(List<EntitySequence> sequences) {
        Set<String> prefixes = new HashSet<>();
        for (EntitySequence sequence : sequences) {
            String prefix = sequence.getPrefix();
            if (prefixes.contains(prefix)) {
                return false;
            }
            prefixes.add(prefix);
        }
        return true;
    }

    private boolean isAnyActive(List<EntitySequence> sequences) {
        for (EntitySequence entitySequence : sequences) {
            if (entitySequence.isActive()) {
                return true;
            }
        }
        return false;
    }

    private void storeAndRemoveEntitySequences() {
        Collection<List<EntitySequence>> sequencesCollection = entitySequences.values();
        List<EntitySequence> sequences = new ArrayList<>();
        for (List<EntitySequence> list : sequencesCollection) {
            sequences.addAll(list);
        }
        removeEntitySequences(sequences);
        storeEntitySequences(sequences);
    }

    public void removeEntitySequences(final List<EntitySequence> sequences) {
        // First one is necessary to remove the deleted sequences
        List<EntitySequence> toRemove = entitySequenceDAO.findEntitySequencesNotIn(sequences);
        for (final EntitySequence entitySequence : toRemove) {
            try {
                entitySequenceDAO.remove(entitySequence);
            } catch (InstanceNotFoundException e) {
                throw new ValidationException(_("Some sequences to be removed do not exist"));
            } catch (IllegalArgumentException e) {
                throw new ValidationException(e.getMessage());
            }
        }
    }

    public void storeEntitySequences(List<EntitySequence> sequences) {
        // It updates the sequences that are not active first
        List<EntitySequence> toSaveAfter = new ArrayList<>();
        for (EntitySequence entitySequence : sequences) {
            if ( entitySequence.isActive() ) {
                toSaveAfter.add(entitySequence);
            } else {
                entitySequenceDAO.save(entitySequence);
            }
        }
        for (EntitySequence entitySequence : toSaveAfter) {
            entitySequenceDAO.save(entitySequence);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void cancel() {
        init();
    }

    @Override
    public String getCompanyCode() {
        if (configuration == null) {
            return null;
        }
        return configuration.getCompanyCode();
    }

    @Override
    public void setCompanyCode(String companyCode) {
        if (configuration != null) {
            configuration.setCompanyCode(companyCode);
        }
    }

    @Override
    public Boolean getGenerateCodeForCriterion() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForCriterion();
    }

    @Override
    public void setGenerateCodeForCriterion(Boolean generateCodeForCriterion) {
        if (configuration != null) {
            configuration.setGenerateCodeForCriterion(generateCodeForCriterion);
        }
    }

    @Override
    public Boolean isAutocompleteLogin() {
        if (configuration == null) {
            return null;
        }
        return (configuration.isAutocompleteLogin() && (!configuration.getChangedDefaultAdminPassword()));
    }

    @Override
    public void setAutocompleteLogin(Boolean autocompleteLogin) {
        if (configuration != null) {
            configuration.setAutocompleteLogin(autocompleteLogin);
        }
    }

    @Override
    public Boolean isChangedDefaultPasswdAdmin() {
        return configuration != null ? configuration.getChangedDefaultAdminPassword() : false;
    }

    @Override
    public Boolean getGenerateCodeForWorkReportType() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForWorkReportType();
    }

    @Override
    public void setGenerateCodeForWorkReportType(Boolean generateCodeForWorkReportType) {
        if (configuration != null) {
            configuration.setGenerateCodeForWorkReportType(generateCodeForWorkReportType);
        }
    }

    @Override
    public Boolean getGenerateCodeForCalendarExceptionType() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForCalendarExceptionType();
    }

    @Override
    public void setGenerateCodeForCalendarExceptionType(Boolean generateCodeForCalendarExceptionType) {
        if (configuration != null) {
            configuration.setGenerateCodeForCalendarExceptionType(generateCodeForCalendarExceptionType);
        }
    }

    @Override
    public void setGenerateCodeForCostCategory(Boolean generateCodeForCostCategory) {
        if (configuration != null) {
            configuration.setGenerateCodeForCostCategory(generateCodeForCostCategory);
        }
    }

    @Override
    public Boolean getGenerateCodeForCostCategory() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForCostCategory();
    }

    @Override
    public Boolean getGenerateCodeForLabel() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForLabel();
    }

    @Override
    public void setGenerateCodeForLabel(Boolean generateCodeForLabel) {
        if (configuration != null) {
            configuration.setGenerateCodeForLabel(generateCodeForLabel);
        }
    }

    @Override
    public Boolean getGenerateCodeForWorkReport() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForWorkReport();
    }

    @Override
    public void setGenerateCodeForWorkReport(Boolean generateCodeForWorkReport) {
        if (configuration != null) {
            configuration.setGenerateCodeForWorkReport(generateCodeForWorkReport);
        }
    }

    @Override
    public Boolean getGenerateCodeForResources() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForResources();
    }

    @Override
    public void setGenerateCodeForResources(Boolean generateCodeForResources) {
        if (configuration != null) {
            configuration.setGenerateCodeForResources(generateCodeForResources);
        }
    }

    @Override
    public Boolean getGenerateCodeForTypesOfWorkHours() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForTypesOfWorkHours();
    }

    @Override
    public void setGenerateCodeForTypesOfWorkHours(Boolean generateCodeForTypesOfWorkHours) {
        if (configuration != null) {
            configuration.setGenerateCodeForTypesOfWorkHours(generateCodeForTypesOfWorkHours);
        }
    }

    @Override
    public Boolean getGenerateCodeForMaterialCategories() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForMaterialCategories();
    }

    @Override
    public void setGenerateCodeForMaterialCategories(Boolean generateCodeForMaterialCategories) {
        if (configuration != null) {
            configuration.setGenerateCodeForMaterialCategories(generateCodeForMaterialCategories);
        }
    }

    @Override
    public Boolean getGenerateCodeForUnitTypes() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForUnitTypes();
    }

    @Override
    public void setGenerateCodeForBaseCalendars(Boolean generateCodeForBaseCalendars) {
        if (configuration != null) {
            configuration.setGenerateCodeForBaseCalendars(generateCodeForBaseCalendars);
        }
    }

    @Override
    public Boolean getGenerateCodeForBaseCalendars() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForBaseCalendars();
    }

    @Override
    public void setGenerateCodeForUnitTypes(Boolean generateCodeForUnitTypes) {
        if (configuration != null) {
            configuration.setGenerateCodeForUnitTypes(generateCodeForUnitTypes);
        }
    }

    @Override
    public Boolean getGenerateCodeForExpenseSheets() {
        if (configuration == null) {
            return null;
        }
        return configuration.getGenerateCodeForExpenseSheets();
    }

    @Override
    public void setGenerateCodeForExpenseSheets(Boolean generateCodeForExpenseSheets) {
        if (configuration != null) {
            configuration.setGenerateCodeForExpenseSheets(generateCodeForExpenseSheets);
        }
    }

    @Override
    public Boolean isMonteCarloMethodTabVisible() {
        if (configuration == null) {
            return null;
        }
        return configuration.isMonteCarloMethodTabVisible();
    }

    @Override
    public void setMonteCarloMethodTabVisible(Boolean visible) {
        if (configuration != null) {
            configuration.setMonteCarloMethodTabVisible(visible);
        }
    }

    public List<EntitySequence> getEntitySequences(EntityNameEnum entityName) {
        return entitySequences.get(entityName);
    }

    public void addEntitySequence(EntityNameEnum entityName, String prefix, Integer digits) {
        List<EntitySequence> sequences = entitySequences.get(entityName);
        EntitySequence entitySequence = EntitySequence.create(prefix, entityName, digits);
        if (sequences.isEmpty()) {
            entitySequence.setActive(true);
        }
        sequences.add(entitySequence);
    }

    public void removeEntitySequence(EntitySequence entitySequence) throws IllegalArgumentException {
        entitySequences.get(entitySequence.getEntityName()).remove(entitySequence);
    }

    public boolean checkPrefixFormat(EntitySequence sequence) {
        return (sequence.isWithoutLowBarConstraint() && sequence.isPrefixWithoutWhiteSpacesConstraint());
    }

    @Override
    public List<ProgressType> getProgressTypes() {
        return ProgressType.getAll();
    }

    @Override
    public void setProgressType(ProgressType progressType) {
        configuration.setProgressType(progressType);
    }

    @Override
    public ProgressType getProgressType() {
        return configuration.getProgressType();
    }

    @Override
    public String getCompanyLogoURL() {
        return configuration.getCompanyLogoURL();
    }

    @Override
    public void setCompanyLogoURL(String companyLogoURL) {
        configuration.setCompanyLogoURL(companyLogoURL);
    }

    @Override
    public void setLdapConfiguration(LDAPConfiguration ldapConfiguration) {
        configuration.setLdapConfiguration(ldapConfiguration);
    }
    @Override
    public LDAPConfiguration getLdapConfiguration() {
        return configuration.getLdapConfiguration();
    }

    @Override
    public boolean isCheckNewVersionEnabled() {
        return configuration.isCheckNewVersionEnabled();
    }

    @Override
    public void setCheckNewVersionEnabled(boolean checkNewVersionEnabled) {
        configuration.setCheckNewVersionEnabled(checkNewVersionEnabled);
    }

    private static Map<String, String> getAllCurrencies() {
        Map<String, String> currencies = new TreeMap<>();
        for (Locale locale : Locale.getAvailableLocales()) {
            if (StringUtils.isNotBlank(locale.getCountry())) {
                Currency currency = Currency.getInstance(locale);
                currencies.put(currency.getCurrencyCode(), currency.getSymbol(locale));
            }
        }
        return currencies;
    }

    @Override
    public Set<String> getCurrencies() {
        return currencies.keySet();
    }

    @Override
    public String getCurrencySymbol(String currencyCode) {
        return currencies.get(currencyCode);
    }

    @Override
    public String getCurrencyCode() {
        return configuration.getCurrencyCode();
    }

    @Override
    public void setCurrency(String currencyCode) {
        if (configuration != null) {
            configuration.setCurrencyCode(currencyCode);
            configuration.setCurrencySymbol(currencies.get(currencyCode));
        }
    }

    @Override
    public TypeOfWorkHours getPersonalTimesheetsTypeOfWorkHours() {
        return configuration.getPersonalTimesheetsTypeOfWorkHours();
    }

    @Override
    public void setPersonalTimesheetsTypeOfWorkHours(TypeOfWorkHours typeOfWorkHours) {
        if (configuration != null) {
            configuration.setPersonalTimesheetsTypeOfWorkHours(typeOfWorkHours);
        }
    }

    @Override
    public PersonalTimesheetsPeriodicityEnum getPersonalTimesheetsPeriodicity() {
        return configuration.getPersonalTimesheetsPeriodicity();
    }

    @Override
    public void setPersonalTimesheetsPeriodicity(PersonalTimesheetsPeriodicityEnum personalTimesheetsPeriodicity) {
        configuration.setPersonalTimesheetsPeriodicity(personalTimesheetsPeriodicity);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAnyPersonalTimesheetAlreadySaved() {
        return !workReportDAO.isAnyPersonalTimesheetAlreadySaved();
    }

    @Override
    public Integer getSecondsPlanningWarning() {
        return configuration.getSecondsPlanningWarning();
    }

    @Override
    public void setSecondsPlanningWarning(Integer secondsPlanningWarning) {
        configuration.setSecondsPlanningWarning(secondsPlanningWarning);
    }

    @Override
    public String getRepositoryLocation() {
        return configuration.getRepositoryLocation();
    }

    @Override
    public void setRepositoryLocation(String location) {
        configuration.setRepositoryLocation(location);
    }

    private void saveConnectors() {
        for (Connector connector : connectors) {
            connectorDAO.save(connector);
        }
    }

    @Override
    public List<Connector> getConnectors() {
        return Collections.unmodifiableList(connectors);
    }

    @Override
    public Connector getConnectorByName(String name) {
        if (name == null || connectors == null) {
            return null;
        }

        for (Connector connector : connectors) {
            if (connector.getName().equals(name)) {
                return connector;
            }
        }
        return null;
    }

    @Override
    public boolean scheduleOrUnscheduleJobs(Connector connector) {
        return jobSchedulerModel.scheduleOrUnscheduleJobs(connector);
    }

    @Override
    public TypeOfWorkHours getBudgetDefaultTypeOfWorkHours() {
        return configuration.getBudgetDefaultTypeOfWorkHours();
    }

    @Override
    public void setBudgetDefaultTypeOfWorkHours(TypeOfWorkHours typeOfWorkHours) {
        if (configuration != null) {
            configuration.setBudgetDefaultTypeOfWorkHours(typeOfWorkHours);
        }
    }

    @Override
    public Boolean getEnabledAutomaticBudget() {
        if (configuration == null) {
            return null;
        }
        return (configuration.isEnabledAutomaticBudget());
    }

    @Override
    public void setEnabledAutomaticBudget(Boolean enabledAutomaticBudget) {
        if (configuration != null) {
            configuration.setEnabledAutomaticBudget(enabledAutomaticBudget);
        }
    }

}

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

import java.util.List;
import java.util.Set;

import org.libreplan.business.calendars.entities.BaseCalendar;
import org.libreplan.business.common.entities.Connector;
import org.libreplan.business.common.entities.EntityNameEnum;
import org.libreplan.business.common.entities.EntitySequence;
import org.libreplan.business.common.entities.LDAPConfiguration;
import org.libreplan.business.common.entities.PersonalTimesheetsPeriodicityEnum;
import org.libreplan.business.common.entities.ProgressType;
import org.libreplan.business.costcategories.entities.TypeOfWorkHours;

/**
 * Contract for {@link ConfigurationModel}.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 * @author Vova Perebykivskyi <vova@libreplan-enterprise.com>
 */
public interface IConfigurationModel {

    /**
     * Non conversational steps.
     */
    List<BaseCalendar> getCalendars();

    /**
     * Initial conversation steps.
     */
    void init();

    /**
     * Intermediate conversation steps.
     */
    BaseCalendar getDefaultCalendar();

    void setDefaultCalendar(BaseCalendar calendar);

    String getCompanyCode();

    void setCompanyCode(String companyCode);

    Boolean getGenerateCodeForWorkReportType();

    void setGenerateCodeForWorkReportType(Boolean generateCodeForWorkReportType);

    Boolean getGenerateCodeForCalendarExceptionType();

    void setGenerateCodeForCostCategory(Boolean generateCodeForCostCategory);

    Boolean getGenerateCodeForCostCategory();

    void setGenerateCodeForCalendarExceptionType(Boolean generateCodeForCalendarExceptionType);

    Boolean getGenerateCodeForCriterion();

    void setGenerateCodeForCriterion(Boolean generateCodeForCriterion);

    Boolean getGenerateCodeForLabel();

    void setGenerateCodeForLabel(Boolean generateCodeForLabel);

    Boolean getGenerateCodeForWorkReport();

    void setGenerateCodeForWorkReport(Boolean generateCodeForWorkReport);

    Boolean getGenerateCodeForResources();

    void setGenerateCodeForResources(Boolean generateCodeForResources);

    Boolean getGenerateCodeForTypesOfWorkHours();

    void setGenerateCodeForTypesOfWorkHours(Boolean generateCodeForTypesOfWorkHours);

    Boolean getGenerateCodeForMaterialCategories();

    void setGenerateCodeForMaterialCategories(Boolean generateCodeForMaterialCategories);

    List<EntitySequence> getEntitySequences(EntityNameEnum entityName);

    void addEntitySequence(EntityNameEnum entityName, String prefix, Integer digits);

    void removeEntitySequence(EntitySequence entitySequence) throws IllegalArgumentException;

    Boolean isMonteCarloMethodTabVisible();

    void setMonteCarloMethodTabVisible(Boolean visible);

    /**
     * Final conversation steps.
     */
    void confirm();

    void cancel();

    Boolean getGenerateCodeForUnitTypes();

    void setGenerateCodeForUnitTypes(Boolean generateCodeForUnitTypes);

    void setGenerateCodeForBaseCalendars(Boolean generateCodeForBaseCalendars);

    Boolean getGenerateCodeForBaseCalendars();

    boolean checkPrefixFormat(EntitySequence sequence);

    List<ProgressType> getProgressTypes();

    void setProgressType(ProgressType progressType);

    ProgressType getProgressType();

    String getCompanyLogoURL();

    void setCompanyLogoURL(String companyLogoURL);

    void setLdapConfiguration(LDAPConfiguration ldapConfiguration);

    LDAPConfiguration getLdapConfiguration();

    Boolean isAutocompleteLogin();

    Boolean isChangedDefaultPasswdAdmin();

    void setAutocompleteLogin(Boolean autocompleteLogin);

    boolean isCheckNewVersionEnabled();

    void setCheckNewVersionEnabled(boolean checkNewVersionEnabled);

    Boolean getGenerateCodeForExpenseSheets();

    void setGenerateCodeForExpenseSheets(Boolean generateCodeForExpenseSheets);

    Set<String> getCurrencies();

    String getCurrencySymbol(String currencyCode);

    String getCurrencyCode();

    void setCurrency(String currencyCode);

    TypeOfWorkHours getPersonalTimesheetsTypeOfWorkHours();

    void setPersonalTimesheetsTypeOfWorkHours(TypeOfWorkHours typeOfWorkHours);

    PersonalTimesheetsPeriodicityEnum getPersonalTimesheetsPeriodicity();

    void setPersonalTimesheetsPeriodicity(PersonalTimesheetsPeriodicityEnum personalTimesheetsPeriodicity);

    boolean isAnyPersonalTimesheetAlreadySaved();

    Integer getSecondsPlanningWarning();

    void setSecondsPlanningWarning(Integer planningWarningExitWithoutSavingSeconds);

    String getRepositoryLocation();

    void setRepositoryLocation(String location);

    List<Connector> getConnectors();

    Connector getConnectorByName(String name);

    boolean scheduleOrUnscheduleJobs(Connector connector);

    TypeOfWorkHours getBudgetDefaultTypeOfWorkHours();

    void setBudgetDefaultTypeOfWorkHours(TypeOfWorkHours typeOfWorkHours);

    Boolean getEnabledAutomaticBudget();

    void setEnabledAutomaticBudget(Boolean enabledAutomaticBudget);

}

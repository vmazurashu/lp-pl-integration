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

package org.libreplan.business.common.entities;

import org.apache.commons.lang3.StringUtils;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import org.hibernate.validator.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.libreplan.business.calendars.entities.BaseCalendar;
import org.libreplan.business.common.BaseEntity;
import org.libreplan.business.costcategories.entities.TypeOfWorkHours;

/**
 * Application configuration variables.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 * @author Cristina Alvarino Perez <cristina.alvarino@comtecsf.es>
 * @author Ignacio Diaz Teijido <ignacio.diaz@comtecsf.es>
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 * @author Vova Perebykivskyi <vova@libreplan-enterprise.com>
 */
public class Configuration extends BaseEntity {

    private BaseCalendar defaultCalendar;

    private String companyCode;

    private Boolean generateCodeForProjectLog = true;

    private Boolean generateCodeForCriterion = true;

    private Boolean generateCodeForLabel = true;

    private Boolean generateCodeForWorkReport = true;

    private Boolean generateCodeForResources = true;

    private Boolean generateCodeForTypesOfWorkHours = true;

    private Boolean generateCodeForMaterialCategories = true;

    private Boolean generateCodeForUnitTypes = true;

    private Boolean generateCodeForBaseCalendars = true;

    private Boolean monteCarloMethodTabVisible = false;

    private Boolean generateCodeForWorkReportType = true;

    private Boolean generateCodeForCalendarExceptionType = true;

    private Boolean generateCodeForCostCategory = true;

    private Boolean changedDefaultAdminPassword = false;

    private Boolean changedDefaultWsreaderPassword = false;

    private Boolean changedDefaultWswriterPassword = false;

    private Boolean changedDefaultWssubcontractingPassword = false;

    private Boolean changedDefaultManagerPassword = false;

    private Boolean changedDefaultHresourcesPassword = false;

    private Boolean changedDefaultOutsourcingPassword = false;

    private Boolean changedDefaultReportsPassword = false;

    private Boolean autocompleteLogin = true;

    private ProgressType progressType = ProgressType.SPREAD_PROGRESS;

    private String companyLogoURL = "";

    private Boolean scenariosVisible = false;

    private LDAPConfiguration ldapConfiguration;

    private Boolean checkNewVersionEnabled = true;

    private Boolean allowToGatherUsageStatsEnabled = true;

    private Boolean generateCodeForExpenseSheets = true;

    /**
     * Currency code according to ISO-4217 (3 letters)
     */
    private String currencyCode = "EUR";

    private String currencySymbol = "€";

    private TypeOfWorkHours personalTimesheetsTypeOfWorkHours;

    private PersonalTimesheetsPeriodicityEnum personalTimesheetsPeriodicity = PersonalTimesheetsPeriodicityEnum.MONTHLY;

    private Integer secondsPlanningWarning = 30;

    private TypeOfWorkHours budgetDefaultTypeOfWorkHours;

    private Boolean enabledAutomaticBudget = false;

    /**
     * Maximum users configurable directly in database for SaaS products. If
     * zero it means that there isn't any limitation.
     */
    private Integer maxUsers = 0;

    /**
     * Maximum resources configurable directly in database for SaaS products. If
     * zero it means that there isn't any limitation.
     */
    private Integer maxResources = 0;

    private String repositoryLocation;

    public static Configuration create() {
        return create(new Configuration());
    }


    public void setDefaultCalendar(BaseCalendar defaultCalendar) {
        this.defaultCalendar = defaultCalendar;
    }

    @NotNull(message = "default calendar not specified")
    public BaseCalendar getDefaultCalendar() {
        return defaultCalendar;
    }

    public void setCompanyCode(String companyCode) {
        if (companyCode != null) {
            companyCode = companyCode.trim();
        }

        this.companyCode = companyCode;
    }

    @NotEmpty(message = "company code not specified")
    public String getCompanyCode() {
        return companyCode;
    }

    @AssertTrue(message = "company code cannot contain whitespaces")
    public boolean isCompanyCodeWithoutWhiteSpacesConstraint() {
        return !((companyCode == null) || (companyCode.isEmpty())) && !companyCode.contains(" ");
    }

    @AssertTrue(message = "host not specified")
    public boolean isLdapHostWithoutWhiteSpacesConstraint() {
        return !getLdapConfiguration().getLdapAuthEnabled() || !StringUtils.isBlank(getLdapConfiguration().getLdapHost());

    }

    @AssertTrue(message = "port not specified")
    public boolean isLdapPortWithoutWhiteSpacesConstraint() {
        return !getLdapConfiguration().getLdapAuthEnabled() || !StringUtils.isBlank(getLdapConfiguration().getLdapPort());
    }

    @AssertTrue(message = "base not specified")
    public boolean isLdapBaseWithoutWhiteSpacesConstraint() {
        return !getLdapConfiguration().getLdapAuthEnabled() || !StringUtils.isBlank(getLdapConfiguration().getLdapBase());
    }

    @AssertTrue(message = "userId not specified")
    public boolean isLdapUserIdWithoutWhiteSpacesConstraint() {
        return !getLdapConfiguration().getLdapAuthEnabled() || !StringUtils.isBlank(getLdapConfiguration().getLdapUserId());
    }

    public void setGeneratedCodeForProjectLog(Boolean generateCodeForProjectLog) {
        this.generateCodeForProjectLog = generateCodeForProjectLog;
    }
    public Boolean getGenerateCodeForProjectLog(){ return generateCodeForProjectLog;}

    public void setGenerateCodeForCriterion(Boolean generateCodeForCriterion) {
        this.generateCodeForCriterion = generateCodeForCriterion;
    }

    public Boolean getGenerateCodeForCriterion() {
        return generateCodeForCriterion;
    }

    public void setGenerateCodeForLabel(Boolean generateCodeForLabel) {
        this.generateCodeForLabel = generateCodeForLabel;
    }

    public Boolean getGenerateCodeForLabel() {
        return generateCodeForLabel;
    }

    public void setGenerateCodeForWorkReport(Boolean generateCodeForWorkReport) {
        this.generateCodeForWorkReport = generateCodeForWorkReport;
    }

    public Boolean getGenerateCodeForWorkReport() {
        return generateCodeForWorkReport;
    }

    public void setGenerateCodeForResources(Boolean generateCodeForResources) {
        this.generateCodeForResources = generateCodeForResources;
    }

    public Boolean getGenerateCodeForResources() {
        return generateCodeForResources;
    }

    public void setGenerateCodeForTypesOfWorkHours(Boolean generateCodeForTypesOfWorkHours) {
        this.generateCodeForTypesOfWorkHours = generateCodeForTypesOfWorkHours;
    }

    public Boolean getGenerateCodeForTypesOfWorkHours() {
        return generateCodeForTypesOfWorkHours;
    }

    public void setGenerateCodeForMaterialCategories(Boolean generateCodeForMaterialCategories) {
        this.generateCodeForMaterialCategories = generateCodeForMaterialCategories;
    }

    public Boolean getGenerateCodeForMaterialCategories() {
        return generateCodeForMaterialCategories;
    }

    public void setGenerateCodeForUnitTypes(Boolean generateCodeForUnitTypes) {
        this.generateCodeForUnitTypes = generateCodeForUnitTypes;
    }

    public Boolean getGenerateCodeForUnitTypes() {
        return generateCodeForUnitTypes;
    }

    public Boolean isMonteCarloMethodTabVisible() {
        return monteCarloMethodTabVisible;
    }

    public void setMonteCarloMethodTabVisible(Boolean monteCarloMethodTabVisible) {
        this.monteCarloMethodTabVisible = monteCarloMethodTabVisible;
    }

    public Boolean isScenariosVisible() {
        return scenariosVisible;
    }

    public void setScenariosVisible(Boolean scenariosVisible) {
        this.scenariosVisible = scenariosVisible;
    }

    public void setGenerateCodeForBaseCalendars(Boolean generateCodeForBaseCalendars) {
        this.generateCodeForBaseCalendars = generateCodeForBaseCalendars;
    }

    public Boolean getGenerateCodeForBaseCalendars() {
        return generateCodeForBaseCalendars;
    }

    public void setGenerateCodeForWorkReportType(Boolean generateCodeForWorkReportType) {
        this.generateCodeForWorkReportType = generateCodeForWorkReportType;
    }

    public Boolean getGenerateCodeForWorkReportType() {
        return generateCodeForWorkReportType;
    }

    public void setGenerateCodeForCalendarExceptionType(Boolean generateCodeForCalendarExceptionType) {
        this.generateCodeForCalendarExceptionType = generateCodeForCalendarExceptionType;
    }

    public Boolean getGenerateCodeForCalendarExceptionType() {
        return this.generateCodeForCalendarExceptionType;
    }

    public void setGenerateCodeForCostCategory(Boolean generateCodeForCostCategory) {
        this.generateCodeForCostCategory = generateCodeForCostCategory;
    }

    public Boolean getGenerateCodeForCostCategory() {
        return generateCodeForCostCategory;
    }

    public Boolean getGenerateCodeForExpenseSheets() {
        return this.generateCodeForExpenseSheets;
    }

    public void setGenerateCodeForExpenseSheets(Boolean generateCodeForExpenseSheets) {
        this.generateCodeForExpenseSheets = generateCodeForExpenseSheets;
    }

    public void setProgressType(ProgressType progressType) {
        this.progressType = progressType;
    }

    public ProgressType getProgressType() {
        return (progressType == null) ? ProgressType.SPREAD_PROGRESS : progressType;
    }

    public void setCompanyLogoURL(String companyLogoURL) {
        if (companyLogoURL != null) {
            companyLogoURL = companyLogoURL.trim();
        }

        this.companyLogoURL = companyLogoURL;
    }

    public String getCompanyLogoURL() {
        return companyLogoURL;
    }

    public void setChangedDefaultAdminPassword(Boolean changedDefaultAdminPassword) {
        this.changedDefaultAdminPassword = changedDefaultAdminPassword;
    }

    public Boolean getChangedDefaultAdminPassword() {
        return changedDefaultAdminPassword == null ? false : changedDefaultAdminPassword;
    }

    public void setChangedDefaultWsreaderPassword(Boolean changedDefaultWsreaderPassword) {
        this.changedDefaultWsreaderPassword = changedDefaultWsreaderPassword;
    }

    public Boolean getChangedDefaultWsreaderPassword() {
        return changedDefaultWsreaderPassword != null ? changedDefaultWsreaderPassword : false;
    }

    public void setChangedDefaultWswriterPassword(Boolean changedDefaultWswriterPassword) {
        this.changedDefaultWswriterPassword = changedDefaultWswriterPassword;
    }

    public Boolean getChangedDefaultWswriterPassword() {
        return changedDefaultWswriterPassword != null ? changedDefaultWswriterPassword : false;
    }

    public void setChangedDefaultWssubcontractingPassword(Boolean changedDefaultWssubcontractingPassword) {
        this.changedDefaultWssubcontractingPassword = changedDefaultWssubcontractingPassword;
    }

    public Boolean getChangedDefaultWssubcontractingPassword() {
        return changedDefaultWssubcontractingPassword != null ? changedDefaultWssubcontractingPassword : false;
    }

    public void setChangedDefaultManagerPassword(Boolean changedDefaultManagerPassword) {
        this.changedDefaultManagerPassword = changedDefaultManagerPassword;
    }

    public Boolean getChangedDefaultManagerPassword() {
        return changedDefaultManagerPassword != null ? changedDefaultManagerPassword : false;
    }

    public void setChangedDefaultHresourcesPassword(Boolean changedDefaultHresourcesPassword) {
        this.changedDefaultHresourcesPassword = changedDefaultHresourcesPassword;
    }

    public Boolean getChangedDefaultHresourcesPassword() {
        return changedDefaultHresourcesPassword != null ? changedDefaultHresourcesPassword : false;
    }

    public void setChangedDefaultOutsourcingPassword(Boolean changedDefaultOutsourcingPassword) {
        this.changedDefaultOutsourcingPassword = changedDefaultOutsourcingPassword;
    }

    public Boolean getChangedDefaultOutsourcingPassword() {
        return changedDefaultOutsourcingPassword != null ? changedDefaultOutsourcingPassword : false;
    }

    public void setChangedDefaultReportsPassword(Boolean changedDefaultReportsPassword) {
        this.changedDefaultReportsPassword = changedDefaultReportsPassword;
    }

    public Boolean getChangedDefaultReportsPassword() {
        return changedDefaultReportsPassword != null ? changedDefaultReportsPassword : false;
    }

    public LDAPConfiguration getLdapConfiguration() {
        return ldapConfiguration;
    }

    public void setLdapConfiguration(LDAPConfiguration ldapConfiguration) {
        this.ldapConfiguration = ldapConfiguration;
    }

    public Boolean isAutocompleteLogin() {
        return this.autocompleteLogin != null ? this.autocompleteLogin : true;
    }

    public void setAutocompleteLogin(Boolean autocompleteLogin) {
        this.autocompleteLogin = autocompleteLogin;
    }

    public boolean isCheckNewVersionEnabled() {
        return checkNewVersionEnabled != null ? checkNewVersionEnabled : true;
    }

    public void setCheckNewVersionEnabled(boolean checkNewVersionEnabled) {
        this.checkNewVersionEnabled = checkNewVersionEnabled;
    }

    public boolean isAllowedToGatherUsageStatsEnabled() {
        return allowToGatherUsageStatsEnabled != null ? allowToGatherUsageStatsEnabled : false;
    }

    public void setAllowToGatherUsageStatsEnabled(boolean allowToGatherUsageStatsEnabled) {
        this.allowToGatherUsageStatsEnabled = allowToGatherUsageStatsEnabled;
    }

    @NotNull(message = "currency code not specified")
    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    @NotNull(message = "currency symbol not specified")
    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    public TypeOfWorkHours getPersonalTimesheetsTypeOfWorkHours() {
        return personalTimesheetsTypeOfWorkHours;
    }

    public void setPersonalTimesheetsTypeOfWorkHours(TypeOfWorkHours typeOfWorkHours) {
        personalTimesheetsTypeOfWorkHours = typeOfWorkHours;
    }

    public TypeOfWorkHours getBudgetDefaultTypeOfWorkHours() {
        return budgetDefaultTypeOfWorkHours;
    }

    public void setBudgetDefaultTypeOfWorkHours(TypeOfWorkHours typeOfWorkHours) {
        budgetDefaultTypeOfWorkHours = typeOfWorkHours;
    }

    public PersonalTimesheetsPeriodicityEnum getPersonalTimesheetsPeriodicity() {
        return personalTimesheetsPeriodicity;
    }

    public void setPersonalTimesheetsPeriodicity(PersonalTimesheetsPeriodicityEnum personalTimesheetsPeriodicity) {
        this.personalTimesheetsPeriodicity = personalTimesheetsPeriodicity;
    }

    public Integer getMaxUsers() {
        return maxUsers;
    }

    public Integer getMaxResources() {
        return maxResources;
    }

    @Min(value = 0, message = "seconds planning warning cannot be negative")
    @NotNull(message = "seconds planning warning not specified")
    public Integer getSecondsPlanningWarning() {
        return secondsPlanningWarning;
    }

    public void setSecondsPlanningWarning(Integer secondsPlanningWarning) {
        this.secondsPlanningWarning = secondsPlanningWarning;
    }

    public Boolean isEnabledAutomaticBudget() {
        return enabledAutomaticBudget;
    }

    public void setEnabledAutomaticBudget(Boolean enabledAutomaticBudget) {
        this.enabledAutomaticBudget = enabledAutomaticBudget;
    }

    public String getRepositoryLocation() {
        return repositoryLocation;
    }

    public void setRepositoryLocation(String repositoryLocation) {
        this.repositoryLocation = repositoryLocation;
    }

}

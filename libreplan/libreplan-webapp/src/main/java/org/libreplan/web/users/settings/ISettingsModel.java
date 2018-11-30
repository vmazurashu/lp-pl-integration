/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2011 ComtecSF, S.L.
 * Copyright (C) 2013 Igalia, S.L.
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
package org.libreplan.web.users.settings;

import java.util.List;

import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.labels.entities.Label;
import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.settings.entities.Language;


/**
 * Model for UI operations related to user settings
 *
 * @author Cristina Alvarino Perez <cristina.alvarino@comtecsf.es>
 * @author Ignacio Diaz Teijido <ignacio.diaz@comtecsf.es>
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 */
public interface ISettingsModel {

    void setApplicationLanguage(Language applicationLanguage);

    Language getApplicationLanguage();

    void initEditLoggedUser();

    void confirmSave() throws ValidationException;

    void setExpandCompanyPlanningViewCharts(
            boolean expandCompanyPlanningViewCharts);

    boolean isExpandResourceLoadViewCharts();

    void setExpandResourceLoadViewCharts(boolean expandResourceLoadViewCharts);

    boolean isExpandOrderPlanningViewCharts();

    void setExpandOrderPlanningViewCharts(boolean expandOrderPlanningViewCharts);

    boolean isExpandCompanyPlanningViewCharts();

    void setLastName(String lastName);

    String getLastName();

    void setFirstName(String firstName);

    String getFirstName();

    String getEmail();

    void setEmail(String email);

    String getLoginName();

    boolean isBound();

    Integer getProjectsFilterPeriodSince();

    void setProjectsFilterPeriodSince(Integer period);

    Integer getProjectsFilterPeriodTo();

    void setProjectsFilterPeriodTo(Integer period);

    Integer getResourcesLoadFilterPeriodSince();

    void setResourcesLoadFilterPeriodSince(Integer period);

    Integer getResourcesLoadFilterPeriodTo();

    void setResourcesLoadFilterPeriodTo(Integer period);

    Label getProjectsFilterLabel();

    List<Label> getAllLabels();

    void setProjectsFilterLabel(Label label);

    List<Criterion> getAllCriteria();

    Criterion getResourcesLoadFilterCriterion();

    void setResourcesLoadFilterCriterion(Criterion criterion);

    boolean isShowResourcesOn();

    void setShowResourcesOn(boolean showResourcesOn);

    boolean isShowAdvancesOn();

    void setShowAdvancesOn(boolean showAdvancesOn);

    boolean isShowReportedHoursOn();

    void setShowReportedHoursOn(boolean showReportedHoursOn);

    boolean isShowLabelsOn();

    void setShowLabelsOn(boolean showLabelsOn);

    boolean isShowMoneyCostBarOn();

    void setShowMoneyCostBarOn(boolean showMoneyCostBarOn);

    boolean isProjectsFilterFinishedOn();

    void setProjectsFilterFinishedOn(boolean projectsFilterFinishedOn);
}

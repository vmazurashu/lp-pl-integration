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

package org.libreplan.web.reports;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.libreplan.business.labels.daos.ILabelDAO;
import org.libreplan.business.labels.entities.Label;
import org.libreplan.business.reports.dtos.HoursWorkedPerResourceDTO;
import org.libreplan.business.reports.dtos.LabelFilterType;
import org.libreplan.business.resources.daos.ICriterionTypeDAO;
import org.libreplan.business.resources.daos.IResourceDAO;
import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.resources.entities.CriterionType;
import org.libreplan.business.resources.entities.Resource;
import org.libreplan.business.resources.entities.ResourceEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Diego Pino Garcia <dpino@igalia.com>
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class HoursWorkedPerWorkerModel implements IHoursWorkedPerWorkerModel {

    @Autowired
    private IResourceDAO resourceDAO;

    @Autowired
    private ILabelDAO labelDAO;

    @Autowired
    private ICriterionTypeDAO criterionTypeDAO;

    private Set<Resource> selectedResources = new HashSet<>();

    private List<Label> selectedLabels = new ArrayList<>();

    private List<Criterion> selectedCriterions = new ArrayList<>();

    private List<Criterion> allCriterions = new ArrayList<>();

    private String selectedCriteria;

    private String selectedLabel;

    private boolean hasChangeCriteria = false;

    private boolean hasChangeLabels = false;

    private static List<ResourceEnum> applicableResources = new ArrayList<>();

    static {
        applicableResources.add(ResourceEnum.WORKER);
    }

    private boolean showReportMessage = false;

    @Transactional(readOnly = true)
    public JRDataSource getHoursWorkedPerWorkerReport(List<Resource> resources,
                                                      List<Label> labels,
                                                      LabelFilterType labelFilterType,
                                                      List<Criterion> criterions,
                                                      Date startingDate,
                                                      Date endingDate) {

        final List<HoursWorkedPerResourceDTO> workingHoursPerWorkerList = resourceDAO.getWorkingHoursPerWorker(
                resources, labels, labelFilterType, criterions, startingDate, endingDate);

        if ( workingHoursPerWorkerList != null && !workingHoursPerWorkerList.isEmpty() ) {
            Collections.sort(workingHoursPerWorkerList);
            setShowReportMessage(false);

            return new JRBeanCollectionDataSource(workingHoursPerWorkerList);
        } else {
            setShowReportMessage(true);

            return new JREmptyDataSource();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void init() {
        this.selectedResources.clear();
        this.selectedLabels.clear();
        this.selectedCriterions.clear();

        allCriterions.clear();
        loadAllCriterions();
    }

    @Override
    public Set<Resource> getResources() {
        return this.selectedResources;
    }

    @Override
    public void removeSelectedResource(Resource resource) {
        this.selectedResources.remove(resource);
    }

    @Override
    public boolean addSelectedResource(Resource resource) {
        if ( this.selectedResources.contains(resource) )
            return false;

        this.selectedResources.add(resource);
        return true;
    }

    void setShowReportMessage(boolean showReportMessage) {
        this.showReportMessage = showReportMessage;
    }

    @Override
    public boolean isShowReportMessage() {
        return showReportMessage;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Label> getAllLabels(){
        List<Label> allLabels = labelDAO.getAll();

        /* Initialize the labels */
        for (Label label : allLabels) {
            label.getType().getName();
        }
        Collections.sort(allLabels);

        return allLabels;
    }

    @Override
    public void removeSelectedLabel(Label label) {
        this.selectedLabels.remove(label);
        hasChangeLabels = true;
    }

    @Override
    public boolean addSelectedLabel(Label label) {
        if ( this.selectedLabels.contains(label) ) {
            return false;
        }
        this.selectedLabels.add(label);
        hasChangeLabels = true;

        return true;
    }

    @Override
    public List<Label> getSelectedLabels() {
        return selectedLabels;
    }

    @Override
    public List<Criterion> getCriterions() {
        Collections.sort(allCriterions);

        return this.allCriterions;
    }

    private void loadAllCriterions() {
        List<CriterionType> listTypes = getCriterionTypes();
        for (CriterionType criterionType : listTypes) {
            if ( criterionType.isEnabled() ) {
                Set<Criterion> listCriterion = getDirectCriterions(criterionType);
                addCriterionWithItsType(listCriterion);
            }
        }
    }

    private static Set<Criterion> getDirectCriterions(CriterionType criterionType) {
        Set<Criterion> criterions = new HashSet<>();
        for (Criterion criterion : criterionType.getCriterions()) {
            if ( criterion.getParent() == null ) {
                criterions.add(criterion);
            }
        }
        return criterions;
    }

    private void addCriterionWithItsType(Set<Criterion> children) {
        for (Criterion criterion : children) {
            if ( criterion.isActive() ) {
                allCriterions.add(criterion);
                addCriterionWithItsType(criterion.getChildren());
            }
        }
    }

    private List<CriterionType> getCriterionTypes() {
        return criterionTypeDAO.getCriterionTypesByResources(applicableResources);
    }

    @Override
    public void removeSelectedCriterion(Criterion criterion) {
        this.selectedCriterions.remove(criterion);
        hasChangeCriteria = true;
    }

    @Override
    public boolean addSelectedCriterion(Criterion criterion) {
        if ( this.selectedCriterions.contains(criterion) ) {
            return false;
        }
        this.selectedCriterions.add(criterion);
        hasChangeCriteria = true;

        return true;
    }

    @Override
    public List<Criterion> getSelectedCriterions() {
        return selectedCriterions;
    }

    public void setSelectedLabel(String selectedLabel) {
        this.selectedLabel = selectedLabel;
    }

    public String getSelectedLabel() {
        if ( hasChangeLabels ) {
            this.selectedLabel = null;
            Iterator<Label> iterator = this.selectedLabels.iterator();
            if ( iterator.hasNext() ) {
                this.selectedLabel = "";
                this.selectedLabel = this.selectedLabel.concat(iterator.next().getName());
            }
            while ( iterator.hasNext() ) {
                this.selectedLabel = this.selectedLabel.concat(", " + iterator.next().getName());
            }
            hasChangeLabels = false;
        }
        return selectedLabel;
    }

    public void setSelectedCriteria(String selectedCriteria) {
        this.selectedCriteria = selectedCriteria;
    }

    public String getSelectedCriteria() {
        if ( hasChangeCriteria ) {
            this.selectedCriteria = null;
            Iterator<Criterion> iterator = this.selectedCriterions.iterator();

            if ( iterator.hasNext() ) {
                this.selectedCriteria = "";
                this.selectedCriteria = this.selectedCriteria.concat(iterator.next().getName());
            }

            while ( iterator.hasNext() ) {
                this.selectedCriteria = this.selectedCriteria.concat(", " + iterator.next().getName());
            }

            hasChangeCriteria = false;
        }

        return selectedCriteria;
    }

}

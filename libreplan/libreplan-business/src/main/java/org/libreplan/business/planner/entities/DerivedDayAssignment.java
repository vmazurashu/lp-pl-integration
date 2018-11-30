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
package org.libreplan.business.planner.entities;

import org.apache.commons.lang3.Validate;
import javax.validation.constraints.NotNull;
import org.joda.time.LocalDate;
import org.libreplan.business.common.BaseEntity;
import org.libreplan.business.resources.entities.Resource;
import org.libreplan.business.resources.entities.Worker;
import org.libreplan.business.scenarios.entities.Scenario;
import org.libreplan.business.util.deepcopy.OnCopy;
import org.libreplan.business.util.deepcopy.Strategy;
import org.libreplan.business.workingday.EffortDuration;


/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 *
 */
public class DerivedDayAssignment extends DayAssignment {

/*    @Deprecated
    public static DerivedDayAssignment create(LocalDate day, int hours,
            Resource resource, DerivedAllocation derivedAllocation) {
        return create(new DerivedDayAssignment(day, hours(hours), resource,
                derivedAllocation));
    }*/

    private static DerivedDayAssignment create(LocalDate day,  EffortDuration hours, Resource resource,
                                               DerivedDayAssignmentsContainer container) {
        return create(new DerivedDayAssignment(day, hours, resource, container));
    }

    public static DerivedDayAssignment create(LocalDate day, EffortDuration duration, Resource resource,
                                              DerivedAllocation derivedAllocation) {
        return create(new DerivedDayAssignment(day, duration, resource, derivedAllocation));
    }

    /**
     * Constructor for Hibernate. DO NOT USE!
     */
    public DerivedDayAssignment() {
        parentState = new ContainerParentState();
    }

    private abstract class ParentState {

        protected abstract DerivedAllocation getAllocation();
        public abstract Scenario getScenario();

    }

    private class TransientParentState extends ParentState {

        private final DerivedAllocation parent;

        private TransientParentState(DerivedAllocation parent) {
            Validate.notNull(parent);
            this.parent = parent;
        }

        @Override
        protected DerivedAllocation getAllocation() {
            return this.parent;
        }

        @Override
        public Scenario getScenario() {
            return null;
        }

    }

    private class ContainerParentState extends ParentState {

        public ContainerParentState() {
        }

        @Override
        protected DerivedAllocation getAllocation() {
            return container.getResourceAllocation();
        }
        @Override
        public Scenario getScenario() {
            return container.getScenario();
        }

    }

    private class DetachedState extends ParentState {

        @Override
        protected DerivedAllocation getAllocation() {
            return null;
        }

        @Override
        public Scenario getScenario() {
            return null;
        }

    }

    @NotNull
    private DerivedDayAssignmentsContainer container;

    @OnCopy(Strategy.IGNORE)
    private ParentState parentState;

    private DerivedDayAssignment(LocalDate day, EffortDuration hours, Resource resource) {
        super(day, hours, resource);
        Validate.isTrue(resource instanceof Worker);
    }

    private DerivedDayAssignment(LocalDate day, EffortDuration hours, Resource resource,
                                DerivedAllocation derivedAllocation) {
        this(day, hours, resource);
        this.parentState = new TransientParentState(derivedAllocation);
    }

    private DerivedDayAssignment(LocalDate day, EffortDuration hours, Resource resource,
                                 DerivedDayAssignmentsContainer container) {
        this(day, hours, resource);
        Validate.notNull(container);
        this.container = container;
        this.parentState = new ContainerParentState();
    }

    public DerivedAllocation getAllocation() {
        return parentState.getAllocation();
    }

    DerivedDayAssignment copyAsChildOf(DerivedDayAssignmentsContainer container) {
        return create(this.getDay(), this.getDuration(), this.getResource(), container);
    }

    DerivedDayAssignment copyAsChildOf(DerivedAllocation derivedAllocation) {
        return create(this.getDay(), this.getDuration(), this.getResource(), derivedAllocation);
    }

    @Override
    protected BaseEntity getParent() {
        return parentState.getAllocation();
    }

    @Override
    public Scenario getScenario() {
        return parentState.getScenario();
    }

    @Override
    public DayAssignment withDuration(EffortDuration newDuration) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void detachFromAllocation() {
        this.parentState = new DetachedState();
    }

}

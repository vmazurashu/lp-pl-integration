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
package org.zkoss.ganttz.timetracker.zoom;

import java.util.Iterator;

import org.apache.commons.lang3.Validate;
import org.joda.time.LocalDate;
import org.joda.time.ReadablePeriod;

/**
 * @author Óscar González Fernández
 *
 */
public abstract class TimeTrackerStateWithSubintervalsFitting extends TimeTrackerState {

    protected TimeTrackerStateWithSubintervalsFitting(IDetailItemModifier firstLevelModifier,
                                                      IDetailItemModifier secondLevelModifier) {

        super(firstLevelModifier, secondLevelModifier);
    }

    private final class PeriodicalGenerator extends LazyGenerator<LocalDate> {

        private final ReadablePeriod period;

        private PeriodicalGenerator(LocalDate first, ReadablePeriod period) {
            super(first);
            Validate.notNull(period);
            this.period = period;
        }

        @Override
        protected LocalDate next(LocalDate last) {
            return last.plus(period);
        }
    }

    @Override
    protected Iterator<LocalDate> getPeriodsFirstLevelGenerator(final LocalDate start) {
        return new PeriodicalGenerator(start, getPeriodFirstLevel());
    }

    @Override
    protected Iterator<LocalDate> getPeriodsSecondLevelGenerator(LocalDate start) {
        return new PeriodicalGenerator(start, getPeriodSecondLevel());
    }

    protected abstract ReadablePeriod getPeriodFirstLevel();

    protected abstract ReadablePeriod getPeriodSecondLevel();

}

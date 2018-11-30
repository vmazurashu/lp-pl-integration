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

import org.joda.time.Days;
import org.joda.time.LocalDate;

/**
 * Describes levels of zooming (time-zooming) on Gantt panel.
 *
 * @author Francisco Javier Moran Rúa
 */
public enum ZoomLevel {

    DETAIL_ONE(_("Year")) {
        @Override
        public TimeTrackerState getTimeTrackerState(IDetailItemModifier firstLevel, IDetailItemModifier secondLevel) {
            return new DetailOneTimeTrackerState(firstLevel, secondLevel);
        }

        @Override
        public boolean isSuitableFor(int days) {
            return days > 950;
        }
    },

    DETAIL_TWO(_("Quarter")) {
        @Override
        public TimeTrackerState getTimeTrackerState(IDetailItemModifier firstLevel, IDetailItemModifier secondLevel) {
            return new DetailTwoTimeTrackerState(firstLevel, secondLevel);
        }

        @Override
        public boolean isSuitableFor(int days) {
            return days > 550;
        }
    },

    DETAIL_THREE(_("Month")) {
        @Override
        public TimeTrackerState getTimeTrackerState(IDetailItemModifier firstLevel, IDetailItemModifier secondLevel) {
            return new DetailThreeTimeTrackerState(firstLevel, secondLevel);
        }

        @Override
        public boolean isSuitableFor(int days) {
            return days > 175;
        }
    },

    DETAIL_FOUR(_("Week")) {
        @Override
        public TimeTrackerState getTimeTrackerState(IDetailItemModifier firstLevel, IDetailItemModifier secondLevel) {
            return new DetailFourTimeTrackerState(firstLevel, secondLevel);
        }

        @Override
        public boolean isSuitableFor(int days) {
            return days > 50;
        }
    },

    DETAIL_FIVE(_("Day")) {
        @Override
        public TimeTrackerState getTimeTrackerState(IDetailItemModifier firstLevel, IDetailItemModifier secondLevel) {
            return new DetailFiveTimeTrackerState(firstLevel, secondLevel);
        }

        @Override
        public boolean isSuitableFor(int days) {
            return true;
        }
    };

    private String internalName;

    ZoomLevel(String name) {
        this.internalName = name;
    }

    /**
     * Forces to mark the string as needing translation.
     */
    private static String _(String string) {
        return string;
    }

    public String getInternalName() {
        return internalName;
    }

    /**
     * @return if there is no next, returns <code>this</code>. Otherwise returns the next one.
     */
    public ZoomLevel next() {
        final int next = ordinal() + 1;

        return next == ZoomLevel.values().length ? this : ZoomLevel.values()[next];
    }

    /**
     * @return if there is no previous, returns <code>this</code>. Otherwise returns the previous one.
     */
    public ZoomLevel previous() {
        return ordinal() == 0 ? this : ZoomLevel.values()[ordinal() - 1];
    }

    public abstract TimeTrackerState getTimeTrackerState(
            IDetailItemModifier firstLevel, IDetailItemModifier secondLevel);

    @Override
    public String toString() {
        return _(internalName);
    }

    public static ZoomLevel getFromString(String zoomLevelParameter) {
        ZoomLevel requiredZoomLevel = ZoomLevel.DETAIL_ONE;

        if (zoomLevelParameter != null) {
            for (ZoomLevel z : ZoomLevel.values()) {
                if (zoomLevelParameter.equals(z.internalName)) {
                    requiredZoomLevel = z;
                }
            }
        }
        return requiredZoomLevel;

    }

    public static ZoomLevel getDefaultZoomByDates(LocalDate initDate, LocalDate endDate) {
        if (initDate != null && endDate != null) {
            int days = Days.daysBetween(initDate, endDate).getDays();

            for (ZoomLevel each : ZoomLevel.values()) {
                if (each.isSuitableFor(days)) {
                    return each;
                }
            }
        }
        return ZoomLevel.DETAIL_ONE;
    }

    protected abstract boolean isSuitableFor(int days);

}

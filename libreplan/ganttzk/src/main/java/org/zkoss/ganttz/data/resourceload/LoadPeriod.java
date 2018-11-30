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

package org.zkoss.ganttz.data.resourceload;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.zkoss.ganttz.data.GanttDate;

public class LoadPeriod {

    private static final Log LOG = LogFactory.getLog(LoadPeriod.class);

    private final GanttDate start;

    private final GanttDate end;

    private final LoadLevel loadLevel;

    private final String availableEffort;

    private final String assignedEffort;

    public LoadPeriod(
            GanttDate start,
            GanttDate end,
            String availableEffort,
            String assignedEffort,
            LoadLevel loadLevel) {

        Validate.notNull(start);
        Validate.notNull(end);
        Validate.notNull(loadLevel);
        Validate.notNull(availableEffort);
        Validate.notNull(assignedEffort);
        Validate.isTrue(start.compareTo(end) <= 0);

        this.start = start;
        this.end = end;
        this.loadLevel = loadLevel;
        this.availableEffort = availableEffort;
        this.assignedEffort = assignedEffort;
    }

    public GanttDate getStart() {
        return start;
    }

    public GanttDate getEnd() {
        return end;
    }

    public boolean overlaps(LoadPeriod other) {
        return start.compareTo(other.end) < 0 && end.compareTo(other.start) > 0;
    }

    /**
     * @param notOverlappingPeriods
     * @return
     * @throws IllegalArgumentException
     *             if some of the LoadPeriod overlaps
     */
    public static List<LoadPeriod> sort(Collection<? extends LoadPeriod> notOverlappingPeriods)
            throws IllegalArgumentException {

        ArrayList<LoadPeriod> result = new ArrayList<>(notOverlappingPeriods);
        Collections.sort(result, new Comparator<LoadPeriod>() {

            @Override
            public int compare(LoadPeriod o1, LoadPeriod o2) {
                if ( o1.overlaps(o2) ) {
                    LOG.warn(o1 + " overlaps with " + o2);
                    throw new IllegalArgumentException(o1 + " overlaps with " + o2);
                }

                int comparison = o1.start.compareTo(o2.start);
                if ( comparison != 0 ) {
                    return comparison;
                }

                return o1.end.compareTo(o2.end);
            }
        });

        return result;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public LoadLevel getLoadLevel() {
        return loadLevel;
    }

    public String getAvailableEffort() {
        return availableEffort;
    }

    public String getAssignedEffort() {
        return assignedEffort;
    }
}

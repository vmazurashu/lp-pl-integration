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

package org.zkoss.ganttz.data.criticalpath;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.zkoss.ganttz.data.GanttDate;
import org.zkoss.ganttz.data.IDependency;


/**
 * Class that represents a node of the graph in order to calculate the critical
 * path.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public class Node<T, D extends IDependency<T>> {

    private T task;
    private Set<T> previousTasks = new HashSet<T>();
    private Set<T> nextTasks = new HashSet<T>();

    private Integer earliestStart = null;
    private Integer earliestFinish = null;
    private Integer latestStart = null;
    private Integer latestFinish = null;

    private LocalDate beginDate = null;
    private LocalDate endDate = null;

    public Node(T task, Set<? extends T> previousTasks,
            Set<? extends T> nextTasks, GanttDate startDate, GanttDate endDate) {
        this.task = task;

        if (startDate != null) {
            this.beginDate = new LocalDate(startDate.toDayRoundedDate());
        }
        if (endDate != null) {
            this.endDate = new LocalDate(endDate.toDayRoundedDate());
        }

        this.earliestStart = 0;
        this.earliestFinish = getDuration();

        if (previousTasks != null) {
            this.previousTasks = new HashSet<T>(previousTasks);
        }
        if (nextTasks != null) {
            this.nextTasks = new HashSet<T>(nextTasks);
        }
    }

    public T getTask() {
        return task;
    }

    public Set<T> getPreviousTasks() {
        return Collections.unmodifiableSet(previousTasks);
    }

    public void addPreviousTask(T task) {
        previousTasks.add(task);
    }

    public Set<T> getNextTasks() {
        return Collections.unmodifiableSet(nextTasks);
    }

    public void addNextTask(T task) {
        nextTasks.add(task);
    }

    public int getEarliestStart() {
        return earliestStart;
    }

    public int getEarliestFinish() {
        return earliestFinish;
    }

    public int getLatestStart() {
        return latestStart;
    }

    public void setEarliestStart(int earliestStart) {
        if (this.earliestStart < earliestStart) {
            this.earliestStart = earliestStart;
            this.earliestFinish = earliestStart + getDuration();
        }
    }

    public void setLatestFinish(int latestFinish) {
        if ((this.latestFinish == null) || (this.latestFinish > latestFinish)) {
                this.latestFinish = latestFinish;
                this.latestStart = latestFinish - getDuration();
        }
    }

    public int getLatestFinish() {
        return latestFinish;
    }

    public int getDuration() {
        if (task == null) {
            return 0;
        }

        return Days.daysBetween(beginDate, endDate).getDays();
    }

    @Override
    public String toString() {
        return "Task: " + getDuration() + " (" + earliestStart + ","
                + earliestFinish + ") (" + latestStart + "," + latestFinish
                + ")";
    }

}

/*
 * This file is part of LibrePlan
 *
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

package org.libreplan.web.dashboard;

import static org.libreplan.web.I18nHelper._;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.zkoss.zk.ui.util.Clients;

/**
 * @author Diego Pino García <dpino@igalia.com>
 */
public class GlobalProgressChart {

    public static String SPREAD_PROGRESS;

    public static String ALL_TASKS_HOURS;

    public static String CRITICAL_PATH_HOURS;

    public static String CRITICAL_PATH_DURATION;

    private Map<String, BigDecimal> current = new LinkedHashMap<>();

    private Map<String, BigDecimal> expected = new LinkedHashMap<>();

    private List<Series> series = new ArrayList<>();

    private GlobalProgressChart() {
        series.add(Series.create(_("Current"), "#004469"));
        series.add(Series.create(_("Expected"), "#3C90BE"));

        SPREAD_PROGRESS = _("Spreading progress");
        ALL_TASKS_HOURS = _("By all tasks hours");
        CRITICAL_PATH_HOURS = _("By critical path hours");
        CRITICAL_PATH_DURATION = _("By critical path duration");
    }

    public void current(String key, BigDecimal value) {
        current.put(key, value);
    }

    public void expected(String key, BigDecimal value) {
        expected.put(key, value);
    }

    public static GlobalProgressChart create() {
        return new GlobalProgressChart();
    }

    public String getPercentages() {
        return String.format("'[%s, %s]'",
                jsonifyPercentages(current.values()),
                jsonifyPercentages(expected.values()));
    }

    private String jsonifyPercentages(Collection<BigDecimal> array) {
        List<String> result = new ArrayList<>();

        int i = 1;
        for (BigDecimal each : array) {
            result.add(String.format(Locale.ROOT, "[%.2f, %d]", each.doubleValue(), i++));
        }
        return String.format("[%s]", StringUtils.join(result, ","));
    }

    private String jsonify(Collection<?> list) {
        Collection<String> result = new ArrayList<>();
        for (Object each : list) {
            result.add(jsonify(each));
        }
        return String.format("[%s]", StringUtils.join(result, ','));
    }

    private String jsonify(Object value) {
        return String
                .format((value instanceof String) ? "\"%s\"" : "%s", value)
                .replaceAll("'", "\\\\'");
    }

    public String getSeries() {
        return jsonify(series);
    }

    /**
     * The order of the ticks is taken from the keys in current.
     *
     * @return {@link String}
     */
    public String getTicks() {
        return jsonify(current.keySet());
    }

    public void render() {
        String params = String.format(
                "'{\"title\": %s, \"label\": %s, \"ticks\": %s, \"series\": %s}'",
                jsonify(_("Project progress percentage")),
                jsonify(_("Progress percentage per progress type")),
                getTicks(), getSeries());

        String command = String.format("global_progress.render(%s, %s);", getPercentages(), params);
        Clients.evalJavaScript(command);
    }

    static class Series {

        private String label;

        private String color;

        private Series() {
        }

        public static Series create(String label) {
            Series series = new Series();
            series.label = label;
            return series;
        }

        public static Series create(String label, String color) {
            Series series = new Series();
            series.label = label;
            series.color = color;
            return series;
        }

        @Override
        public String toString() {
            return String.format("{\"label\": \"%s\", \"color\": \"%s\"}", label, color);
        }

    }

}

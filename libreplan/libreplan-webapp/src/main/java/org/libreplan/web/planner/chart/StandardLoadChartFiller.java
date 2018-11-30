package org.libreplan.web.planner.chart;

import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.SortedMap;

import org.joda.time.LocalDate;
import org.libreplan.business.planner.chart.ILoadChartData;
import org.libreplan.business.workingday.EffortDuration;
import org.zkforge.timeplot.Plotinfo;
import org.zkoss.ganttz.util.Interval;

public abstract class StandardLoadChartFiller extends LoadChartFiller {

    @Override
    protected Plotinfo[] getPlotInfo(Interval interval) {
        final ILoadChartData data = getDataOn(interval);

        Plotinfo plotInfoLoad = createPlotinfoFromDurations(getLoad(data), interval);
        plotInfoLoad.setFillColor(COLOR_ASSIGNED_LOAD);
        plotInfoLoad.setLineWidth(0);

        Plotinfo plotInfoMax = createPlotinfoFromDurations(getCalendarMaximumAvailability(data), interval);
        plotInfoMax.setLineColor(COLOR_CAPABILITY_LINE);
        plotInfoMax.setFillColor("#FFFFFF");
        plotInfoMax.setLineWidth(2);

        Plotinfo plotInfoOverload = createPlotinfoFromDurations(getOverload(data), interval);
        plotInfoOverload.setFillColor(COLOR_OVERLOAD);
        plotInfoOverload.setLineWidth(0);

        return new Plotinfo[] { plotInfoOverload, plotInfoMax, plotInfoLoad };
    }

    protected abstract ILoadChartData getDataOn(Interval interval);

    protected LocalDate getStart(LocalDate explicitlySpecifiedStart, Interval interval) {
        return explicitlySpecifiedStart == null
                ? interval.getStart()
                : Collections.max(asList(explicitlySpecifiedStart, interval.getStart()));
    }

    @SuppressWarnings("unchecked")
    protected LocalDate getEnd(LocalDate explicitlySpecifiedEnd, Interval interval) {
        return explicitlySpecifiedEnd == null
                ? interval.getFinish()
                : Collections.min(asList(explicitlySpecifiedEnd, interval.getFinish()));
    }

    private SortedMap<LocalDate, EffortDuration> getLoad(ILoadChartData data) {
        return groupAsNeededByZoom(data.getLoad());
    }

    private SortedMap<LocalDate, EffortDuration> getOverload(ILoadChartData data) {
        return groupAsNeededByZoom(data.getOverload());
    }

    private SortedMap<LocalDate, EffortDuration> getCalendarMaximumAvailability(ILoadChartData data) {
        return groupAsNeededByZoom(data.getAvailability());
    }

}

/*
 * This file is part of LibrePlan
 *
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

package org.libreplan.web.dashboard;

import static org.libreplan.web.I18nHelper._;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.planner.entities.TaskElement;
import org.libreplan.business.planner.entities.TaskStatusEnum;
import org.libreplan.web.dashboard.DashboardModel.Interval;
import org.libreplan.web.planner.order.PlanningStateCreator.PlanningState;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;

import br.com.digilabs.jqplot.Chart;
import br.com.digilabs.jqplot.JqPlotUtils;
import br.com.digilabs.jqplot.chart.BarChart;
import br.com.digilabs.jqplot.chart.PieChart;
import br.com.digilabs.jqplot.elements.Serie;

/**
 * Controller for dashboardfororder view.
 *
 * @author Nacho Barrientos <nacho@igalia.com>
 * @author Diego Pino García <dpino@igalia.com>
 * @author Vova Perebykivskyi <vova@libreplan-enterprise.com>
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DashboardController extends GenericForwardComposer {

    private IDashboardModel dashboardModel;

    private Label lblOvertimeRatio;

    private Label lblAvailabilityRatio;

    private Label lblAbsolute;

    private org.zkoss.zk.ui.Component costStatus;

    private Div projectDashboardChartsDiv;

    private Div projectDashboardNoTasksWarningDiv;

    public DashboardController() {
        if ( dashboardModel == null ) {
            dashboardModel = (IDashboardModel) SpringUtil.getBean("dashboardModel");
        }
    }

    @Override
    public void doAfterCompose(org.zkoss.zk.ui.Component comp) throws Exception {
        super.doAfterCompose(comp);
    }

    public String loadResourceFile(String filename) {
        final String newline = "\n";

        ApplicationContext ctx = new ClassPathXmlApplicationContext();
        Resource res = ctx.getResource(filename);
        BufferedReader reader;
        StringBuilder sb = new StringBuilder();

        try {
            reader = new BufferedReader(new InputStreamReader(res.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append(newline);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    public void setCurrentOrder(PlanningState planningState, List<TaskElement> criticalPath) {
        final Order order = planningState.getOrder();

        dashboardModel.setCurrentOrder(planningState, criticalPath);
        if (dashboardModel.tasksAvailable()) {
            if (self != null) {
                renderGlobalProgress();
                renderTaskStatus();
                renderTaskCompletationLag();
                renderDeadlineViolation();
                renderMarginWithDeadline();
                renderEstimationAccuracy();
                renderCostStatus(order);
                renderOvertimeRatio();
                renderAvailabilityRatio();
            }
            showCharts();
        } else {
            hideCharts();
        }
    }

    private void renderOvertimeRatio() {
        BigDecimal overtimeRatio = dashboardModel.getOvertimeRatio();
        lblOvertimeRatio.setValue(showAsPercentage(overtimeRatio));
        String valueMeaning = (overtimeRatio.compareTo(BigDecimal.ZERO) == 0) ? "positive" : "negative";
        lblOvertimeRatio.setSclass("dashboard-label-remarked " + valueMeaning);
    }

    private String showAsPercentage(BigDecimal overtimeRatio) {
        return overtimeRatio.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP) + " %";
    }

    private void renderAvailabilityRatio() {
        lblAvailabilityRatio.setValue(showAsPercentage(dashboardModel.getAvailabilityRatio()));
    }

    private void renderCostStatus(Order order) {
        CostStatusController costStatusController = getCostStatusController();
        costStatusController.setOrder(order);
        costStatusController.render();
    }

    private CostStatusController getCostStatusController() {
        return (CostStatusController) costStatus.getAttribute("controller");
    }

    private void renderMarginWithDeadline() {

        Integer absoluteMargin = dashboardModel.getAbsoluteMarginWithDeadLine();
        BigDecimal relativeMargin = dashboardModel.getMarginWithDeadLine();

        if ( (lblAbsolute != null) && (absoluteMargin != null) ) {
            lblAbsolute.setValue(_(
                    "There is a margin of {0} days with the project global deadline ({1}%).",
                    absoluteMargin,
                    (new DecimalFormat("#.##")).format(relativeMargin.doubleValue() * 100)));
        } else {
            lblAbsolute.setValue(_("No project deadline defined"));
        }

    }

    private void renderDeadlineViolation() {
        final String divId = "deadline-violation";

        PieChart<Number> pieChart = new PieChart<>(_("Task deadline violations"));
        pieChart.addValue(_("On schedule"), dashboardModel.getPercentageOfOnScheduleTasks());
        pieChart.addValue(_("Violated deadline"), dashboardModel.getPercentageOfTasksWithViolatedDeadline());
        pieChart.addValue(_("No deadline"), dashboardModel.getPercentageOfTasksWithNoDeadline());

        pieChart.addIntervalColors("#8fbe86", "#eb6b71", "#cfcfcf");

        renderPieChart(pieChart, divId);
    }

    /**
     * Use this method to render a {@link PieChart}.
     *
     * FIXME:
     * jqplot4java doesn't include a method for changing the colors or a {@link PieChart}.
     * The only way to do it is to add the colors to an
     * Interval, generate the output Javascript code and replace the string 'intervalColors' by 'seriesColors'.
     *
     * @param chart
     * @param divId
     */
    private void renderPieChart(Chart<?> chart, String divId) {
        String jsCode = JqPlotUtils.createJquery(chart, divId);
        jsCode = jsCode.replace("intervalColors", "seriesColors");
        Clients.evalJavaScript(jsCode);
    }

    private void renderChart(Chart<?> chart, String divId) {
        String jsCode = JqPlotUtils.createJquery(chart, divId);
        Clients.evalJavaScript(jsCode);
    }

    private void renderTaskCompletationLag() {
        final String divId = "task-completation-lag";

        BarChart<Integer> barChart;
        barChart = new BarChart<>(_("Task Completation Lead/Lag"));

        barChart.setFillZero(true);
        barChart.setHighlightMouseDown(true);
        barChart.setStackSeries(false);
        barChart.setBarMargin(30);

        barChart.addSeries(new Serie("Tasks"));

        TaskCompletionData taskCompletionData = TaskCompletionData.create(dashboardModel);

        // TODO resolve deprecated
        barChart.setTicks(taskCompletionData.getTicks());

        barChart.addValues(taskCompletionData.getValues());

        barChart.getAxes()
                .getXaxis()
                .setLabel(_("Days Interval (Calculated as task completion end date minus estimated end date)"));

        barChart.getAxes().yAxisInstance().setLabel(_("Number of tasks"));

        renderChart(barChart, divId);
    }

    private void renderEstimationAccuracy() {
        final String divId = "estimation-accuracy";

        BarChart<Integer> barChart;
        barChart = new BarChart<>(_("Estimation deviation on completed tasks"));

        barChart.setFillZero(true);
        barChart.setHighlightMouseDown(true);
        barChart.setStackSeries(false);
        barChart.setBarMargin(30);

        barChart.addSeries(new Serie("Tasks"));

        EstimationAccuracy estimationAccuracyData = EstimationAccuracy.create(dashboardModel);

        // TODO resolve deprecated
        barChart.setTicks(estimationAccuracyData.getTicks());

        barChart.addValues(estimationAccuracyData.getValues());

        barChart.getAxes()
                .getXaxis()
                .setLabel(_("% Deviation interval (difference % between consumed and estimated hours)"));

        barChart.getAxes().yAxisInstance().setLabel(_("Number of tasks"));

        renderChart(barChart, divId);
    }

    private String statusLegend(TaskStatusEnum status, Map<TaskStatusEnum, Integer> taskStatus) {
        return _(status.toString()) + String.format(_(" (%d tasks)"), taskStatus.get(status));
    }

    private void renderTaskStatus() {
        final String divId = "task-status";

        Map<TaskStatusEnum, Integer> taskStatus = dashboardModel.calculateTaskStatus();
        PieChart<Number> taskStatusPieChart = new PieChart<>(_("Task Status"));

        taskStatusPieChart.addValue(
                statusLegend(TaskStatusEnum.FINISHED, taskStatus),
                dashboardModel.getPercentageOfFinishedTasks());

        taskStatusPieChart.addValue(
                statusLegend(TaskStatusEnum.IN_PROGRESS, taskStatus),
                dashboardModel.getPercentageOfInProgressTasks());

        taskStatusPieChart.addValue(
                statusLegend(TaskStatusEnum.READY_TO_START, taskStatus),
                dashboardModel.getPercentageOfReadyToStartTasks());

        taskStatusPieChart.addValue(
                statusLegend(TaskStatusEnum.BLOCKED, taskStatus),
                dashboardModel.getPercentageOfBlockedTasks());

        taskStatusPieChart.addIntervalColors("#d599e8", "#4c99e8", "#8fbe86", "#ffbb6b");

        renderPieChart(taskStatusPieChart, divId);
    }

    private void renderGlobalProgress() {
        GlobalProgressChart globalProgressChart = GlobalProgressChart.create();

        // Current values
        globalProgressChart.current(
                GlobalProgressChart.CRITICAL_PATH_DURATION,
                dashboardModel.getCriticalPathProgressByDuration());

        globalProgressChart.current(
                GlobalProgressChart.CRITICAL_PATH_HOURS,
                dashboardModel.getCriticalPathProgressByNumHours());

        globalProgressChart.current(
                GlobalProgressChart.ALL_TASKS_HOURS,
                dashboardModel.getAdvancePercentageByHours());

        globalProgressChart.current(
                GlobalProgressChart.SPREAD_PROGRESS,
                dashboardModel.getSpreadProgress());

        // Expected values
        globalProgressChart.expected(
                GlobalProgressChart.CRITICAL_PATH_DURATION,
                dashboardModel.getExpectedCriticalPathProgressByDuration());

        globalProgressChart.expected(
                GlobalProgressChart.CRITICAL_PATH_HOURS,
                dashboardModel.getExpectedCriticalPathProgressByNumHours());

        globalProgressChart.expected(
                GlobalProgressChart.ALL_TASKS_HOURS,
                dashboardModel.getExpectedAdvancePercentageByHours());

        globalProgressChart.expected(
                GlobalProgressChart.SPREAD_PROGRESS,
                BigDecimal.ZERO);

        globalProgressChart.render();
    }

    private void showCharts() {
        projectDashboardChartsDiv.setVisible(true);
        projectDashboardNoTasksWarningDiv.setVisible(false);
    }

    private void hideCharts() {
        projectDashboardChartsDiv.setVisible(false);
        projectDashboardNoTasksWarningDiv.setVisible(true);
    }

    /**
     * @author Diego Pino García<dpino@igalia.com>
     */
    static class TaskCompletionData {

        private final IDashboardModel dashboardModel;

        private Map<Interval, Integer> taskCompletionData;

        private TaskCompletionData(IDashboardModel dashboardModel) {
            this.dashboardModel = dashboardModel;
        }

        public static TaskCompletionData create(IDashboardModel dashboardModel) {
            return new TaskCompletionData(dashboardModel);
        }

        private Map<Interval, Integer> getData() {
            if ( taskCompletionData == null) {
                taskCompletionData = dashboardModel.calculateTaskCompletion();
            }
            return taskCompletionData;
        }

        String[] getTicks() {
            Set<Interval> intervals = getData().keySet();
            String[] result = new String[intervals.size()];
            int i = 0;

            for (Interval each : intervals) {
                result[i++] = each.toString();

            }
            return result;
        }

        public Collection<Integer> getValues() {
            return getData().values();
        }

    }

    /**
     * @author Diego Pino García<dpino@igalia.com>
     */
    static class EstimationAccuracy {

        private final IDashboardModel dashboardModel;

        private Map<Interval, Integer> estimationAccuracyData;

        private EstimationAccuracy(IDashboardModel dashboardModel) {
            this.dashboardModel = dashboardModel;
        }

        public static EstimationAccuracy create(IDashboardModel dashboardModel) {
            return new EstimationAccuracy(dashboardModel);
        }

        private Map<Interval, Integer> getData() {
            if (estimationAccuracyData == null) {
                estimationAccuracyData = dashboardModel.calculateEstimationAccuracy();
            }
            return estimationAccuracyData;
        }

        String[] getTicks() {
            Set<Interval> intervals = getData().keySet();
            String[] result = new String[intervals.size()];
            int i = 0;

            for (Interval each : intervals) {
                result[i++] = each.toString();

            }
            return result;

        }

        public Collection<Integer> getValues() {
            return getData().values();
        }

    }

}

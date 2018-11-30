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

package org.zkoss.ganttz;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.GregorianCalendar;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.zkoss.ganttz.adapters.IDisabilityConfiguration;
import org.zkoss.ganttz.data.GanttDate;
import org.zkoss.ganttz.data.Task;
import org.zkoss.ganttz.util.ComponentsFinder;
import org.zkoss.util.Locales;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.KeyEvent;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Constraint;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Treerow;

import static org.zkoss.ganttz.i18n.I18nHelper._;

/**
 * Row composer for Tasks details Tree <br />
 *
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 * @author Jeroen Baten <jeroen@jeroenbaten.nl>
 */
public class LeftTasksTreeRow extends GenericForwardComposer {

    public interface ILeftTasksTreeNavigator {

        LeftTasksTreeRow getBelowRow();

        LeftTasksTreeRow getAboveRow();
    }

    private final Task task;

    private Label nameLabel;

    private Textbox nameBox;

    private Label startDateLabel;

    private Textbox startDateTextBox;

    private Label endDateLabel;

    private Textbox endDateTextBox;

    private Datebox openedDateBox = null;

    private DateFormat dateFormat;

    private Planner planner;

    private Div hoursStatusDiv;

    private Div budgetStatusDiv;

    private final ILeftTasksTreeNavigator leftTasksTreeNavigator;

    private final IDisabilityConfiguration disabilityConfiguration;

    private Properties properties;

    private static final String PROPERTIES_FILENAME = "libreplan.properties";

    private static final int CALENDAR_START_YEAR = 2001;

    private static final int MINIMUM_MONTH = 1;

    private static final int MINIMUM_DAY = 1;

    public static LeftTasksTreeRow create(IDisabilityConfiguration disabilityConfiguration,
                                          Task bean,
                                          ILeftTasksTreeNavigator taskDetailnavigator,
                                          Planner planner) {

        return new LeftTasksTreeRow(disabilityConfiguration, bean, taskDetailnavigator, planner);
    }

    private LeftTasksTreeRow(IDisabilityConfiguration disabilityConfiguration,
                             Task task,
                             ILeftTasksTreeNavigator leftTasksTreeNavigator,
                             Planner planner) {

        this.disabilityConfiguration = disabilityConfiguration;
        this.task = task;
        this.dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locales.getCurrent());
        this.leftTasksTreeNavigator = leftTasksTreeNavigator;
        this.planner = planner;
        setUpProperties();
    }

    private void setUpProperties () {
        // Getting properties from file (libreplan-business/src/main/resources/libreplan.properties)
        properties = new Properties();
        InputStream inputStream = LeftTasksTreeRow.class.getClassLoader().getResourceAsStream(PROPERTIES_FILENAME);
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Task getTask() {
        return task;
    }

    public Textbox getNameBox() {
        return nameBox;
    }

    public void setNameBox(Textbox nameBox) {
        this.nameBox = nameBox;
    }


    public Task getData() {
        return task;
    }

    /**
     * When a text box associated to a datebox is requested to show the datebox,
     * the corresponding datebox is shown
     * @param component
     *            the component that has received focus
     */
    public void userWantsDateBox(Component component) {
        if ( component == startDateTextBox ) {
            if ( canChangeStartDate() ) {
                createDateBox(startDateTextBox);
            }
        }

        if ( component == endDateTextBox ) {
            if ( canChangeEndDate() ) {
                createDateBox(endDateTextBox);
            }
        }
    }

    public void createDateBox(Textbox textbox) {
        openedDateBox = new Datebox();
        openedDateBox.setFormat("short");
        openedDateBox.setButtonVisible(false);

        try {
            openedDateBox.setValue(dateFormat.parse(textbox.getValue()));
        } catch (ParseException e) {
            return;
        }

        registerOnEnterOpenDateBox(openedDateBox);
        registerBlurListener(openedDateBox);
        registerOnChangeDatebox(openedDateBox, textbox);

        textbox.setVisible(false);
        textbox.getParent().appendChild(openedDateBox);
        openedDateBox.setFocus(true);
        openedDateBox.setOpen(true);

        openedDateBox.setConstraint(generateConstraintForDates());
    }

    private Constraint generateConstraintForDates() {
        return  new Constraint() {
            @Override
            public void validate(Component comp, Object value) throws WrongValueException {

                // Getting parameters from properties file
                int yearLimit = Integer.parseInt(properties.getProperty("yearLimit"));
                int minimumYear = Integer.parseInt(properties.getProperty("minimumYear"));

                DateTime today = new DateTime();
                DateTime maximum = today.plusYears(yearLimit);

                DateTime minimum =
                        new DateTime(new GregorianCalendar(minimumYear, MINIMUM_MONTH, MINIMUM_DAY).getTime());

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yy");

                // Need to call dateFormat.set2DigitYearStart to force parser not to parse date to previous century
                simpleDateFormat.set2DigitYearStart(
                        new GregorianCalendar(CALENDAR_START_YEAR, MINIMUM_MONTH, MINIMUM_DAY).getTime());

                Date date = null;

                /*
                 * Need to check value type because constraint is created for textbox and datebox.
                 * Textbox returns value in String. Datebox returns value in java.util.Date.
                 * Also need to take last two year digits because Datebox component formats it's value.
                 */

                if (value instanceof Date) {
                    try {

                        // Using DateTime (Joda Time class) because java.util.Date.getYear() returns invalid value
                        DateTime correct = new DateTime(value);
                        String year = Integer.valueOf(correct.getYear()).toString().substring(2);

                        // TODO Resolve deprecated methods
                        date = simpleDateFormat
                                .parse(((Date) value).getMonth() + "/" + ((Date) value).getDate() + "/" + year);

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    try {
                        date = simpleDateFormat.parse((String) value);
                    } catch (ParseException ignored) {
                    }
                }

                DateTime dateTimeInTextbox = new DateTime(date);

                if (dateTimeInTextbox.isAfter(maximum)) {
                    throw new WrongValueException(
                            comp,
                            _("The date you entered is invalid") + ". " +
                                    _("Please enter date not before") + " " + minimumYear +
                                    " " + _("and no later than") + " " + maximum.getYear());
                }
                if (dateTimeInTextbox.isBefore(minimum)) {
                    throw new WrongValueException(
                            comp,
                            _("The date you entered is invalid") + ". " +
                                    _("Please enter date not before") + " " + minimumYear +
                                    " " + _("and no later than") + " " + maximum.getYear());
                }
            }
        };
    }

    private enum Navigation {
        LEFT,
        UP,
        RIGHT,
        DOWN;

        public static Navigation getIntentFrom(KeyEvent keyEvent) {
            return values()[keyEvent.getKeyCode() - 37];
        }
    }

    public void focusGoUp(int position) {
        LeftTasksTreeRow aboveDetail = leftTasksTreeNavigator.getAboveRow();
        if ( aboveDetail != null ) {
            aboveDetail.receiveFocus(position);
        }
    }

    public void receiveFocus() {
        receiveFocus(0);
    }

    public void receiveFocus(int position) {
        this.getTextBoxes().get(position).focus();
    }

    public void focusGoDown(int position) {
        LeftTasksTreeRow belowDetail = leftTasksTreeNavigator.getBelowRow();
        if ( belowDetail != null ) {
            belowDetail.receiveFocus(position);
        } else {
            getListDetails().getGoingDownInLastArrowCommand().doAction();
        }
    }

    private LeftTasksTree getListDetails() {
        Component current = nameBox;
        while (!(current instanceof LeftTasksTree)) {
            current = current.getParent();
        }

        return (LeftTasksTree) current;
    }

    public void userWantsToMove(Textbox textbox, KeyEvent keyEvent) {
        Navigation navigation = Navigation.getIntentFrom(keyEvent);
        List<Textbox> textBoxes = getTextBoxes();
        int position = textBoxes.indexOf(textbox);
        switch (navigation) {
            case UP:
                focusGoUp(position);
                break;

            case DOWN:
                focusGoDown(position);
                break;

            default:
                throw new RuntimeException("case not covered: " + navigation);
        }
    }

    private List<Textbox> getTextBoxes() {
        return Arrays.asList(nameBox, startDateTextBox, endDateTextBox);
    }

    /**
     * When the dateBox loses focus the corresponding textbox is shown instead.
     * @param dateBox
     *            the component that has lost focus
     */
    public void dateBoxHasLostFocus(Datebox dateBox) {
        dateBox.getPreviousSibling().setVisible(true);
        dateBox.setParent(null);
    }

    @Override
    public void doAfterCompose(Component component) throws Exception {
        super.doAfterCompose(component);
        findComponents((Treerow) component);
        registerTextboxesListeners();
        updateComponents();
        task.addFundamentalPropertiesChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateComponents();
            }
        });

    }

    private void registerTextboxesListeners() {
        if ( disabilityConfiguration.isTreeEditable() ) {
            registerKeyboardListener(nameBox);
            registerOnChange(nameBox);
            registerKeyboardListener(startDateTextBox);
            registerKeyboardListener(endDateTextBox);
            registerOnEnterListener(startDateTextBox);
            registerOnEnterListener(endDateTextBox);
            registerOnChange(startDateTextBox);
            registerOnChange(endDateTextBox);

            /*
             * Setting constraints right after creating texboxes.
             * This need to be done because constraints must work at first change of textbox.
             */
            startDateTextBox.setConstraint(generateConstraintForDates());
            endDateTextBox.setConstraint(generateConstraintForDates());
        }
    }

    private void findComponents(Treerow row) {
        List<Component> rowChildren = row.getChildren();
        List<Treecell> treeCells = ComponentsFinder.findComponentsOfType(Treecell.class, rowChildren);
        assert treeCells.size() == 4;

        findComponentsForNameCell(treeCells.get(0));
        findComponentsForStartDateCell(treeCells.get(1));
        findComponentsForEndDateCell(treeCells.get(2));
        findComponentsForStatusCell(treeCells.get(3));
    }

    private static Textbox findTextBoxOfCell(Treecell treecell) {
        List<Component> children = treecell.getChildren();
        return ComponentsFinder.findComponentsOfType(Textbox.class, children).get(0);
    }

    private void findComponentsForNameCell(Treecell treecell) {
        if ( disabilityConfiguration.isTreeEditable() ) {
            nameBox = (Textbox) treecell.getChildren().get(0);
        } else {
            nameLabel = (Label) treecell.getChildren().get(0);
        }
    }

    private void registerKeyboardListener(final Textbox textBox) {
        textBox.addEventListener("onCtrlKey", event -> userWantsToMove(textBox, (KeyEvent) event));
    }

    private void registerOnChange(final Component component) {
        component.addEventListener("onChange", event -> updateBean(component));
    }

    private void registerOnChangeDatebox(final Datebox datebox, final Textbox textbox) {
        datebox.addEventListener("onChange", event -> {
            textbox.setValue(dateFormat.format(datebox.getValue()));
            updateBean(textbox);
        });
    }

    private void registerOnEnterListener(final Textbox textBox) {
        textBox.addEventListener("onOK", event -> userWantsDateBox(textBox));
    }

    private void registerOnEnterOpenDateBox(final Datebox datebox) {
        datebox.addEventListener("onOK", event -> datebox.setOpen(true));
    }

    private void findComponentsForStartDateCell(Treecell treecell) {
        if ( disabilityConfiguration.isTreeEditable() ) {
            startDateTextBox = findTextBoxOfCell(treecell);
        } else {
            startDateLabel = (Label) treecell.getChildren().get(0);
        }
    }

    private void findComponentsForEndDateCell(Treecell treecell) {
        if ( disabilityConfiguration.isTreeEditable() ) {
            endDateTextBox = findTextBoxOfCell(treecell);
        } else {
            endDateLabel = (Label) treecell.getChildren().get(0);
        }
    }

    private void findComponentsForStatusCell(Treecell treecell) {
        List<Component> children = treecell.getChildren();

        Hlayout hlayout = ComponentsFinder.findComponentsOfType(Hlayout.class, children).get(0);

        hoursStatusDiv = (Div) hlayout.getChildren().get(0);
        // there is a <label> "/" between the divs
        budgetStatusDiv = (Div) hlayout.getChildren().get(2);

    }

    private void registerBlurListener(final Datebox datebox) {
        datebox.addEventListener("onBlur", event -> dateBoxHasLostFocus(datebox));
    }

    public void updateBean(Component updatedComponent) {
        if ( updatedComponent == getNameBox() ) {

            task.setName(getNameBox().getValue());

            if ( StringUtils.isEmpty(getNameBox().getValue()) ) {
                getNameBox().setValue(task.getName());
            }

        } else if ( updatedComponent == getStartDateTextBox() ) {

            try {
                final Date begin = dateFormat.parse(getStartDateTextBox().getValue());
                task.doPositionModifications(position -> position.moveTo(GanttDate.createFrom(begin)));
            } catch (ParseException e) {
                // Do nothing as textbox is rested in the next sentence
            }

            getStartDateTextBox().setValue(dateFormat.format(task.getBeginDate().toDayRoundedDate()));

        } else if ( updatedComponent == getEndDateTextBox() ) {

            try {
                Date newEnd = dateFormat.parse(getEndDateTextBox().getValue());
                task.resizeTo(LocalDate.fromDateFields(newEnd));
            } catch (ParseException e) {
                // Do nothing as textbox is rested in the next sentence
            }

            getEndDateTextBox().setValue(asString(task.getEndDate().toDayRoundedDate()));
        }

        planner.updateTooltips();
    }

    private void updateComponents() {
        if ( disabilityConfiguration.isTreeEditable() ) {
            getNameBox().setValue(task.getName());
            getNameBox().setDisabled(!canRenameTask());
            getNameBox().setTooltiptext(task.getName());

            getStartDateTextBox().setDisabled(!canChangeStartDate());
            getEndDateTextBox().setDisabled(!canChangeEndDate());

            getStartDateTextBox().setValue(asString(task.getBeginDate().toDayRoundedDate()));
            getEndDateTextBox().setValue(asString(task.getEndDate().toDayRoundedDate()));
        } else {
            nameLabel.setValue(task.getName());
            nameLabel.setTooltiptext(task.getName());
            nameLabel.setSclass("clickable-rows");

            nameLabel.addEventListener(Events.ON_CLICK,
                    arg0 -> Executions.getCurrent().sendRedirect("/planner/index.zul;order=" + task.getProjectCode()));

            startDateLabel.setValue(asString(task.getBeginDate().toDayRoundedDate()));
            endDateLabel.setValue(asString(task.getEndDate().toDayRoundedDate()));
        }

        setHoursStatus(task.getProjectHoursStatus(), task.getTooltipTextForProjectHoursStatus());

        setBudgetStatus(task.getProjectBudgetStatus(), task.getTooltipTextForProjectBudgetStatus());
    }

    private boolean canChangeStartDate() {
        return disabilityConfiguration.isMovingTasksEnabled() && task.canBeExplicitlyMoved();
    }

    private boolean canChangeEndDate() {
        return disabilityConfiguration.isResizingTasksEnabled() && task.canBeExplicitlyResized();
    }

    private boolean canRenameTask() {
        return disabilityConfiguration.isRenamingTasksEnabled();
    }

    private String asString(Date date) {
        return dateFormat.format(date);
    }

    public Textbox getStartDateTextBox() {
        return startDateTextBox;
    }

    public void setStartDateTextBox(Textbox startDateTextBox) {
        this.startDateTextBox = startDateTextBox;
    }

    public Textbox getEndDateTextBox() {
        return endDateTextBox;
    }

    public void setEndDateTextBox(Textbox endDateTextBox) {
        this.endDateTextBox = endDateTextBox;
    }

    private void setHoursStatus(ProjectStatusEnum status, String tooltipText) {
        hoursStatusDiv.setSclass(getProjectStatusSclass(status));
        hoursStatusDiv.setTooltiptext(tooltipText);
        onProjectStatusClick(hoursStatusDiv);
    }

    private void setBudgetStatus(ProjectStatusEnum status, String tooltipText) {
        budgetStatusDiv.setSclass(getProjectStatusSclass(status));
        budgetStatusDiv.setTooltiptext(tooltipText);
        onProjectStatusClick(budgetStatusDiv);
    }

    private String getProjectStatusSclass(ProjectStatusEnum status) {
        String cssClass;

        switch (status) {
            case MARGIN_EXCEEDED:
                cssClass = "status-red";
                break;

            case WITHIN_MARGIN:
                cssClass = "status-orange";
                break;

            case AS_PLANNED:

            default:
                cssClass = "status-green";
        }

        return cssClass;
    }

    private void onProjectStatusClick(Component statucComp) {
        if ( !disabilityConfiguration.isTreeEditable() ) {
            statucComp.addEventListener(
                    Events.ON_CLICK,
                    arg0 -> Executions.getCurrent().sendRedirect("/planner/index.zul;order=" + task.getProjectCode()));
        }
    }
}

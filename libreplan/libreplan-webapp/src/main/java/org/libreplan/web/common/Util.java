/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
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

package org.libreplan.web.common;

import static org.libreplan.web.I18nHelper._;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.libreplan.business.common.BaseEntity;
import org.libreplan.business.common.Configuration;
import org.libreplan.business.common.Registry;
import org.springframework.web.context.ContextLoaderListener;
import org.zkoss.bind.DefaultBinder;
import org.zkoss.ganttz.util.ComponentsFinder;
import org.zkoss.image.AImage;
import org.zkoss.image.Image;
import org.zkoss.util.Locales;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zkplus.databind.AnnotateDataBinder;
import org.zkoss.zkplus.databind.DataBinder;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Column;

/**
 * Utilities class.
 * <br />
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 * @author Vova Perebykivskyi <vova@libreplan-enterprise.com>
 */
public class Util {

    private static final Log LOG = LogFactory.getLog(Util.class);

    /**
     * Special chars from {@link DecimalFormat} class.
     */
    private static final String[] DECIMAL_FORMAT_SPECIAL_CHARS = { "0", ",", ".", "\u2030", "%", "#", ";", "-" };

    private static final String RELOADED_COMPONENTS_ATTR = Util.class.getName() + ":" + "reloaded";

    private static final ThreadLocal<Boolean> ignoreCreateBindings = new ThreadLocal<Boolean>() {
        protected Boolean initialValue() {
            return false;
        }
    };

    /**
     * Static object that contains logo image.
     */
    public static Image logo;

    private Util() {
    }

    /**
     * Forces to reload the bindings of the provided components if there is an associated {@link DefaultBinder}.
     *
     * @param toReload
     *            the components to reload
     */
    public static void reloadBindings(Component... toReload) {
        reloadBindings(ReloadStrategy.FORCE, toReload);
    }

    public enum ReloadStrategy {
        /**
         * If the {@link DefaultBinder} exists the bindings are reloaded no matter what.
         */
        FORCE,

        /**
         * Once the bindings for a component have been manually loaded in one
         * request, subsequent calls for reload the bindings of the same
         * component or descendants using this strategy are ignored.
         */
        ONE_PER_REQUEST;

        public static boolean isForced(ReloadStrategy reloadStrategy) {
            return reloadStrategy == ReloadStrategy.FORCE;
        }
    }

    /**
     * Reload the bindings of the provided components if there is an associated
     * {@link DefaultBinder} and the {@link ReloadStrategy} allows it.
     *
     * @param toReload
     *            the components to reload
     */
    public static void reloadBindings(ReloadStrategy reloadStrategy, Component... toReload) {
        reloadBindings(ReloadStrategy.isForced(reloadStrategy), toReload);
    }

    private static void reloadBindings(boolean forceReload, Component... toReload) {
        for (Component reload : toReload) {

            // TODO resolve deprecated
            DataBinder binder = Util.getBinder(reload);

            if (binder != null && (forceReload || notReloadedInThisRequest(reload))) {
                binder.loadComponent(reload);
                markAsReloadedForThisRequest(reload);
            }
        }
    }

    private static boolean notReloadedInThisRequest(Component reload) {
        return !getReloadedComponents(reload).contains(reload);
    }

    private static Set<Component> getReloadedComponents(Component component) {
        Execution execution = component.getDesktop().getExecution();

        @SuppressWarnings("unchecked")
        Set<Component> result = (Set<Component>) execution.getAttribute(RELOADED_COMPONENTS_ATTR);

        if (result == null) {
            result = new HashSet<>();
            execution.setAttribute(RELOADED_COMPONENTS_ATTR, result);
        }

        return result;
    }

    private static void markAsReloadedForThisRequest(Component component) {
        Set<Component> reloadedComponents = getReloadedComponents(component);
        reloadedComponents.add(component);
        reloadedComponents.addAll(getAllDescendants(component));
    }

    private static void markAsNotReloadedForThisRequest(Component component) {
        Set<Component> reloadedComponents = getReloadedComponents(component);
        reloadedComponents.remove(component);
        reloadedComponents.removeAll(getAllDescendants(component));
    }

    @SuppressWarnings("unchecked")
    private static List<Component> getAllDescendants(Component component) {
        List<Component> result = new ArrayList<>();
        for (Component each : component.getChildren()) {
            result.add(each);
            result.addAll(getAllDescendants(each));
        }

        return result;
    }

    public static void saveBindings(Component... toReload) {
        for (Component reload : toReload) {
            /* TODO resolve deprecated */
            DataBinder binder = Util.getBinder(reload);

            if (binder != null) {
                binder.saveComponent(reload);
            }
        }
    }

    /** TODO resolve deprecated */
    public static DataBinder getBinder(Component component) {
        return (DataBinder) component.getAttribute("binder", true);
    }

    public static void executeIgnoringCreationOfBindings(Runnable action) {
        try {
            ignoreCreateBindings.set(true);
            action.run();
        } finally {
            ignoreCreateBindings.set(false);
        }
    }

    public static void createBindingsFor(Component result) {
        if (ignoreCreateBindings.get()) {
            return;
        }

        /* TODO resolve deprecated */
        AnnotateDataBinder binder = new AnnotateDataBinder(result, true);

        /*
         * Before it was:
         * setAttribute("binder", binder, true)
         * And it is not correct. Because API changed ( even more before it was setVariable("binder", binder, true) ).
         * Boolean value for setAttribute() means recursive actions, but in setVariable() it was not so.
         * And after, it still was calling method setAttribute() with (attr1, attr2, !booleanValue).
         */
        result.setAttribute("binder", binder, false);

        markAsNotReloadedForThisRequest(result);
    }

    /**
     * Generic interface to represent a class with a typical get method.
     *
     * @author Manuel Rego Casasnovas <mrego@igalia.com>
     * @param <T>
     *           The type of the variable to be returned.
     */
    @FunctionalInterface
    public interface Getter<T> {
        /**
         * Typical get method that returns a variable.
         * @return A variable of type <T>.
         */
        T get();
    }

    /**
     * Generic interface to represent a class with a typical set method.
     *
     * @author Manuel Rego Casasnovas <mrego@igalia.com>
     * @param <T>
     *            The type of the variable to be set.
     */
    @FunctionalInterface
    public interface Setter<T> {
        /**
         * Typical set method to store a variable.
         * @param value
         *            A variable of type <T> to be set.
         */
        void set(T value);
    }

    /**
     * Binds a {@link Textbox} with a {@link Getter}. The {@link Getter} will be
     * used to get the value that is going to be showed in the {@link Textbox}.
     *
     * @param textBox
     *            The {@link Textbox} to be bound
     * @param getter
     *            The {@link Getter} interface that will implement a get method.
     * @return The {@link Textbox} bound
     */
    public static Textbox bind(Textbox textBox, Getter<String> getter) {
        textBox.setValue(getter.get());
        textBox.setDisabled(true);
        return textBox;
    }

    /**
     * Binds a {@link Textbox} with a {@link Getter}. The {@link Getter} will be
     * used to get the value that is going to be showed in the {@link Textbox}.
     * The {@link Setter} will be used to store the value inserted by the user in the {@link Textbox}.
     *
     * @param textBox
     *            The {@link Textbox} to be bound
     * @param getter
     *            The {@link Getter} interface that will implement a get method.
     * @param setter
     *            The {@link Setter} interface that will implement a set method.
     * @return The {@link Textbox} bound
     */
    public static Textbox bind(final Textbox textBox, final Getter<String> getter, final Setter<String> setter) {
        textBox.setValue(getter.get());
        textBox.addEventListener(Events.ON_CHANGE, event -> {
            InputEvent newInput = (InputEvent) event;
            String value = newInput.getValue();
            setter.set(value);
            textBox.setValue(getter.get());
        });

        return textBox;
    }

    /**
     * Binds a {@link Textbox} with a {@link Getter}. The {@link Getter} will be
     * used to get the value that is going to be showed in the {@link Textbox}.
     *
     * @param comboBox
     *            The {@link Combobox} to be bound
     * @param getter
     *            The {@link Getter} interface that will implement a get method.
     * @return The {@link Combobox} bound
     */
    public static Combobox bind(Combobox comboBox, Getter<Comboitem> getter) {
        comboBox.setSelectedItem(getter.get());
        comboBox.setDisabled(true);
        return comboBox;
    }

    /**
     * Binds a {@link Textbox} with a {@link Getter}. The {@link Getter} will be
     * used to get the value that is going to be showed in the {@link Textbox}.
     * The {@link Setter} will be used to store the value inserted by the user in the {@link Textbox}.
     *
     * @param comboBox
     *            The {@link Combobox} to be bound
     * @param getter
     *            The {@link Getter} interface that will implement a get method.
     * @param setter
     *            The {@link Setter} interface that will implement a set method.
     * @return The {@link Combobox} bound
     */
    public static Combobox bind(final Combobox comboBox,
                                final Getter<Comboitem> getter,
                                final Setter<Comboitem> setter) {

        comboBox.setSelectedItem(getter.get());

        comboBox.addEventListener("onSelect",  event -> {
            setter.set(comboBox.getSelectedItem());
            comboBox.setSelectedItem(getter.get());
        });

        return comboBox;
    }

    /**
     * Binds a {@link Intbox} with a {@link Getter}. The {@link Getter} will be
     * used to get the value that is going to be showed in the {@link Intbox}
     *
     * @param intBox
     *            The {@link Intbox} to be bound
     * @param getter
     *            The {@link Getter} interface that will implement a get method.
     * @return The {@link Intbox} bound
     */
    public static Intbox bind(Intbox intBox, Getter<Integer> getter) {
        intBox.setValue(getter.get());
        intBox.setDisabled(true);

        return intBox;
    }

    /**
     * Binds a {@link Intbox} with a {@link Getter}.
     * The {@link Getter} will be used to get the value that is going to be showed in the {@link Intbox}.
     * The {@link Setter} will be used to store the value inserted by the user in the {@link Intbox}.
     *
     * @param intBox
     *            The {@link Intbox} to be bound
     * @param getter
     *            The {@link Getter} interface that will implement a get method.
     * @param setter
     *            The {@link Setter} interface that will implement a set method.
     * @return The {@link Intbox} bound
     */
    public static Intbox bind(final Intbox intBox, final Getter<Integer> getter, final Setter<Integer> setter) {
        intBox.setValue(getter.get());

        intBox.addEventListener(Events.ON_CHANGE, event -> {
            InputEvent newInput = (InputEvent) event;
            String value = newInput.getValue().trim();

            if (value.isEmpty()) {
                value = "0";
            }

            setter.set(Integer.valueOf(value));
            intBox.setValue(getter.get());
        });

        return intBox;
    }

    /**
     * Binds a {@link Datebox} with a {@link Getter}.
     * The {@link Getter} will be used to get the value that is going to be showed in the {@link Datebox}.
     *
     * @param dateBox
     *            The {@link Datebox} to be bound
     * @param getter
     *            The {@link Getter} interface that will implement a get method.
     * @return The {@link Datebox} bound
     */
    public static Datebox bind(final Datebox dateBox, final Getter<Date> getter) {
        dateBox.setValue(getter.get());
        dateBox.setDisabled(true);

        return dateBox;
    }

    /**
     * Binds a {@link Datebox} with a {@link Getter}.
     * The {@link Getter} will be used to get the value that is going to be showed in the {@link Datebox}.
     * The {@link Setter} will be used to store the value inserted by the user in the {@link Datebox}.
     *
     * @param dateBox
     *            The {@link Datebox} to be bound
     * @param getter
     *            The {@link Getter} interface that will implement a get method.
     * @param setter
     *            The {@link Setter} interface that will implement a set method.
     * @return The {@link Datebox} bound
     */
    public static Datebox bind(final Datebox dateBox, final Getter<Date> getter, final Setter<Date> setter) {
        dateBox.setValue(getter.get());

        dateBox.addEventListener(Events.ON_CHANGE, event -> {
            setter.set(dateBox.getValue());
            dateBox.setValue(getter.get());
        });

        return dateBox;
    }

    /**
     * Binds a {@link Timebox} with a {@link Getter}.
     * The {@link Getter} will be used to get the value that is going to be showed in the {@link Timebox}.
     *
     * @param timeBox
     *            The {@link Timebox} to be bound
     * @param getter
     *            The {@link Getter} interface that will implement a get method.
     * @return The {@link Timebox} bound
     */
    public static Timebox bind(final Timebox timeBox, final Getter<Date> getter) {
        timeBox.setValue(getter.get());
        timeBox.setDisabled(true);

        return timeBox;
    }

    /**
     * Binds a {@link Timebox} with a {@link Getter}.
     * The {@link Getter} will be used to get the value that is going to be showed in the {@link Timebox}.
     * The {@link Setter} will be used to store the value inserted by the user in the {@link Timebox}.
     *
     * @param timeBox
     *            The {@link Timebox} to be bound
     * @param getter
     *            The {@link Getter} interface that will implement a get method.
     * @param setter
     *            The {@link Setter} interface that will implement a set method.
     * @return The {@link Timebox} bound
     */
    public static Timebox bind(final Timebox timeBox, final Getter<Date> getter, final Setter<Date> setter) {
        timeBox.setValue(getter.get());

        timeBox.addEventListener(Events.ON_CHANGE, event -> {
            setter.set(timeBox.getValue());
            timeBox.setValue(getter.get());
        });

        return timeBox;
    }

    /**
     * Binds a {@link Decimalbox} with a {@link Getter}.
     * The {@link Getter} will be used to get the value that is going to be showed in the {@link Decimalbox}.
     *
     * @param decimalBox
     *            The {@link Decimalbox} to be bound
     * @param getter
     *            The {@link Getter} interface that will implement a get method.
     * @return The {@link Decimalbox} bound
     */
    public static Decimalbox bind(final Decimalbox decimalBox, final Getter<BigDecimal> getter) {
        decimalBox.setValue(getter.get());
        decimalBox.setDisabled(true);

        return decimalBox;
    }

    /**
     * Binds a {@link Decimalbox} with a {@link Getter}.
     * The {@link Getter} will be used to get the value that is going to be showed in the {@link Decimalbox}.
     * The {@link Setter} will be used to store the value inserted by the user in the {@link Decimalbox}.
     *
     * @param decimalBox
     *            The {@link Decimalbox} to be bound
     * @param getter
     *            The {@link Getter} interface that will implement a get method.
     * @param setter
     *            The {@link Setter} interface that will implement a set method.
     * @return The {@link Decimalbox} bound
     */
    public static Decimalbox bind(final Decimalbox decimalBox,
                                  final Getter<BigDecimal> getter,
                                  final Setter<BigDecimal> setter) {

        decimalBox.setValue(getter.get());

        decimalBox.addEventListener(Events.ON_CHANGE, event -> {
            setter.set(decimalBox.getValue());
            decimalBox.setValue(getter.get());
        });

        return decimalBox;
    }

    /**
     * Binds a {@link Checkbox} with a {@link Getter}.
     * The {@link Getter} will be used to get the value that is going to be showed in the {@link Checkbox}.
     *
     * @param decimalBox
     *            The {@link Checkbox} to be bound
     * @param getter
     *            The {@link Getter} interface that will implement a get method.
     * @return The {@link Checkbox} bound
     */
    public static Checkbox bind(final Checkbox checkBox, final Getter<Boolean> getter) {
        checkBox.setChecked(getter.get());
        checkBox.setDisabled(true);
        return checkBox;
    }

    /**
     * Binds a {@link Checkbox} with a {@link Getter}.
     * The {@link Getter} will be used to get the value that is going to be showed in the {@link Checkbox}.
     * The {@link Setter} will be used to store the value inserted by the user in the {@link Checkbox}.
     *
     * @param checkBox
     * @param getter
     *            The {@link Getter} interface that will implement a get method.
     * @param setter
     *            The {@link Setter} interface that will implement a set method.
     * @return The {@link Checkbox} bound
     */
    public static Checkbox bind(final Checkbox checkBox, final Getter<Boolean> getter, final Setter<Boolean> setter) {
        checkBox.setChecked(getter.get());

        checkBox.addEventListener(Events.ON_CHECK, event -> {
            setter.set(checkBox.isChecked());
            checkBox.setChecked(getter.get());
        });
        return checkBox;
    }

    /**
     * Binds a {@link Checkbox} with a {@link Getter}.
     * The {@link Getter} will be used to get the value that is going to be showed in the {@link Checkbox}.
     *
     * @param radio
     *            The {@link Radio} to be bound
     * @param getter
     *            The {@link Getter} interface that will implement a get method.
     * @return The {@link Radio} bound
     */
    public static Radio bind(final Radio radio, final Getter<Boolean> getter) {
        radio.setSelected(getter.get());
        radio.setDisabled(true);
        return radio;
    }

    /**
     * Binds a {@link Radio} with a {@link Getter}.
     * The {@link Getter} will be used to get the value that is going to be showed in the {@link Radio}.
     * The {@link Setter} will be used to store the value inserted by the user in the {@link Radio}.
     *
     * @param radio
     *            The {@link Radio} to be bound
     * @param getter
     *            he {@link Getter} interface that will implement a get method.
     * @param setter
     *            The {@link Setter} interface that will implement a set method.
     * @return The {@link Radio} bound
     */
    public static Radio bind(final Radio radio, final Getter<Boolean> getter, final Setter<Boolean> setter) {
        radio.setSelected(getter.get());

        radio.addEventListener(Events.ON_CHECK, event -> {
            setter.set(radio.isSelected());
            radio.setChecked(getter.get());
        });

        return radio;
    }

    /**
     * Binds a {@link Bandbox} with a {@link Getter}.
     * The {@link Getter} will be used to get the value that is going to be showed in the {@link Bandbox}.
     *
     * @param bandBox
     *            The {@link Bandbox} to be bound
     * @param getter
     *            The {@link Getter} interface that will implement a get method.
     * @return The {@link Bandbox} bound
     */
    public static Bandbox bind(Bandbox bandBox, Getter<String> getter) {
        bandBox.setValue(getter.get());
        bandBox.setDisabled(true);
        return bandBox;
    }

    /**
     * Binds a {@link Bandbox} with a {@link Getter}.
     * The {@link Getter} will be used to get the value that is going to be showed in the {@link Bandbox}.
     * The {@link Setter} will be used to store the value inserted by the user in the {@link Bandbox}.
     *
     * @param bandBox
     *            The {@link Bandbox} to be bound
     * @param getter
     *            The {@link Getter} interface that will implement a get method.
     * @param setter
     *            The {@link Setter} interface that will implement a set method.
     * @return The {@link Bandbox} bound
     */
    public static Bandbox bind(final Bandbox bandBox, final Getter<String> getter, final Setter<String> setter) {
        bandBox.setValue(getter.get());

        bandBox.addEventListener(Events.ON_CHANGE, event -> {
            InputEvent newInput = (InputEvent) event;
            String value = newInput.getValue();
            setter.set(value);
            bandBox.setValue(getter.get());
        });

        return bandBox;
    }

    /**
     * Creates an edit button with class and icon already set.
     *
     * @param eventListener
     *            A event listener for {@link Events#ON_CLICK}
     * @return An edit {@link Button}
     */
    public static Button createEditButton(EventListener eventListener) {
        Button result = new Button();
        result.setTooltiptext(_("Edit"));
        result.setSclass("icono");
        result.setImage("/common/img/ico_editar1.png");
        result.setHoverImage("/common/img/ico_editar.png");

        result.addEventListener(Events.ON_CLICK, eventListener);

        return result;
    }

    /**
     * Creates a remove button with class and icon already set.
     *
     * @param eventListener
     *            A event listener for {@link Events#ON_CLICK}
     * @return A remove {@link Button}
     */
    public static Button createRemoveButton(EventListener eventListener) {
        Button result = new Button();
        result.setTooltiptext(_("Remove"));
        result.setSclass("icono");
        result.setImage("/common/img/ico_borrar1.png");
        result.setHoverImage("/common/img/ico_borrar.png");

        result.addEventListener(Events.ON_CLICK, eventListener);

        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Component> T findComponentAt(Component container, String idOfComponentToBeFound) {
        return (T) container.getFellow(idOfComponentToBeFound);
    }

    public interface ICreation<T extends Component> {
        T createAt(Component parent);
    }

    public static <T extends Component> T findOrCreate(Component container,
                                                       Class<T> klassOfComponentToFind,
                                                       ICreation<T> ifNotFound) {
        @SuppressWarnings("unchecked")
        List<T> existent = ComponentsFinder.findComponentsOfType(klassOfComponentToFind, container.getChildren());
        if (!existent.isEmpty()) {
            return existent.get(0);
        }

        return ifNotFound.createAt(container);
    }

    /**
     * It removes all listeners registered for eventName and adds the new listener.
     * It's ensured that the only listener left in the component for events of name eventName is uniqueListener.
     *
     * @param component
     * @param eventName
     * @param uniqueListener
     */
    public static void ensureUniqueListener(Component component, String eventName, EventListener uniqueListener) {
        ensureUniqueListeners(component, eventName, uniqueListener);
    }

    /**
     * It removes all listeners registered for eventName and adds the new listeners.
     * It's ensured that the only listeners left in the component for events of name eventName is uniqueListeners.
     *
     * @param component
     * @param eventName
     * @param uniqueListeners
     *            new listeners to add
     */
    public static void ensureUniqueListeners(Component component, String eventName, EventListener... uniqueListeners) {
        // TODO Replace deprecated method
        Iterator<?> listenerIterator = component.getListenerIterator(eventName);

        while (listenerIterator.hasNext()) {
            listenerIterator.next();
            listenerIterator.remove();
        }
        for (EventListener each : uniqueListeners) {
            component.addEventListener(eventName, each);
        }
    }

    public static void setSort(Column column, String sortSpec) {
        try {
            column.setSort(sortSpec);
        } catch (Exception e) {
            LOG.error("failed to set sort property for: " + column + " with: " + sortSpec, e);
        }
    }

    /**
     * Gets currency symbol from {@link Configuration} object.
     *
     * @return Currency symbol configured in the application
     */
    public static String getCurrencySymbol() {
        return Registry.getTransactionService().runOnReadOnlyTransaction(
                () -> Registry.getConfigurationDAO().getConfiguration().getCurrencySymbol());
    }

    /**
     * Returns the value using the money format, that means, 2 figures for the
     * decimal part and concatenating the currency symbol from {@link Configuration} object.
     */
    public static String addCurrencySymbol(BigDecimal value) {
        BigDecimal valueToReturn = value == null ? BigDecimal.ZERO : value;
        DecimalFormat decimalFormat = (DecimalFormat) DecimalFormat.getInstance();
        decimalFormat.applyPattern(getMoneyFormat());

        return decimalFormat.format(valueToReturn);
    }

    /**
     * Gets money format for a {@link Decimalbox} using 2 figures for the
     * decimal part and concatenating the currency symbol.
     *
     * @return Format for a {@link Decimalbox} <code>###.##</code> plus currency symbol
     */
    public static String getMoneyFormat() {
        return "###.## " + escapeDecimalFormatSpecialChars(getCurrencySymbol());
    }

    /**
     * Escapes special chars used in {@link DecimalFormat} to define the number
     * format that appear in the <code>currencySymbol</code>.
     */
    private static String escapeDecimalFormatSpecialChars(String currencySymbol) {
        String stringToReturn = currencySymbol;

        for (String specialChar : DECIMAL_FORMAT_SPECIAL_CHARS) {
            stringToReturn = stringToReturn.replace(specialChar, "'" + specialChar + "'");
        }

        return stringToReturn;
    }

    /**
     * Appends the <code>text</code> as a {@link Label} into the specified {@link Row}.
     */
    public static void appendLabel(Row row, String text) {
        row.appendChild(new Label(text));
    }

    /**
     * Appends a edit button and a remove button to the {@link Row} inside a
     * {@link Hbox} and adds the <code>ON_CLICK</code> event over the
     * {@link Row} for the edit operation.<br />
     *
     * The edit button will call the <code>editButtonListener</code> when
     * clicked and the remove button the <code>removeButtonListener</code>.
     * <br />
     *
     * If <code>removeButtonListener</code> is null, it only adds the edit
     * button and the <code>ON_CLICK</code> event.
     *
     * @return An array of 1 or 2 positions (depending if
     *         <code>removeButtonListener</code> param is or not
     *         <code>null</code>) with the edit and remove buttons. As maybe you
     *         need to disable any of them depending on different situations.
     */
    public static Button[] appendOperationsAndOnClickEvent(Row row,
                                                           EventListener editButtonListener,
                                                           EventListener removeButtonListener) {

        Button[] buttons = new Button[removeButtonListener != null ? 2 : 1];

        Hbox hbox = new Hbox();
        buttons[0] = Util.createEditButton(editButtonListener);
        hbox.appendChild(buttons[0]);

        if (removeButtonListener != null) {
            buttons[1] = Util.createRemoveButton(removeButtonListener);
            hbox.appendChild(buttons[1]);
        }
        row.appendChild(hbox);

        row.addEventListener(Events.ON_CLICK, editButtonListener);

        return buttons;
    }

    /**
     * Checks if the <code>entity</code> is contained in the provided <code>list</code>.
     */
    public static boolean contains(List<? extends BaseEntity> list, BaseEntity entity) {
        for (BaseEntity each : list) {
            if (each.getId() != null && entity.getId() != null && each.getId().equals(entity.getId())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the {@link HttpServletResponse} from the current {@link Execution}
     * and uses the method {@link HttpServletResponse#sendError(int)} with the
     * code {@link HttpServletResponse#SC_FORBIDDEN}.
     */
    public static void sendForbiddenStatusCodeInHttpServletResponse() {
        try {
            HttpServletResponse response = (HttpServletResponse) Executions.getCurrent().getNativeResponse();
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Format specific <code>date</code> using the {@link DateFormat#DEFAULT} format and showing only date without time.
     */
    public static String formatDate(Date date) {
        return date == null ? "" : DateFormat.getDateInstance(DateFormat.DEFAULT, Locales.getCurrent()).format(date);
    }

    /**
     * Format specific <code>date</code> using the {@link DateFormat#DEFAULT} format and showing both date and time.
     */
    public static String formatDateTime(Date dateTime) {
        return dateTime == null
                ? ""
                : DateFormat
                    .getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locales.getCurrent())
                    .format(dateTime);
    }

    /**
     * Format specific <code>date</code> using the {@link DateFormat#DEFAULT} format and showing only date without time.
     */
    public static String formatDate(DateTime dateTime) {
        return dateTime == null ? "" : formatDate(dateTime.toDate());
    }

    /**
     * Format specific <code>date</code> using the {@link DateFormat#DEFAULT} format and showing only date without time.
     */
    public static String formatDate(LocalDate date) {
        return date == null ? "" : formatDate(date.toDateTimeAtStartOfDay());
    }

    /**
     * Format specific <code>time</code> using the {@link DateFormat#SHORT} format and showing only the time.
     */
    public static String formatTime(Date time) {
        return time == null ? "" : DateFormat.getTimeInstance(DateFormat.SHORT, Locales.getCurrent()).format(time);
    }

    /**
     * Format specific <code>time</code> using the {@link DateFormat#SHORT} format and showing only the time.
     */
    public static String formatTime(LocalTime time) {
        return time == null ? "" : formatTime(time.toDateTimeToday().toDate());
    }

    /**
     * Setter of {@link Util#logo}.
     * Will trigger after uploading new image.
     *
     * @param name
     */
    static void setLogoFromTarget(String name) {
        try {
            logo = new AImage(ContextLoaderListener
                    .getCurrentWebApplicationContext()
                    .getResource(name)
                    .getFile()
                    .getPath());

        } catch (IOException ignored) {
        }
    }

    /**
     * Setter of {@link Util#logo}.
     * But it will trigger only if {@link Util#logo} is null.
     * So it is just kind of attempt to find logo with known data.
     */
    static void findLogo() {
        String name = Registry
                .getConfigurationDAO()
                .getConfigurationWithReadOnlyTransaction()
                .getCompanyLogoURL();

        try {
            if ( !name.isEmpty() ) {
                logo = new AImage(ContextLoaderListener
                        .getCurrentWebApplicationContext()
                        .getResource(name)
                        .getFile()
                        .getPath());
            }
        } catch (IOException ignored) {
        }
    }

}

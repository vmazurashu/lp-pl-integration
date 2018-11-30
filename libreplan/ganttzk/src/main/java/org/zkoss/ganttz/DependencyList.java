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

import static org.zkoss.ganttz.i18n.I18nHelper._;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import org.zkoss.ganttz.data.Dependency;
import org.zkoss.ganttz.data.DependencyType;
import org.zkoss.ganttz.data.Task;
import org.zkoss.ganttz.timetracker.TimeTracker;
import org.zkoss.ganttz.timetracker.TimeTrackerComponent;
import org.zkoss.ganttz.timetracker.zoom.IZoomLevelChangedListener;
import org.zkoss.ganttz.timetracker.zoom.ZoomLevel;
import org.zkoss.ganttz.util.ComponentsFinder;
import org.zkoss.ganttz.util.MenuBuilder;
import org.zkoss.ganttz.util.MenuBuilder.ItemAction;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.ext.AfterCompose;
import org.zkoss.zul.Menupopup;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.impl.XulElement;

/**
 * @author Francisco Javier Moran Rúa <jmoran@igalia.com>
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 */
public class DependencyList extends XulElement implements AfterCompose {

    private final class ChangeTypeAction implements ItemAction<DependencyComponent> {

        private final DependencyType type;

        private ChangeTypeAction(DependencyType type) {
            this.type = type;
        }

        @Override
        public void onEvent(final DependencyComponent chosen, Event event) {
            boolean canBeAdded = context.changeType(chosen.getDependency(), type);

            if ( !canBeAdded ) {
                warnUser(_("The specified dependency is not allowed"));
            }
        }

        private void warnUser(String message) {
            Messagebox.show(message, null, Messagebox.OK, Messagebox.EXCLAMATION, 0, null);
        }
    }

    private final class DependencyVisibilityToggler implements PropertyChangeListener {

        private final Task source;

        private final Task destination;

        private final DependencyComponent dependencyComponent;

        private DependencyVisibilityToggler(Task source, Task destination, DependencyComponent dependencyComponent) {
            this.source = source;
            this.destination = destination;
            this.dependencyComponent = dependencyComponent;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if ( !"visible".equals(evt.getPropertyName()) ) {
                return;
            }

            if ( dependencyMustBeVisible() != isDependencyNowVisible() ) {
                toggleDependencyExistence(dependencyMustBeVisible());
            }
        }

        void toggleDependencyExistence(boolean visible) {
            if ( visible ) {
                appendChild(dependencyComponent);
                dependencyComponent.afterCompose();
                addContextMenu(dependencyComponent);
            } else {
                removeChild(dependencyComponent);
            }
        }

        boolean isDependencyNowVisible() {
            return dependencyComponent.getParent() != null;
        }

        boolean dependencyMustBeVisible() {
            return source.isVisible() && destination.isVisible();
        }
    }

    private transient IZoomLevelChangedListener listener;

    private final FunctionalityExposedForExtensions<?> context;

    private Menupopup contextMenu;

    private Menupopup limitingContextMenu;

    public DependencyList(FunctionalityExposedForExtensions<?> context) {
        this.context = context;
    }

    private List<DependencyComponent> getDependencyComponents() {
        List<Component> children = getChildren();
        return ComponentsFinder.findComponentsOfType(DependencyComponent.class, children);
    }

    void addDependencyComponent(final DependencyComponent dependencyComponent) {
        TaskComponent source = dependencyComponent.getSource();
        TaskComponent destination = dependencyComponent.getDestination();

        DependencyVisibilityToggler visibilityToggler =
                new DependencyVisibilityToggler(source.getTask(), destination.getTask(), dependencyComponent);

        source.getTask().addVisibilityPropertiesChangeListener(visibilityToggler);
        destination.getTask().addVisibilityPropertiesChangeListener(visibilityToggler);
        dependencyComponent.setVisibilityChangeListener(visibilityToggler);

        boolean dependencyMustBeVisible = visibilityToggler.dependencyMustBeVisible();
        visibilityToggler.toggleDependencyExistence(dependencyMustBeVisible);

        if ( dependencyMustBeVisible ) {
            dependencyComponent.redrawDependency();
        }
    }

    private void addContextMenu(DependencyComponent dependencyComponent) {
        Menupopup contextMenu = dependencyComponent.hasLimitingTasks() ? getLimitingContextMenu() : getContextMenu();

        dependencyComponent.setContext(contextMenu);
    }

    private GanttPanel getGanttPanel() {
        return (GanttPanel) getParent();
    }

    void setDependencyComponents(List<DependencyComponent> dependencyComponents) {
        for (DependencyComponent dependencyComponent : dependencyComponents) {
            addDependencyComponent(dependencyComponent);
        }
    }

    @Override
    public void afterCompose() {
        if ( listener == null ) {

            /* Do not replace it with lambda */
            listener = new IZoomLevelChangedListener() {
                @Override
                public void zoomLevelChanged(ZoomLevel detailLevel) {
                    if ( !isInPage() ) {
                        return;
                    }
                    for (DependencyComponent dependencyComponent : getDependencyComponents()) {
                        dependencyComponent.zoomChanged();
                    }
                }
            };

            getTimeTracker().addZoomListener(listener);
        }

        addContextMenu();
    }

    private boolean isInPage() {
        return getParent() != null && getGanttPanel() != null && getGanttPanel().getParent() != null;
    }

    private TimeTracker getTimeTracker() {
        return getTimeTrackerComponent().getTimeTracker();
    }

    private void addContextMenu() {
        for (DependencyComponent dependencyComponent : getDependencyComponents()) {
            addContextMenu(dependencyComponent);
        }
    }

    private Menupopup getLimitingContextMenu() {
        if ( limitingContextMenu == null ) {

            MenuBuilder<DependencyComponent> contextMenuBuilder =
                    MenuBuilder.on(getPage(), getDependencyComponents()).item(
                            _("Erase"),
                            "/common/img/ico_borrar.png",
                            (chosen, event) -> context.removeDependency(chosen.getDependency()));

            limitingContextMenu = contextMenuBuilder.create();
        }

        return limitingContextMenu;
    }

    private Menupopup getContextMenu() {
        if ( contextMenu == null ) {

            MenuBuilder<DependencyComponent> contextMenuBuilder =
                    MenuBuilder.on(getPage(), getDependencyComponents()).item(
                            _("Erase"),
                            "/common/img/ico_borrar.png",
                            ((chosen, event) -> context.removeDependency(chosen.getDependency())));

            contextMenuBuilder.item(_("Set End-Start"), null, new ChangeTypeAction(DependencyType.END_START));

            contextMenuBuilder.item(_("Set Start-Start"), null, new ChangeTypeAction(DependencyType.START_START));

            contextMenuBuilder.item(_("Set End-End"), null, new ChangeTypeAction(DependencyType.END_END));

            contextMenu = contextMenuBuilder.create();

        }

        return contextMenu;
    }

    private TimeTrackerComponent getTimeTrackerComponent() {
        return getGanttPanel().getTimeTrackerComponent();
    }

    void redrawDependencies() {
        redrawDependencyComponents(getDependencyComponents());
    }

    private void redrawDependencyComponents(List<DependencyComponent> dependencyComponents) {
        for (DependencyComponent dependencyComponent : dependencyComponents) {
            dependencyComponent.redrawDependency();
        }
    }

    void taskRemoved(Task task) {
        for (DependencyComponent dependencyComponent : DependencyList.this.getDependencyComponents()) {
            if ( dependencyComponent.contains(task) ) {
                removeDependencyComponent(dependencyComponent);
            }
        }
    }

    public void remove(Dependency dependency) {
        for (DependencyComponent dependencyComponent : DependencyList.this.getDependencyComponents()) {
            if ( dependencyComponent.hasSameSourceAndDestination(dependency) ) {
                removeDependencyComponent(dependencyComponent);
            }
        }
    }

    private void removeDependencyComponent(DependencyComponent dependencyComponent) {
        // Remove the visibility listener attached to the tasks
        TaskComponent source = dependencyComponent.getSource();
        TaskComponent destination = dependencyComponent.getDestination();
        PropertyChangeListener listener = dependencyComponent.getVisibilityChangeListener();

        source.getTask().removeVisibilityPropertiesChangeListener(listener);
        destination.getTask().removeVisibilityPropertiesChangeListener(listener);

        // Remove other change listeners
        dependencyComponent.removeChangeListeners();

        // Remove the dependency itself
        this.removeChild(dependencyComponent);
    }
}

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

package org.libreplan.web.planner.order;

import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.planner.entities.TaskElement;
import org.zkoss.ganttz.extensions.ICommand;

/**
 * Contract for {@link SaveCommandBuilder} <br />
 *
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 * @author Manuel Rego Casasnovas <rego@igalia.com>
 */
public interface ISaveCommand extends ICommand<TaskElement> {

    public interface IAfterSaveListener {
        void onAfterSave();
    }

    public void addListener(IAfterSaveListener listener);

    public void removeListener(IAfterSaveListener listener);

    public String getImage();

    public interface IBeforeSaveActions {
        public void doActions();
    }

    public interface IAfterSaveActions {
        public void doActions();
    }

    void save(IBeforeSaveActions beforeSaveActions);

    void save(IBeforeSaveActions beforeSaveActions,
            IAfterSaveActions afterSaveActions) throws ValidationException;

    void setDisabled(boolean disabled);

    @Override
    boolean isDisabled();

}

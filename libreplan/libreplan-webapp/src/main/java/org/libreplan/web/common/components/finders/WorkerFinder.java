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

package org.libreplan.web.common.components.finders;

import java.util.Collections;
import java.util.List;

import org.libreplan.business.resources.daos.IWorkerDAO;
import org.libreplan.business.resources.entities.Worker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Diego Pino Garcia <dpino@igalia.com>
 *
 * Implements a {@link IFinder} class for providing {@link Worker}
 * elements
 *
 */
@Repository
public class WorkerFinder extends Finder implements IFinder {

    @Autowired
    private IWorkerDAO workerDAO;

    @Transactional(readOnly = true)
    public List<Worker> getAll() {
        List<Worker> result = workerDAO.getWorkers();
        Collections.sort(result);
        return result;
    }

    @Override
    public String _toString(Object value) {
        final Worker worker = (Worker) value;
        return (worker != null) ? worker.getName() + " - " + worker.getNif() : "";
    }

}

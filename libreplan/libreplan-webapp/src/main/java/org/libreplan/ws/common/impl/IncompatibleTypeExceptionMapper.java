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

package org.libreplan.ws.common.impl;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.libreplan.ws.common.api.IncompatibleTypeException;
import org.libreplan.ws.common.api.InternalErrorDTO;
import org.springframework.stereotype.Component;

/**
 * Exception mapper for {@link IncompatibleTypeException}.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@Provider
@Component("incompatibleTypeExceptionMapper")
public class IncompatibleTypeExceptionMapper implements
        ExceptionMapper<IncompatibleTypeException> {

    public Response toResponse(IncompatibleTypeException e) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                new InternalErrorDTO(e.getMessage(), Util.getStackTrace(e)))
                .type("application/xml").build();
    }

}

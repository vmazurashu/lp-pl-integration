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

package org.libreplan.ws.typeofworkhours.impl;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.libreplan.business.common.daos.IIntegrationEntityDAO;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.costcategories.daos.ITypeOfWorkHoursDAO;
import org.libreplan.business.costcategories.entities.TypeOfWorkHours;
import org.libreplan.ws.common.api.InstanceConstraintViolationsListDTO;
import org.libreplan.ws.common.impl.GenericRESTService;
import org.libreplan.ws.typeofworkhours.api.ITypeOfWorkHoursService;
import org.libreplan.ws.typeofworkhours.api.TypeOfWorkHoursDTO;
import org.libreplan.ws.typeofworkhours.api.TypeOfWorkHoursListDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * REST-based implementation of <code>ITypeOfWorkHoursService</code>.
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */

@Path("/typeofworkhours/")
@Produces("application/xml")
@Service("typeOfWorkHoursServiceREST")
public class TypeOfWorkHoursServiceREST extends
        GenericRESTService<TypeOfWorkHours, TypeOfWorkHoursDTO> implements
        ITypeOfWorkHoursService {

    @Autowired
    private ITypeOfWorkHoursDAO typeOfWorkHoursDAO;

    @Override
    @GET
    @Transactional(readOnly = true)
    public TypeOfWorkHoursListDTO getTypeOfWorkHours() {
        return new TypeOfWorkHoursListDTO(findAll());
    }

    @Override
    @POST
    @Consumes("application/xml")
    public InstanceConstraintViolationsListDTO addTypeOfWorkHours(
            TypeOfWorkHoursListDTO typeOfWorkHoursListDTO) {

        return save(typeOfWorkHoursListDTO.typeOfWorkHoursDTOs);

    }

    @Override
    protected TypeOfWorkHours toEntity(TypeOfWorkHoursDTO entityDTO) {
        return TypeOfWorkHoursConverter.toEntity(entityDTO);
    }

    @Override
    protected TypeOfWorkHoursDTO toDTO(TypeOfWorkHours entity) {
        return TypeOfWorkHoursConverter.toDTO(entity);
    }

    @Override
    protected IIntegrationEntityDAO<TypeOfWorkHours> getIntegrationEntityDAO() {
        return typeOfWorkHoursDAO;
    }

    @Override
    protected void updateEntity(TypeOfWorkHours entity,
            TypeOfWorkHoursDTO entityDTO)
            throws ValidationException {

        TypeOfWorkHoursConverter.updateTypeOfWorkHours(entity, entityDTO);

    }

    @Override
    @GET
    @Path("/{code}/")
    @Transactional(readOnly = true)
    public Response getTypeOfWorkHours(@PathParam("code") String code) {
        return getDTOByCode(code);
    }
}

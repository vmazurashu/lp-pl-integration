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

package org.libreplan.ws.materials.impl;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.libreplan.business.common.daos.IIntegrationEntityDAO;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.materials.daos.IMaterialCategoryDAO;
import org.libreplan.business.materials.entities.MaterialCategory;
import org.libreplan.ws.common.api.InstanceConstraintViolationsListDTO;
import org.libreplan.ws.common.impl.GenericRESTService;
import org.libreplan.ws.materials.api.IMaterialService;
import org.libreplan.ws.materials.api.MaterialCategoryDTO;
import org.libreplan.ws.materials.api.MaterialCategoryListDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * REST-based implementation of <code>IMaterialService</code>.
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
@Path("/materialcategories/")
@Produces("application/xml")
@Service("materialServiceREST")
public class MaterialServiceREST extends
        GenericRESTService<MaterialCategory, MaterialCategoryDTO> implements
        IMaterialService {

    @Autowired
    private IMaterialCategoryDAO materialCategoryDAO;

    @Override
    @GET
    @Transactional(readOnly = true)
    public MaterialCategoryListDTO getMaterials() {
        return new MaterialCategoryListDTO(findAll());
    }

    @Override
    @POST
    @Consumes("application/xml")
    public InstanceConstraintViolationsListDTO addMaterials(
            MaterialCategoryListDTO materialCategoryListDTO) {
        return save(materialCategoryListDTO.materialCategoryDTOs);
    }

    @Override
    protected MaterialCategory toEntity(MaterialCategoryDTO entityDTO) {
        return MaterialConverter.toEntity(entityDTO);
    }

    @Override
    protected MaterialCategoryDTO toDTO(MaterialCategory entity) {
        return MaterialConverter.toDTO(entity);
    }

    @Override
    protected IIntegrationEntityDAO<MaterialCategory> getIntegrationEntityDAO() {
        return materialCategoryDAO;
    }

    @Override
    protected void updateEntity(MaterialCategory entity,
            MaterialCategoryDTO entityDTO)
            throws ValidationException {

        MaterialConverter.updateMaterialCategory(entity, entityDTO);

    }

    @Override
    @GET
    @Path("/{code}/")
    @Transactional(readOnly = true)
    public Response getMaterial(@PathParam("code") String code) {
        return getDTOByCode(code);
    }

}

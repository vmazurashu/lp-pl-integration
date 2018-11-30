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

package org.libreplan.business.test.materials.daos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.libreplan.business.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_FILE;
import static org.libreplan.business.test.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_TEST_FILE;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.materials.daos.IMaterialCategoryDAO;
import org.libreplan.business.materials.entities.MaterialCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { BUSINESS_SPRING_CONFIG_FILE,
        BUSINESS_SPRING_CONFIG_TEST_FILE })
/**
 * Test for {@MaterialDAO}
 *
 * @author Jacobo Aragunde Perez <jaragunde@igalia.com>
 *
 */
public class MaterialCategoryDAOTest {

    @Autowired
    IMaterialCategoryDAO materialCategoryDAO;

    @Test
    @Transactional
    public void testInSpringContainer() {
        assertNotNull(materialCategoryDAO);
    }

    private MaterialCategory createValidMaterialCategory() {
        MaterialCategory materialCategory = MaterialCategory.create(UUID.randomUUID().toString());
        return materialCategory;
    }

    @Test
    @Transactional
    public void testSaveMaterialCategory() {
        MaterialCategory materialCategory = createValidMaterialCategory();
        materialCategoryDAO.save(materialCategory);
        assertTrue(materialCategory.getId() != null);
    }

    @Test
    @Transactional
    public void testRemoveMaterialCategory() throws InstanceNotFoundException {
        MaterialCategory materialCategory = createValidMaterialCategory();
        materialCategoryDAO.save(materialCategory);
        materialCategoryDAO.remove(materialCategory.getId());
        assertFalse(materialCategoryDAO.exists(materialCategory.getId()));
    }

    @Test
    @Transactional
    public void testListMaterialCategories() {
        int previous = materialCategoryDAO.list(MaterialCategory.class).size();
        MaterialCategory materialCategory = createValidMaterialCategory();
        materialCategoryDAO.save(materialCategory);
        List<MaterialCategory> list = materialCategoryDAO.list(MaterialCategory.class);
        assertEquals(previous + 1, list.size());
    }

    @Test
    @Transactional
    public void testListChildrenMaterialCategories() {
        MaterialCategory category = createValidMaterialCategory();
        MaterialCategory subcategory = createValidMaterialCategory();
        int previous = category.getSubcategories().size();
        category.addSubcategory(subcategory);
        materialCategoryDAO.save(category);
        Set<MaterialCategory> childrenList = category.getSubcategories();
        assertEquals(previous + 1, childrenList.size());
    }

    @Test
    @Transactional
    public void testRemoveChildrenMaterialCategories() {
        MaterialCategory category = createValidMaterialCategory();
        MaterialCategory subcategory = createValidMaterialCategory();
        category.addSubcategory(subcategory);
        materialCategoryDAO.save(category);
        int previous = category.getSubcategories().size();

        category.removeSubcategory(subcategory);
        materialCategoryDAO.save(category);
        Set<MaterialCategory> childrenList = category.getSubcategories();
        assertEquals(previous - 1, childrenList.size());
    }

    @Test
    @Transactional
    public void testSaveMaterialSubcategoryTopDown() {
        MaterialCategory category = createValidMaterialCategory();
        MaterialCategory subcategory = createValidMaterialCategory();
        category.addSubcategory(subcategory);
        //materialCategoryDAO.save(subcategory); //unnecessary due to cascade=all
        materialCategoryDAO.save(category);
        List<MaterialCategory> list = materialCategoryDAO.list(MaterialCategory.class);
        for(MaterialCategory listCategory:list) {
            if(listCategory.getId()==category.getId()) {
                assertEquals(1, listCategory.getSubcategories().size());
            }
            if(listCategory.getId()==subcategory.getId()) {
                assertNotNull(listCategory.getParent());
                assertEquals(category.getId(), listCategory.getParent().getId());
            }
        }
    }

}

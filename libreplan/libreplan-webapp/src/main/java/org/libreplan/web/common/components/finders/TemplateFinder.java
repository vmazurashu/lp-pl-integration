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

import java.util.List;

import org.libreplan.business.templates.entities.OrderElementTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;

/**
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 */
abstract class TemplateFinder<T extends OrderElementTemplate> extends BandboxFinder {

    private final Class<T> type;

    TemplateFinder(Class<T> type) {
        this.type = type;
    }

    @Override
    public boolean entryMatchesText(Object obj, String text) {
        return templateMatchesText(type.cast(obj), text);
    }

    @Override
    public String[] getHeaders() {
        return new String[] { "Name" };
    }

    @Override
    public ListitemRenderer getItemRenderer() {
        return new ListitemRenderer() {

            @Override
            public void render(Listitem item, Object data, int i) {
                T template = type.cast(data);
                item.setValue(data);

                Listcell nameCell = new Listcell();
                nameCell.setParent(item);
                nameCell.setLabel(template.getName());
                nameCell.setStyle("width:300px");
            }
        };
    }

    private boolean templateMatchesText(T template, String text) {
        String objectString = normalize(objectToString(template));

        return objectString.contains(normalize(text));
    }

    private String normalize(String text) {
        return text.trim().toLowerCase();
    }

    @Override
    public String objectToString(Object obj) {
        T template = type.cast(obj);

        return extractStringFor(template);
    }

    private String extractStringFor(T template) {
        return template.getName();
    }

    @Override
    @Transactional(readOnly = true)
    public List<? extends OrderElementTemplate> getAll() {
        return getTemplates();
    }

    protected abstract List<T> getTemplates();

}

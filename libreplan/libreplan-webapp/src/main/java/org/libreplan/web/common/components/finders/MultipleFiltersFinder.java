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

import org.apache.commons.lang3.StringUtils;
import org.libreplan.business.common.AdHocTransactionService;
import org.libreplan.business.common.IAdHocTransactionService;
import org.libreplan.business.hibernate.notification.ISnapshotRefresherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.libreplan.web.I18nHelper._;

abstract class MultipleFiltersFinder implements IMultipleFiltersFinder {

    @Autowired
    private IAdHocTransactionService adHocTransactionService;

    @Autowired
    private ISnapshotRefresherService snapshotRefresherService;

    private List<FilterPair> listMatching = new ArrayList<>();

    private final String headers[] = {};

    MultipleFiltersFinder() {

    }

    public void reset() {
        /* Do nothing */
    }

    public IAdHocTransactionService getAdHocTransactionService() {
        return adHocTransactionService;
    }

    public ISnapshotRefresherService getSnapshotRefresher() {
        return snapshotRefresherService;
    }

    @SuppressWarnings("unchecked")
    protected <T> Callable<T> onTransaction(Callable<T> callable) {
        return AdHocTransactionService.readOnlyProxy(getAdHocTransactionService(), Callable.class, callable);
    }

    public void setAdHocTransactionService(IAdHocTransactionService adHocTransactionService) {
        this.adHocTransactionService = adHocTransactionService;
    }

    List<FilterPair> getListMatching() {
        return listMatching;
    }

    public void setListMatching(List<FilterPair> listMatching) {
        this.listMatching = listMatching;
    }

    public ListitemRenderer getFilterPairRenderer() {
        return filterPairRenderer;
    }

    void addNoneFilter() {
        getListMatching().add(new FilterPair(FilterEnumNone.None, FilterEnumNone.None.toString(), null));
    }

    public String objectToString(Object obj) {
        FilterPair filterPair = (FilterPair) obj;
        return filterPair.getType() + "(" + filterPair.getPattern() + "); ";
    }

    public String getNewFilterText(String inputText) {
        String[] filtersText = inputText.split(";");
        return getLastText(filtersText);
    }

    private String getLastText(String[] texts) {
        Integer last = texts.length - 1;
        if (texts.length > 0) {
            return texts[last];
        } else {
            return "";
        }
    }

    public boolean isValidNewFilter(List filterValues, Object obj) {
        FilterPair filter = (FilterPair) obj;

        return !filter.getType().equals(FilterEnumNone.None);
    }

    public boolean isValidFormatText(List filterValues, String value) {
        if (filterValues.isEmpty()) {
            return true;
        }

        updateDeletedFilters(filterValues, value);
        value = StringUtils.deleteWhitespace(value);
        String[] values = value.split(";");
        if (values.length != filterValues.size()) {
            return false;
        }

        for (FilterPair filterPair : (List<FilterPair>) filterValues) {
            String filterPairText = filterPair.getType() + "("
                    + filterPair.getPattern() + ")";
            if (!isFilterAdded(values, filterPairText)) {
                return false;
            }
        }

        return true;
    }

    public boolean updateDeletedFilters(List filterValues, String value) {
        String[] values = value.split(";");
        List<FilterPair> list = new ArrayList<>();
        list.addAll(filterValues);

        boolean someRemoved = false;
        if (values.length < filterValues.size() + 1) {
            for (FilterPair filterPair : list) {
                String filter = filterPair.getType() + "("
                        + filterPair.getPattern() + ")";
                if (!isFilterAdded(values, filter)) {
                    filterValues.remove(filterPair);
                    someRemoved = true;
                }
            }
        }

        return someRemoved;
    }

    private boolean isFilterAdded(String[] values, String filter) {
        for (String value : values) {
            if (isFilterEquals(value, filter)) {
                return true;
            }
        }
        return false;
    }

    boolean isFilterEquals(String value, String filter) {
        value = value.replace(" ", "");
        filter = StringUtils.deleteWhitespace(filter);

        return (filter.equals(value));
    }

    public String[] getHeaders() {
        return headers;
    }

    public ListitemRenderer getItemRenderer() {
        return filterPairRenderer;
    }

    /**
     * Render for {@link FilterPair}.
     *
     * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
     */
    private final ListitemRenderer filterPairRenderer = new ListitemRenderer() {

        @Override
        public void render(Listitem item, Object data, int i) {
            FilterPair filterPair = (FilterPair) data;
            item.setValue(data);

            final Listcell labelPattern = new Listcell();
            labelPattern.setLabel(filterPair.getPattern());
            labelPattern.setParent(item);

            final Listcell labelType = new Listcell();
            labelType.setLabel(_(filterPair.getTypeComplete()));
            labelType.setParent(item);

        }
    };

}

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

package org.libreplan.web.common.entrypoints;

import static org.libreplan.web.I18nHelper._;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.libreplan.web.common.Util;
import org.libreplan.web.common.converters.IConverter;
import org.libreplan.web.common.converters.IConverterFactory;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.BookmarkEvent;

/**
 * Handler for EntryPoints.
 * In other way it is also wrapper for URL redirecting.
 * <br />
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 * @author Vova Perebykivskyi <vova@libreplan-enterprise.com>
 */
public class EntryPointsHandler<T> {

    private static final String MANUALLY_SET_PARAMS = "PARAMS";

    private static final String FLAG_ATTRIBUTE = EntryPointsHandler.class.getName() + "_";

    private static final Log LOG = LogFactory.getLog(EntryPointsHandler.class);

    private static class EntryPointMetadata {

        private final Method method;

        private final EntryPoint annotation;

        private EntryPointMetadata(Method method, EntryPoint annotation) {
            this.method = method;
            this.annotation = annotation;
        }
    }

    private final IExecutorRetriever executorRetriever;

    private Map<String, EntryPointMetadata> metadata = new HashMap<>();

    private final String page;

    private final IConverterFactory converterFactory;

    private static final ThreadLocal<List<String>> linkCapturer = new ThreadLocal<>();

    public EntryPointsHandler(IConverterFactory converterFactory,
                              IExecutorRetriever executorRetriever,
                              Class<T> interfaceDefiningEntryPoints) {

        Validate.isTrue(interfaceDefiningEntryPoints.isInterface());
        this.converterFactory = converterFactory;
        this.executorRetriever = executorRetriever;
        EntryPoints entryPoints = interfaceDefiningEntryPoints.getAnnotation(EntryPoints.class);

        Validate.notNull(
                entryPoints,
                _(
                        "{0} annotation required on {1}",
                        EntryPoints.class.getName(),
                        interfaceDefiningEntryPoints.getName()));

        this.page = entryPoints.page();

        for (Method method : interfaceDefiningEntryPoints.getMethods()) {
            EntryPoint entryPoint = method.getAnnotation(EntryPoint.class);
            if (entryPoint != null) {
                metadata.put(method.getName(), new EntryPointMetadata(method, entryPoint));
            }
        }
    }

    public static void setupEntryPointsForThisRequest(HttpServletRequest request, Map<String, String> entryPoints) {
        request.setAttribute(MANUALLY_SET_PARAMS, entryPoints);
    }

    public interface ICapture {

        void capture();
    }

    /**
     * It capture the first redirect done via an {@link EntryPoint} in the provided {@link ICapture} and returns the path.
     *
     * @see #capturePaths(ICapture)
     * @param redirects
     * @throws IllegalStateException
     *             if no {@link EntryPoint} point call is done.
     */
    public static String capturePath(ICapture redirects) {
        List<? extends String> result = capturePaths(redirects);
        if (result.isEmpty()) {
            throw new IllegalStateException("a call to an entry point should be done");
        }
        return result.get(0);
    }

    /**
     * It captures the redirects done via {@link EntryPoint} in the provided {@link ICapture} and returns the paths.
     *
     * @param redirects
     * @return {@link List<? extends String>}
     */
    public static List<? extends String> capturePaths(ICapture redirects) {
        linkCapturer.set(new ArrayList<>());
        try {
            redirects.capture();
            List<String> list = linkCapturer.get();

            if (list == null) {
                throw new RuntimeException(ICapture.class.getName() + " cannot be nested");
            }

            return Collections.unmodifiableList(list);
        } finally {
            linkCapturer.set(null);
        }
    }

    public void doTransition(String methodName, Object... values) {
        if (!metadata.containsKey(methodName)) {
            LOG.error("Method " + methodName +
                    "doesn't represent a state(It doesn't have a " +
                    EntryPoint.class.getSimpleName() + " annotation). Nothing will be done");

            return;
        }

        String fragment = buildFragment(methodName, values);

        if (linkCapturer.get() != null) {
            linkCapturer.get().add(buildRedirectURL(fragment));
            return;
        }

        if ( isFlaggedInThisRequest()) {
            return;
        }
        flagAlreadyExecutedInThisRequest();

        String requestPath = executorRetriever.getCurrent().getDesktop().getRequestPath();

        if (requestPath.contains(page)) {
            doBookmark(fragment);
        } else {
            sendRedirect(fragment);
        }
    }

    private String buildFragment(String methodName, Object... values) {
        EntryPointMetadata linkableMetadata = metadata.get(methodName);
        Class<?>[] types = linkableMetadata.method.getParameterTypes();
        String[] parameterNames = linkableMetadata.annotation.value();
        String[] stringRepresentations = new String[parameterNames.length];

        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
            IConverter<?> converterFor = converterFactory.getConverterFor(type);
            stringRepresentations[i] = converterFor.asStringUngeneric(values[i]);
        }

        return getFragment(parameterNames, stringRepresentations);
    }

    private boolean isFlaggedInThisRequest() {
        return getRequest().getAttribute(FLAG_ATTRIBUTE) == this;
    }

    private void flagAlreadyExecutedInThisRequest() {
        getRequest().setAttribute(FLAG_ATTRIBUTE, this);
    }

    private void doBookmark(String fragment) {
        executorRetriever.getCurrent().getDesktop().setBookmark(stripPound(fragment));
    }

    private String stripPound(String fragment) {
        return fragment.startsWith("#") ? fragment.substring(1) : fragment;
    }

    private void sendRedirect(String fragment) {
        String uri = buildRedirectURL(fragment);
        executorRetriever.getCurrent().sendRedirect(uri);
    }

    /**
     * After migration from ZK 5 to ZK 8 it starts to throw 404 error on pages that were redirected with parameters.
     * Solution is to make question mark (?) symbol after page.
     *
     * Before: http://localhost:8080/myaccount/personalTimesheet.zul;date=2016-07-08;resource=WORKER0004
     *
     * After: http://localhost:8080/myaccount/personalTimesheet.zul?date=2016-07-08;resource=WORKER0004
     */
    private String buildRedirectURL(String fragment) {
        return page + "?" + stripPound(fragment);
    }

    private String getFragment(String[] parameterNames, String[] stringRepresentations) {

        StringBuilder result = new StringBuilder();

        if (parameterNames.length > 0) {
            result.append("#");
        }

        for (int i = 0; i < parameterNames.length; i++) {
            result.append(parameterNames[i]);

            if (stringRepresentations[i] != null) {
                result.append("=").append(stringRepresentations[i]);
            }

            if (i < parameterNames.length - 1) {
                result.append(";");
            }
        }

        return result.toString();
    }

    private static void callMethod(Object target, Method superclassMethod, Object[] params) {
        try {
            Method method = target.getClass().getMethod(superclassMethod.getName(), superclassMethod.getParameterTypes());
            method.invoke(target, params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <S extends T> boolean applyIfMatches(S controller) {
        HttpServletRequest request = getRequest();

        if (request.getAttribute(MANUALLY_SET_PARAMS) != null) {
            return applyIfMatches(controller, (Map<String, String>) request.getAttribute(MANUALLY_SET_PARAMS));
        }

        return request.getQueryString() != null
                ? applyIfMatches(controller, request.getRequestURI() + ";" + request.getQueryString())
                : applyIfMatches(controller, request.getRequestURI());
    }

    private HttpServletRequest getRequest() {
        return (HttpServletRequest) executorRetriever.getCurrent().getNativeRequest();
    }

    public <S extends T> boolean applyIfMatches(S controller, String fragment) {
        if ( isFlaggedInThisRequest()) {
            return false;
        }

        String string = insertSemicolonIfNeeded(fragment);
        Map<String, String> matrixParams = MatrixParameters.extract(string);

        return applyIfMatches(controller, matrixParams);
    }

    private <S> boolean applyIfMatches(final S controller, Map<String, String> matrixParams) {
        flagAlreadyExecutedInThisRequest();

        Set<String> matrixParamsNames = matrixParams.keySet();

        for (Entry<String, EntryPointMetadata> entry : metadata.entrySet()) {

            final EntryPointMetadata entryPointMetadata = entry.getValue();

            EntryPoint entryPointAnnotation = entryPointMetadata.annotation;

            HashSet<String> requiredParams = new HashSet<>(Arrays.asList(entryPointAnnotation.value()));

            if (matrixParamsNames.equals(requiredParams)) {

                final Object[] arguments =
                        retrieveArguments(matrixParams, entryPointAnnotation, entryPointMetadata.method.getParameterTypes());

                Util.executeIgnoringCreationOfBindings(
                        () -> callMethod(controller, entryPointMetadata.method, arguments));

                return true;
            }
        }

        return false;
    }

    public <S extends T> void register(final S controller, Page page) {
        registerBookmarkListener(controller, page);
        applyIfMatches(controller);
    }

    public <S extends T> void registerBookmarkListener(final S controller, Page page) {
        page.addEventListener("onBookmarkChange", event -> {
            BookmarkEvent bookmarkEvent = (BookmarkEvent) event;
            String bookmark = bookmarkEvent.getBookmark();
            applyIfMatches(controller, bookmark);
        });
    }

    private String insertSemicolonIfNeeded(String uri) {
        return !uri.startsWith(";") ? ";" + uri : uri;
    }

    private Object[] retrieveArguments(
            Map<String, String> matrixParams, EntryPoint linkToStateAnnotation, Class<?>[] parameterTypes) {

        Object[] result = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            String argumentName = linkToStateAnnotation.value()[i];
            String parameterValue = matrixParams.get(argumentName);

            IConverter<?> converter = converterFactory.getConverterFor(parameterTypes[i]);

            result[i] = converter.asObject(parameterValue);
        }

        return result;
    }
}

/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2010-2011 Wireless Galicia, S.L.
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

package org.libreplan.business.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * It contains the current version of project and implements of singleton pattern.
 * <br />
 * It also has a cached value with information about last project version published.
 * It checks the last version against a URL.
 *
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 * @author Vova Perebykivskyi <vova@libreplan-enterprise.com>
 */
public class VersionInformation {

    private static final Log LOG = LogFactory.getLog(VersionInformation.class);

    /**
     * URL with a text file only with last number version of LibrePlan.
     */
    private static final String LIBREPLAN_VERSION_URL = "http://libreplan.org/VERSION";


    /**
     * Delay to wait till we check the URL again.
     * 1 Day.
     */
    private static final long DELAY_TO_CHECK_URL = 24 * 60 * 60 * 1000L;

    private static final VersionInformation singleton = new VersionInformation();

    private String projectVersion;

    private boolean newVersionCached = false;

    private Date lastVersionCachedDate = new Date();

    private VersionInformation() {
    }

    private void loadNewVersionFromURL() {
        lastVersionCachedDate = new Date();
        try {
            URL url = getURL();
            String lastVersion = (new BufferedReader(new InputStreamReader(url.openStream()))).readLine();
            if (projectVersion != null && lastVersion != null) {
                Integer currentVersion = Integer.parseInt(projectVersion.replace(".", ""));
                Integer versionFromURL = Integer.parseInt(lastVersion.replace(".", ""));
                newVersionCached = versionFromURL > currentVersion;
            }
        } catch (MalformedURLException e) {
            LOG.warn("Problems generating URL to check LibrePlan version. MalformedURLException: " + e.getMessage());
        } catch (IOException e) {
            LOG.info("Could not check LibrePlan version information from " +
                    LIBREPLAN_VERSION_URL + ". IOException: " + e.getMessage());
        }
    }

    private URL getURL() throws MalformedURLException {
        return new URL(LIBREPLAN_VERSION_URL);
    }

    public static VersionInformation getInstance() {
        return singleton;
    }

    /**
     * It returns the current version of the project for retrieval at any place.
     */
    public static String getVersion() {
        return singleton.getProjectVersion();
    }

    public String getProjectVersion() {
        return projectVersion;
    }

    public void setProjectVersion(String argVersion) {
        projectVersion = argVersion;
        loadNewVersionFromURL();
    }

    /**
     * Returns true if a new version of the project is published.
     */
    public static boolean isNewVersionAvailable() {
        return singleton.checkIsNewVersionAvailable();
    }

    /**
     * If there is a new version already detected, it doesn't check it again.
     * Otherwise, during one day it returns the cached value. And it checks it again after that time.
     */
    private boolean checkIsNewVersionAvailable() {
        if ( !newVersionCached ) {
            long oneDayLater = lastVersionCachedDate.getTime() + DELAY_TO_CHECK_URL;
            if ( oneDayLater < new Date().getTime() ) {
                loadNewVersionFromURL();
            }
        }
        return newVersionCached;
    }

    public static String getLastVersion() {
        String lastVersion = "";
        try {
            URL url = new URL(LIBREPLAN_VERSION_URL);
            lastVersion = (new BufferedReader(new InputStreamReader(url.openStream()))).readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lastVersion;
    }
}

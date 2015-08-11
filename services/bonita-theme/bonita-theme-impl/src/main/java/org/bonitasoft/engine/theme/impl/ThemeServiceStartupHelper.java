/**
 * Copyright (C) 2015 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.theme.impl;

import java.io.IOException;
import java.io.InputStream;

import org.bonitasoft.engine.builder.BuilderFactory;
import org.bonitasoft.engine.commons.io.IOUtil;
import org.bonitasoft.engine.theme.ThemeService;
import org.bonitasoft.engine.theme.builder.SThemeBuilder;
import org.bonitasoft.engine.theme.builder.SThemeBuilderFactory;
import org.bonitasoft.engine.theme.builder.SThemeUpdateBuilder;
import org.bonitasoft.engine.theme.builder.SThemeUpdateBuilderFactory;
import org.bonitasoft.engine.theme.exception.SThemeCreationException;
import org.bonitasoft.engine.theme.exception.SThemeNotFoundException;
import org.bonitasoft.engine.theme.exception.SThemeReadException;
import org.bonitasoft.engine.theme.exception.SThemeUpdateException;
import org.bonitasoft.engine.theme.model.STheme;
import org.bonitasoft.engine.theme.model.SThemeType;

/**
 * @author Celine Souchet
 * @author Philippe Ozil
 */
public class ThemeServiceStartupHelper {

    static final String BONITA_PORTAL_THEME_DEFAULT = "bonita-portal-theme";

    static final String BONITA_MOBILE_THEME_DEFAULT = "bonita-mobile-theme";

    private static final String ZIP = ".zip";

    private final ThemeService themeService;

    public ThemeServiceStartupHelper(final ThemeService themeService) {
        this.themeService = themeService;
    }

    /**
     * Create the default Portal and Mobile themes if they do not already exist else do nothing
     * 
     * @throws IOException
     * @throws SThemeCreationException
     * @throws SThemeReadException
     */
    public void createOrUpdateDefaultThemes() throws IOException, SThemeCreationException, SThemeReadException, SThemeUpdateException {
        createOrUpdateDefaultTheme(SThemeType.MOBILE, BONITA_MOBILE_THEME_DEFAULT);
        createOrUpdateDefaultTheme(SThemeType.PORTAL, BONITA_PORTAL_THEME_DEFAULT);
    }

    void createOrUpdateDefaultTheme(SThemeType portal, String themeDefault) throws IOException, SThemeReadException, SThemeUpdateException,
            SThemeCreationException {
        final byte[] defaultThemeZip = getFileContent(themeDefault + ZIP);
        try {
            final STheme theme = themeService.getTheme(portal, true);
            if (defaultThemeZip.length != theme.getContent().length) {
                updateDefaultTheme(theme, defaultThemeZip, portal);
            }
        } catch (SThemeNotFoundException e) {
            createDefaultTheme(defaultThemeZip, portal);
        }
    }

    void createDefaultTheme(byte[] defaultThemeZip, SThemeType type) throws IOException, SThemeCreationException {
        if (defaultThemeZip != null && defaultThemeZip.length > 0) {
            final STheme sTheme = buildSTheme(defaultThemeZip, getCssContent(type), type);
            themeService.createTheme(sTheme);
        }
    }

    void updateDefaultTheme(STheme theme, byte[] defaultThemeZip, SThemeType mobile) throws IOException, SThemeUpdateException {
        final SThemeUpdateBuilder updateBuilder = BuilderFactory.get(SThemeUpdateBuilderFactory.class).createNewInstance();
        updateBuilder.setContent(defaultThemeZip);
        updateBuilder.setCSSContent(getCssContent(mobile));
        updateBuilder.setLastUpdateDate(System.currentTimeMillis());
        themeService.updateTheme(theme, updateBuilder.done());
    }

    byte[] getCssContent(SThemeType type) throws IOException {
        if (type.equals(SThemeType.PORTAL)) {
            return IOUtil.getZipEntryContent("bonita.css", getFileContent(BONITA_PORTAL_THEME_DEFAULT + "-css" + ZIP));
        }
        return null;
    }

    STheme buildSTheme(final byte[] defaultThemeZip, final byte[] defaultThemeCss, final SThemeType type) {
        final long lastUpdateDate = System.currentTimeMillis();
        final SThemeBuilder sThemeBuilder = BuilderFactory.get(SThemeBuilderFactory.class).createNewInstance(defaultThemeZip, true, type, lastUpdateDate);
        sThemeBuilder.setCSSContent(defaultThemeCss);
        return sThemeBuilder.done();
    }

    byte[] getFileContent(final String fileName) throws IOException {
        try (final InputStream inputStream = getResourceAsStream(fileName)) {
            if (inputStream == null) {
                return null;
            }
            return IOUtil.getAllContentFrom(inputStream);
        }
    }

    private InputStream getResourceAsStream(final String fileName) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
    }
}

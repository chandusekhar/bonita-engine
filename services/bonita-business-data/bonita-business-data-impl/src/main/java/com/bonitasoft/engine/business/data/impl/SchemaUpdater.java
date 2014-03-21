/*******************************************************************************
 * Copyright (C) 2014 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 *******************************************************************************/
package com.bonitasoft.engine.business.data.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.internal.StandardServiceRegistryImpl;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.hibernate.tool.hbm2ddl.SchemaUpdateScript;

/**
 * @author Matthieu Chaffotte
 */
public class SchemaUpdater {

    private final Configuration configuration;

    private final Dialect dialect;

    private final List<Exception> exceptions = new ArrayList<Exception>();

    public SchemaUpdater(final Configuration configuration) throws HibernateException {
        this.configuration = configuration;
        dialect = Dialect.getDialect(configuration.getProperties());
    }

    private StandardServiceRegistryImpl createServiceRegistry(final Properties properties) {
        Environment.verifyProperties(properties);
        ConfigurationHelper.resolvePlaceHolders(properties);
        return (StandardServiceRegistryImpl) new ServiceRegistryBuilder().applySettings(properties).buildServiceRegistry();
    }

    public void execute() {
        exceptions.clear();
        final Properties props = new Properties();
        props.putAll(dialect.getDefaultProperties());
        props.putAll(configuration.getProperties());
        final StandardServiceRegistryImpl serviceRegistry = createServiceRegistry(props);

        Connection connection = null;
        DatabaseMetadata meta;
        try {
            try {
                connection = serviceRegistry.getService(ConnectionProvider.class).getConnection();
                meta = new DatabaseMetadata(connection, dialect, configuration);
            } catch (final SQLException sqle) {
                exceptions.add(sqle);
                throw sqle;
            }
            final List<SchemaUpdateScript> scripts = configuration.generateSchemaUpdateScriptList(dialect, meta);
            executeScripts(connection, scripts);
        } catch (final Exception e) {
            exceptions.add(e);
        } finally {
            try {
                if (connection != null) {
                    // Sybase fails if we don't do that
                    connection.clearWarnings();
                    connection.close();
                }
                serviceRegistry.destroy();
            } catch (final Exception e) {
                exceptions.add(e);
            }
        }
    }

    private void executeScripts(final Connection connection, final List<SchemaUpdateScript> scripts) throws SQLException {
        final Statement statement = connection.createStatement();
        for (final SchemaUpdateScript script : scripts) {
            try {
                statement.executeUpdate(script.getScript());
            } catch (final SQLException e) {
                if (!script.isQuiet()) {
                    exceptions.add(e);
                }
            }
        }
        statement.close();
    }

}

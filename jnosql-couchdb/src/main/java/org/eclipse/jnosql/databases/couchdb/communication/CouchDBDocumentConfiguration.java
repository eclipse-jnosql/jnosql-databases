/*
 *
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 *
 */
package org.eclipse.jnosql.databases.couchdb.communication;

import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;

import java.util.Arrays;
import java.util.Objects;

import static org.eclipse.jnosql.databases.couchdb.communication.CouchDBConfigurations.COMPRESSION;
import static org.eclipse.jnosql.databases.couchdb.communication.CouchDBConfigurations.CONNECTION_TIMEOUT;
import static org.eclipse.jnosql.databases.couchdb.communication.CouchDBConfigurations.ENABLE_SSL;
import static org.eclipse.jnosql.databases.couchdb.communication.CouchDBConfigurations.MAX_CACHE_ENTRIES;
import static org.eclipse.jnosql.databases.couchdb.communication.CouchDBConfigurations.MAX_CONNECTIONS;
import static org.eclipse.jnosql.databases.couchdb.communication.CouchDBConfigurations.MAX_OBJECT_SIZE_BYTES;
import static org.eclipse.jnosql.databases.couchdb.communication.CouchDBConfigurations.PORT;
import static org.eclipse.jnosql.databases.couchdb.communication.CouchDBConfigurations.SOCKET_TIMEOUT;
import static org.eclipse.jnosql.databases.couchdb.communication.CouchDBConfigurations.TOKEN;

/**
 * The CouchDB implementation of {@link DatabaseConfiguration}  that returns
 * {@link CouchDBDocumentManagerFactory}.
 * @see CouchDBConfigurations
 */
public class CouchDBDocumentConfiguration implements DatabaseConfiguration {



    @Override
    public CouchDBDocumentManagerFactory apply(Settings settings) {
        Objects.requireNonNull(settings, "settings is required");
        CouchDBHttpConfigurationBuilder configuration = new CouchDBHttpConfigurationBuilder();

        settings.getSupplier(Arrays.asList(CouchDBConfigurations.HOST, Configurations.HOST))
                .map(Object::toString)
                .ifPresent(configuration::host);
        settings.getSupplier(Arrays.asList(CouchDBConfigurations.USER, Configurations.USER))
                .map(Object::toString)
                .ifPresent(configuration::username);
        settings.getSupplier(Arrays.asList(CouchDBConfigurations.PASSWORD, Configurations.PASSWORD))
                .map(Object::toString)
                .ifPresent(configuration::password);

        settings.get(TOKEN, String.class).ifPresent(configuration::token);
        settings.get(PORT, Integer.class).ifPresent(configuration::port);
        settings.get(MAX_CONNECTIONS, Integer.class).ifPresent(configuration::maxConnections);
        settings.get(CONNECTION_TIMEOUT, Integer.class).ifPresent(configuration::connectionTimeout);

        settings.get(SOCKET_TIMEOUT, Integer.class).ifPresent(configuration::socketTimeout);
        settings.get(MAX_OBJECT_SIZE_BYTES, Integer.class).ifPresent(configuration::maxObjectSizeBytes);
        settings.get(MAX_CACHE_ENTRIES, Integer.class).ifPresent(configuration::maxCacheEntries);

        settings.get(ENABLE_SSL, Boolean.class).ifPresent(configuration::enableSSL);
        settings.get(COMPRESSION, Boolean.class).ifPresent(configuration::compression);

        return new CouchDBDocumentManagerFactory(configuration.build());
    }
}
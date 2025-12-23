/*
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
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.orientdb.communication;


import com.orientechnologies.orient.core.db.ODatabasePool;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;

import java.util.Arrays;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * The OrientDB implementation of {@link DatabaseManagerFactory}
 */
public class OrientDBDocumentManagerFactory implements DatabaseManagerFactory {

    private final String host;
    private final String user;
    private final String password;
    private final ODatabaseType storageType;
    private final OrientDB orient;

    OrientDBDocumentManagerFactory(String host, String user, String password, String storageType) {
        this.host = URLPrefix.formatURL(host);
        this.user = user;
        this.password = password;
        this.storageType = ofNullable(storageType)
                .map(String::toUpperCase)
                .map(ODatabaseType::valueOf)
                .orElse(ODatabaseType.PLOCAL);
        this.orient = new OrientDB(this.host, this.user, this.password, getOrientDBConfig());
    }

    enum URLPrefix {

        EMBEDDED("embedded"),
        REMOTE("remote");

        private final String prefix;

        URLPrefix(String prefix) {
            this.prefix = prefix;
        }

        static String formatURL(String host) {
            URLPrefix prefix= of(host)
                    .orElseThrow(() ->
                    new IllegalArgumentException("The host url is invalid. " +
                            "Prefix is needed: possible kind of URLs are 'embedded' or 'remote', " +
                            "also for the case of remote and distributed can be specified multiple nodes using comma"));
            if(!host.toLowerCase().startsWith(prefix.prefix)){
                return prefix.prefix + ":" + host;
            }
            return host;
        }

        static Optional<URLPrefix> of(String url) {
            if (url == null || url.isBlank()) {
                return Optional.empty();
            }
            String[] parts = url.split(":", 2);
            if (parts.length == 2) {
                String prefix = parts[0];
                return Arrays.stream(URLPrefix.values())
                        .filter(urlPrefix -> urlPrefix.prefix.equalsIgnoreCase(prefix))
                        .findFirst();
            }
            return Optional.empty();
        }
    }

    private OrientDBConfig getOrientDBConfig() {
        return URLPrefix.of(this.host)
                .filter(URLPrefix.EMBEDDED::equals)
                .map(urlPrefix -> OrientDBConfig
                        .builder()
                        .addGlobalUser(user, password, "*")
                        .build())
                .orElseGet(OrientDBConfig::defaultConfig);
    }

    @Override
    public OrientDBDocumentManager apply(String database) {
        requireNonNull(database, "database is required");
        orient.createIfNotExists(database, storageType);
        ODatabasePool pool = new ODatabasePool(orient, database, user, password);
        return new DefaultOrientDBDocumentManager(pool, database);

    }

    @Override
    public void close() {
        orient.close();
    }
}

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

import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;

import java.util.Objects;

/**
 * The couchdb implementation of {@link DatabaseManagerFactory} that returns {@link DefaultCouchDBDocumentManager}.
 * <br/>
 * Closing a {@link CouchDBDocumentManagerFactory} has no effect.
 */
public class CouchDBDocumentManagerFactory implements DatabaseManagerFactory {


    private final CouchDBHttpConfiguration configuration;

    CouchDBDocumentManagerFactory(CouchDBHttpConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public CouchDBDocumentManager apply(String database) {
        Objects.requireNonNull(database, "database is required");

        CouchDBHttpClient client = configuration.getClient(database);
        client.createDatabase();
        return new DefaultCouchDBDocumentManager(client, database);
    }


    /**
     * Closing a {@link CouchDBDocumentManagerFactory} has no effect.
     */
    @Override
    public void close() {
    }
}

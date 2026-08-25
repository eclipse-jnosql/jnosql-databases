/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License 2.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *   and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.databases.couchbase.communication;


import com.couchbase.client.java.Cluster;
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;

import java.util.Objects;

/**
 * Provides the couchbase document manager factory implementation.
 */
public class CouchbaseDocumentManagerFactory implements DatabaseManagerFactory {


    private final CouchbaseSettings settings;
    private final Cluster cluster;
    CouchbaseDocumentManagerFactory(CouchbaseSettings settings) {
        this.settings = settings;
        this.cluster = settings.getCluster();
    }

    @Override
    public CouchbaseDocumentManager apply(String database)  {
        Objects.requireNonNull(database, "database is required");
        return new DefaultCouchbaseDocumentManager(cluster, database);
    }


    @Override
    public void close() {
        cluster.close();
    }
}

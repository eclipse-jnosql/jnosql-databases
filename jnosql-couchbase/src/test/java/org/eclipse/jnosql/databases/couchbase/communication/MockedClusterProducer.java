/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
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

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.DatabaseType;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Produces the real Couchbase communication manager on top of a mocked {@link Cluster}.
 */
@Alternative
@ApplicationScoped
public class MockedClusterProducer {

    public static final Cluster CLUSTER = mock(Cluster.class);
    private static final Bucket BUCKET = mock(Bucket.class, RETURNS_DEEP_STUBS);

    @Produces
    public CouchbaseDocumentManager manager() {
        return new DefaultCouchbaseDocumentManager(CLUSTER, "library");
    }

    @Produces
    @Database(DatabaseType.DOCUMENT)
    public DatabaseManager documentManager() {
        return manager();
    }

    public static void reset() {
        org.mockito.Mockito.reset(CLUSTER);
        when(BUCKET.defaultScope().name()).thenReturn("_default");
        when(CLUSTER.bucket("library")).thenReturn(BUCKET);
        QueryResult result = mock(QueryResult.class);
        when(result.rowsAsObject()).thenReturn(List.of());
        when(CLUSTER.query(anyString())).thenReturn(result);
        when(CLUSTER.query(anyString(), any(QueryOptions.class))).thenReturn(result);
    }
}

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
package org.eclipse.jnosql.databases.arangodb.communication;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.interceptor.Interceptor;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.DatabaseType;
import org.mockito.invocation.Invocation;

import java.util.List;
import java.util.Spliterators;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

/**
 * Produces the real ArangoDB communication manager on top of a mocked driver.
 */
@Alternative
@Priority(Interceptor.Priority.APPLICATION)
@ApplicationScoped
public class MockedArangoProducer {

    public static final ArangoDB ARANGO = mock(ArangoDB.class);
    public static final ArangoDatabase DATABASE = mock(ArangoDatabase.class);

    @Produces
    public ArangoDBDocumentManager manager() {
        return new DefaultArangoDBDocumentManager("library", ARANGO);
    }

    @Produces
    @Database(DatabaseType.DOCUMENT)
    public DatabaseManager documentManager() {
        return manager();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void reset() {
        org.mockito.Mockito.reset(ARANGO, DATABASE);
        var collection = mock(ArangoCollection.class);
        ArangoCursor<Object> cursor = mock(ArangoCursor.class, RETURNS_DEEP_STUBS);
        when(ARANGO.db("library")).thenReturn(DATABASE);
        when(ARANGO.getAccessibleDatabases()).thenReturn(List.of("library"));
        when(DATABASE.name()).thenReturn("library");
        when(DATABASE.arango()).thenReturn(ARANGO);
        when(DATABASE.collection(org.mockito.ArgumentMatchers.anyString())).thenReturn(collection);
        when(collection.exists()).thenReturn(true);
        when(cursor.spliterator()).thenAnswer(invocation -> Spliterators.emptySpliterator());
        when(DATABASE.query(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyMap(), org.mockito.ArgumentMatchers.any()))
                .thenReturn((ArangoCursor) cursor);
    }

    /**
     * @return the executed AQL requests
     */
    public static List<Invocation> requests() {
        return mockingDetails(DATABASE).getInvocations().stream()
                .filter(call -> call.getMethod().getName().equals("query")).toList();
    }
}

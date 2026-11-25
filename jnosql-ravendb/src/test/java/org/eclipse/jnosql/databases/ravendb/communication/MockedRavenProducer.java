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
package org.eclipse.jnosql.databases.ravendb.communication;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.interceptor.Interceptor;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.DatabaseType;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Produces the real RavenDB communication manager on top of a mocked document store.
 */
@Alternative
@Priority(Interceptor.Priority.APPLICATION)
@ApplicationScoped
public class MockedRavenProducer {

    public static final DocumentStore STORE = mock(DocumentStore.class);
    public static final IDocumentSession SESSION = mock(IDocumentSession.class);
    public static final IDocumentQuery<HashMap> QUERY = mock(IDocumentQuery.class);

    @Produces
    @Database(DatabaseType.DOCUMENT)
    public DatabaseManager documentManager() {
        return new RavenDBDocumentManager(STORE, "library");
    }

    @SuppressWarnings("unchecked")
    public static void reset() {
        org.mockito.Mockito.reset(STORE, SESSION, QUERY);
        when(STORE.openSession()).thenReturn(SESSION);
        when(SESSION.query(any(Class.class), any(net.ravendb.client.documents.queries.Query.class))).thenReturn(QUERY);
        when(QUERY.toList()).thenReturn(java.util.List.of());
    }
}

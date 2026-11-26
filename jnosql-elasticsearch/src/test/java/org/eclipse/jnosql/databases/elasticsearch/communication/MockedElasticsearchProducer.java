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
package org.eclipse.jnosql.databases.elasticsearch.communication;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.interceptor.Interceptor;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.mapping.Database;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

/**
 * Produces the real Elasticsearch communication manager on top of a mocked client. A search that is allowed to
 * reach the client fails with an {@link IOException}, so the captured {@link SearchRequest} can be inspected.
 */
@Alternative
@Priority(Interceptor.Priority.APPLICATION)
@ApplicationScoped
public class MockedElasticsearchProducer {

    public static final ElasticsearchClient CLIENT = mock(ElasticsearchClient.class, RETURNS_DEEP_STUBS);

    public static DefaultElasticsearchDocumentManager manager() {
        return new DefaultElasticsearchDocumentManager(CLIENT, "library");
    }

    @Produces
    @Database(org.eclipse.jnosql.mapping.DatabaseType.DOCUMENT)
    public DatabaseManager documentManager() {
        return manager();
    }

    @SuppressWarnings("unchecked")
    public static void reset() {
        Mockito.reset(CLIENT);
        try {
            when(CLIENT.search(any(SearchRequest.class), eq(java.util.Map.class)))
                    .thenThrow(new IOException("no Elasticsearch server in this test"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return every call made to the client, including the mapping lookup
     */
    public static List<Invocation> calls() {
        return mockingDetails(CLIENT).getInvocations().stream().toList();
    }

    /**
     * @return the search requests that reached the client
     */
    public static List<SearchRequest> searches() {
        return calls().stream().filter(call -> call.getMethod().getName().equals("search"))
                .map(call -> call.getArgument(0, SearchRequest.class)).toList();
    }
}

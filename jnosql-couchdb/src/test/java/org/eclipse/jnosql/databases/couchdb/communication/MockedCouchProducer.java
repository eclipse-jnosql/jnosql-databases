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
package org.eclipse.jnosql.databases.couchdb.communication;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.interceptor.Interceptor;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.DatabaseType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Produces the real CouchDB manager and Mango converter over an HTTP client that records every request and
 * fails it, so no server is needed.
 */
@Alternative
@Priority(Interceptor.Priority.APPLICATION)
@ApplicationScoped
public class MockedCouchProducer {

    public static final RecordingClient CLIENT = new RecordingClient();

    public static DefaultCouchDBDocumentManager manager() {
        var configuration = new CouchDBHttpConfiguration("localhost", 5984, 10, 1000, 1000,
                false, null, null, null, false, 8192, 10);
        return new DefaultCouchDBDocumentManager(new CouchDBHttpClient(configuration, CLIENT, "library"), "library");
    }

    @Produces
    @Database(DatabaseType.DOCUMENT)
    public DatabaseManager documentManager() {
        return manager();
    }

    /**
     * An HTTP client that records the method, the URI and the JSON body of each request.
     */
    public static final class RecordingClient extends CloseableHttpClient {

        public record Call(String method, String uri, String body) {
        }

        private final List<Call> calls = new ArrayList<>();

        public void reset() {
            calls.clear();
        }

        public List<Call> calls() {
            return List.copyOf(calls);
        }

        @Override
        protected org.apache.hc.client5.http.impl.classic.CloseableHttpResponse doExecute(HttpHost target,
                ClassicHttpRequest request, HttpContext context) throws IOException {
            try {
                var body = request.getEntity() == null ? "" : EntityUtils.toString(request.getEntity());
                calls.add(new Call(request.getMethod(), request.getRequestUri(), body));
            } catch (Exception e) {
                throw new IOException(e);
            }
            throw new IOException("no CouchDB server in this test");
        }

        @Override
        public void close() {
        }

        @Override
        public void close(org.apache.hc.core5.io.CloseMode closeMode) {
        }
    }
}

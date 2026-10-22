/*
 *
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
 *
 */
package org.eclipse.jnosql.databases.couchdb.communication;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.CloseMode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouchDBDocumentManagerFactoryTest {

    @Test
    void shouldCloseClientWhenInitializationFails() {
        var fake = new FakeClient(false);
        var factory = new CouchDBDocumentManagerFactory(configuration(fake));

        assertThatThrownBy(() -> factory.apply("test_database"))
                .isInstanceOf(CouchDBHttpClientException.class)
                .satisfies(e -> assertThat(e.getSuppressed()).isEmpty());

        assertThat(fake.closed.get()).isEqualTo(1);
    }

    @Test
    void shouldCloseClientAndKeepOriginalExceptionWhenCloseFails() {
        var fake = new FakeClient(true);
        var factory = new CouchDBDocumentManagerFactory(configuration(fake));

        assertThatThrownBy(() -> factory.apply("test_database"))
                .isInstanceOf(CouchDBHttpClientException.class)
                .hasMessageContaining("An error accessing the database")
                .satisfies(e -> assertThat(e.getSuppressed()).hasSize(1));

        assertThat(fake.closed.get()).isEqualTo(1);
    }

    private CouchDBHttpConfiguration configuration(FakeClient fake) {
        return new CouchDBHttpConfiguration("localhost", 5984, 10, 1000, 1000,
                false, null, null, null, false, 8192, 10) {
            @Override
            public CouchDBHttpClient getClient(String database) {
                return new CouchDBHttpClient(this, fake, database);
            }
        };
    }

    private static final class FakeClient extends CloseableHttpClient {

        private final AtomicInteger closed = new AtomicInteger();
        private final boolean failOnClose;

        private FakeClient(boolean failOnClose) {
            this.failOnClose = failOnClose;
        }

        @Override
        protected org.apache.hc.client5.http.impl.classic.CloseableHttpResponse doExecute(
                HttpHost target, ClassicHttpRequest request, HttpContext context) throws IOException {
            throw new IOException("forbidden");
        }

        @Override
        public void close() throws IOException {
            closed.incrementAndGet();
            if (failOnClose) {
                throw new IOException("close failed");
            }
        }

        @Override
        public void close(CloseMode closeMode) {
            closed.incrementAndGet();
        }
    }
}

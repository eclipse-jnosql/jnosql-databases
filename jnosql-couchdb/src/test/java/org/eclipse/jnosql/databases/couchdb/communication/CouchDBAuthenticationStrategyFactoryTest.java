
/*
 *
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
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

import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CouchDBAuthenticationStrategyFactory")
class CouchDBAuthenticationStrategyFactoryTest {

    @Nested
    @DisplayName("Factory strategy resolution")
    class FactoryResolution {

        @Test
        @DisplayName("should return Basic strategy when username and password are provided")
        void shouldReturnBasicStrategy() {
            CouchDBAuthenticationStrategy strategy =
                    CouchDBAuthenticationStrategyFactory.of("admin", "secret", null);

            assertThat(strategy)
                    .isInstanceOf(CouchDBAuthenticationStrategyFactory.Basic.class);
        }

        @Test
        @DisplayName("should return Bearer strategy when token is provided and username/password are null")
        void shouldReturnBearerStrategy() {
            CouchDBAuthenticationStrategy strategy =
                    CouchDBAuthenticationStrategyFactory.of(null, null, "jwt-token");

            assertThat(strategy)
                    .isInstanceOf(CouchDBAuthenticationStrategyFactory.Bearer.class);
        }

        @Test
        @DisplayName("should return None strategy when no authentication data is provided")
        void shouldReturnNoneStrategy() {
            CouchDBAuthenticationStrategy strategy =
                    CouchDBAuthenticationStrategyFactory.of(null, null, null);

            assertThat(strategy)
                    .isInstanceOf(CouchDBAuthenticationStrategyFactory.None.class);
        }
    }

    @Nested
    @DisplayName("Authentication strategy behavior")
    class StrategyBehavior {

        @Test
        @DisplayName("Basic strategy should apply Authorization header using Basic scheme")
        void shouldApplyBasicAuthorizationHeader() {
            CouchDBAuthenticationStrategy strategy =
                    CouchDBAuthenticationStrategyFactory.of("admin", "secret", null);

            BasicHttpRequest request =
                    new BasicHttpRequest("GET", "/");

            strategy.apply(request);

            assertThat(request.getFirstHeader(HttpHeaders.AUTHORIZATION))
                    .isNotNull()
                    .extracting(NameValuePair::getValue)
                    .isEqualTo("Basic YWRtaW46c2VjcmV0");
        }

        @Test
        @DisplayName("Bearer strategy should apply Authorization header using Bearer scheme")
        void shouldApplyBearerAuthorizationHeader() {
            CouchDBAuthenticationStrategy strategy =
                    CouchDBAuthenticationStrategyFactory.of(null, null, "jwt-token");

            BasicHttpRequest request =
                    new BasicHttpRequest("GET", "/");

            strategy.apply(request);

            assertThat(request.getFirstHeader(HttpHeaders.AUTHORIZATION))
                    .isNotNull()
                    .extracting(NameValuePair::getValue)
                    .isEqualTo("Bearer jwt-token");
        }

        @Test
        @DisplayName("None strategy should not apply any Authorization header")
        void shouldNotApplyAnyAuthorizationHeader() {
            CouchDBAuthenticationStrategy strategy =
                    CouchDBAuthenticationStrategyFactory.of(null, null, null);

            BasicHttpRequest request =
                    new BasicHttpRequest("GET", "/");

            strategy.apply(request);

            assertThat(request.getFirstHeader(HttpHeaders.AUTHORIZATION))
                    .isNull();
        }
    }
}
/*
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
 */
package org.eclipse.jnosql.databases.couchdb.communication;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpUriRequest;

import java.util.Base64;
import java.util.Objects;


/**
 * Factory class for creating instances of {@link CouchDBAuthenticationStrategy}.
 * Provides different strategies for authenticating requests against CouchDB,
 * depending on the provided credentials or tokens.
 * This is a utility class and cannot be instantiated.
 */
final class CouchDBAuthenticationStrategyFactory {

    private static final None NONE = new None();
    private static final String BASIC = "Basic ";
    private static final String BEARER = "Bearer ";

    private CouchDBAuthenticationStrategyFactory() {
    }

    public static CouchDBAuthenticationStrategy of(String username, String password, String token) {
        if (username != null && password != null) {
            String toEncode = username + ":" + password;
            String basicHashPassword =
                    BASIC + Base64.getEncoder().encodeToString(toEncode.getBytes());
            return new Basic(basicHashPassword);
        }

        if (token != null) {
            return new Bearer(token);
        }
        return NONE;
    }

    static class Basic implements CouchDBAuthenticationStrategy {

        private final String authorizationHeader;

        Basic(String authorizationHeader) {
            this.authorizationHeader = Objects.requireNonNull(
                    authorizationHeader, "authorizationHeader must not be null");
        }

        @Override
        public void apply(HttpUriRequest request) {
            request.setHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
    }

    static class Bearer implements CouchDBAuthenticationStrategy {


        private final String token;

        Bearer(String token) {
            this.token = Objects.requireNonNull(token, "token must not be null");
        }

        @Override
        public void apply(HttpUriRequest request) {
            request.setHeader(HttpHeaders.AUTHORIZATION, BEARER + token);
        }
    }

    static class None implements CouchDBAuthenticationStrategy {

        @Override
        public void apply(HttpUriRequest request) {
            // intentionally no-op
        }
    }
}

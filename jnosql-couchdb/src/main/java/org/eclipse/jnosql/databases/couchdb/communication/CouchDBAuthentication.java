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

import java.util.Base64;


/**
 * This DTO is used to encapsulate the username, password, and authentication token.
 */
class CouchDBAuthentication {

    private final String username;
    private final String password;
    private final String token;
    private final String basicHashPassword;

    private CouchDBAuthentication(String username, String password, String token, String basicHashPassword) {
        this.username = username;
        this.password = password;
        this.token = token;
        this.basicHashPassword = basicHashPassword;
    }

    public static CouchDBAuthentication ofBasic(String username, String password, String token) {
        String toEncode = username + ":" + password;
        String basicHashPassword = "Basic " + Base64.getEncoder().encodeToString(toEncode.getBytes());
        return new CouchDBAuthentication(username, password, token, basicHashPassword);
    }
}

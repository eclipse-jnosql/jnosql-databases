/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
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

class CouchDBHttpConfigurationBuilder {

    private String host = "localhost";
    private int port = 5984;
    private int maxConnections = 20;
    private int connectionTimeout = 1000;
    private int socketTimeout = 10000;
    private boolean enableSSL = false;

    private String username;
    private String password;
    private String token;

    private boolean compression = false;
    private int maxObjectSizeBytes = 8192;
    private int maxCacheEntries = 1000;

    public CouchDBHttpConfigurationBuilder withPort(int port) {
        this.port = port;
        return this;
    }

    public CouchDBHttpConfigurationBuilder withMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
        return this;
    }

    public CouchDBHttpConfigurationBuilder withConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public CouchDBHttpConfigurationBuilder withSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    public CouchDBHttpConfigurationBuilder withMaxObjectSizeBytes(int maxObjectSizeBytes) {
        this.maxObjectSizeBytes = maxObjectSizeBytes;
        return this;
    }

    public CouchDBHttpConfigurationBuilder withMaxCacheEntries(int maxCacheEntries) {
        this.maxCacheEntries = maxCacheEntries;
        return this;
    }


    public CouchDBHttpConfigurationBuilder withCompression(boolean compression) {
        this.compression = compression;
        return this;
    }

    public CouchDBHttpConfigurationBuilder withHost(String host) {
        this.host = host;
        return this;
    }

    public CouchDBHttpConfigurationBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public CouchDBHttpConfigurationBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public CouchDBHttpConfigurationBuilder withEnableSSL(boolean enableSSL) {
        this.enableSSL = enableSSL;
        return this;
    }

    public CouchDBHttpConfigurationBuilder withToken(String token) {
        this.token = token;
        return this;
    }

    public CouchDBHttpConfiguration build() {
        return new CouchDBHttpConfiguration(host, port, maxConnections, connectionTimeout,
                socketTimeout, enableSSL, username, password, token,
                compression,
                maxObjectSizeBytes, maxCacheEntries);
    }


}
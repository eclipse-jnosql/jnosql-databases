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

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.cache.CacheConfig;
import org.apache.hc.client5.http.impl.cache.CachingHttpClients;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;

class CouchDBHttpConfiguration {

    private final String host;
    private final int port;
    private final int maxConnections;
    private final int connectionTimeout;
    private final int socketTimeout;
    private final boolean enableSSL;

    private final String username;
    private final String password;
    private final String token;
    private final CouchDBAuthenticationStrategy authenticationStrategy;


    private final boolean compression;
    private final int maxObjectSizeBytes;
    private final int maxCacheEntries;
    private final String url;


    CouchDBHttpConfiguration(String host, int port, int maxConnections,
                             int connectionTimeout, int socketTimeout,
                             boolean enableSSL, String username,
                             String password,
                             String token,
                             boolean compression, int maxObjectSizeBytes,
                             int maxCacheEntries) {
        this.host = host;
        this.port = port;
        this.maxConnections = maxConnections;
        this.connectionTimeout = connectionTimeout;
        this.socketTimeout = socketTimeout;
        this.enableSSL = enableSSL;
        this.username = username;
        this.password = password;
        this.token = token;
        this.compression = compression;
        this.maxObjectSizeBytes = maxObjectSizeBytes;
        this.maxCacheEntries = maxCacheEntries;
        this.url = createUrl();
        this.authenticationStrategy = CouchDBAuthenticationStrategyFactory.of(username, password, token);
    }

    private String createUrl() {
        StringBuilder url = new StringBuilder();
        if (enableSSL) {
            url.append("https://");
        } else {
            url.append("http://");
        }
        url.append(host).append(':').append(port).append('/');
        return url.toString();
    }

    public CouchDBHttpClient getClient(String database) {
        return new CouchDBHttpClient(this, getHttpClient(), database);
    }

    public String getUrl() {
        return url;
    }


    private CloseableHttpClient getHttpClient() {
        CacheConfig cacheConfig = CacheConfig.custom()
                .setMaxCacheEntries(maxCacheEntries)
                .setMaxObjectSize(maxObjectSizeBytes)
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectionTimeout))
                .setResponseTimeout(Timeout.ofMilliseconds(socketTimeout))
                .setContentCompressionEnabled(compression)
                .build();

        var connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxConnections);

        return CachingHttpClients.custom()
                .setCacheConfig(cacheConfig)
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig).build();
    }

    public CouchDBAuthenticationStrategy strategy() {
        return authenticationStrategy;
    }
}

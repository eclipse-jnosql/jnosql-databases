
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
 *   Alessandro Moscatelli
 */
package org.eclipse.jnosql.databases.elasticsearch.communication;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jsonb.JsonbJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * The implementation of {@link DatabaseConfiguration}
 * that returns {@link ElasticsearchDocumentManagerFactory}.
 *
 * @see ElasticsearchConfigurations
 */
public class ElasticsearchDocumentConfiguration implements DatabaseConfiguration {

    private static final int DEFAULT_PORT = 9200;

    private final List<HttpHost> httpHosts = new ArrayList<>();

    private final List<Header> headers = new ArrayList<>();

    public ElasticsearchDocumentConfiguration() {
    }

    /**
     * Adds a host in the configuration.
     *
     * @param host the host
     * @throws NullPointerException when host is null
     */
    public void add(HttpHost host) {
        this.httpHosts.add(Objects.requireNonNull(host, "host is required"));
    }

    /**
     * Adds a header in the configuration.
     *
     * @param header the header
     * @throws NullPointerException when header is null
     */
    public void add(Header header) {
        this.headers.add(Objects.requireNonNull(header, "header is required"));
    }

    @Override
    public ElasticsearchDocumentManagerFactory apply(Settings settings) {
        ElasticsearchClient elasticsearchClient = buildElasticsearchClient(settings);

        return new ElasticsearchDocumentManagerFactory(elasticsearchClient);
    }

    public ElasticsearchClient buildElasticsearchClient(Settings settings) {
        requireNonNull(settings, "settings is required");

        List<HttpHost> configuredHosts = settings
                .prefixSupplier(asList(ElasticsearchConfigurations.HOST, Configurations.HOST))
                .stream()
                .map(Object::toString)
                .map(host -> ElasticsearchAddress.of(host, DEFAULT_PORT))
                .map(ElasticsearchAddress::toHttpHost)
                .toList();

        this.httpHosts.addAll(configuredHosts);

        Rest5ClientBuilder builder = Rest5Client.builder(httpHosts.toArray(HttpHost[]::new));

        final Optional<String> username = settings
                .getSupplier(asList(Configurations.USER, ElasticsearchConfigurations.USER))
                .map(Object::toString);

        final Optional<String> password = settings
                .getSupplier(asList(Configurations.PASSWORD, ElasticsearchConfigurations.PASSWORD))
                .map(Object::toString);

        username.ifPresent(user -> addBasicAuthenticationHeader(user, password.orElse("")));

        builder.setDefaultHeaders(headers.toArray(Header[]::new));

        Rest5Client httpClient = builder.build();

        var transport = new Rest5ClientTransport(httpClient, new JsonbJsonpMapper());

        return new ElasticsearchClient(transport);
    }

    private void addBasicAuthenticationHeader(String username, String password) {
        String credentials = username + ":" + password;
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        this.headers.add(new BasicHeader("Authorization", "Basic " + encodedCredentials));
    }
}
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
package org.eclipse.jnosql.databases.mongodb.communication;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.SettingsBuilder;
import org.eclipse.jnosql.communication.driver.ConfigurationReader;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;


/**
 * The MongoDB implementation to {@link DatabaseConfiguration} that returns  {@link MongoDBDocumentManagerFactory}
 *
 * @see MongoDBDocumentConfigurations
 */
public class MongoDBDocumentConfiguration implements DatabaseConfiguration {

    /**
     * Default MongoDB port.
     */
    static final int DEFAULT_PORT = 27017;


    /**
     * Creates a {@link MongoDBDocumentManagerFactory} from map configurations
     *
     * @param configurations the configurations map
     * @return a MongoDBDocumentManagerFactory instance
     * @throws NullPointerException when the configurations is null
     */
    public MongoDBDocumentManagerFactory get(Map<String, String> configurations) throws NullPointerException {
        requireNonNull(configurations, "configurations is required");
        SettingsBuilder builder = Settings.builder();
        configurations.forEach(builder::put);
        return apply(builder.build());
    }

    /**
     * Creates a {@link MongoDBDocumentManagerFactory} from mongoClient
     *
     * @param mongoClient the mongo client {@link MongoClient}
     * @return a MongoDBDocumentManagerFactory instance
     * @throws NullPointerException when the mongoClient is null
     */
    public MongoDBDocumentManagerFactory get(MongoClient mongoClient) throws NullPointerException {
        requireNonNull(mongoClient, "mongo client is required");
        return new MongoDBDocumentManagerFactory(mongoClient);
    }


    @Override
    public MongoDBDocumentManagerFactory apply(Settings settings) {
        requireNonNull(settings, "settings is required");

        List<ServerAddress> servers = getServers(settings);
        Optional<String> applicationName = getApplicationName(settings);

        if (servers.isEmpty()) {
            return createFromConnectionString(settings, applicationName.orElse(null));
        }

        var mongoClientSettings = createClientSettings(settings, servers, applicationName.orElse(null));
        var mongoClient = MongoClients.create(mongoClientSettings);
        return new MongoDBDocumentManagerFactory(mongoClient);
    }

    private List<ServerAddress> getServers(Settings settings) {
        return settings.prefixSupplier(List.of(
                        MongoDBDocumentConfigurations.HOST,
                        Configurations.HOST
                ))
                .stream()
                .map(Object::toString)
                .map(HostPortConfiguration::new)
                .map(HostPortConfiguration::toServerAddress)
                .toList();
    }

    private Optional<String> getApplicationName(Settings settings) {
        return settings.get(
                MongoDBDocumentConfigurations.APPLICATION_NAME,
                String.class
        );
    }

    private MongoClientSettings createClientSettings(
            Settings settings,
            List<ServerAddress> servers,
            String applicationName) {

        MongoClientSettings.Builder builder = MongoClientSettings.builder()
                .applyToClusterSettings(cluster -> cluster.hosts(servers));

        MongoAuthentication.of(settings)
                .ifPresent(builder::credential);

        Optional.ofNullable(applicationName).ifPresent(builder::applicationName);

        return builder.build();
    }

    private MongoDBDocumentManagerFactory createFromConnectionString(Settings settings, String applicationName) {
        Optional<ConnectionString> connectionString = settings.get(MongoDBDocumentConfigurations.URL, String.class).map(ConnectionString::new);

        if (connectionString.isEmpty()) {
            return new MongoDBDocumentManagerFactory(MongoClients.create());
        }

        MongoClientSettings.Builder builder = MongoClientSettings.builder().applyConnectionString(connectionString.orElseThrow());

        Optional.ofNullable(applicationName).ifPresent(builder::applicationName);
        MongoClient mongoClient = MongoClients.create(builder.build());
        return new MongoDBDocumentManagerFactory(mongoClient);
    }
}

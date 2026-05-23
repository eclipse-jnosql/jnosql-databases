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
    public MongoDBDocumentManagerFactory apply(Settings settings) throws NullPointerException {
        requireNonNull(settings, "settings is required");

        List<ServerAddress> servers = settings
                .prefixSupplier(Arrays.asList(MongoDBDocumentConfigurations.HOST,
                        Configurations.HOST))
                .stream()
                .map(Object::toString)
                .map(HostPortConfiguration::new)
                .map(HostPortConfiguration::toServerAddress)
                .toList();

        var applicationName = settings.get(MongoDBDocumentConfigurations.APPLICATION_NAME, String.class);

        if (servers.isEmpty()) {
            return createMongoDBDocumentManagerFactory(settings, applicationName.orElse(null));
        }

        Optional<MongoCredential> credential = MongoAuthentication.of(settings);
        final MongoClientSettings mongoClientSettings = credential.map(c -> {
            var builderSettings = MongoClientSettings.builder().credential(c).applyToClusterSettings(builder -> builder.hosts(servers));
            applicationName.ifPresent(builderSettings::applicationName);
            return builderSettings;
        }).orElseGet(() -> {
            var settingsBuilder = MongoClientSettings.builder();
            applicationName.ifPresent(settingsBuilder::applicationName);
            return settingsBuilder.applyToClusterSettings(builder -> builder.hosts(servers));
        }).build();
        return new MongoDBDocumentManagerFactory(MongoClients.create(mongoClientSettings));
    }

    private static MongoDBDocumentManagerFactory createMongoDBDocumentManagerFactory(Settings settings, String applicationName) {
        var connectionString = settings
                .get(MongoDBDocumentConfigurations.URL, String.class)
                .map(ConnectionString::new);

        if (connectionString.isEmpty()) {
            return new MongoDBDocumentManagerFactory(MongoClients.create());
        }
        MongoClientSettings.Builder builder = MongoClientSettings.builder()
                .applyConnectionString(connectionString.orElseThrow());
        Optional.ofNullable(applicationName).ifPresent(builder::applicationName);
        MongoClientSettings mongoClientSettings = builder.build();
        MongoClient mongoClient = MongoClients.create(mongoClientSettings);
        return new MongoDBDocumentManagerFactory(mongoClient);
    }

    public MongoDBDocumentManagerFactory get(String pathFileConfig) throws NullPointerException {
        requireNonNull(pathFileConfig, "settings is required");

        Map<String, String> configuration = ConfigurationReader.from(pathFileConfig);
        return get(configuration);
    }
}

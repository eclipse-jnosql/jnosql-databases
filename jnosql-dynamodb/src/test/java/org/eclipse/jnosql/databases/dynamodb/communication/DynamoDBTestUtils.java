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
package org.eclipse.jnosql.databases.dynamodb.communication;

import jakarta.nosql.DiscriminatorColumn;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.SettingsBuilder;
import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;
import org.eclipse.jnosql.mapping.core.config.MappingConfigurations;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.function.UnaryOperator;

public enum DynamoDBTestUtils {

    CONFIG;

    private final GenericContainer dynamodb =
            new GenericContainer("amazon/dynamodb-local:latest")
                    //.withReuse(true)
                    .withExposedPorts(8000)
                    .waitingFor(Wait.defaultWaitStrategy());

    BucketManagerFactory getBucketManagerFactory() {
        Settings settings = getSettings();
        return getBucketManagerFactory(settings);
    }

    private static DynamoDBBucketManagerFactory getBucketManagerFactory(Settings settings) {
        DynamoDBKeyValueConfiguration configuration = new DynamoDBKeyValueConfiguration();
        return configuration.apply(settings);
    }

    DynamoDBDatabaseManagerFactory getDocumentManagerFactory() {
        Settings settings = getSettings();
        return getDocumentManagerFactory(settings);
    }

    DynamoDBDatabaseManagerFactory getDocumentManagerFactory(Settings settings) {
        var configuration = new DynamoDBDocumentConfiguration();
        return configuration.apply(settings);
    }

    public Settings getSettings() {
        dynamodb.start();
        String dynamoDBHost = getDynamoDBHost(dynamodb.getHost(), dynamodb.getFirstMappedPort());
        return getSettings(dynamoDBHost);
    }

    Settings getSettings(String dynamoDBHost) {
        return getSettingsBuilder(builder -> builder
                .put(DynamoDBConfigurations.ENDPOINT, dynamoDBHost))
                .build();
    }

    public Settings customSetting(SettingsBuilder builder) {
        var defaultSetting = getSettings();
        var customSetting = builder.build();
        return Settings.builder()
                .putAll(defaultSetting.toMap())
                .putAll(customSetting.toMap())
                .build();
    }

    @NotNull
    public static SettingsBuilder getSettingsBuilder(UnaryOperator<SettingsBuilder> builder) {
        return builder.apply(Settings.builder())
                .put(MappingConfigurations.DOCUMENT_DATABASE, "test")
                .put(DynamoDBConfigurations.AWS_ACCESSKEY, System.getProperty("AWS_ACCESS_KEY_ID", "AKIAIOSFODNN7EXAMPLE"))
                .put(DynamoDBConfigurations.AWS_SECRET_ACCESS, System.getProperty("AWS_SECRET_ACCESS_KEY", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"))
                .put(DynamoDBConfigurations.PROFILE, System.getProperty("AWS_PROFILE", "default"))
                .put(DynamoDBConfigurations.REGION, "us-west-2")
                .put(DynamoDBConfigurations.ENTITY_PARTITION_KEY, DiscriminatorColumn.DEFAULT_DISCRIMINATOR_COLUMN);
    }

    public void setupSystemProperties(SettingsBuilder builder) {
        Settings settings = customSetting(builder);
        System.getProperties().putAll(settings.toMap());
        System.out.println(System.getProperties());
    }

    @NotNull
    String getDynamoDBHost(String host, int port) {
        return "http://" + host + ":" + port;
    }

    void shutDown() {
        dynamodb.close();
    }

    DynamoDbClient getDynamoDbClient() {
        var settings = getSettings();
        return getDynamoDbClient(settings);
    }

    DynamoDbClient getDynamoDbClient(Settings settings) {
        DynamoDBBuilderSync builderSync = new DynamoDBBuilderSync();
        settings.get(DynamoDBConfigurations.ENDPOINT, String.class).ifPresent(builderSync::endpoint);
        settings.get(DynamoDBConfigurations.AWS_ACCESSKEY, String.class).ifPresent(builderSync::awsAccessKey);
        settings.get(DynamoDBConfigurations.AWS_SECRET_ACCESS, String.class).ifPresent(builderSync::awsSecretAccess);
        settings.get(DynamoDBConfigurations.PROFILE, String.class).ifPresent(builderSync::profile);
        settings.get(DynamoDBConfigurations.REGION, String.class).ifPresent(builderSync::region);
        return builderSync.build();
    }

}

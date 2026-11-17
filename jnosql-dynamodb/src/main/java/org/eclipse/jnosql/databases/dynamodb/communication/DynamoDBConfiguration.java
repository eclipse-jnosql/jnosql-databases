/*
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
 */

package org.eclipse.jnosql.databases.dynamodb.communication;

import org.eclipse.jnosql.communication.Settings;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.net.URI;

import static java.util.Objects.requireNonNull;


/**
 * Base class of the DynamoDB configurations: {@link DynamoDBDocumentConfiguration} for the document model and
 * {@link DynamoDBKeyValueConfiguration} for the key-value model. Use one of them to create a manager factory.
 *
 * <p>The AWS client created by {@code apply(Settings)} is configured only from {@link DynamoDBConfigurations}
 * entries. The builder-related methods of this class do not affect it.</p>
 *
 * @see DynamoDBConfigurations
 */
public class DynamoDBConfiguration {

/**
 * The DynamoDB client builder.
 */
    protected DynamoDbClientBuilder builder = DynamoDbClient.builder();
/**
 * The asynchronous DynamoDB client builder.
 */
    protected DynamoDbAsyncClientBuilder builderAsync = DynamoDbAsyncClient.builder();


/**
 * Replaces the builder held by this configuration. It is not used by {@code apply(Settings)}, which builds
 * the client from {@link DynamoDBConfigurations}.
 *
 * @param builder the DynamoDB client builder
 * @throws NullPointerException if {@code builder} is {@code null}
 */
    public void syncBuilder(DynamoDbClientBuilder builder)  {
        requireNonNull(builder, "builder is required");
        this.builder = builder;
    }

/**
 * Replaces the asynchronous builder held by this configuration. It is not used by {@code apply(Settings)}.
 *
 * @param builderAsync the asynchronous DynamoDB client builder
 * @throws NullPointerException if {@code builderAsync} is {@code null}
 */
    public void asyncBuilder(DynamoDbAsyncClientBuilder builderAsync)  {
        requireNonNull(builderAsync, "asyncBuilder is required");
        this.builderAsync = builderAsync;
    }

/**
 * Overrides the endpoint of the builders held by this configuration. It is not used by
 * {@code apply(Settings)}; to change the endpoint of the created client, set
 * {@link DynamoDBConfigurations#ENDPOINT}.
 *
 * @param endpoint the endpoint URI, such as {@code http://localhost:8000}
 * @throws IllegalArgumentException if {@code endpoint} is not a valid URI
 */
    public void setEndPoint(String endpoint) {
        builder.endpointOverride(URI.create(endpoint));
        builderAsync.endpointOverride(URI.create(endpoint));
    }

/**
 * Returns the get dynamo db.
 *
 * @param settings the settings
 * @return the result
 */
    protected DynamoDbClient getDynamoDB(Settings settings) {
        DynamoDBBuilderSync dynamoDB = new DynamoDBBuilderSync();
        DynamoDBBuilders.load(settings, dynamoDB);
        return dynamoDB.build();
    }

/**
 * Returns the get dynamo dbasync.
 *
 * @param settings the settings
 * @return the result
 */
    protected DynamoDbAsyncClient getDynamoDBAsync(Settings settings) {
        DynamoDBBuilderASync dynamoDB = new DynamoDBBuilderASync();
        DynamoDBBuilders.load(settings, dynamoDB);
        return dynamoDB.build();
    }
}

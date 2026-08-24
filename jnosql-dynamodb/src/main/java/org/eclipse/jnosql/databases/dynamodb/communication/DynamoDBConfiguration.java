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


public class DynamoDBConfiguration {

    protected DynamoDbClientBuilder builder = DynamoDbClient.builder();
    protected DynamoDbAsyncClientBuilder builderAsync = DynamoDbAsyncClient.builder();


    public void syncBuilder(DynamoDbClientBuilder builder)  {
        requireNonNull(builder, "builder is required");
        this.builder = builder;
    }

    public void asyncBuilder(DynamoDbAsyncClientBuilder builderAsync)  {
        requireNonNull(builderAsync, "asyncBuilder is required");
        this.builderAsync = builderAsync;
    }

    public void setEndPoint(String endpoint) {
        builder.endpointOverride(URI.create(endpoint));
        builderAsync.endpointOverride(URI.create(endpoint));
    }

    protected DynamoDbClient getDynamoDB(Settings settings) {
        DynamoDBBuilderSync dynamoDB = new DynamoDBBuilderSync();
        DynamoDBBuilders.load(settings, dynamoDB);
        return dynamoDB.build();
    }

    protected DynamoDbAsyncClient getDynamoDBAsync(Settings settings) {
        DynamoDBBuilderASync dynamoDB = new DynamoDBBuilderASync();
        DynamoDBBuilders.load(settings, dynamoDB);
        return dynamoDB.build();
    }
}
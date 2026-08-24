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

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;

import java.net.URI;

/**
 * Provides dynamo dbbuilder async support.
 */
public class DynamoDBBuilderASync implements DynamoDBBuilder {

    private final DynamoDbAsyncClientBuilder dynamoDB = DynamoDbAsyncClient.builder();

    private String awsAccessKey;
    private String awsSecretAccess;

    @Override
    public void endpoint(String endpoint) {
        dynamoDB.endpointOverride(URI.create(endpoint));

    }

    @Override
    public void region(String region) {
        dynamoDB.region(Region.of(region));
    }

    @Override
    public void profile(String profile) {
        dynamoDB.credentialsProvider(ProfileCredentialsProvider.builder()
                .profileName(profile)
                .build());

    }

/**
 * Returns the build.
 *
 * @return the result
 */
    public DynamoDbAsyncClient build() {

        boolean accessKey = awsAccessKey != null && !awsAccessKey.isEmpty();
        boolean secretAccess = awsSecretAccess != null && !awsSecretAccess.isEmpty();

        if (accessKey && secretAccess) {

            AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(awsAccessKey, awsSecretAccess);
            AwsCredentialsProvider staticCredentialsProvider = StaticCredentialsProvider.create(awsBasicCredentials);
            dynamoDB.credentialsProvider(staticCredentialsProvider);
        }

        return dynamoDB.build();
    }

    @Override
    public void awsAccessKey(String awsAccessKey) {
        this.awsAccessKey = awsAccessKey;
    }

    @Override
    public void awsSecretAccess(String awsSecretAccess) {
        this.awsSecretAccess = awsSecretAccess;
    }
}

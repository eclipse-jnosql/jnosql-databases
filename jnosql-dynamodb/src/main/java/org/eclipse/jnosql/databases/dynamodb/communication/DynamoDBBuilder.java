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

/**
 * Collects the connection options of a DynamoDB client: endpoint, region and credentials. The driver feeds it
 * from {@link DynamoDBConfigurations} entries; {@link DynamoDBBuilderSync} and {@link DynamoDBBuilderASync}
 * build the synchronous and the asynchronous AWS client from it.
 *
 * @see DynamoDBConfigurations
 */
public interface DynamoDBBuilder {

    /**
     * Overrides the service endpoint, for example to target DynamoDB Local.
     *
     * @param endpoint the endpoint URI, such as {@code http://localhost:8000}
     * @throws IllegalArgumentException if {@code endpoint} is not a valid URI
     */
    void endpoint(String endpoint);

    /**
     * Sets the AWS region.
     *
     * @param region the region id, such as {@code us-east-1}
     */
    void region(String region);

    /**
     * Resolves credentials from a named profile of the AWS shared credentials file.
     *
     * @param profile the profile name
     */
    void profile(String profile);

    /**
     * Sets the access key id. Static credentials are used only when both the access key and the secret key are
     * set and not empty; they then replace the {@link #profile(String) profile} credentials.
     *
     * @param awsAccessKey the AWS access key id
     */
    void awsAccessKey(String awsAccessKey);

    /**
     * Sets the secret access key. See {@link #awsAccessKey(String)} for when static credentials apply.
     *
     * @param awsSecretAccess the AWS secret access key
     */
    void awsSecretAccess(String awsSecretAccess);

}

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

import java.util.function.Supplier;

/**
 * Configuration keys read by the DynamoDB drivers. Each constant is a {@link Supplier} of its property name,
 * so it can be used directly as a key of {@link org.eclipse.jnosql.communication.Settings}.
 *
 * <p>Example:</p>
 * <pre>{@code
 * Settings settings = Settings.builder()
 *         .put(DynamoDBConfigurations.REGION, "us-east-1")
 *         .put(DynamoDBConfigurations.CREATE_TABLES, true)
 *         .build();
 * }</pre>
 *
 * @see DynamoDBDocumentConfiguration
 * @see DynamoDBKeyValueConfiguration
 */
public enum  DynamoDBConfigurations implements Supplier<String> {

/**
 * The endpoint that overrides the default AWS endpoint, such as {@code http://localhost:8000} for a local
 * DynamoDB.
 */
    ENDPOINT("jnosql.dynamodb.endpoint"),
/**
 * The AWS region, such as {@code us-east-1}.
 */
    REGION("jnosql.dynamodb.region"),
/**
 * The name of the AWS credentials profile used to authenticate.
 */
    PROFILE("jnosql.dynamodb.profile"),
/**
 * The AWS access key. Static credentials are used only when both this key and {@link #AWS_SECRET_ACCESS}
 * are defined, and they take precedence over {@link #PROFILE}.
 */
    AWS_ACCESSKEY("jnosql.dynamodb.awsaccesskey"),
/**
 * The AWS secret access key, used together with {@link #AWS_ACCESSKEY}.
 */
    AWS_SECRET_ACCESS("jnosql.dynamodb.secretaccess"),
/**
 * The name of the partition key attribute that holds the entity name in every item. Defaults to
 * {@code @entity}.
 */
    ENTITY_PARTITION_KEY("jnosql.dynamodb.entity.pk"),
/**
 * Whether the document driver creates a table that does not exist yet. Defaults to {@code false}, so a missing
 * table fails the operation.
 */
    CREATE_TABLES("jnosql.dynamodb.create.tables");

    private final String configuration;

    DynamoDBConfigurations(String configuration) {
        this.configuration = configuration;
    }

    /**
     * Returns the property name of this configuration.
     *
     * @return the property name
     */
    @Override
    public String get() {
        return configuration;
    }
}

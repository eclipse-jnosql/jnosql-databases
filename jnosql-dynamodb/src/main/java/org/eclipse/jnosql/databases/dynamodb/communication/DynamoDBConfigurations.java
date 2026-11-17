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
 * {@snippet lang="java" :
 * Settings settings = Settings.builder()
 *         .put(DynamoDBConfigurations.REGION, "us-east-1")
 *         .put(DynamoDBConfigurations.CREATE_TABLES, true)
 *         .build();
 * }
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
 * Whether the document driver creates a table that does not exist yet. Defaults to {@code false}, so a missing
 * table fails the operation.
 */
    CREATE_TABLES("jnosql.dynamodb.create.tables"),
/**
 * The name of the partition key attribute of an entity table. The property name is a template: replace
 * {@code %s} with the table name, as in {@code jnosql.dynamodb.users.pk}. Defaults to {@code _id}.
 */
    ENTITY_PARTITION_KEY("jnosql.dynamodb.%s.pk"),
/**
 * The provisioned read capacity units of a table created by the driver. The property name is a template:
 * replace {@code %s} with the table name.
 */
    ENTITY_READ_CAPACITY_UNITS("jnosql.dynamodb.%s.read.capacity.units"),
/**
 * The provisioned write capacity units of a table created by the driver. The property name is a template:
 * replace {@code %s} with the table name.
 */
    ENTITY_WRITE_CAPACITY_UNITS("jnosql.dynamodb.%s.write.capacity.units"),
    ;

    private final String configuration;

    DynamoDBConfigurations(String configuration) {
        this.configuration = configuration;
    }

    /**
     * Returns the property name of this configuration. For {@link #ENTITY_PARTITION_KEY},
     * {@link #ENTITY_READ_CAPACITY_UNITS} and {@link #ENTITY_WRITE_CAPACITY_UNITS}, the name contains a
     * {@code %s} placeholder for the table name.
     *
     * @return the property name
     */
    @Override
    public String get() {
        return configuration;
    }
}

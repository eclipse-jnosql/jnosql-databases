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

import java.util.function.Supplier;

/**
 * Defines the dynamo dbconfigurations options.
 */
public enum  DynamoDBConfigurations implements Supplier<String> {

/**
 * Performs the endpoint operation.
 */
    ENDPOINT("jnosql.dynamodb.endpoint"),
/**
 * Performs the region operation.
 */
    REGION("jnosql.dynamodb.region"),
/**
 * Performs the profile operation.
 */
    PROFILE("jnosql.dynamodb.profile"),
/**
 * Performs the aws accesskey operation.
 */
    AWS_ACCESSKEY("jnosql.dynamodb.awsaccesskey"),
/**
 * Performs the aws secret access operation.
 */
    AWS_SECRET_ACCESS("jnosql.dynamodb.secretaccess"),
/**
 * Performs the create tables operation.
 */
    CREATE_TABLES("jnosql.dynamodb.create.tables"),
/**
 * Performs the entity partition key operation.
 */
    ENTITY_PARTITION_KEY("jnosql.dynamodb.%s.pk"),
/**
 * Performs the entity read capacity units operation.
 */
    ENTITY_READ_CAPACITY_UNITS("jnosql.dynamodb.%s.read.capacity.units"),
/**
 * Performs the entity write capacity units operation.
 */
    ENTITY_WRITE_CAPACITY_UNITS("jnosql.dynamodb.%s.write.capacity.units"),
    ;

    private final String configuration;

    DynamoDBConfigurations(String configuration) {
        this.configuration = configuration;
    }

    @Override
    public String get() {
        return configuration;
    }
}

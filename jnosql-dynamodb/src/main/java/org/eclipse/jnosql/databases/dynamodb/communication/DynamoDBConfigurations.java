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

public enum  DynamoDBConfigurations implements Supplier<String> {

    ENDPOINT("jnosql.dynamodb.endpoint"),
    REGION("jnosql.dynamodb.region"),
    PROFILE("jnosql.dynamodb.profile"),
    AWS_ACCESSKEY("jnosql.dynamodb.awsaccesskey"),
    AWS_SECRET_ACCESS("jnosql.dynamodb.secretaccess"),
    CREATE_TABLES("jnosql.dynamodb.create.tables"),
    ENTITY_PARTITION_KEY("jnosql.dynamodb.%s.pk"),
    ENTITY_READ_CAPACITY_UNITS("jnosql.dynamodb.%s.read.capacity.units"),
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

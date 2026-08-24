/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 * and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *
 * Maximillian Arruda
 */

package org.eclipse.jnosql.databases.dynamodb.communication;

import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Optional;

public class DynamoDBDatabaseManagerFactory implements DatabaseManagerFactory {

    private final DynamoDbClient dynamoDB;
    private final Settings settings;

    public DynamoDBDatabaseManagerFactory(DynamoDbClient dynamoDB, Settings settings) {
        this.dynamoDB = dynamoDB;
        this.settings = settings;
    }

    @Override
    public DynamoDBDatabaseManager apply(String database) {
        return new DefaultDynamoDBDatabaseManager(database, dynamoDB, settings);
    }

    @Override
    public void close() {
        Optional.ofNullable(this.dynamoDB).ifPresent(DynamoDbClient::close);
    }
}

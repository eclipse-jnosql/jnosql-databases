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
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;

/**
 * The DynamoDB implementation of {@link DatabaseConfiguration} for the document model. It creates a
 * {@link DynamoDBDatabaseManagerFactory} whose managers store each entity in a DynamoDB table named after it.
 *
 * <p>Example:</p>
 * {@snippet lang="java" :
 * Settings settings = Settings.builder()
 *         .put(DynamoDBConfigurations.ENDPOINT, "http://localhost:8000")
 *         .put(DynamoDBConfigurations.REGION, "us-east-1")
 *         .put(DynamoDBConfigurations.CREATE_TABLES, true)
 *         .build();
 * DynamoDBDatabaseManagerFactory factory = new DynamoDBDocumentConfiguration().apply(settings);
 * DynamoDBDatabaseManager manager = factory.apply("my-database");
 * }
 *
 * @see DynamoDBConfigurations
 */
public class DynamoDBDocumentConfiguration extends DynamoDBConfiguration
        implements DatabaseConfiguration {

            /**
             * Creates a configuration with default settings.
             */
            public DynamoDBDocumentConfiguration() {
            }

    /**
     * {@inheritDoc}
     *
     * <p>The settings are kept by the factory and read again when tables are resolved, so keys such as
     * {@link DynamoDBConfigurations#CREATE_TABLES} apply to every manager it creates.</p>
     *
     * @throws NullPointerException if {@code settings} is {@code null}
     */
    @Override
    public DynamoDBDatabaseManagerFactory apply(Settings settings) {
        var dynamoDB = getDynamoDB(settings);
        return new DynamoDBDatabaseManagerFactory(dynamoDB, settings);
    }
}

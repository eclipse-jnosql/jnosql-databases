/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
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
 * Provides dynamo dbdocument configuration settings.
 */
public class DynamoDBDocumentConfiguration extends DynamoDBConfiguration
        implements DatabaseConfiguration {

            /**
             * Creates a new dynamo dbdocument configuration instance.
             */
            public DynamoDBDocumentConfiguration() {
            }
    @Override
    public DynamoDBDatabaseManagerFactory apply(Settings settings) {
        var dynamoDB = getDynamoDB(settings);
        return new DynamoDBDatabaseManagerFactory(dynamoDB, settings);
    }
}

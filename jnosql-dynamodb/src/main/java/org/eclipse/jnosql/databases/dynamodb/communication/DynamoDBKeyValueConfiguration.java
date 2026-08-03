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

import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.keyvalue.KeyValueConfiguration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Provides dynamo dbkey value configuration settings.
 */
public class DynamoDBKeyValueConfiguration extends DynamoDBConfiguration
        implements KeyValueConfiguration {

            /**
             * Creates a new dynamo dbkey value configuration instance.
             */
            public DynamoDBKeyValueConfiguration() {
            }

    @Override
    public DynamoDBBucketManagerFactory apply(Settings settings) {
        DynamoDbClient dynamoDB = getDynamoDB(settings);
        return new DynamoDBBucketManagerFactory(dynamoDB);
    }

}

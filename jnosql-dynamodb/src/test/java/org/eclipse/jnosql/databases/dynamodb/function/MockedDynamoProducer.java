/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.dynamodb.function;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.interceptor.Interceptor;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.databases.dynamodb.communication.DefaultDynamoDBDatabaseManager;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.DatabaseType;
import org.mockito.Mockito;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.paginators.ScanIterable;

import java.util.Spliterators;

/**
 * Produces the real DynamoDB manager over a mocked SDK client so tests can capture the native requests.
 */
@Alternative
@Priority(Interceptor.Priority.APPLICATION)
@ApplicationScoped
public class MockedDynamoProducer {

    public static final DynamoDbClient CLIENT = Mockito.mock(DynamoDbClient.class);

    public static void reset() {
        Mockito.reset(CLIENT);
        var description = TableDescription.builder().tableName("MockedAccount").build();
        Mockito.when(CLIENT.describeTable(Mockito.any(DescribeTableRequest.class)))
                .thenReturn(DescribeTableResponse.builder().table(description).build());
        var iterable = Mockito.mock(ScanIterable.class);
        Mockito.when(iterable.spliterator()).thenAnswer(i -> Spliterators.emptySpliterator());
        Mockito.when(CLIENT.scanPaginator(Mockito.any(ScanRequest.class))).thenReturn(iterable);
    }

    @Produces
    @Database(DatabaseType.DOCUMENT)
    public DatabaseManager documentManager() {
        reset();
        return new DefaultDynamoDBDatabaseManager("database", CLIENT, Settings.builder().build());
    }
}

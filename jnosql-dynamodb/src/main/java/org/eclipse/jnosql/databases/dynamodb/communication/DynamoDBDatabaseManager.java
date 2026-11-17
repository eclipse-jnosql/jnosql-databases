/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.stream.Stream;

/**
 * The {@link DatabaseManager} of the DynamoDB document driver. Besides the inherited operations, it executes
 * PartiQL statements and exposes the underlying AWS client.
 *
 * <p>Example:</p>
 * <pre>{@code
 * DynamoDBDatabaseManager manager = factory.apply("my-database");
 * Stream<CommunicationEntity> active = manager.partiQL(
 *         "SELECT * FROM users WHERE status = ?", "ACTIVE");
 * }</pre>
 *
 * <p>Obtain an instance from {@link DynamoDBDatabaseManagerFactory}.</p>
 */
public interface DynamoDBDatabaseManager extends DatabaseManager {

    /**
     * Executes a PartiQL statement and returns every matching item. DynamoDB supports a limited subset of
     * <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ql-reference.html">PartiQL</a>.
     *
     * <p>Positional parameters bind to the {@code ?} placeholders in order. Each item becomes a
     * {@link CommunicationEntity} named after the item's entity attribute, so the statement must select it, as
     * {@code SELECT *} does.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * Stream<CommunicationEntity> active = manager.partiQL(
     *         "SELECT * FROM users WHERE status = ?", "ACTIVE");
     * }</pre>
     *
     * @param query the PartiQL statement
     * @param params the values bound to the {@code ?} placeholders
     * @return a {@link Stream} of {@link CommunicationEntity} representing the query result
     * @throws NullPointerException when {@code query} is {@code null}
     */
    Stream<CommunicationEntity> partiQL(String query, Object... params);


    /**
     * Returns the AWS client used by this manager, for operations the driver does not expose. The client is
     * shared with the factory and closed when the factory is closed.
     *
     * @return the {@link DynamoDbClient} instance
     */
    DynamoDbClient dynamoDbClient();


}

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
 * {@snippet lang="java" :
 * DynamoDBDatabaseManager manager = factory.apply("my-database");
 * Stream<CommunicationEntity> active = manager.partiQL(
 *         "SELECT * FROM users WHERE status = ?", "users", "ACTIVE");
 * }
 *
 * <p>Obtain an instance from {@link DynamoDBDatabaseManagerFactory}.</p>
 */
public interface DynamoDBDatabaseManager extends DatabaseManager {

    /**
     * Executes a PartiQL statement and returns every matching item. DynamoDB supports a limited subset of
     * <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ql-reference.html">PartiQL</a>.
     *
     * <p>The result is read completely, following DynamoDB pagination, before the stream is returned; the
     * stream is empty when nothing matches. Positional parameters bind to the {@code ?} placeholders in order.</p>
     *
     * <p>Example:</p>
     * {@snippet lang="java" :
     * Stream<CommunicationEntity> active = manager.partiQL(
     *         "SELECT * FROM users WHERE status = ?", "users", "ACTIVE");
     * }
     *
     * @param query the PartiQL statement
     * @param entityName the name given to every returned {@link CommunicationEntity}, usually the table name
     * @param params the values bound to the {@code ?} placeholders
     * @return a {@link Stream} of {@link CommunicationEntity} representing the query result
     * @throws NullPointerException if {@code query} is {@code null}
     */
    Stream<CommunicationEntity> partiQL(String query, String entityName, Object... params);


    /**
     * Returns the AWS client used by this manager, for operations the driver does not expose. The client is
     * shared with the factory and closed when the factory is closed.
     *
     * @return the {@link DynamoDbClient} instance
     */
    DynamoDbClient dynamoDbClient();


}

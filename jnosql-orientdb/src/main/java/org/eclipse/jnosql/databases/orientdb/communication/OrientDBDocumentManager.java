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
package org.eclipse.jnosql.databases.orientdb.communication;


import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.util.Map;
import java.util.stream.Stream;

/**
 * The OrientDB implementation of {@link DatabaseManager}, bound to one OrientDB database. Besides the common
 * document operations it runs OrientDB SQL and subscribes to live queries.
 *
 * <p>Example:</p>
 * <pre>{@code
 * Stream<CommunicationEntity> adults = manager.sql("SELECT FROM Person WHERE age >= ?", 18);
 * }</pre>
 *
 * <p>TTL is not supported: {@code insert(CommunicationEntity, Duration)} throws
 * {@link UnsupportedOperationException}.</p>
 */
public interface OrientDBDocumentManager extends DatabaseManager {

    /**
     * Executes an OrientDB SQL command with positional parameters.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Stream<CommunicationEntity> result = manager.sql("SELECT FROM Person WHERE name = ?", "Ada");
     * }</pre>
     *
     * @param query  the OrientDB SQL
     * @param params the values bound to the {@code ?} placeholders, in order
     * @return the result rows converted to entities
     * @throws NullPointerException when query is null
     */
    Stream<CommunicationEntity> sql(String query, Object... params);

    /**
     * Executes an OrientDB SQL command with named parameters.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Stream<CommunicationEntity> result = manager.sql("SELECT FROM Person WHERE name = :name",
     *         Map.of("name", "Ada"));
     * }</pre>
     *
     * @param query  the OrientDB SQL
     * @param params the values by name, without the {@code :} prefix
     * @return the result rows converted to entities
     * @throws NullPointerException when either query or params are null
     */
    Stream<CommunicationEntity> sql(String query, Map<String, Object> params);

    /**
     * Subscribes to a live query built from a {@link SelectQuery}. The callbacks are invoked when matching
     * records are created, updated or deleted.
     *
     * @param query     the query to watch
     * @param callbacks the callbacks for create, update and delete events
     * @throws NullPointerException when either query or callbacks are null
     */
    void live(SelectQuery query, OrientDBLiveCallback<CommunicationEntity> callbacks);

    /**
     * Subscribes to a live query written in OrientDB SQL. The callbacks are invoked when matching
     * records are created, updated or deleted.
     *
     * @param query     the OrientDB SQL live query, such as {@code LIVE SELECT FROM Person}
     * @param callbacks the callbacks for create, update and delete events
     * @param params    the values bound to the {@code ?} placeholders, in order
     * @throws NullPointerException when either query or callbacks are null
     */
    void live(String query, OrientDBLiveCallback<CommunicationEntity> callbacks, Object... params);
}

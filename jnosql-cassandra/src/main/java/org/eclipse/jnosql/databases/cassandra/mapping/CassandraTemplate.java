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

package org.eclipse.jnosql.databases.cassandra.mapping;


import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.mapping.column.ColumnTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A Cassandra-specific extension of {@link ColumnTemplate}, providing additional functionality for interacting
 * with a Cassandra database using CQL queries and different consistency levels.
 */
public interface CassandraTemplate extends ColumnTemplate {

    /**
     * Saves an entity with a specified {@link ConsistencyLevel}.
     *
     * @param <T>    the type of the entity
     * @param entity the entity to be saved
     * @param level  the desired {@link ConsistencyLevel} for the operation
     * @return the saved entity
     * @throws NullPointerException if either {@code entity} or {@code level} is {@code null}
     */
    <T> T save(T entity, ConsistencyLevel level);

    /**
     * Saves multiple entities with a specified {@link ConsistencyLevel} and a time-to-live (TTL) duration.
     *
     * @param <T>      the type of the entities
     * @param entities the iterable collection of entities to be saved
     * @param ttl      the time-to-live duration for the records
     * @param level    the desired {@link ConsistencyLevel} for the operation
     * @return an iterable containing the saved entities
     * @throws NullPointerException if {@code entities}, {@code ttl}, or {@code level} is {@code null}
     */
    <T> Iterable<T> save(Iterable<T> entities, Duration ttl, ConsistencyLevel level);

    /**
     * Saves multiple entities with a specified {@link ConsistencyLevel}.
     *
     * @param <T>      the type of the entities
     * @param entities the iterable collection of entities to be saved
     * @param level    the desired {@link ConsistencyLevel} for the operation
     * @return an iterable containing the saved entities
     * @throws NullPointerException if {@code entities} or {@code level} is {@code null}
     */
    <T> Iterable<T> save(Iterable<T> entities, ConsistencyLevel level);

    /**
     * Saves an entity with a specified {@link ConsistencyLevel} and a time-to-live (TTL) duration.
     *
     * @param <T>    the type of the entity
     * @param entity the entity to be saved
     * @param ttl    the time-to-live duration for the record
     * @param level  the desired {@link ConsistencyLevel} for the operation
     * @return the saved entity
     * @throws NullPointerException if {@code entity}, {@code ttl}, or {@code level} is {@code null}
     */
    <T> T save(T entity, Duration ttl, ConsistencyLevel level);

    /**
     * Deletes records based on a {@link DeleteQuery} with a specified {@link ConsistencyLevel}.
     *
     * @param query the delete query defining the criteria for deletion
     * @param level the desired {@link ConsistencyLevel} for the operation
     * @throws NullPointerException if either {@code query} or {@code level} is {@code null}
     */
    void delete(DeleteQuery query, ConsistencyLevel level);

    /**
     * Returns the count of records that match the given {@link SelectQuery} using the specified
     * {@link ConsistencyLevel}.
     *
     * @param query the select query defining the criteria for counting; must not be {@code null}
     * @param level the Cassandra consistency level to use for the operation; must not be {@code null}
     * @return the number of records matching the query
     * @throws NullPointerException if {@code query} or {@code level} is {@code null}
     */
    long count(SelectQuery query, ConsistencyLevel level);

    /**
     * Executes a {@link SelectQuery} using a specified {@link ConsistencyLevel} and retrieves the matching records.
     *
     * @param <T>   the type of the result
     * @param query the select query defining the criteria for data retrieval
     * @param level the desired {@link ConsistencyLevel} for the operation
     * @return a stream of results matching the query criteria
     */
    <T> Stream<T> find(SelectQuery query, ConsistencyLevel level);

    /**
     * Executes a raw CQL query.
     *
     * @param <T>   the type of the result
     * @param query the CQL query to be executed
     * @return a stream containing the results of the query
     * @throws NullPointerException if {@code query} is {@code null}
     */
    <T> Stream<T> cql(String query);

    /**
     * Executes a CQL query with named parameters.
     * <p>
     * Example usage:
     * <pre>{@code
     * template.cql("SELECT * FROM users WHERE id = :id", Map.of("id", 1));
     * }</pre>
     *
     * @param <T>    the type of the result
     * @param query  the CQL query with named placeholders
     * @param values a map of parameter names to values
     * @return a stream containing the results of the query
     * @throws NullPointerException if {@code query} is {@code null}
     */
    <T> Stream<T> cql(String query, Map<String, Object> values);

    /**
     * Executes a CQL query with positional parameters.
     *
     * @param <T>    the type of the result
     * @param query  the CQL query with positional placeholders
     * @param params the values to be bound to the query placeholders
     * @return a stream containing the results of the query
     * @throws NullPointerException if {@code query} is {@code null}
     */
    <T> Stream<T> cql(String query, Object... params);

    /**
     * Executes a {@link SimpleStatement} in Cassandra.
     *
     * @param <T>       the type of the result
     * @param statement the prepared {@link SimpleStatement} to be executed
     * @return a stream containing the results of the query
     * @throws NullPointerException if {@code statement} is {@code null}
     */
    <T> Stream<T> execute(SimpleStatement statement);

}

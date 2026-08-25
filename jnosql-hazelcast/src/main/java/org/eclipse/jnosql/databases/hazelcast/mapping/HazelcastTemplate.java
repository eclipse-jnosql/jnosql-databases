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
package org.eclipse.jnosql.databases.hazelcast.mapping;

import com.hazelcast.query.Predicate;
import org.eclipse.jnosql.mapping.keyvalue.KeyValueTemplate;

import java.util.Collection;
import java.util.Map;

/**
 * A specialized {@link KeyValueTemplate} for Hazelcast,
 * providing methods to execute queries using SQL-like expressions and predicates.
 * This template facilitates querying key-value structures stored in a Hazelcast instance.
 * It supports both SQL-like queries with named parameters and Hazelcast-specific predicates.
 *
 * Example usage:
 * <pre>
 * {@code
 * @Inject
 * private HazelcastTemplate hazelcastTemplate;
 *
 * // Query using SQL-like syntax
 * Collection<Movie> movies = hazelcastTemplate.sql("name = :name", Map.of("name", "Inception"));
 * movies.forEach(System.out::println);
 *
 * // Query using Hazelcast Predicate
 * Predicate<String, Movie> predicate = Predicates.equal("genre", "Sci-Fi");
 * Collection<Movie> sciFiMovies = hazelcastTemplate.sql(predicate);
 * sciFiMovies.forEach(System.out::println);
 * }
 * </pre>
 *
 * @see KeyValueTemplate
 */
public interface HazelcastTemplate extends KeyValueTemplate {

    /**
     * Executes a Hazelcast query using SQL-like syntax.
     * The query should follow Hazelcast's SQL-like query syntax for key-value stores.
     *
     * @param <T>   the entity type
     * @param query the SQL-like query string
     * @return a collection of matching entities
     * @throws NullPointerException if the query is null
     */
    <T> Collection<T> sql(String query);

    /**
     * Executes a Hazelcast query with named parameters.
     * Example usage:
     * <pre>
     * {@code
     * Collection<Movie> movies = hazelcastTemplate.sql("name = :name", Map.of("name", "The Matrix"));
     * }
     * </pre>
     *
     * @param <T>    the entity type
     * @param query  the SQL-like query string
     * @param params a map of named parameters to bind in the query
     * @return a collection of matching entities
     * @throws NullPointerException if the query or params are null
     */
    <T> Collection<T> sql(String query, Map<String, Object> params);

    /**
     * Executes a Hazelcast query using a {@link Predicate}.
     * The predicate-based approach is useful for filtering key-value pairs
     * based on specific criteria.
     *
     * @param <K>       the key type
     * @param <V>       the value type
     * @param predicate the Hazelcast predicate for filtering data
     * @return a collection of values that match the predicate
     * @throws NullPointerException if the predicate is null
     */
    <K, V> Collection<V> sql(Predicate<K, V> predicate);

}

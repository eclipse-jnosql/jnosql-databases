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
package org.eclipse.jnosql.databases.arangodb.mapping;



import org.eclipse.jnosql.mapping.document.DocumentTemplate;
import org.eclipse.jnosql.mapping.graph.GraphTemplate;

import java.util.Map;
import java.util.stream.Stream;
/**
 * A specialized {@link DocumentTemplate} for ArangoDB, providing methods to execute
 * queries using the ArangoDB Query Language (AQL).
 *
 *  This template allows executing AQL queries with named parameters and supports
 * result serialization either through Eclipse JNoSQL or directly via ArangoDB.
 */
public interface ArangoDBTemplate extends DocumentTemplate, GraphTemplate {

    /**
     * Executes an ArangoDB query using the ArangoDB Query Language (AQL).
     *
     * <p>Example query:</p>
     * <pre>{@code
     * FOR u IN users FILTER u.status == @status RETURN u
     * }</pre>
     *
     * <p>The conversion of query results to entity objects is handled by Eclipse JNoSQL,
     * applying all supported annotations.</p>
     *
     * @param <T>    the entity type
     * @param query  the AQL query string
     * @param params a map containing named parameters for the query
     * @return a {@link Stream} of entities representing the query result
     * @throws NullPointerException if {@code query} or {@code params} is {@code null}
     */
    <T> Stream<T> aql(String query, Map<String, Object> params);

    /**
     * Executes an ArangoDB query using AQL with direct serialization via ArangoDB.
     *
     * <p>Example query:</p>
     * <pre>{@code
     * FOR u IN users FILTER u.status == @status RETURN u
     * }</pre>
     *
     * <p>The serialization of query results is performed directly by ArangoDB using
     * {@link com.arangodb.ArangoDatabase#query(String, Class)}, bypassing Eclipse JNoSQL
     * converters. Consequently, annotations supported by Eclipse JNoSQL are ignored.</p>
     *
     * @param <T>    the expected result type
     * @param query  the AQL query string
     * @param params a map containing named parameters for the query
     * @param type   the target class for result serialization
     * @return a {@link Stream} of results of type {@code T}
     * @throws NullPointerException if {@code query}, {@code params}, or {@code type} is {@code null}
     */
    <T> Stream<T> aql(String query, Map<String, Object> params, Class<T> type);

    /**
     * Executes an ArangoDB query using AQL with an empty parameter map.
     *
     * <p>Example query:</p>
     * <pre>{@code
     * FOR u IN users FILTER u.status == @status RETURN u
     * }</pre>
     *
     * <p>The serialization of query results is performed directly by ArangoDB using
     * {@link com.arangodb.ArangoDatabase#query(String, Class)}. This means that
     * Eclipse JNoSQL annotations will not be considered.</p>
     *
     * @param <T>   the expected result type
     * @param query the AQL query string
     * @param type  the target class for result serialization
     * @return a {@link Stream} of results of type {@code T}
     * @throws NullPointerException if {@code query} or {@code type} is {@code null}
     */
    <T> Stream<T> aql(String query, Class<T> type);
}

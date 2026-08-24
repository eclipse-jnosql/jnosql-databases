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
package org.eclipse.jnosql.databases.couchbase.mapping;


import com.couchbase.client.java.json.JsonObject;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;

import java.util.stream.Stream;

/**
 * A Couchbase-specific extension of {@link DocumentTemplate} that enables querying using N1QL.
 * <p>
 * This interface provides methods to execute Couchbase's N1QL queries, allowing dynamic parameterized
 * queries and plain queries.
 * </p>
 *
 * Example Usage
 * <pre>{@code
 * @Inject
 * private CouchbaseTemplate template;
 *
 * // Query with named parameters
 * JsonObject params = JsonObject.create().put("status", "active");
 * Stream<User> activeUsers = template.n1qlQuery("SELECT * FROM users WHERE status = $status", params);
 *
 * // Plain query execution
 * Stream<User> allUsers = template.n1qlQuery("SELECT * FROM users");
 * }</pre>
 */
public interface CouchbaseTemplate extends DocumentTemplate {


    /**
     * Executes an N1QL query with named parameters and returns the query result.
     * Example query:
     * {@code SELECT * FROM users WHERE status = $status}
     *
     * @param <T>       the entity type
     * @param n1qlQuery the N1QL query to execute
     * @param params    the parameters for the query
     * @return a {@link Stream} of entities representing the query result
     * @throws NullPointerException if either {@code n1qlQuery} or {@code params} is null
     */
    <T> Stream<T> n1qlQuery(String n1qlQuery, JsonObject params);

    /**
     * Executes a plain N1QL query without parameters and returns the query result.
     * Example query:
     * {@code SELECT * FROM users}
     *
     * @param <T>       the entity type
     * @param n1qlQuery the N1QL query to execute
     * @return a {@link Stream} of entities representing the query result
     * @throws NullPointerException if {@code n1qlQuery} is null
     */
    <T> Stream<T> n1qlQuery(String n1qlQuery);


}

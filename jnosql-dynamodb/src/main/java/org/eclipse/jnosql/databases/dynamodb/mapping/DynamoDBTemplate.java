/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.jnosql.databases.dynamodb.mapping;

import org.eclipse.jnosql.mapping.document.DocumentTemplate;

import java.util.stream.Stream;

/**
 * The {@code DynamoDBTemplate} is an interface that extends {@link DocumentTemplate} and
 * provides methods for executing Dynamo DB queries using the PartiQL Language.
 * <p>DynamoDB supports a limited subset of
 * <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ql-reference.html">PartiQL</a>.
 * </p>
 * <p>
 * It allows you to interact with the DynamoDB database using PartiQL queries to retrieve and
 * process data in a more flexible and customizable way.
 * </p>
 *
 * @see DocumentTemplate
 */
public interface DynamoDBTemplate extends DocumentTemplate {

    /**
     * Executes a DynamoDB query using
     * <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ql-reference.html">PartiQL</a>.
     *
     * @param query the PartiQL query
     * @param entityType the class of the result entity type
     * @return a {@link Stream} of results representing the query result
     * @throws NullPointerException  when the query is null
     */
    default <T> Stream<T> partiQL(String query, Class<T> entityType){
        return partiQL(query, entityType, new Object[0]);
    }

    /**
     * Executes a DynamoDB query using
     * <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ql-reference.html">PartiQL</a> with parameters.
     * <p>Example query: {@code SELECT * FROM users WHERE status = ?}</p>
     *
     * @param query the PartiQL query
     * @param entityType the class of the result entity type
     * @return a {@link Stream} of results representing the query result
     * @throws NullPointerException  when the query is null
     */
    <T> Stream<T> partiQL(String query, Class<T> entityType, Object... params);
}

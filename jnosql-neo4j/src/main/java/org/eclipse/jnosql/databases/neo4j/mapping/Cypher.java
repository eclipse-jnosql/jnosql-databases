/*
 *  Copyright (c) 2025 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.neo4j.mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation for defining Cypher queries in Neo4J repositories.
 * This annotation allows users to specify a Cypher query directly on repository methods,
 * enabling custom query execution within {@code Neo4JRepository}.
 * Example usage:
 * <pre>
 * {@code
 * @Cypher("MATCH (n:Person) WHERE n.name = $name RETURN n")
 * List<Person> findByName(@Param("name") String name);
 * }
 * </pre>
 *
 * The {@code value} attribute should contain a valid Cypher query. Query parameters
 * can be defined using the {@code $parameterName} syntax, which will be replaced by
 * method parameters annotated with {@code @Param}.
 *
 * @see Neo4JRepository
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Cypher {

    /**
     * The Cypher query to be executed.
     *
     * @return The Cypher query string.
     */

    String value();
}
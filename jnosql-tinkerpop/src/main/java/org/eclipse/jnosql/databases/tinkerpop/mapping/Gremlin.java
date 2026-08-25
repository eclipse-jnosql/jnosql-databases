/*
 *  Copyright (c) 2025 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.tinkerpop.mapping;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method executes a Gremlin query using TinkerPop.
 * The query will be executed against a graph database supporting the TinkerPop API.
 *
 * Example usage:
 * <pre>
 * {@code
 * @Gremlin("g.V().hasLabel('Book').has('title', title)")
 * List<Book> findByTitle(@Param("title") String title);
 * }
 * </pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Gremlin {

    /**
     * The Gremlin query to execute.
     * Supports parameter substitution using method parameters annotated with @Param.
     *
     * @return the Gremlin query
     */
    String value();
}

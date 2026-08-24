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
package org.eclipse.jnosql.databases.arangodb.mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define a dynamic AQL (ArangoDB Query Language) query for methods
 * in the {@link ArangoDBRepository} interface.
 * This annotation enables executing custom AQL queries directly from repository methods,
 * similar to how queries are defined in other JNoSQL repositories.
 *
 * Example usage:
 * <pre>{@code
 * @AQL("FOR p IN Person RETURN p")
 * List<Person> findAll();
 * }</pre>
 *
 *Parameterized query:
 * <pre>{@code
 * @AQL("FOR p IN Person FILTER p.name == @name RETURN p")
 * List<Person> findByName(@Param("name") String name);
 * }</pre>
 *
 * @see ArangoDBRepository
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AQL {

    /**
     * The AQL query string to be executed.
     *
     * @return the AQL query
     */
    String value();
}

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
package org.eclipse.jnosql.databases.cassandra.mapping;

import org.eclipse.jnosql.mapping.ProviderQuery;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to define a dynamic CQL query method in {@link CassandraRepository}.
 * Methods annotated with {@code @CQL} allow the execution of custom Cassandra Query Language (CQL) statements
 * within repository interfaces.
 * Example usage:
 * <pre>{@code
 * @CQL("SELECT * FROM users WHERE username = :username")
 * List<User> findByUsername(@Param("username") String username);
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@ProviderQuery("cql")
public @interface CQL {

    /**
     * The CQL query string to be executed.
     *
     * @return the CQL query string
     */
    String value();
}
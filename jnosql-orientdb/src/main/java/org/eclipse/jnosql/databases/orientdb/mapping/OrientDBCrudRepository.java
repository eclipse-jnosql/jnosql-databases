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
package org.eclipse.jnosql.databases.orientdb.mapping;


import org.eclipse.jnosql.mapping.NoSQLRepository;

/**
 * A repository interface for OrientDB that extends {@link NoSQLRepository}.
 * <p>
 * This interface allows interaction with OrientDB as a document-oriented NoSQL database,
 * supporting standard CRUD operations and custom queries using the {@link SQL} annotation.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @Repository
 * public interface UserRepository extends OrientDBCrudRepository<User, String> {
 *
 *     @SQL("SELECT FROM User WHERE age > :age")
 *     List<User> findUsersByAge(@Param("age") int age);
 *
 *     @SQL("SELECT FROM User WHERE name = :name")
 *     List<User> findByName(@Param("name") String name);
 * }
 * }
 * </pre>
 *
 * @param <T> the entity type
 * @param <K> the entity ID type
 */
public interface OrientDBCrudRepository<T, K> extends NoSQLRepository<T, K> {
}

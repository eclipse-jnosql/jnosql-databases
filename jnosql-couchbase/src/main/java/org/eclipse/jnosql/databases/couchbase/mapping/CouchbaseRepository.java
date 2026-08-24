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


import org.eclipse.jnosql.mapping.NoSQLRepository;

/**
 * A Couchbase-specific extension of {@link NoSQLRepository}.
 * This repository interface provides built-in CRUD operations and supports dynamic queries
 * using the {@link N1QL} annotation for executing Couchbase N1QL queries.
 *
 * Example Usage
 * <pre>{@code
 * @Repository
 * interface UserRepository extends CouchbaseRepository<User, String> {
 *
 *     @N1QL("SELECT * FROM users WHERE name = $name")
 *     List<User> findByName(@Param("name") String name);
 *
 *     @N1QL("SELECT * FROM users WHERE age > $age")
 *     List<User> findByAgeGreaterThan(@Param("age") int age);
 * }
 * }</pre>
 *
 * @param <T> the entity type
 * @param <K> the entity ID type
 */
public interface CouchbaseRepository<T, K> extends NoSQLRepository<T, K> {
}

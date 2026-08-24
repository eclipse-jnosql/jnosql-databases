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


import org.eclipse.jnosql.mapping.NoSQLRepository;

/**
 * A Cassandra-specific extension of {@link NoSQLRepository}, providing repository-style data access.
 * This interface extends the generic {@link NoSQLRepository}, allowing seamless integration with Cassandra's
 * schema-less NoSQL database model while leveraging query capabilities provided by {@link CQL}.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @Repository
 * public interface UserRepository extends CassandraRepository<User, String> {
 *
 *     @CQL("SELECT * FROM users WHERE username = :username")
 *     List<User> findByUsername(@Param("username") String username);
 *
 *     @CQL("DELETE FROM users WHERE id = :id")
 *     void deleteById(@Param("id") String id);
 * }
 * }</pre>
 *
 * @param <T> the entity type
 * @param <K> the primary key type of the entity
 */
public interface CassandraRepository<T, K> extends NoSQLRepository<T, K> {

}

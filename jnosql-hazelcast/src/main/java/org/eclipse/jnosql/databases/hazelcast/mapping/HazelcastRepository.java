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
package org.eclipse.jnosql.databases.hazelcast.mapping;


import org.eclipse.jnosql.mapping.NoSQLRepository;

/**
 * A Hazelcast-specific extension of {@link NoSQLRepository}, providing
 * key-value data storage and retrieval using Hazelcast.
 * This repository interface allows for defining custom queries using
 * {@link Query} annotations and enables CRUD operations for entities.
 *
 * Example usage:
 * <pre>
 * {@code
 * @Repository
 * interface ProductRepository extends HazelcastRepository<Product, String> {
 *
 *     @Query("category = :category")
 *     Set<Product> findByCategory(@Param("category") String category);
 * }
 * }
 * </pre>
 *
 * @param <T> the entity type
 * @param <K> the identifier type
 * @see Query
 * @see NoSQLRepository
 */
public interface HazelcastRepository<T, K> extends NoSQLRepository<T, K> {
}

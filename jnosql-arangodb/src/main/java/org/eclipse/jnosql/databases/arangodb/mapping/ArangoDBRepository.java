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


import org.eclipse.jnosql.mapping.NoSQLRepository;


/**
 * A repository interface for ArangoDB, extending the generic {@link NoSQLRepository}.
 * This repository supports executing custom AQL queries via the {@link AQL} annotation.
 *
 * Example usage:
 * <pre>{@code
 * @Repository
 * public interface PersonRepository extends ArangoDBRepository<Person, String> {
 *
 *     @AQL("FOR p IN Person RETURN p")
 *     List<Person> findAll();
 *
 *     @AQL("FOR p IN Person FILTER p.name == @name RETURN p")
 *     List<Person> findByName(@Param("name") String name);
 * }
 * }</pre>
 *
 * @param <T> the entity type
 * @param <K> the entity ID type
 * @see AQL
 */
public interface ArangoDBRepository<T, K> extends NoSQLRepository<T, K> {
}

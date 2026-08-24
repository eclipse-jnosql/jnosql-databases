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
package org.eclipse.jnosql.databases.neo4j.mapping;


import org.eclipse.jnosql.mapping.NoSQLRepository;

/**
 * A repository interface for interacting with Neo4J databases using the Jakarta Data API.
 * <p>
 * This interface extends {@link NoSQLRepository}, providing
 * generic CRUD operations for entities stored in a Neo4J database.
 * It also allows defining custom Cypher queries using the {@link Cypher} annotation.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @Repository
 * public interface PersonRepository extends Neo4JRepository<Person, String> {
 *
 *     @Cypher("MATCH (p:Person) WHERE p.name = $name RETURN p")
 *     List<Person> findByName(@Param("name") String name);
 * }
 * }
 * </pre>
 *
 * @param <T> the entity type representing nodes in the Neo4J database.
 * @param <K> the entity ID type, typically a {@link String} corresponding to the element ID.
 *
 * @see NoSQLRepository
 * @see Cypher
 */
public interface Neo4JRepository<T, K> extends NoSQLRepository<T, K> {

}

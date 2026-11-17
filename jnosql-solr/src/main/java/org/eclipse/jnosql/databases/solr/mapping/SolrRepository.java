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
package org.eclipse.jnosql.databases.solr.mapping;


import org.eclipse.jnosql.mapping.NoSQLRepository;

/**
 * A {@link NoSQLRepository} for Apache Solr entities. Besides the inherited operations, its methods can be
 * annotated with {@link Solr} to run a Solr query.
 *
 * <pre>{@code
 * @Repository
 * public interface PersonRepository extends SolrRepository<Person, String> {
 *
 *     @Solr("name:@name AND _entity:person")
 *     List<Person> findByName(@Param("name") String name);
 * }
 * }</pre>
 *
 * @param <T> the entity type
 * @param <K> the entity id type
 * @see Solr
 * @see Param
 */
public interface SolrRepository<T, K> extends NoSQLRepository<T, K> {
}

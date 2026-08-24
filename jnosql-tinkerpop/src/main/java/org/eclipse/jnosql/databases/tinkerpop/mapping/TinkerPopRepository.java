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

import org.eclipse.jnosql.mapping.NoSQLRepository;

/**
 * A repository interface for executing {@link Gremlin} queries
 * using Apache TinkerPop (Gremlin).
 * <p>
 * This interface is meant to be extended by user-defined repositories that want to execute
 * Gremlin traversals against a graph database such as JanusGraph, Neptune, or TinkerGraph.
 *
 * <p>
 * Example usage:
 * <pre>{@code
 * public interface BookRepository extends TinkerPopRepository<Book, String> {
 *
 *     @Gremlin("g.V().hasLabel('Book').has('title', @title)")
 *     List<Book> findByTitle(@Param("title") String title);
 * }
 * }</pre>
 *
 * @param <T>  the entity type
 * @param <ID> the ID type
 */
public interface TinkerPopRepository <T, ID> extends NoSQLRepository<T, ID> {
}

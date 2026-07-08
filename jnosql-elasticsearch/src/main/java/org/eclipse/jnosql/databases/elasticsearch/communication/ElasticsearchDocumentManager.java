/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.elasticsearch.communication;


import co.elastic.clients.elasticsearch.core.SearchRequest;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;

import java.util.stream.Stream;

/**
 * Elasticsearch-specific {@link DatabaseManager} contract for document communication.
 * <p>
 * This interface extends the standard JNoSQL {@link DatabaseManager} operations
 * with Elasticsearch-native search support. The inherited methods provide the
 * regular document operations such as insert, update, delete, select, count, and
 * close, while {@link #search(SearchRequest)} allows callers to execute a native
 * Elasticsearch {@link SearchRequest}.
 * </p>
 * <p>
 * The native search method is useful when an application needs Elasticsearch
 * features that are not directly represented by the JNoSQL communication API,
 * such as custom query DSL structures, aggregations, highlighting, sorting,
 * pagination, or other Elasticsearch-specific search capabilities.
 * </p>
 * <p>
 * Implementations are expected to execute the request against the index managed
 * by this {@link DatabaseManager} and convert the Elasticsearch search hits back
 * into {@link CommunicationEntity} instances.
 * </p>
 *
 * @see DatabaseManager
 * @see SearchRequest
 * @see CommunicationEntity
 */
public interface ElasticsearchDocumentManager extends DatabaseManager {

    /**
     * Find entities from {@link SearchRequest}
     *
     * @param query the search request builder
     * @return the objects from search
     * @throws NullPointerException when the search request builder is null
     */
    Stream<CommunicationEntity> search(SearchRequest query) throws NullPointerException;

}

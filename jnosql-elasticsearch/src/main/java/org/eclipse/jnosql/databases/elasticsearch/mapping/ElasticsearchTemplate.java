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
 */
package org.eclipse.jnosql.databases.elasticsearch.mapping;


import co.elastic.clients.elasticsearch.core.SearchRequest;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;

import java.util.stream.Stream;

/**
 * Elasticsearch-specific {@link DocumentTemplate} contract for the mapping layer.
 * <p>
 * This interface extends the standard Eclipse JNoSQL {@link DocumentTemplate}
 * operations with support for native Elasticsearch search requests. The inherited
 * template methods provide the regular mapped document operations, while
 * {@link #search(SearchRequest)} allows callers to execute an Elasticsearch
 * {@link SearchRequest} and receive mapped entity instances.
 * </p>
 * <p>
 * The native search method is useful when an application needs Elasticsearch
 * features that are not directly represented by the standard JNoSQL mapping API,
 * such as custom query DSL structures, aggregations, highlighting, sorting,
 * pagination, or other Elasticsearch-specific search capabilities.
 * </p>
 * <p>
 * Implementations are expected to delegate the native request to the underlying
 * Elasticsearch communication layer and map the returned documents back to the
 * corresponding entity type managed by Eclipse JNoSQL.
 * </p>
 *
 * <pre>{@code
 * @Inject
 * private ElasticsearchTemplate elasticsearchTemplate;
 *
 * SearchRequest request = new SearchRequest.Builder()
 *         .index("documents")
 *         .query(query -> query.match(match -> match
 *                 .field("title")
 *                 .query("Eclipse JNoSQL")))
 *         .build();
 *
 * Stream<Document> documents = elasticsearchTemplate.search(request);
 * documents.forEach(System.out::println);
 * }</pre>
 *
 * @see DocumentTemplate
 * @see SearchRequest
 */
public interface ElasticsearchTemplate extends DocumentTemplate {

    /**
     * Executes a search query using the provided {@link SearchRequest}.
     * The search query should be built using Elasticsearch's client API and passed
     * to this method. The results will be mapped to the specified entity type
     * and returned as a stream.
     *
     * @param <T>   the entity type
     * @param query the Elasticsearch query request
     * @return a stream of entities resulting from the search query
     * @throws NullPointerException if the query is null
     */
    <T> Stream<T> search(SearchRequest query);
}

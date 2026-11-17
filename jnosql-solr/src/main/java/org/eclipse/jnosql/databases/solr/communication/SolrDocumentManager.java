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
package org.eclipse.jnosql.databases.solr.communication;

import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * The Apache Solr implementation of {@link DatabaseManager}, bound to one Solr collection or core. It adds
 * native Solr queries to the common document operations.
 *
 * <p>Example:</p>
 * {@snippet lang="java" :
 * List<CommunicationEntity> result = manager.solr("title:java AND year:@year", Map.of("year", 2023));
 * }
 *
 * <p>TTL is not supported: {@link #insert(CommunicationEntity, Duration)} throws
 * {@link UnsupportedOperationException}.</p>
 */
public interface SolrDocumentManager extends DatabaseManager {

    /**
     * Executes a Solr native query. The text is sent as the Solr {@code q} parameter.
     *
     * <p>Example:</p>
     * {@snippet lang="java" :
     * List<CommunicationEntity> result = manager.solr("title:java");
     * }
     *
     * @param query the query
     * @return the matching documents
     * @throws NullPointerException when query is null
     * @throws SolrException        when Solr fails to execute the query
     */
    List<CommunicationEntity> solr(String query);

    /**
     * Executes a Solr native query with parameters. Each {@code @name} in the query is replaced by the
     * escaped value of the entry with that name in {@code params}.
     *
     * <p>Example:</p>
     * {@snippet lang="java" :
     * List<CommunicationEntity> result = manager.solr("title:@title", Map.of("title", "java"));
     * }
     *
     * @param query  the query
     * @param params the values by parameter name, without the {@code @} prefix
     * @return the matching documents
     * @throws NullPointerException when query, params, or any parameter name or value is null
     * @throws SolrException        when Solr fails to execute the query
     */
    List<CommunicationEntity> solr(String query, Map<String, ?> params);
}

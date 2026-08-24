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


import org.eclipse.jnosql.mapping.document.DocumentTemplate;

import java.util.List;
import java.util.Map;

/**
 * A {@link DocumentTemplate} to Apache Solr
 */
public interface SolrTemplate extends DocumentTemplate {

    /**
     * Executes a Solr native query
     *
     * @param <T>   the entity type
     * @param query the query
     * @return the result
     * @throws NullPointerException when query is null
     */
    <T> List<T> solr(String query);

    /**
     * Executes a Solr native query with params.
     *
     * @param <T>    the entity type
     * @param query  the query
     * @param params the params
     * @return the result
     * @throws NullPointerException when there is null parameter
     */
    <T> List<T> solr(String query, Map<String, ?> params);
}

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


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.solr.communication.SolrDocumentManager;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.semistructured.AbstractSemiStructuredTemplate;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.semistructured.EventPersistManager;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * The Default implementation of {@link SolrTemplate}
 */
@Typed(SolrTemplate.class)
@ApplicationScoped
class DefaultSolrTemplate extends AbstractSemiStructuredTemplate implements SolrTemplate {

    private final Instance<SolrDocumentManager> manager;

    private final EntityConverter converter;

    private final EventPersistManager persistManager;

    private final EntitiesMetadata entities;

    private final Converters converters;

    @Inject
    DefaultSolrTemplate(Instance<SolrDocumentManager> manager,
                        EntityConverter converter,
                        EventPersistManager persistManager,
                        EntitiesMetadata entities,
                        Converters converters) {
        this.manager = manager;
        this.converter = converter;
       this.persistManager = persistManager;
        this.entities = entities;
        this.converters = converters;
    }

    DefaultSolrTemplate() {
        this(null, null, null, null, null);
    }

    @Override
    protected EntityConverter converter() {
        return converter;
    }

    @Override
    protected SolrDocumentManager manager() {
        return manager.get();
    }

    @Override
    protected EventPersistManager eventManager() {
        return persistManager;
    }

    @Override
    protected EntitiesMetadata entities() {
        return entities;
    }

    @Override
    protected Converters converters() {
        return converters;
    }

    @Override
    public <T> List<T> solr(String query) {
        requireNonNull(query, "query is required");
        return manager.get().solr(query).stream()
                .map(converter::toEntity)
                .map(d -> (T) d)
                .collect(Collectors.toList());
    }

    @Override
    public <T> List<T> solr(String query, Map<String, ?> params) {
        requireNonNull(query, "query is required");
        requireNonNull(params, "params is required");
        return manager.get().solr(query, params).stream()
                .map(converter::toEntity)
                .map(d -> (T) d)
                .collect(Collectors.toList());
    }
}

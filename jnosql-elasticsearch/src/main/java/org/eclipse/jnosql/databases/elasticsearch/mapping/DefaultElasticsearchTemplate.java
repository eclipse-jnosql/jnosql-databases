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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.databases.elasticsearch.communication.ElasticsearchDocumentManager;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.semistructured.AbstractSemiStructuredTemplate;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.semistructured.EventPersistManager;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * The Default implementation of {@link ElasticsearchTemplate}
 */

@Typed(ElasticsearchTemplate.class)
@ApplicationScoped
class DefaultElasticsearchTemplate extends AbstractSemiStructuredTemplate
        implements ElasticsearchTemplate {

    private final Instance<ElasticsearchDocumentManager> manager;

    private final EntityConverter converter;

    private final EventPersistManager eventManager;

    private final  EntitiesMetadata entities;

    private final  Converters converters;

    @Inject
    DefaultElasticsearchTemplate(Instance<ElasticsearchDocumentManager> manager,
                            EntityConverter converter,
                            EventPersistManager eventManager,
                            EntitiesMetadata entities,
                            Converters converters) {
        this.manager = manager;
        this.converter = converter;
        this.eventManager = eventManager;
        this.entities = entities;
        this.converters = converters;
    }

    DefaultElasticsearchTemplate() {
        this(null, null, null, null, null);
    }

    @Override
    protected EntityConverter converter() {
        return converter;
    }

    @Override
    protected DatabaseManager manager() {
        return manager.get();
    }

    @Override
    protected EventPersistManager eventManager() {
        return eventManager;
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
    public <T> Stream<T> search(SearchRequest request) {
        Objects.requireNonNull(request, "request is required");
        Stream<CommunicationEntity> entities = manager.get().search(request);
        return entities.map(converter::toEntity).map(e -> (T) e);
    }
}

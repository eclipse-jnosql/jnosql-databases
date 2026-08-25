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
package org.eclipse.jnosql.databases.orientdb.mapping;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.databases.orientdb.communication.OrientDBDocumentManager;
import org.eclipse.jnosql.databases.orientdb.communication.OrientDBLiveCallback;
import org.eclipse.jnosql.databases.orientdb.communication.OrientDBLiveCallbackBuilder;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.semistructured.AbstractSemiStructuredTemplate;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.semistructured.EventPersistManager;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * The Default implementation of {@link OrientDBTemplate}
 */
@Typed(OrientDBTemplate.class)
@ApplicationScoped
class DefaultOrientDBTemplate extends AbstractSemiStructuredTemplate
        implements OrientDBTemplate {

    private final Instance<OrientDBDocumentManager> manager;

    private final EntityConverter converter;

    private final EventPersistManager persistManager;

    private final EntitiesMetadata entities;

    private final Converters converters;

    @Inject
    DefaultOrientDBTemplate(Instance<OrientDBDocumentManager> manager,
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

    DefaultOrientDBTemplate() {
        this(null, null, null, null, null);
    }

    @Override
    protected EntityConverter converter() {
        return converter;
    }

    @Override
    protected OrientDBDocumentManager manager() {
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
    public <T> Stream<T> sql(String query, Object... params) {
        return manager.get().sql(query, params).map(converter::toEntity)
                .map(e -> (T) e);
    }

    @Override
    public <T> Stream<T> sql(String query, Map<String, Object> params) {
        return manager.get().sql(query, params).map(converter::toEntity)
                .map(e -> (T) e);
    }

    @Override
    public <T> void live(SelectQuery query, OrientDBLiveCallback<T> callBacks) {
        Objects.requireNonNull(query, "query is required");
        Objects.requireNonNull(callBacks, "callBacks is required");
        manager.get().live(query, bindCallbacks(callBacks));
    }

    @Override
    public <T> void live(String query, OrientDBLiveCallback<T> callBacks, Object... params) {
        Objects.requireNonNull(query, "query is required");
        Objects.requireNonNull(callBacks, "callBack is required");
        manager.get().live(query, bindCallbacks(callBacks), params);
    }

    private <T> OrientDBLiveCallback<CommunicationEntity> bindCallbacks(OrientDBLiveCallback<T> callBacks) {
        return OrientDBLiveCallbackBuilder.builder()
                .onCreate(d -> callBacks.getCreateCallback().ifPresent(callback -> callback.accept(converter.toEntity(d))))
                .onUpdate(d -> callBacks.getUpdateCallback().ifPresent(callback -> callback.accept(converter.toEntity(d))))
                .onDelete(d -> callBacks.getDeleteCallback().ifPresent(callback -> callback.accept(converter.toEntity(d))))
                .build();
    }
}

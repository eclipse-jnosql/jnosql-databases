/*
 *  Copyright (c) 2022, 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.couchbase.mapping;


import com.couchbase.client.java.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.databases.couchbase.communication.CouchbaseDocumentManager;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.semistructured.AbstractSemiStructuredTemplate;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.semistructured.EventPersistManager;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * The Default implementation of {@link CouchbaseTemplate}
 */
@Typed(CouchbaseTemplate.class)
@ApplicationScoped
class DefaultCouchbaseTemplate extends AbstractSemiStructuredTemplate
        implements CouchbaseTemplate {

    private final Supplier<CouchbaseDocumentManager> manager;

    private final EntityConverter converter;


    private final EventPersistManager persistManager;

    private final EntitiesMetadata entities;

    private final Converters converters;

    @Inject
    DefaultCouchbaseTemplate(Instance<CouchbaseDocumentManager> manager,
                             EntityConverter converter,
                             EventPersistManager persistManager,
                             EntitiesMetadata entities,
                             Converters converters) {
        this.manager = Objects.requireNonNull(manager, "manager is required")::get;
        this.converter = Objects.requireNonNull(converter, "converter is required");
        this.persistManager = Objects.requireNonNull(persistManager, "persistManager is required");
        this.entities = Objects.requireNonNull(entities, "entities is required");
        this.converters = Objects.requireNonNull(converters, "converters is required");
    }

    DefaultCouchbaseTemplate(Supplier<CouchbaseDocumentManager> manager,
                             EntityConverter converter,
                             EventPersistManager persistManager,
                             EntitiesMetadata entities,
                             Converters converters) {
        this.manager = Objects.requireNonNull(manager, "manager is required");
        this.converter = Objects.requireNonNull(converter, "converter is required");
        this.persistManager = Objects.requireNonNull(persistManager, "persistManager is required");
        this.entities = Objects.requireNonNull(entities, "entities is required");
        this.converters = Objects.requireNonNull(converters, "converters is required");
    }

    DefaultCouchbaseTemplate() {
        this.manager = null;
        this.converter = null;
        this.persistManager = null;
        this.entities = null;
        this.converters = null;
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
    public <T> Stream<T> n1qlQuery(String n1qlQuery, JsonObject params) {
        requireNonNull(n1qlQuery, "n1qlQuery is required");
        requireNonNull(params, "params is required");
        return manager.get().n1qlQuery(n1qlQuery, params)
                .map(converter::toEntity)
                .map(d -> (T) d);
    }

    @Override
    public <T> Stream<T> n1qlQuery(String n1qlQuery) {
        requireNonNull(n1qlQuery, "n1qlQuery is required");
        return manager.get().n1qlQuery(n1qlQuery)
                .map(converter::toEntity)
                .map(d -> (T) d);
    }

}

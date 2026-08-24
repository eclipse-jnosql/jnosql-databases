/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.oracle.mapping;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.databases.oracle.communication.OracleNoSQLDocumentManager;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.semistructured.AbstractSemiStructuredTemplate;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.semistructured.EventPersistManager;

import java.util.Objects;
import java.util.stream.Stream;

@Typed(OracleNoSQLTemplate.class)
@ApplicationScoped
class DefaultOracleNoSQLTemplate extends AbstractSemiStructuredTemplate implements OracleNoSQLTemplate {

    private final Instance<OracleNoSQLDocumentManager> manager;

    private final  EntityConverter converter;

    private final  EventPersistManager persistManager;

    private final  EntitiesMetadata entities;

    private final  Converters converters;

    @Inject
    DefaultOracleNoSQLTemplate(Instance<OracleNoSQLDocumentManager> manager,
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

    DefaultOracleNoSQLTemplate() {
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
    public <T> Stream<T> sql(String query) {
        Objects.requireNonNull(query, "query is required");
        return manager.get().sql(query).map(converter::toEntity).map(d -> (T) d);
    }

    @Override
    public <T> Stream<T> sql(String query, Object... params) {
        Objects.requireNonNull(query, "query is required");
        Objects.requireNonNull(params, "params is required");
        return manager.get().sql(query, params).map(converter::toEntity).map(d -> (T) d);
    }
}

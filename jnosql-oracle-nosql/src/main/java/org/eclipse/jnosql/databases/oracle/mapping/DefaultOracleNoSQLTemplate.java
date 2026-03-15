/*
 *  Copyright (c) 2024,2026 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.oracle.mapping;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.oracle.communication.OracleNoSQLDocumentManager;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.semistructured.AbstractSemiStructuredTemplate;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.semistructured.EventPersistManager;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Typed(OracleNoSQLTemplate.class)
@ApplicationScoped
class DefaultOracleNoSQLTemplate extends AbstractSemiStructuredTemplate implements OracleNoSQLTemplate {

    private final Supplier<OracleNoSQLDocumentManager> manager;

    private final EntityConverter converter;

    private final EventPersistManager persistManager;

    private final EntitiesMetadata entities;

    private final Converters converters;

    DefaultOracleNoSQLTemplate() {
        this((Supplier<OracleNoSQLDocumentManager>) null, null, null, null, null);
    }

    @Inject
    DefaultOracleNoSQLTemplate(Instance<OracleNoSQLDocumentManager> manager,
                               EntityConverter converter,
                               EventPersistManager persistManager,
                               EntitiesMetadata entities,
                               Converters converters) {
        this(Objects.requireNonNull(manager, "manager is required")::get,
                converter,
                persistManager,
                entities,
                converters);
    }

    DefaultOracleNoSQLTemplate(Supplier<OracleNoSQLDocumentManager> manager,
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

    @Override
    protected EntityConverter converter() {
        return converter;
    }

    @Override
    protected OracleNoSQLDocumentManager manager() {
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

/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *
 * Maximillian Arruda
 */

package org.eclipse.jnosql.databases.dynamodb.mapping;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.databases.dynamodb.communication.DynamoDBDatabaseManager;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.semistructured.AbstractSemiStructuredTemplate;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.semistructured.EventPersistManager;

import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

@Typed(DynamoDBTemplate.class)
@ApplicationScoped
class DefaultDynamoDBTemplate extends AbstractSemiStructuredTemplate implements DynamoDBTemplate {

    private final Instance<DynamoDBDatabaseManager> manager;

    private final EntityConverter converter;

    private final EventPersistManager persistManager;

    private final EntitiesMetadata entitiesMetadata;

    private final Converters converters;

    @Inject
    DefaultDynamoDBTemplate(Instance<DynamoDBDatabaseManager> manager,
                            EntityConverter converter,
                            EventPersistManager persistManager,
                            EntitiesMetadata entitiesMetadata,
                            Converters converters) {
        this.manager = manager;
        this.converter = converter;
        this.persistManager = persistManager;
        this.entitiesMetadata = entitiesMetadata;
        this.converters = converters;
    }

    /**
     * Required by CDI/Reflection/Test purposes
     * Don't use it
     */
    DefaultDynamoDBTemplate() {
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
        return entitiesMetadata;
    }

    @Override
    protected Converters converters() {
        return converters;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Stream<T> partiQL(String query, Class<T> entityType, Object... params) {
        requireNonNull(query, "query is required");
        requireNonNull(entityType, "entityType is required");
        requireNonNull(params, "params is required");
        var entityMetadata = entities().findByClassName(entityType.getName())
                .orElseThrow(() -> new IllegalArgumentException("Entity type not found: " + entityType.getName()));
        return manager.get().partiQL(query, entityMetadata.name(), params).map(converter::toEntity).map(d -> (T) d);
    }
}

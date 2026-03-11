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
package org.eclipse.jnosql.databases.mongodb.mapping;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.bson.BsonValue;
import org.bson.conversions.Bson;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.databases.mongodb.communication.MongoDBDocumentManager;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.metadata.EntityMetadata;
import org.eclipse.jnosql.mapping.semistructured.AbstractSemiStructuredTemplate;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.semistructured.EventPersistManager;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;


@ApplicationScoped
@Typed(MongoDBTemplate.class)
class DefaultMongoDBTemplate extends AbstractSemiStructuredTemplate implements MongoDBTemplate {

    private final Supplier<MongoDBDocumentManager> manager;

    private final EntityConverter converter;

    private final EntitiesMetadata entities;

    private final Converters converters;

    private final EventPersistManager persistManager;


    @Inject
    DefaultMongoDBTemplate(Instance<MongoDBDocumentManager> manager,
                           EntityConverter converter,
                           EntitiesMetadata entities,
                           Converters converters,
                           EventPersistManager persistManager) {
        this.manager = Objects.requireNonNull(manager, "manager is required")::get;
        this.converter = Objects.requireNonNull(converter, "converter is required");
        this.entities = Objects.requireNonNull(entities, "entities is required");
        this.converters = Objects.requireNonNull(converters, "converters is required");
        this.persistManager = Objects.requireNonNull(persistManager, "persistManager is required");
    }

    DefaultMongoDBTemplate(Supplier<MongoDBDocumentManager> manager,
                           EntityConverter converter,
                           EntitiesMetadata entities,
                           Converters converters,
                           EventPersistManager persistManager) {
        this.manager = Objects.requireNonNull(manager, "manager is required");
        this.converter = Objects.requireNonNull(converter, "converter is required");
        this.entities = Objects.requireNonNull(entities, "entities is required");
        this.converters = Objects.requireNonNull(converters, "converters is required");
        this.persistManager = Objects.requireNonNull(persistManager, "persistManager is required");
    }

    DefaultMongoDBTemplate() {
        this.manager = null;
        this.converter = null;
        this.entities = null;
        this.converters = null;
        this.persistManager = null;
    }

    @Override
    protected EntityConverter converter() {
        return converter;
    }

    @Override
    protected MongoDBDocumentManager manager() {
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
    public long delete(String collectionName, Bson filter) {
        Objects.requireNonNull(collectionName, "collectionName is required");
        Objects.requireNonNull(filter, "filter is required");
        return this.manager().delete(collectionName, filter);
    }

    @Override
    public <T> long delete(Class<T> entity, Bson filter) {
        Objects.requireNonNull(entity, "Entity is required");
        Objects.requireNonNull(filter, "filter is required");
        EntityMetadata entityMetadata = this.entities.get(entity);
        return this.manager().delete(entityMetadata.name(), filter);
    }

    @Override
    public <T> Stream<T> select(String collectionName, Bson filter) {
        Objects.requireNonNull(collectionName, "collectionName is required");
        Objects.requireNonNull(filter, "filter is required");
        Stream<CommunicationEntity> entityStream = this.manager().select(collectionName, filter);
        return entityStream.map(this.converter::toEntity);
    }

    @Override
    public <T> Stream<T> select(Class<T> entity, Bson filter) {
        Objects.requireNonNull(entity, "entity is required");
        Objects.requireNonNull(filter, "filter is required");
        EntityMetadata entityMetadata = this.entities.get(entity);
        Stream<CommunicationEntity> entityStream = this.manager().select(entityMetadata.name(), filter);
        return entityStream.map(this.converter::toEntity);
    }

    @Override
    public Stream<Map<String, BsonValue>> aggregate(String collectionName, Bson... pipeline) {
        Objects.requireNonNull(collectionName, "collectionName is required");
        Objects.requireNonNull(pipeline, "pipeline is required");
        return this.manager().aggregate(collectionName, pipeline);
    }

    @Override
    public <T> Stream<Map<String, BsonValue>> aggregate(Class<T> entity, Bson... pipeline) {
        Objects.requireNonNull(entity, "entity is required");
        Objects.requireNonNull(pipeline, "pipeline is required");
        EntityMetadata entityMetadata = this.entities.get(entity);
        return this.manager().aggregate(entityMetadata.name(), pipeline);
    }

    @Override
    public <T> Stream<T> aggregate(String collectionName, List<Bson> pipeline) {
        Objects.requireNonNull(collectionName, "collectionName is required");
        Objects.requireNonNull(pipeline, "pipeline is required");
        return this.manager().aggregate(collectionName, pipeline)
                .map(this.converter::toEntity);
    }

    @Override
    public <T> Stream<T> aggregate(Class<T> entity, List<Bson> pipeline) {
        Objects.requireNonNull(entity, "entity is required");
        Objects.requireNonNull(pipeline, "pipeline is required");
        EntityMetadata entityMetadata = this.entities.get(entity);
        return this.manager().aggregate(entityMetadata.name(), pipeline)
                .map(this.converter::toEntity);
    }

    @Override
    public long count(String collectionName, Bson filter) {
        Objects.requireNonNull(collectionName, "collection name is required");
        Objects.requireNonNull(filter, "filter is required");
        return this.manager().count(collectionName, filter);
    }

    @Override
    public <T> long count(Class<T> entity, Bson filter) {
        Objects.requireNonNull(entity, "entity is required");
        Objects.requireNonNull(filter, "filter is required");
        EntityMetadata entityMetadata = this.entities.get(entity);
        return this.manager().count(entityMetadata.name(), filter);
    }

}

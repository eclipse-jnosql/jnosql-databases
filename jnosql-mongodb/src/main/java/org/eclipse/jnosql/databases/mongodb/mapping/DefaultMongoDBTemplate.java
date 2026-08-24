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
import java.util.stream.Stream;


@ApplicationScoped
@Typed(MongoDBTemplate.class)
class DefaultMongoDBTemplate extends AbstractSemiStructuredTemplate implements MongoDBTemplate {

    private Instance<MongoDBDocumentManager> manager;

    private EntityConverter converter;

    private EntitiesMetadata entities;

    private Converters converters;

    private EventPersistManager persistManager;


    @Inject
    DefaultMongoDBTemplate(Instance<MongoDBDocumentManager> manager,
                           EntityConverter converter,
                           EntitiesMetadata entities,
                           Converters converters,
                           EventPersistManager persistManager) {
        this.manager = manager;
        this.converter = converter;
        this.entities = entities;
        this.converters = converters;
        this.persistManager = persistManager;
    }

    DefaultMongoDBTemplate() {
        this(null, null, null, null, null);
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

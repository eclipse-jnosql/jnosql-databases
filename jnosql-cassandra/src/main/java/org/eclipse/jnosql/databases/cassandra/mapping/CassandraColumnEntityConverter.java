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
package org.eclipse.jnosql.databases.cassandra.mapping;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.metadata.CollectionFieldMetadata;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.metadata.FieldMetadata;
import org.eclipse.jnosql.mapping.semistructured.AttributeFieldValue;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.semistructured.ProjectorConverter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

@ApplicationScoped
@Typed(CassandraColumnEntityConverter.class)
class CassandraColumnEntityConverter extends EntityConverter {

    private final EntitiesMetadata entities;

    private final Converters converters;

    private final ProjectorConverter projectorConverter;

    @Inject
    CassandraColumnEntityConverter(EntitiesMetadata entities, Converters converters, ProjectorConverter projectorConverter) {
        this.entities = Objects.requireNonNull(entities, "entities is required");
        this.converters = Objects.requireNonNull(converters, "converters is required");
        this.projectorConverter = Objects.requireNonNull(projectorConverter, "projectorConverter is required");
    }

    CassandraColumnEntityConverter() {
        this.entities = null;
        this.converters = null;
        this.projectorConverter = null;
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
    protected ProjectorConverter projectorConverter() {
        return projectorConverter;
    }

    @Override
    protected <T> Consumer<String> feedObject(T instance, List<Element> columns, Map<String, FieldMetadata> fieldsGroupByName) {
        return k -> {
            FieldMetadata field = fieldsGroupByName.get(k);
            if (field.udt().isPresent()) {
                Optional<Element> column = columns.stream().filter(c -> c.name().equals(k)).findFirst();
                setUDTField(instance, column, field);
            } else {
                super.feedObject(instance, columns, fieldsGroupByName).accept(k);
            }
        };
    }

    private <T> void setUDTField(T instance, Optional<Element> column, FieldMetadata field) {
        if (column.isPresent() && org.eclipse.jnosql.databases.cassandra.communication.UDT.class.isInstance(column.get())) {
            org.eclipse.jnosql.databases.cassandra.communication.UDT udt =
                    org.eclipse.jnosql.databases.cassandra.communication.UDT.class.cast(column.get());
            Object columns = udt.get();
            if (StreamSupport.stream(Iterable.class.cast(columns).spliterator(), false)
                    .allMatch(Iterable.class::isInstance)) {
                var collectionFieldMetadata = CollectionFieldMetadata.class.cast(field);
                Collection collection = collectionFieldMetadata.collectionInstance();
                List<List<Element>> embeddable = (List<List<Element>>) columns;
                for (List<Element> columnList : embeddable) {
                    Object element = toEntity(collectionFieldMetadata.elementType(), columnList);
                    collection.add(element);
                }
                field.write(instance, collection);
            } else {
                Object value = toEntity(field.type(), (List<Element>) columns);
                field.write(instance, value);
            }
        }
    }

    @Override
    protected AttributeFieldValue to(FieldMetadata field, Object entityInstance) {

        Object value = field.read(entityInstance);
        Optional<String> annotation = field.udt();
        return annotation.<AttributeFieldValue>map(v -> new CassandraUDTType(v, value, field))
                .orElseGet( () -> super.to(field, entityInstance));
    }
}
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
 *   Alessandro Moscatelli
 */
package org.eclipse.jnosql.databases.mongodb.mapping;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.bson.conversions.Bson;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.databases.mongodb.communication.MongoDBDocumentManager;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;
import org.eclipse.jnosql.mapping.document.spi.DocumentExtension;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.metadata.EntityMetadata;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.semistructured.EventPersistManager;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class, DocumentTemplate.class, MongoDBTemplate.class})
@AddPackages(Music.class)
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class,
        DocumentExtension.class})
class DefaultMongoDBTemplateTest {

    @Inject
    private EntityConverter converter;

    @Inject
    private EventPersistManager persistManager;

    @Inject
    private EntitiesMetadata entities;

    @Inject
    private Converters converters;

    private MongoDBTemplate template;

    private MongoDBDocumentManager manager;

    @BeforeEach
    void setUp() {
        this.manager = mock(MongoDBDocumentManager.class);
        Instance instance = mock(Instance.class);
        when(instance.get()).thenReturn(manager);
        template = new DefaultMongoDBTemplate(instance, converter, entities, converters, persistManager);
    }

    @Test
    void shouldReturnErrorOnDeleteMethod() {
        Bson filter = eq("name", "Poliana");
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.delete((String) null, null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.delete("Collection", null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.delete((String) null, filter));

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.delete(Birthday.class, null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.delete((Class<Object>) null, filter));
    }

    @Test
    void shouldDeleteWithCollectionName() {
        Bson filter = eq("name", "Poliana");
        template.delete("Person", filter);
        Mockito.verify(manager).delete("Person", filter);
    }

    @Test
    void shouldDeleteWithEntity() {
        Bson filter = eq("name", "Poliana");
        template.delete(Birthday.class, filter);
        Mockito.verify(manager).delete("Birthday", filter);
    }

    @Test
    void shouldDeleteAll() {
        EntityMetadata metadata = entities.get(Birthday.class);
        DeleteQuery query = DeleteQuery.delete().from(metadata.name()).build();
        template.deleteAll(Birthday.class);
        Mockito.verify(manager).delete(query);
    }

    @Test
    void shouldReturnErrorOnSelectMethod() {
        Bson filter = eq("name", "Poliana");

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.select((String) null, null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.select("Collection", null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.select((String) null, filter));

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.select((Class<?>) null, null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.select(Birthday.class, null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.select((Class<?>) null, filter));
    }

    @Test
    void shouldSelectWithCollectionName() {
        var entity = CommunicationEntity.of("Birthday", Arrays
                .asList(Element.of("_id", "Poliana"),
                        Element.of("age", 30)));
        Bson filter = eq("name", "Poliana");
        Mockito.when(manager.select("Birthday", filter))
                .thenReturn(Stream.of(entity));
        Stream<Birthday> stream = template.select("Birthday", filter);
        assertThat(stream).isNotNull();
        Birthday poliana = stream.findFirst()
                .orElseThrow(() -> new IllegalStateException("There is an issue on the test"));

        assertThat(poliana).isNotNull();
        assertThat(poliana.getName()).isEqualTo("Poliana");
        assertThat(poliana.getAge()).isEqualTo(30);
    }

    @Test
    void shouldSelectWithEntity() {
        var entity = CommunicationEntity.of("Birthday", Arrays
                .asList(Element.of("_id", "Poliana"),
                        Element.of("age", 30)));
        Bson filter = eq("name", "Poliana");
        Mockito.when(manager.select("Birthday", filter))
                .thenReturn(Stream.of(entity));
        Stream<Birthday> stream = template.select(Birthday.class, filter);
        assertThat(stream).isNotNull();
        Birthday poliana = stream.findFirst()
                .orElseThrow(() -> new IllegalStateException("There is an issue on the test"));

        assertThat(poliana).isNotNull();
        assertThat(poliana.getName()).isEqualTo("Poliana");
        assertThat(poliana.getAge()).isEqualTo(30);
    }

    @Test
    void shouldReturnErrorOnAggregateMethod() {
        var collectionName = "AnyCollection";
        var bson = eq("name", "Poliana");
        var pipeline = Collections.singletonList(bson);
        var pipelineArray = new Bson[]{bson, bson};

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.aggregate((String) null, (List<Bson>) null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.aggregate((String) null, (Bson[]) null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.aggregate((String) null, (Bson) null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.aggregate((String) null, pipeline));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.aggregate((String) null, pipelineArray));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.aggregate((String) null, bson));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.aggregate(collectionName, (List<Bson>) null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.aggregate(collectionName, (Bson[]) null));

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.aggregate((Class<?>) null, (List<Bson>) null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.aggregate((Class<?>) null, (Bson[]) null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.aggregate((Class<?>) null, (Bson) null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.aggregate((String) null, pipeline));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.aggregate((String) null, pipelineArray));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.aggregate((String) null, bson));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.aggregate(Birthday.class, (List<Bson>) null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.aggregate(Birthday.class, (Bson[]) null));

    }

    @Test
    void shouldAggregateWithCollectionName() {
        Bson[] predicates = {
                Aggregates.match(eq("name", "Poliana")),
                Aggregates.group("$stars", Accumulators.sum("count", 1))
        };

        template.aggregate("Person", predicates);
        Mockito.verify(manager).aggregate("Person", predicates);
    }

    @Test
    void shouldAggregateWithEntity() {
        Bson[] predicates = {
                Aggregates.match(eq("name", "Poliana")),
                Aggregates.group("$stars", Accumulators.sum("count", 1))
        };

        template.aggregate(Birthday.class, predicates);
        Mockito.verify(manager).aggregate("Birthday", predicates);
    }

    @Test
    void shouldCountByFilterWithCollectionName() {
        var filter = eq("name", "Poliana");

        template.count("Person", filter);

        Mockito.verify(manager).count("Person", filter);
    }

    @Test
    void shouldCountByFilterWithEntity() {
        var filter = eq("name", "Poliana");

        template.count(Birthday.class, filter);

        Mockito.verify(manager).count("Birthday", filter);
    }

    @Test
    void shouldReturnErrorOnCountByFilterMethod() {
        var filter = eq("name", "Poliana");
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.count((String) null, null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.count((String) null, filter));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.count("Person", null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.count((Class<Birthday>) null, null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.count((Class<Birthday>) null, filter));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> template.count(Birthday.class, null));
    }
}

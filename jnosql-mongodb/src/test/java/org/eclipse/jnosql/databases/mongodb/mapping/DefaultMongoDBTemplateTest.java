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
import org.eclipse.jnosql.mapping.core.spi.EntityMetadataExtension;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;
import org.eclipse.jnosql.mapping.document.spi.DocumentExtension;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.metadata.EntityMetadata;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.semistructured.EventPersistManager;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class, DocumentTemplate.class, MongoDBTemplate.class})
@AddPackages(Music.class)
@AddPackages(Reflections.class)
@AddExtensions({EntityMetadataExtension.class,
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
        assertThrows(NullPointerException.class,() -> template.delete((String) null, null));
        assertThrows(NullPointerException.class,() -> template.delete("Collection", null));
        assertThrows(NullPointerException.class,() -> template.delete((String) null, filter));

        assertThrows(NullPointerException.class,() -> template.delete(BirthdayPerson.class, null));
        assertThrows(NullPointerException.class,() -> template.delete((Class<Object>) null, filter));
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
        template.delete(BirthdayPerson.class, filter);
        Mockito.verify(manager).delete("BirthdayPerson", filter);
    }

    @Test
    void shouldDeleteAll() {
        EntityMetadata metadata = entities.get(BirthdayPerson.class);
        DeleteQuery query = DeleteQuery.delete().from(metadata.name()).build();
        template.deleteAll(BirthdayPerson.class);
        Mockito.verify(manager).delete(query);
    }

    @Test
    void shouldReturnErrorOnSelectMethod() {
        Bson filter = eq("name", "Poliana");

        assertThrows(NullPointerException.class, () -> template.select((String) null, null));
        assertThrows(NullPointerException.class, () -> template.select("Collection", null));
        assertThrows(NullPointerException.class, () -> template.select((String) null, filter));

        assertThrows(NullPointerException.class, () -> template.select((Class<?>) null, null));
        assertThrows(NullPointerException.class, () -> template.select(BirthdayPerson.class, null));
        assertThrows(NullPointerException.class, () -> template.select((Class<?>) null, filter));
    }

    @Test
    void shouldSelectWithCollectionName() {
        var entity = CommunicationEntity.of("BirthdayPerson", Arrays
                .asList(Element.of("_id", "Poliana"),
                        Element.of("age", 30)));
        Bson filter = eq("name", "Poliana");
        Mockito.when(manager.select("Person", filter))
                .thenReturn(Stream.of(entity));
        Stream<BirthdayPerson> stream = template.select("Person", filter);
        Assertions.assertNotNull(stream);
        BirthdayPerson poliana = stream.findFirst()
                .orElseThrow(() -> new IllegalStateException("There is an issue on the test"));

        Assertions.assertNotNull(poliana);
        assertEquals("Poliana", poliana.getName());
        assertEquals(30, poliana.getAge());
    }

    @Test
    void shouldSelectWithEntity() {
        var entity = CommunicationEntity.of("BirthdayPerson", Arrays
                .asList(Element.of("_id", "Poliana"),
                        Element.of("age", 30)));
        Bson filter = eq("name", "Poliana");
        Mockito.when(manager.select("BirthdayPerson", filter))
                .thenReturn(Stream.of(entity));
        Stream<BirthdayPerson> stream = template.select(BirthdayPerson.class, filter);
        Assertions.assertNotNull(stream);
        BirthdayPerson poliana = stream.findFirst()
                .orElseThrow(() -> new IllegalStateException("There is an issue on the test"));

        Assertions.assertNotNull(poliana);
        assertEquals("Poliana", poliana.getName());
        assertEquals(30, poliana.getAge());
    }

    @Test
    void shouldReturnErrorOnAggregateMethod() {
        var collectionName = "AnyCollection";
        var bson = eq("name", "Poliana");
        var pipeline = Collections.singletonList(bson);
        var pipelineArray = new Bson[]{bson, bson};

        assertThrows(NullPointerException.class, () -> template.aggregate((String) null, (List<Bson>) null));
        assertThrows(NullPointerException.class, () -> template.aggregate((String) null, (Bson[]) null));
        assertThrows(NullPointerException.class, () -> template.aggregate((String) null, (Bson) null));
        assertThrows(NullPointerException.class, () -> template.aggregate((String) null, pipeline));
        assertThrows(NullPointerException.class, () -> template.aggregate((String) null, pipelineArray));
        assertThrows(NullPointerException.class, () -> template.aggregate((String) null, bson));
        assertThrows(NullPointerException.class, () -> template.aggregate(collectionName, (List<Bson>) null));
        assertThrows(NullPointerException.class, () -> template.aggregate(collectionName, (Bson[]) null));

        assertThrows(NullPointerException.class, () -> template.aggregate((Class<?>) null, (List<Bson>) null));
        assertThrows(NullPointerException.class, () -> template.aggregate((Class<?>) null, (Bson[]) null));
        assertThrows(NullPointerException.class, () -> template.aggregate((Class<?>) null, (Bson) null));
        assertThrows(NullPointerException.class, () -> template.aggregate((String) null, pipeline));
        assertThrows(NullPointerException.class, () -> template.aggregate((String) null, pipelineArray));
        assertThrows(NullPointerException.class, () -> template.aggregate((String) null, bson));
        assertThrows(NullPointerException.class, () -> template.aggregate(BirthdayPerson.class, (List<Bson>) null));
        assertThrows(NullPointerException.class, () -> template.aggregate(BirthdayPerson.class, (Bson[]) null));

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

        template.aggregate(BirthdayPerson.class, predicates);
        Mockito.verify(manager).aggregate("BirthdayPerson", predicates);
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

        template.count(BirthdayPerson.class, filter);

        Mockito.verify(manager).count("BirthdayPerson", filter);
    }

    @Test
    void shouldReturnErrorOnCountByFilterMethod() {
        var filter = eq("name", "Poliana");
        assertThrows(NullPointerException.class, () -> template.count((String) null, null));
        assertThrows(NullPointerException.class, () -> template.count((String) null, filter));
        assertThrows(NullPointerException.class, () -> template.count("Person", null));
        assertThrows(NullPointerException.class, () -> template.count((Class<BirthdayPerson>) null, null));
        assertThrows(NullPointerException.class, () -> template.count((Class<BirthdayPerson>) null, filter));
        assertThrows(NullPointerException.class, () -> template.count(BirthdayPerson.class, null));
    }
}

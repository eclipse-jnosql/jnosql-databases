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
 */
package org.eclipse.jnosql.databases.cassandra.mapping;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.databases.cassandra.communication.CassandraColumnManager;
import org.eclipse.jnosql.mapping.column.ColumnTemplate;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.column.spi.ColumnExtension;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.core.spi.EntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.semistructured.EventPersistManager;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class, ColumnTemplate.class,
        CQL.class})
@AddPackages(MockProducer.class)
@AddPackages(Reflections.class)
@AddExtensions({EntityMetadataExtension.class,
        ColumnExtension.class, CassandraExtension.class})
public class DefaultCassandraTemplateTest {

    @Inject
    private CassandraColumnEntityConverter converter;

    @Inject
    private EventPersistManager persistManager;

    @Inject
    private EntitiesMetadata entities;

    @Inject
    private Converters converters;

    private CassandraTemplate template;

    private CassandraColumnManager manager;

    @BeforeEach
    void setUp() {
        this.manager = mock(CassandraColumnManager.class);
        Instance instance = mock(Instance.class);
        when(instance.get()).thenReturn(manager);
        template = new DefaultCassandraTemplate(instance, converter, persistManager, entities, converters);
    }

    @Test
    void shouldSaveConsistency() {
        var entity = CommunicationEntity.of("Contact", asList(Element.of("name", "Name"), Element.of("age", 20)));
        entity.addNull("home");
        ArgumentCaptor<CommunicationEntity> captor = ArgumentCaptor.forClass(CommunicationEntity.class);

        ConsistencyLevel level = ConsistencyLevel.THREE;

        when(manager.
                save(Mockito.any(CommunicationEntity.class), Mockito.eq(level)))
                .thenReturn(entity);

        Contact contact = new Contact();
        contact.setName("Name");
        contact.setAge(20);
        assertEquals(contact, template.save(contact, level));

        Mockito.verify(manager).save(captor.capture(), Mockito.eq(level));
        assertEquals(entity, captor.getValue());

    }

    @Test
    void shouldSaveConsistencyIterable() {
        CommunicationEntity entity = CommunicationEntity.of("Contact", asList(Element.of("name", "Name"), Element.of("age", 20)));
        entity.addNull("home");
        ArgumentCaptor<CommunicationEntity> captor = ArgumentCaptor.forClass(CommunicationEntity.class);

        ConsistencyLevel level = ConsistencyLevel.THREE;

        when(manager.
                save(Mockito.any(CommunicationEntity.class), Mockito.eq(level)))
                .thenReturn(entity);

        Contact contact = new Contact();
        contact.setName("Name");
        contact.setAge(20);
        assertThat(template.save(Collections.singletonList(contact), level)).contains(contact);
        Mockito.verify(manager).save(captor.capture(), Mockito.eq(level));
        assertEquals(entity, captor.getValue());

    }

    @Test
    void shouldSaveConsistencyDuration() {
        Duration duration = Duration.ofHours(2);
        CommunicationEntity entity = CommunicationEntity.of("Contact", asList(Element.of("name", "Name"), Element.of("age", 20)));
        entity.addNull("home");
        ArgumentCaptor<CommunicationEntity> captor = ArgumentCaptor.forClass(CommunicationEntity.class);

        ConsistencyLevel level = ConsistencyLevel.THREE;
        when(manager.
                save(Mockito.any(CommunicationEntity.class), Mockito.eq(duration),
                        Mockito.eq(level)))
                .thenReturn(entity);

        Contact contact = new Contact();
        contact.setName("Name");
        contact.setAge(20);
        assertEquals(contact, template.save(contact, duration, level));

        Mockito.verify(manager).save(captor.capture(), Mockito.eq(duration), Mockito.eq(level));
        assertEquals(entity, captor.getValue());
    }

    @Test
    void shouldSaveConsistencyDurationIterable() {
        Duration duration = Duration.ofHours(2);
        CommunicationEntity entity = CommunicationEntity.of("Contact", asList(Element.of("name", "Name"), Element.of("age", 20)));
        entity.addNull("home");
        ArgumentCaptor<CommunicationEntity> captor = ArgumentCaptor.forClass(CommunicationEntity.class);

        ConsistencyLevel level = ConsistencyLevel.THREE;
        when(manager.
                save(Mockito.any(CommunicationEntity.class), Mockito.eq(duration),
                        Mockito.eq(level)))
                .thenReturn(entity);

        Contact contact = new Contact();
        contact.setName("Name");
        contact.setAge(20);
        assertThat(template.save(Collections.singletonList(contact), duration, level)).contains(contact);
        Mockito.verify(manager).save(captor.capture(), Mockito.eq(duration), Mockito.eq(level));
        assertEquals(entity, captor.getValue());
    }

    @Test
    void shouldDelete() {

        DeleteQuery query = DeleteQuery.delete().from("columnFamily").build();
        ConsistencyLevel level = ConsistencyLevel.THREE;
        template.delete(query, level);
        verify(manager).delete(query, level);
    }


    @Test
    void shouldFind() {
        Contact contact = new Contact();
        contact.setName("Name");
        contact.setAge(20);

        CommunicationEntity entity = CommunicationEntity.of("Contact", asList(Element.of("name", "Name"), Element.of("age", 20)));
        SelectQuery query = select().from("columnFamily").build();
        ConsistencyLevel level = ConsistencyLevel.THREE;
        when(manager.select(query, level)).thenReturn(Stream.of(entity));

        Stream<Contact> people = template.find(query, level);
        assertThat(people.collect(Collectors.toList())).contains(contact);
    }

    @Test
    void shouldFindCQL() {
        Contact contact = new Contact();
        contact.setName("Name");
        contact.setAge(20);
        String cql = "select * from Person";
        CommunicationEntity entity = CommunicationEntity.of("Contact", asList(Element.of("name", "Name"), Element.of("age", 20)));

        when(manager.cql(cql)).thenReturn(Stream.of(entity));

        List<Contact> people = template.<Contact>cql(cql).collect(Collectors.toList());
        Assertions.assertThat(people).contains(contact);
    }

    @Test
    void shouldFindSimpleStatement() {
        SimpleStatement statement = QueryBuilder.selectFrom("Contact").all().build();
        Contact contact = new Contact();
        contact.setName("Name");
        contact.setAge(20);
        CommunicationEntity entity = CommunicationEntity.of("Contact", asList(Element.of("name", "Name"), Element.of("age", 20)));

        when(manager.execute(statement)).thenReturn(Stream.of(entity));

        List<Contact> people = template.<Contact>execute(statement).collect(Collectors.toList());
        Assertions.assertThat(people).contains(contact);
    }

}

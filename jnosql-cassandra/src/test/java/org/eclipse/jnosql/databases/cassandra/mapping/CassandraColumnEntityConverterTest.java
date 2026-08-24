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
package org.eclipse.jnosql.databases.cassandra.mapping;

import jakarta.inject.Inject;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.databases.cassandra.communication.UDT;
import org.eclipse.jnosql.databases.cassandra.mapping.model.Actor;
import org.eclipse.jnosql.databases.cassandra.mapping.model.AppointmentBook;
import org.eclipse.jnosql.databases.cassandra.mapping.model.Artist;
import org.eclipse.jnosql.databases.cassandra.mapping.model.Director;
import org.eclipse.jnosql.databases.cassandra.mapping.model.History2;
import org.eclipse.jnosql.databases.cassandra.mapping.model.Job;
import org.eclipse.jnosql.databases.cassandra.mapping.model.Money;
import org.eclipse.jnosql.databases.cassandra.mapping.model.Movie;
import org.eclipse.jnosql.databases.cassandra.mapping.model.Worker;
import org.eclipse.jnosql.mapping.column.ColumnTemplate;
import org.eclipse.jnosql.mapping.column.spi.ColumnExtension;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

@EnableAutoWeld
@AddPackages(value = {Converters.class, ColumnTemplate.class, EntityConverter.class,
        CQL.class})
@AddPackages(MockProducer.class)
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class,
        ColumnExtension.class, CassandraExtension.class})
public class CassandraColumnEntityConverterTest {

    @Inject
    private CassandraColumnEntityConverter converter;

    private Element[] columns;

    private Actor actor = Actor.actorBuilder().withAge()
            .withId()
            .withName()
            .withPhones(asList("234", "2342"))
            .withMovieCharacter(Collections.singletonMap("JavaZone", "Jedi"))
            .withMovierRating(Collections.singletonMap("JavaZone", 10))
            .build();

    @BeforeEach
    public void init() {

        columns = new Element[]{Element.of("_id", 12L),
                Element.of("age", 10), Element.of("name", "Otavio"),
                Element.of("phones", asList("234", "2342"))
                , Element.of("movieCharacter", Collections.singletonMap("JavaZone", "Jedi"))
                , Element.of("movieRating", Collections.singletonMap("JavaZone", 10))};
    }

    @Test
    public void shouldConvertPersonToDocument() {

        Artist artist = Artist.builder().withAge()
                .withId(12)
                .withName("Otavio")
                .withPhones(asList("234", "2342")).build();

        CommunicationEntity entity = converter.toCommunication(artist);
        assertThat(entity.name()).isEqualTo("Artist");
        assertThat(entity.size()).isEqualTo(5);
    }

    @Test
    public void shouldConvertActorToDocument() {


        var entity = converter.toCommunication(actor);
        assertThat(entity.name()).isEqualTo("Actor");
        assertThat(entity.size()).isEqualTo(7);


        assertThat(entity.elements()).contains(columns);
    }

    @Test
    public void shouldConvertDocumentToActor() {
        var entity = CommunicationEntity.of("Actor");
        Stream.of(columns).forEach(entity::add);

        Actor actor = converter.toEntity(Actor.class, entity);
        assertThat(actor).isNotNull();
        assertThat(actor.getAge()).isEqualTo(10);
        assertThat(actor.getId()).isEqualTo(12L);
        assertThat(actor.getPhones()).isEqualTo(asList("234", "2342"));
        assertThat(actor.getMovieCharacter()).isEqualTo(Collections.singletonMap("JavaZone", "Jedi"));
        assertThat(actor.getMovieRating()).isEqualTo(Collections.singletonMap("JavaZone", 10));
    }

    @Test
    public void shouldConvertDocumentToActorFromEntity() {
        var entity = CommunicationEntity.of("Actor");
        Stream.of(columns).forEach(entity::add);

        Actor actor = converter.toEntity(entity);
        assertThat(actor).isNotNull();
        assertThat(actor.getAge()).isEqualTo(10);
        assertThat(actor.getId()).isEqualTo(12L);
        assertThat(actor.getPhones()).isEqualTo(asList("234", "2342"));
        assertThat(actor.getMovieCharacter()).isEqualTo(Collections.singletonMap("JavaZone", "Jedi"));
        assertThat(actor.getMovieRating()).isEqualTo(Collections.singletonMap("JavaZone", 10));
    }


    @Test
    public void shouldConvertDirectorToColumn() {

        Movie movie = new Movie("Matrix", 2012, Collections.singleton("Actor"));
        Director director = Director.builderDiretor().withAge(12)
                .withId(12)
                .withName("Otavio")
                .withPhones(asList("234", "2342")).withMovie(movie).build();

        var entity = converter.toCommunication(director);
        assertThat(entity.size()).isEqualTo(6);

        assertThat(director.getName()).isEqualTo(getValue(entity.find("name")));
        assertThat(director.getAge()).isEqualTo(getValue(entity.find("age")));
        assertThat(director.getId()).isEqualTo(getValue(entity.find("_id")));
        assertThat(director.getPhones()).isEqualTo(getValue(entity.find("phones")));


        Element subColumn = entity.find("movie").get();
        List<Element> columns = subColumn.get(new TypeReference<>() {
        });

        assertThat(columns.size()).isEqualTo(3);
        assertThat(subColumn.name()).isEqualTo("movie");
        assertThat(columns.stream().filter(c -> "title".equals(c.name())).findFirst().get().get()).isEqualTo(movie.getTitle());
        assertThat(columns.stream().filter(c -> "year".equals(c.name())).findFirst().get().get()).isEqualTo(movie.getYear());
        assertThat(columns.stream().filter(c -> "actors".equals(c.name())).findFirst().get().get()).isEqualTo(movie.getActors());


    }

    @Test
    public void shouldConvertToEmbeddedClassWhenHasSubColumn() {
        Movie movie = new Movie("Matrix", 2012, Collections.singleton("Actor"));
        Director director = Director.builderDiretor().withAge(12)
                .withId(12)
                .withName("Otavio")
                .withPhones(asList("234", "2342")).withMovie(movie).build();

        var entity = converter.toCommunication(director);
        Director director1 = converter.toEntity(entity);

        assertThat(director1.getMovie()).isEqualTo(movie);
        assertThat(director1.getName()).isEqualTo(director.getName());
        assertThat(director1.getAge()).isEqualTo(director.getAge());
        assertThat(director1.getId()).isEqualTo(director.getId());
    }

    @Test
    public void shouldConvertToEmbeddedClassWhenHasSubColumn2() {
        Movie movie = new Movie("Matrix", 2012, singleton("Actor"));
        Director director = Director.builderDiretor().withAge(12)
                .withId(12)
                .withName("Otavio")
                .withPhones(asList("234", "2342")).withMovie(movie).build();

        var entity = converter.toCommunication(director);
        entity.remove("movie");
        entity.add("movie",
                Arrays.asList(Element.of("title", "Matrix"),
                        Element.of("year", 2012),
                        Element.of("actors", singleton("Actor"))));

        Director director1 = converter.toEntity(entity);

        assertThat(director1.getMovie()).isEqualTo(movie);
        assertThat(director1.getName()).isEqualTo(director.getName());
        assertThat(director1.getAge()).isEqualTo(director.getAge());
        assertThat(director1.getId()).isEqualTo(director.getId());
    }


    @Test
    public void shouldConvertToEmbeddedClassWhenHasSubColumn3() {
        Movie movie = new Movie("Matrix", 2012, singleton("Actor"));
        Director director = Director.builderDiretor().withAge(12)
                .withId(12)
                .withName("Otavio")
                .withPhones(asList("234", "2342")).withMovie(movie).build();

        var entity = converter.toCommunication(director);
        entity.remove("movie");
        Map<String, Object> map = new HashMap<>();
        map.put("title", "Matrix");
        map.put("year", 2012);
        map.put("actors", singleton("Actor"));

        entity.add(Element.of("movie", map));
        Director director1 = converter.toEntity(entity);

        assertThat(director1.getMovie()).isEqualTo(movie);
        assertThat(director1.getName()).isEqualTo(director.getName());
        assertThat(director1.getAge()).isEqualTo(director.getAge());
        assertThat(director1.getId()).isEqualTo(director.getId());
    }


    @Test
    public void shouldConvertToDocumentWhenHaConverter() {
        Worker worker = new Worker();
        Job job = new Job();
        job.setCity("Sao Paulo");
        job.setDescription("Java Developer");
        worker.setName("Bob");
        worker.setSalary(new Money("BRL", BigDecimal.TEN));
        worker.setJob(job);
        var entity = converter.toCommunication(worker);
        assertThat(entity.name()).isEqualTo("Worker");
        assertThat(entity.find("name").get().get()).isEqualTo("Bob");
        assertThat(entity.find("money").get().get()).isEqualTo("BRL 10");
    }

    @Test
    public void shouldConvertToEntityWhenHasConverter() {
        Worker worker = new Worker();
        Job job = new Job();
        job.setCity("Sao Paulo");
        job.setDescription("Java Developer");
        worker.setName("Bob");
        worker.setSalary(new Money("BRL", BigDecimal.TEN));
        worker.setJob(job);
        var entity = converter.toCommunication(worker);
        Worker worker1 = converter.toEntity(entity);
        assertThat(worker1.getSalary()).isEqualTo(worker.getSalary());
        assertThat(worker1.getJob().getCity()).isEqualTo(job.getCity());
        assertThat(worker1.getJob().getDescription()).isEqualTo(job.getDescription());
    }


    @Test
    public void shouldSupportUDT() {
        Address address = new Address();
        address.setCity("California");
        address.setStreet("Street");

        ContactCassandra contact = new ContactCassandra();
        contact.setAge(10);
        contact.setName("Ada");
        contact.setHome(address);

        var entity = converter.toCommunication(contact);
        assertThat(entity.name()).isEqualTo("ContactCassandra");
        Element column = entity.find("home").get();
        UDT udt = UDT.class.cast(column);

        assertThat(udt.userType()).isEqualTo("address");
        assertThat(udt.name()).isEqualTo("home");
        assertThat((List<Element>) udt.get())
                .contains(Element.of("city", "California"), Element.of("street", "Street"));

    }


    @Test
    public void shouldSupportUDTToEntity() {
        var entity = CommunicationEntity.of("ContactCassandra");
        entity.add(Element.of("name", "Poliana"));
        entity.add(Element.of("age", 20));
        List<Element> columns = asList(Element.of("city", "Salvador"),
                Element.of("street", "Jose Anasoh"));
        UDT udt = UDT.builder("address").withName("home")
                .addUDT(columns).build();
        entity.add(udt);

        ContactCassandra contact = converter.toEntity(entity);
        assertThat(contact).isNotNull();
        Address home = contact.getHome();
        assertThat(contact.getName()).isEqualTo("Poliana");
        assertThat(contact.getAge()).isEqualTo(Integer.valueOf(20));
        assertThat(home.getCity()).isEqualTo("Salvador");
        assertThat(home.getStreet()).isEqualTo("Jose Anasoh");

    }

    @Test
    public void shouldSupportTimeStampConverter() {
        History2 history = new History2();
        history.setCalendar(Calendar.getInstance());
        history.setLocalDate(LocalDate.now());
        history.setLocalDateTime(LocalDateTime.now());
        history.setZonedDateTime(ZonedDateTime.now());
        history.setNumber(new java.util.Date().getTime());

        var entity = converter.toCommunication(history);
        assertThat(entity.name()).isEqualTo("History2");
        History2 historyConverted = converter.toEntity(entity);
        assertThat(historyConverted).isNotNull();

    }

    @Test
    public void shouldConvertListUDT() {
        AppointmentBook appointmentBook = new AppointmentBook();
        appointmentBook.setUser("otaviojava");
        appointmentBook.setContacts(asList(new org.eclipse.jnosql.databases.cassandra.mapping.model.Contact("Poliana", "poliana@santana.com"),
                new org.eclipse.jnosql.databases.cassandra.mapping.model.Contact("Ada", "ada@lovelace.com")));

        var entity = converter.toCommunication(appointmentBook);
        assertThat(entity.name()).isEqualTo("AppointmentBook");
        assertThat(entity.find("user").get().get()).isEqualTo("otaviojava");
        UDT column = (UDT) entity.find("contacts").get();

        List<List<Element>> contacts = (List<List<Element>>) column.get();
        assertThat(contacts.size()).isEqualTo(2);
        assertThat(contacts.stream().allMatch(c -> c.size() == 2)).isTrue();
        assertThat(column.userType()).isEqualTo("Contact");

    }

    @Test
    public void shouldConvertListUDTToEntity() {
        List<Iterable<Element>> columns = new ArrayList<>();
        columns.add(asList(Element.of("name", "Poliana"),
                Element.of("description", "poliana")));
        columns.add(asList(Element.of("name", "Ada"),
                Element.of("description", "ada@lovelace.com")));

        CommunicationEntity entity = CommunicationEntity.of("AppointmentBook");
        entity.add(Element.of("user", "otaviojava"));
        entity.add(UDT.builder("Contact").withName("contacts").addUDTs(columns).build());
        AppointmentBook appointmentBook = converter.toEntity(entity);
        List<org.eclipse.jnosql.databases.cassandra.mapping.model.Contact> contacts = appointmentBook.getContacts();
        assertThat(appointmentBook.getUser()).isEqualTo("otaviojava");

        assertThat(contacts).contains(new org.eclipse.jnosql.databases.cassandra.mapping.model.Contact("Poliana", "poliana"),
                new org.eclipse.jnosql.databases.cassandra.mapping.model.Contact("Ada", "ada@lovelace.com"));


    }

    private Object getValue(Optional<Element> document) {
        return document.map(Element::value).map(Value::get).orElse(null);
    }
}
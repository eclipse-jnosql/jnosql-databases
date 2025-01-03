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
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.column.spi.ColumnExtension;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.core.spi.EntityMetadataExtension;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnableAutoWeld
@AddPackages(value = {Converters.class, ColumnTemplate.class, EntityConverter.class,
        CQL.class})
@AddPackages(MockProducer.class)
@AddPackages(Reflections.class)
@AddExtensions({EntityMetadataExtension.class,
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
        assertEquals("Artist", entity.name());
        assertEquals(5, entity.size());
    }

    @Test
    public void shouldConvertActorToDocument() {


        var entity = converter.toCommunication(actor);
        assertEquals("Actor", entity.name());
        assertEquals(7, entity.size());


        assertThat(entity.elements()).contains(columns);
    }

    @Test
    public void shouldConvertDocumentToActor() {
        var entity = CommunicationEntity.of("Actor");
        Stream.of(columns).forEach(entity::add);

        Actor actor = converter.toEntity(Actor.class, entity);
        assertNotNull(actor);
        assertEquals(10, actor.getAge());
        assertEquals(12L, actor.getId());
        assertEquals(asList("234", "2342"), actor.getPhones());
        assertEquals(Collections.singletonMap("JavaZone", "Jedi"), actor.getMovieCharacter());
        assertEquals(Collections.singletonMap("JavaZone", 10), actor.getMovieRating());
    }

    @Test
    public void shouldConvertDocumentToActorFromEntity() {
        var entity = CommunicationEntity.of("Actor");
        Stream.of(columns).forEach(entity::add);

        Actor actor = converter.toEntity(entity);
        assertNotNull(actor);
        assertEquals(10, actor.getAge());
        assertEquals(12L, actor.getId());
        assertEquals(asList("234", "2342"), actor.getPhones());
        assertEquals(Collections.singletonMap("JavaZone", "Jedi"), actor.getMovieCharacter());
        assertEquals(Collections.singletonMap("JavaZone", 10), actor.getMovieRating());
    }


    @Test
    public void shouldConvertDirectorToColumn() {

        Movie movie = new Movie("Matrix", 2012, Collections.singleton("Actor"));
        Director director = Director.builderDiretor().withAge(12)
                .withId(12)
                .withName("Otavio")
                .withPhones(asList("234", "2342")).withMovie(movie).build();

        var entity = converter.toCommunication(director);
        assertEquals(6, entity.size());

        assertEquals(getValue(entity.find("name")), director.getName());
        assertEquals(getValue(entity.find("age")), director.getAge());
        assertEquals(getValue(entity.find("_id")), director.getId());
        assertEquals(getValue(entity.find("phones")), director.getPhones());


        Element subColumn = entity.find("movie").get();
        List<Element> columns = subColumn.get(new TypeReference<>() {
        });

        assertEquals(3, columns.size());
        assertEquals("movie", subColumn.name());
        assertEquals(movie.getTitle(), columns.stream().filter(c -> "title".equals(c.name())).findFirst().get().get());
        assertEquals(movie.getYear(), columns.stream().filter(c -> "year".equals(c.name())).findFirst().get().get());
        assertEquals(movie.getActors(), columns.stream().filter(c -> "actors".equals(c.name())).findFirst().get().get());


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

        assertEquals(movie, director1.getMovie());
        assertEquals(director.getName(), director1.getName());
        assertEquals(director.getAge(), director1.getAge());
        assertEquals(director.getId(), director1.getId());
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

        assertEquals(movie, director1.getMovie());
        assertEquals(director.getName(), director1.getName());
        assertEquals(director.getAge(), director1.getAge());
        assertEquals(director.getId(), director1.getId());
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

        assertEquals(movie, director1.getMovie());
        assertEquals(director.getName(), director1.getName());
        assertEquals(director.getAge(), director1.getAge());
        assertEquals(director.getId(), director1.getId());
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
        assertEquals("Worker", entity.name());
        assertEquals("Bob", entity.find("name").get().get());
        assertEquals("BRL 10", entity.find("money").get().get());
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
        assertEquals(worker.getSalary(), worker1.getSalary());
        assertEquals(job.getCity(), worker1.getJob().getCity());
        assertEquals(job.getDescription(), worker1.getJob().getDescription());
    }


    @Test
    public void shouldSupportUDT() {
        Address address = new Address();
        address.setCity("California");
        address.setStreet("Street");

        Contact contact = new Contact();
        contact.setAge(10);
        contact.setName("Ada");
        contact.setHome(address);

        var entity = converter.toCommunication(contact);
        assertEquals("Contact", entity.name());
        Element column = entity.find("home").get();
        UDT udt = UDT.class.cast(column);

        assertEquals("address", udt.userType());
        assertEquals("home", udt.name());
        assertThat((List<Element>) udt.get())
                .contains(Element.of("city", "California"), Element.of("street", "Street"));

    }


    @Test
    public void shouldSupportUDTToEntity() {
        var entity = CommunicationEntity.of("Contact");
        entity.add(Element.of("name", "Poliana"));
        entity.add(Element.of("age", 20));
        List<Element> columns = asList(Element.of("city", "Salvador"),
                Element.of("street", "Jose Anasoh"));
        UDT udt = UDT.builder("address").withName("home")
                .addUDT(columns).build();
        entity.add(udt);

        Contact contact = converter.toEntity(entity);
        assertNotNull(contact);
        Address home = contact.getHome();
        assertEquals("Poliana", contact.getName());
        assertEquals(Integer.valueOf(20), contact.getAge());
        assertEquals("Salvador", home.getCity());
        assertEquals("Jose Anasoh", home.getStreet());

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
        assertEquals("History2", entity.name());
        History2 historyConverted = converter.toEntity(entity);
        assertNotNull(historyConverted);

    }

    @Test
    public void shouldConvertListUDT() {
        AppointmentBook appointmentBook = new AppointmentBook();
        appointmentBook.setUser("otaviojava");
        appointmentBook.setContacts(asList(new org.eclipse.jnosql.databases.cassandra.mapping.model.Contact("Poliana", "poliana@santana.com"),
                new org.eclipse.jnosql.databases.cassandra.mapping.model.Contact("Ada", "ada@lovelace.com")));

        var entity = converter.toCommunication(appointmentBook);
        assertEquals("AppointmentBook", entity.name());
        assertEquals("otaviojava", entity.find("user").get().get());
        UDT column = (UDT) entity.find("contacts").get();

        List<List<Element>> contacts = (List<List<Element>>) column.get();
        assertEquals(2, contacts.size());
        assertTrue(contacts.stream().allMatch(c -> c.size() == 2));
        assertEquals("Contact", column.userType());

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
        assertEquals("otaviojava", appointmentBook.getUser());

        assertThat(contacts).contains(new org.eclipse.jnosql.databases.cassandra.mapping.model.Contact("Poliana", "poliana"),
                new org.eclipse.jnosql.databases.cassandra.mapping.model.Contact("Ada", "ada@lovelace.com"));


    }

    private Object getValue(Optional<Element> document) {
        return document.map(Element::value).map(Value::get).orElse(null);
    }
}
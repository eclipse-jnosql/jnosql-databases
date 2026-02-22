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

package org.eclipse.jnosql.databases.cassandra.communication;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import jakarta.data.exceptions.NonUniqueResultException;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.DefaultSelectQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.Elements;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.communication.semistructured.UpdateQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.eclipse.jnosql.communication.semistructured.DeleteQuery.delete;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class CassandraColumnManagerTest {

    public static final ConsistencyLevel CONSISTENCY_LEVEL = ConsistencyLevel.ONE;
    private CassandraColumnManager entityManager;


    {
        CassandraColumnManagerFactory managerFactory = ColumnDatabase.INSTANCE.get();
        entityManager = managerFactory.apply(Constants.KEY_SPACE);
    }

    @AfterEach
    void afterEach() {
        DefaultCassandraColumnManager manager = DefaultCassandraColumnManager.class.cast(entityManager);
        CqlSession session = manager.getSession();
        if (!session.isClosed()) {
            entityManager.cql("TRUNCATE " + Constants.KEY_SPACE + '.' + Constants.COLUMN_FAMILY);
            entityManager.cql("TRUNCATE " + Constants.KEY_SPACE + '.' + "users");
        }
    }

    @Test
    void shouldClose() {
        entityManager.close();
        DefaultCassandraColumnManager manager = DefaultCassandraColumnManager.class.cast(entityManager);
        CqlSession session = manager.getSession();
        assertTrue(session.isClosed());
    }

    @Test
    void shouldInsertJustKey() {
        Element key = Elements.of("id", 10L);
        var columnEntity = CommunicationEntity.of(Constants.COLUMN_FAMILY);
        columnEntity.add(key);
        entityManager.insert(columnEntity);
    }


    @Test
    void shouldInsertColumns() {
        var columnEntity = getColumnFamily();
        entityManager.insert(columnEntity);
    }

    @Test
    void shouldInsertWithTtl() throws InterruptedException {
        var columnEntity = getColumnFamily();
        entityManager.insert(columnEntity, Duration.ofSeconds(1L));

        sleep(2_000L);

        List<CommunicationEntity> entities = entityManager.select(select().from(Constants.COLUMN_FAMILY).where("id").eq(10L).build())
                .toList();
        assertTrue(entities.isEmpty());
    }

    @Test
    void shouldInsertIterableWithTtl() throws InterruptedException {
        entityManager.insert(getEntities(), Duration.ofSeconds(1L));

        sleep(2_000L);

        List<CommunicationEntity> entities = entityManager.select(select().from(Constants.COLUMN_FAMILY).build())
                .toList();
        assertTrue(entities.isEmpty());
    }

    @Test
    void shouldReturnErrorWhenInsertWithColumnNull() {

        assertThrows(NullPointerException.class, () -> entityManager.insert((CommunicationEntity) null));
    }

    @Test
    void shouldReturnErrorWhenInsertWithConsistencyLevelNull() {

        assertThrows(NullPointerException.class, () -> entityManager.insert(getColumnFamily(), null));

        assertThrows(NullPointerException.class, () -> entityManager.insert(getEntities(), null));
    }

    @Test
    void shouldReturnErrorWhenInsertWithColumnsNull() {

        assertThrows(NullPointerException.class, () -> entityManager.insert((Iterable<CommunicationEntity>) null));
    }


    @Test
    void shouldInsertColumnsWithConsistencyLevel() {
        var columnEntity = getColumnFamily();
        entityManager.save(columnEntity, CONSISTENCY_LEVEL);
    }


    @Test
    void shouldReturnErrorWhenUpdateWithColumnsNull() {

        assertThrows(NullPointerException.class, () -> entityManager.update((Iterable<CommunicationEntity>) null));

    }

    @Test
    void shouldReturnErrorWhenUpdateWithColumnNull() {
        assertThrows(NullPointerException.class, () -> entityManager.update((CommunicationEntity) null));
    }

    @Test
    void shouldUpdateColumn() {
        var columnEntity = getColumnFamily();
        entityManager.update(columnEntity);
    }

    @Test
    void shouldUpdateColumns() {
        entityManager.update(getEntities());
    }

    @Test
    void shouldUpdateWithUpdateQuery() {
        var entities = StreamSupport
                .stream(entityManager.insert(getEntities()).spliterator(), false)
                .toList();

        record UpdateCommand(String name, List<Element> sets, CriteriaCondition criteriaCondition )implements UpdateQuery {

            @Override
            public Optional<CriteriaCondition> where() {
                return Optional.ofNullable(criteriaCondition());
            }

            @Override
            public SelectQuery toSelectQuery() {
                return new DefaultSelectQuery(0, 0, name, emptyList(), emptyList(), criteriaCondition(), false);
            }
        }

        var entityToUpdate = entities.get(0);
        var idField = entityToUpdate.find("id").orElseThrow();

        entityManager.update(
                new UpdateCommand(entityToUpdate.name(),
                        List.of(Element.of("options", asList(4, 5, 6))),
                        CriteriaCondition.eq(idField.name(), idField.get()))
        );

        var updatedEntity = entityManager.select(select().from(entityToUpdate.name())
                .where(idField.name())
                .eq(idField.get()).build())
                .toList();

                StreamSupport.stream(updatedEntity.spliterator(), false)
                .forEach(e -> {
                    Optional<Element> updateField = e.find("options");
                    assertSoftly(soft -> {
                        soft.assertThat(updateField).isPresent();
                        soft.assertThat(updateField).get().extracting(Element::name).isEqualTo("options");
                        soft.assertThat(updateField).get().extracting(Element::get).isEqualTo( asList(4, 5, 6));
                    });
                });
    }

    @Test
    void shouldReturnErrorWhenSaveHasNullElement() {
        assertThrows(NullPointerException.class, () -> entityManager.save((CommunicationEntity) null, Duration.ofSeconds(1L), ConsistencyLevel.ALL));

        assertThrows(NullPointerException.class, () -> entityManager.save(getColumnFamily(), null, ConsistencyLevel.ALL));

        assertThrows(NullPointerException.class, () -> entityManager.save(getColumnFamily(), Duration.ofSeconds(1L), null));
    }

    @Test
    void shouldReturnErrorWhenSaveIterableHasNullElement() {
        assertThrows(NullPointerException.class, () -> entityManager.save((List<CommunicationEntity>) null, Duration.ofSeconds(1L), ConsistencyLevel.ALL));

        assertThrows(NullPointerException.class, () -> entityManager.save(getEntities(), null, ConsistencyLevel.ALL));

        assertThrows(NullPointerException.class, () -> entityManager.save(getEntities(), Duration.ofSeconds(1L), null));
    }

    @Test
    void shouldFindAll() {
        var columnEntity = getColumnFamily();
        entityManager.insert(columnEntity);

        var query = select().from(columnEntity.name()).build();
        List<CommunicationEntity> entities = entityManager.select(query).toList();
        assertFalse(entities.isEmpty());
    }

    @Test
    void shouldReturnSingleResult() {
        var columnEntity = getColumnFamily();
        entityManager.insert(columnEntity);
        var query = select().from(columnEntity.name()).where("id").eq(10L).build();
        Optional<CommunicationEntity> entity = entityManager.singleResult(query);

        query = select().from(columnEntity.name()).where("id").eq(-10L).build();
        entity = entityManager.singleResult(query);
        assertFalse(entity.isPresent());

    }

    @Test
    void shouldReturnErrorWhenThereIsNotThanOneResultInSingleResult() {
        entityManager.insert(getEntities());
        var query = select().from(Constants.COLUMN_FAMILY).build();
        assertThrows(NonUniqueResultException.class, () -> entityManager.singleResult(query));
    }

    @Test
    void shouldReturnErrorWhenQueryIsNull() {
        assertThrows(NullPointerException.class, () -> entityManager.select(null));

        assertThrows(NullPointerException.class, () -> entityManager.singleResult(null));
    }


    @Test
    void shouldFindById() {

        entityManager.insert(getColumnFamily());

        var query = select().from(Constants.COLUMN_FAMILY).where("id").eq(10L).build();
        List<CommunicationEntity> columnEntity = entityManager.select(query).toList();
        assertFalse(columnEntity.isEmpty());
        List<Element> columns = columnEntity.get(0).elements();
        assertThat(columns.stream().map(Element::name).collect(toList()))
                .contains("name", "version", "options", "id");
        assertThat(columns.stream().map(Element::value).map(Value::get).collect(toList())).contains
                ("Cassandra", 3.2, asList(1, 2, 3), 10L);

    }

    @Test
    void shouldFindByIdWithConsistenceLevel() {

        entityManager.insert(getColumnFamily());
        var query = select().from(Constants.COLUMN_FAMILY).where("id").eq(10L).build();
        List<CommunicationEntity> columnEntity = entityManager.select(query, CONSISTENCY_LEVEL).toList();
        assertFalse(columnEntity.isEmpty());
        List<Element> columns = columnEntity.get(0).elements();
        assertThat(columns.stream().map(Element::name).collect(toList())).contains("name", "version", "options", "id");
        assertThat(columns.stream().map(Element::value).map(Value::get).collect(toList()))
                .contains("Cassandra", 3.2, asList(1, 2, 3), 10L);

    }

    @Test
    void shouldRunNativeQuery() {
        entityManager.insert(getColumnFamily());
        List<CommunicationEntity> entities = entityManager.cql("select * from newKeySpace.newColumnFamily where id=10;")
                .toList();
        assertFalse(entities.isEmpty());
        List<Element> columns = entities.get(0).elements();
        assertThat(columns.stream().map(Element::name).collect(toList())).contains("name", "version", "options", "id");
        assertThat(columns.stream().map(Element::value).map(Value::get).collect(toList()))
                .contains("Cassandra", 3.2, asList(1, 2, 3), 10L);
    }

    @Test
    void shouldRunNativeQuery2() {
        entityManager.insert(getColumnFamily());
        String query = "select * from newKeySpace.newColumnFamily where id = :id;";
        List<CommunicationEntity> entities = entityManager.cql(query, singletonMap("id", 10L)).toList();
        assertFalse(entities.isEmpty());
        List<Element> columns = entities.get(0).elements();
        assertThat(columns.stream().map(Element::name).collect(toList()))
                .contains("name", "version", "options", "id");
        assertThat(columns.stream().map(Element::value).map(Value::get).collect(toList()))
                .contains("Cassandra", 3.2, asList(1, 2, 3), 10L);
    }

    @Test
    void shouldPrepareStatement() {
        entityManager.insert(getColumnFamily());
        CassandraPreparedStatement preparedStatement = entityManager.nativeQueryPrepare("select * from newKeySpace.newColumnFamily where id=?");
        preparedStatement.bind(10L);
        List<CommunicationEntity> entities = preparedStatement.executeQuery().toList();
        List<Element> columns = entities.get(0).elements();
        assertThat(columns.stream().map(Element::name).collect(toList()))
                .contains("name", "version", "options", "id");
        assertThat(columns.stream().map(Element::value).map(Value::get)
                .collect(toList())).contains("Cassandra", 3.2, asList(1, 2, 3), 10L);
    }

    @Test
    void shouldDeleteColumnFamily() {
        entityManager.insert(getColumnFamily());
        CommunicationEntity.of(Constants.COLUMN_FAMILY, singletonList(Elements.of("id", 10L)));
        var query = select().from(Constants.COLUMN_FAMILY).where("id").eq(10L).build();
        var deleteQuery = delete().from(Constants.COLUMN_FAMILY).where("id").eq(10L).build();
        entityManager.delete(deleteQuery);
        List<CommunicationEntity> entities = entityManager.cql("select * from newKeySpace.newColumnFamily where id=10;")
                .toList();
        assertTrue(entities.isEmpty());
    }

    @Test
    void shouldReturnErrorWhenDeleteQueryIsNull() {
        assertThrows(NullPointerException.class, () -> entityManager.delete(null));
    }

    @Test
    void shouldReturnErrorWhenDeleteConsistencyLevelIsNull() {
        assertThrows(NullPointerException.class, () -> entityManager.delete(delete().from(Constants.COLUMN_FAMILY).build(), null));
    }

    @Test
    void shouldDeleteColumnFamilyWithConsistencyLevel() {
        entityManager.insert(getColumnFamily());
        CommunicationEntity.of(Constants.COLUMN_FAMILY, singletonList(Elements.of("id", 10L)));
        var deleteQuery = delete().from(Constants.COLUMN_FAMILY).where("id").eq(10L).build();
        entityManager.delete(deleteQuery, CONSISTENCY_LEVEL);
        List<CommunicationEntity> entities = entityManager.cql("select * from newKeySpace.newColumnFamily where id=10;")
                .toList();
        assertTrue(entities.isEmpty());
    }

    @Test
    void shouldLimitResult() {
        getEntities().forEach(entityManager::insert);
        var query = select().from(Constants.COLUMN_FAMILY).where("id").in(Arrays.asList(1L, 2L, 3L))
                .limit(2).build();
        List<CommunicationEntity> columnFamilyEntities = entityManager.select(query).toList();
        assertEquals(Integer.valueOf(2), Integer.valueOf(columnFamilyEntities.size()));
    }

    private List<CommunicationEntity> getEntities() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", "Cassandra");
        fields.put("version", 3.2);
        fields.put("options", asList(1, 2, 3));
        List<Element> columns = Elements.of(fields);
        var entity = CommunicationEntity.of(Constants.COLUMN_FAMILY, singletonList(Elements.of("id", 1L)));
        var entity1 = CommunicationEntity.of(Constants.COLUMN_FAMILY, singletonList(Elements.of("id", 2L)));
        var entity2 = CommunicationEntity.of(Constants.COLUMN_FAMILY, singletonList(Elements.of("id", 3L)));
        columns.forEach(entity::add);
        columns.forEach(entity1::add);
        columns.forEach(entity2::add);
        return asList(entity, entity1, entity2);
    }

    @Test
    void shouldSupportUDT() {
        var entity = CommunicationEntity.of("users");
        entity.add(Element.of("nickname", "ada"));
        List<Element> columns = new ArrayList<>();
        columns.add(Element.of("firstname", "Ada"));
        columns.add(Element.of("lastname", "Lovelace"));
        UDT udt = UDT.builder("fullname").withName("name")
                .addUDT(columns).build();
        entity.add(udt);
        entityManager.insert(entity);

        var query = select().from("users").build();
        var columnEntity = entityManager.singleResult(query).get();
        var column = columnEntity.find("name").get();
        udt = UDT.class.cast(column);
        List<Element> udtColumns = (List<Element>) udt.get();
        assertEquals("name", udt.name());
        assertEquals("fullname", udt.userType());
        assertThat(udtColumns).contains(Element.of("firstname", "Ada"),
                Element.of("lastname", "Lovelace"));
    }

    @Test
    void shouldSupportAnUDTElement() {
        var entity = CommunicationEntity.of("users");
        entity.add(Element.of("nickname", "Ioda"));
        List<Element> columns = new ArrayList<>();
        columns.add(Element.of("firstname", "Ioda"));
        UDT udt = UDT.builder("fullname").withName("name")
                .addUDT(columns).build();
        entity.add(udt);
        entityManager.insert(entity);

        var query = select().from("users")
                .where("nickname").eq("Ioda")
                .build();

        var columnEntity = entityManager.singleResult(query).get();
        Element column = columnEntity.find("name").get();
        udt = UDT.class.cast(column);
        List<Element> udtColumns = (List<Element>) udt.get();
        assertEquals("name", udt.name());
        assertEquals("fullname", udt.userType());
        assertThat(udtColumns).contains(Element.of("firstname", "Ioda"));
    }


    @Test
    void shouldSupportDate() {
        var entity = CommunicationEntity.of("history");
        entity.add(Element.of("name", "World war II"));
        ZoneId defaultZoneId = ZoneId.systemDefault();
        Instant dateEnd = LocalDate.of(1945, Month.SEPTEMBER, 2).atStartOfDay(defaultZoneId).toInstant();
        LocalDateTime dataStart = LocalDateTime.now();
        entity.add(Element.of("dataStart", LocalDate.of(1939, 9, 1)));
        entity.add(Element.of("dateEnd", dateEnd));
        entityManager.insert(entity);
        var query = select().from("history")
                .where("name").eq("World war II")
                .build();

        var entity1 = entityManager.singleResult(query).get();
        assertNotNull(entity1);

    }

    @Test
    void shouldSupportListUDTs() {
        var entity = createEntityWithIterable();
        entityManager.insert(entity);
    }

    @Test
    void shouldReturnListUDT() {
        var entity = createEntityWithIterable();
        entityManager.insert(entity);

        var query = select().from("contacts").where("user").eq("otaviojava").build();
        var columnEntity = entityManager.singleResult(query).get();
        List<List<Element>> names = (List<List<Element>>) columnEntity.find("names").get().get();
        assertEquals(3, names.size());
        assertTrue(names.stream().allMatch(n -> n.size() == 2));
    }

    @Test
    void shouldCount() {
        var entity = createEntityWithIterable();
        entityManager.insert(entity);
        long contacts = entityManager.count("contacts");
        assertTrue(contacts > 0);
    }

    @Test
    void shouldCountBySelectQuery() {
        entityManager.insert(getColumnFamily());
        var query = select().from(Constants.COLUMN_FAMILY).where("id").eq(10L).build();
        assertSoftly(softly -> {

            softly.assertThat(entityManager.count(query))
                    .as("the implementation is returning an non expected value")
                    .isEqualTo(1);

            softly.assertThatThrownBy(() -> entityManager.count(query, null))
                    .as("must throw NPE when call the count method passing null arguments")
                    .isInstanceOf(NullPointerException.class);

            softly.assertThatThrownBy(() -> entityManager.count(null, null))
                    .as("must throw NPE when call the count method passing null arguments")
                    .isInstanceOf(NullPointerException.class);

            softly.assertThatThrownBy(() -> entityManager.count((SelectQuery) null))
                    .as("must throw NPE when call the count method passing null arguments")
                    .isInstanceOf(NullPointerException.class);

        });

    }

    @Test
    void shouldPagingState() {
        for (long index = 1; index <= 10; index++) {
            var columnFamily = getColumnFamily();
            columnFamily.add("id", index);
            entityManager.insert(columnFamily);
        }

        var query = select().from(Constants.COLUMN_FAMILY).build();
        CassandraQuery cassandraQuery = CassandraQuery.of(query);

        assertFalse(cassandraQuery.getPagingState().isPresent());

        List<CommunicationEntity> entities = entityManager.select(cassandraQuery).toList();
        assertEquals(10, entities.size());
        assertTrue(cassandraQuery.getPagingState().isPresent());
    }

    @Test
    void shouldPaginate() {
        for (long index = 1; index <= 10; index++) {
            var columnFamily = getColumnFamily();
            columnFamily.add("id", index);
            entityManager.insert(columnFamily);
        }

        var query = select().from(Constants.COLUMN_FAMILY).limit(4).skip(2).build();
        List<CommunicationEntity> entities = entityManager.select(query).toList();
        assertEquals(4, entities.size());
    }

    @Test
    void shouldCreateUDTWithSet() {
        var entity = createEntityWithIterableSet();
        entityManager.insert(entity);
        SelectQuery query = select().from("agenda").build();
        var result = entityManager.singleResult(query).get();
        Assertions.assertEquals(Element.of("user", "otaviojava"), result.find("user").get());
        Assertions.assertEquals(2, result.size());
        List<List<Element>> names = (List<List<Element>>) result.find("names").get().get();
        assertEquals(3, names.size());
        assertTrue(names.stream().allMatch(n -> n.size() == 2));
    }

    @Test
    void shouldInsertNullValues(){
        var family = getColumnFamily();
        family.add(Element.of("name", null));
        var columnEntity = entityManager.insert(family);
        var column = columnEntity.find("name");
        assertSoftly(soft -> {
            soft.assertThat(column).get().extracting(Element::name).isEqualTo("name");
            soft.assertThat(column).get().extracting(Element::get).isNull();
        });
    }

    @Test
    void shouldUpdateNullValues(){
        var family = getColumnFamily();
        entityManager.insert(family);
        family.addNull("name");
        var columnEntity = entityManager.update(family);
        var column = columnEntity.find("name");
        assertSoftly(soft -> {
            soft.assertThat(column).get().extracting(Element::name).isEqualTo("name");
            soft.assertThat(column).get().extracting(Element::get).isNull();
        });
    }

    private CommunicationEntity createEntityWithIterable() {
        var entity = CommunicationEntity.of("contacts");
        entity.add(Element.of("user", "otaviojava"));

        List<Iterable<Element>> columns = new ArrayList<>();
        columns.add(asList(Element.of("firstname", "Poliana"), Element.of("lastname", "Santana")));
        columns.add(asList(Element.of("firstname", "Ada"), Element.of("lastname", "Lovelace")));
        columns.add(asList(Element.of("firstname", "Maria"), Element.of("lastname", "Goncalves")));
        UDT udt = UDT.builder("fullname").withName("names")
                .addUDTs(columns).build();
        entity.add(udt);
        return entity;
    }

    private CommunicationEntity createEntityWithIterableSet() {
        var entity = CommunicationEntity.of("agenda");
        entity.add(Element.of("user", "otaviojava"));

        List<Iterable<Element>> columns = new ArrayList<>();
        columns.add(asList(Element.of("firstname", "Poliana"), Element.of("lastname", "Santana")));
        columns.add(asList(Element.of("firstname", "Ada"), Element.of("lastname", "Lovelace")));
        columns.add(asList(Element.of("firstname", "Maria"), Element.of("lastname", "Goncalves")));
        UDT udt = UDT.builder("fullname").withName("names")
                .addUDTs(columns).build();
        entity.add(udt);
        return entity;
    }

    private CommunicationEntity getColumnFamily() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", "Cassandra");
        fields.put("version", 3.2);
        fields.put("options", asList(1, 2, 3));
        List<Element> columns = Elements.of(fields);
        var columnFamily = CommunicationEntity.of(Constants.COLUMN_FAMILY, singletonList(Elements.of("id", 10L)));
        columns.forEach(columnFamily::add);
        return columnFamily;
    }

}
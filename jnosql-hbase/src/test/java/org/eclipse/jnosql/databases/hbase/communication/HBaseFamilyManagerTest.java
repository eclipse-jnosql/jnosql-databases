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

package org.eclipse.jnosql.databases.hbase.communication;

import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
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
public class HBaseFamilyManagerTest {

    private static final String DATA_BASE = "database";
    public static final String FAMILY = "person";
    public static final String ID_FIELD = HBaseUtils.KEY_COLUMN;

    private DatabaseManagerFactory managerFactory;

    private DatabaseManager manager;

    @BeforeEach
    public void setUp() {
        HBaseColumnConfiguration configuration = new HBaseColumnConfiguration();
        configuration.add(FAMILY);
        managerFactory = configuration.apply(Settings.builder().build());
        manager = managerFactory.apply(DATA_BASE);
    }


    @Test
    public void shouldSave() {
        var entity = createEntity();
        manager.insert(entity);
    }

    @Test
    public void shouldReturnErrorWhenKeyIsNotDefined() {
        var entity = CommunicationEntity.of(FAMILY);
        entity.add(Element.of("id", "otaviojava"));
        entity.add(Element.of("age", 26));
        entity.add(Element.of("country", "Brazil"));
        assertThrows(HBaseException.class, () -> manager.insert(entity));
    }

    @Test
    public void shouldFind() {
        manager.insert(createEntity());

        var query = select().from(FAMILY).where(ID_FIELD).eq("otaviojava").build();
        List<CommunicationEntity> columnFamilyEntities = manager.select(query).collect(Collectors.toList());
        assertNotNull(columnFamilyEntities);
        assertFalse(columnFamilyEntities.isEmpty());
        var entity = columnFamilyEntities.getFirst();
        assertEquals(FAMILY, entity.name());
        assertThat(entity.elements()).contains(Element.of(ID_FIELD, "otaviojava"),
                Element.of("age", "26"), Element.of("country", "Brazil"));
    }

    @Test
    public void shouldFindInBatch() {
        manager.insert(createEntity());
        manager.insert(createEntity2());

        var query = select().from(FAMILY).where(ID_FIELD).eq("otaviojava")
                .or(ID_FIELD).eq("poliana").build();

        List<CommunicationEntity> entities = manager.select(query).toList();
        assertEquals(Integer.valueOf(2), Integer.valueOf(entities.size()));

    }

    @Test
    public void shouldDeleteEntity() {
        manager.insert(createEntity());
        var query = select().from(FAMILY).where(ID_FIELD).eq("otaviojava").build();
        var deleteQuery = delete().from(FAMILY).where(ID_FIELD).eq("otaviojava").build();
        manager.delete(deleteQuery);
        List<CommunicationEntity> entities = manager.select(query).toList();
        assertTrue(entities.isEmpty());
    }

    @Test
    public void shouldDeleteEntities() {
        manager.insert(createEntity());
        manager.insert(createEntity2());

        SelectQuery query = select().from(FAMILY).where(ID_FIELD).eq("otaviojava")
                .or(ID_FIELD).eq("poliana").build();

        var deleteQuery = delete().from(FAMILY).where(ID_FIELD).eq("otaviojava")
                .or(ID_FIELD).eq("poliana").build();

        manager.delete(deleteQuery);
        List<CommunicationEntity> entities = manager.select(query).toList();
        assertTrue(entities.isEmpty());
    }

    private CommunicationEntity createEntity() {
        CommunicationEntity entity = CommunicationEntity.of(FAMILY);
        entity.add(Element.of(ID_FIELD, "otaviojava"));
        entity.add(Element.of("age", 26));
        entity.add(Element.of("country", "Brazil"));
        return entity;
    }

    private CommunicationEntity createEntity2() {
        CommunicationEntity entity = CommunicationEntity.of(FAMILY);
        entity.add(Element.of(ID_FIELD, "poliana"));
        entity.add(Element.of("age", 24));
        entity.add(Element.of("country", "Brazil"));
        return entity;
    }


}
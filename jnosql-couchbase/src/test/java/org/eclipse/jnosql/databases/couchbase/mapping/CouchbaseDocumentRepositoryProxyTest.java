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
package org.eclipse.jnosql.databases.couchbase.mapping;

import com.couchbase.client.java.json.JsonObject;
import jakarta.data.repository.Param;
import jakarta.inject.Inject;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;
import org.eclipse.jnosql.mapping.document.spi.DocumentExtension;
import org.eclipse.jnosql.mapping.keyvalue.AbstractKeyValueTemplate;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@EnableAutoWeld
@AddPackages(value = {Converters.class, AbstractKeyValueTemplate.class,
        EntityConverter.class, DocumentTemplate.class, N1QL.class})
@AddPackages(MockProducer.class)
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class,
        DocumentExtension.class, CouchbaseExtension.class})
public class CouchbaseDocumentRepositoryProxyTest {

    private CouchbaseTemplate template;

    @Inject
    private Converters converters;

    @Inject
    private EntitiesMetadata entitiesMetadata;

    private HumanRepository humanRepository;


    @BeforeEach
    public void setUp() {
        this.template = Mockito.mock(CouchbaseTemplate.class);

        CouchbaseDocumentRepositoryProxy handler = new CouchbaseDocumentRepositoryProxy(template,
                HumanRepository.class, converters, entitiesMetadata);

        when(template.insert(any(Human.class))).thenReturn(new Human());
        when(template.insert(any(Human.class), any(Duration.class))).thenReturn(new Human());
        when(template.update(any(Human.class))).thenReturn(new Human());
        humanRepository = (HumanRepository) Proxy.newProxyInstance(HumanRepository.class.getClassLoader(),
                new Class[]{HumanRepository.class},
                handler);
    }


    @Test
    public void shouldFindAll() {
        humanRepository.findAllQuery();
        verify(template).n1qlQuery("select * from Person");
    }

    @Test
    public void shouldFindByNameN1ql() {
        ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
        humanRepository.findByName("Ada");
        verify(template).n1qlQuery(Mockito.eq("select * from Person where name = $name"), captor.capture());

        JsonObject value = captor.getValue();

        assertEquals("Ada", value.getString("name"));
    }

    @Test
    public void shouldSaveUsingInsert() {
        Human human = Human.of("Ada", 10);
        humanRepository.save(human);
        verify(template).insert(eq(human));
    }


    @Test
    public void shouldSaveUsingUpdate() {
        Human human = Human.of("Ada-2", 10);
        when(template.find(Human.class, "Ada-2")).thenReturn(Optional.of(human));
        humanRepository.save(human);
        verify(template).update(eq(human));
    }

    @Test
    public void shouldDelete(){
        humanRepository.deleteById("id");
        verify(template).delete(Human.class, "id");
    }


    @Test
    public void shouldDeleteEntity(){
        Human human = Human.of("Ada", 10);
        humanRepository.delete(human);
        verify(template).delete(Human.class, human.getName());
    }

    interface HumanRepository extends CouchbaseRepository<Human, String> {

        @N1QL("select * from Person")
        List<Human> findAllQuery();

        @N1QL("select * from Person where name = $name")
        List<Human> findByName(@Param("name") String name);
    }
}
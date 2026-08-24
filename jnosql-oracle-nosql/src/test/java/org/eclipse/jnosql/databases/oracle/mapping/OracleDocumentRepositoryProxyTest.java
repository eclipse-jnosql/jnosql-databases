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
package org.eclipse.jnosql.databases.oracle.mapping;

import jakarta.data.repository.Param;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;
import org.eclipse.jnosql.mapping.document.spi.DocumentExtension;
import org.eclipse.jnosql.mapping.keyvalue.spi.KeyValueExtension;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class, DocumentTemplate.class, SQL.class})
@AddPackages(MockProducer.class)
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, KeyValueExtension.class,
        DocumentExtension.class, OracleExtension.class})
public class OracleDocumentRepositoryProxyTest {

    private OracleNoSQLTemplate template;
    @Inject
    private EntitiesMetadata entitiesMetadata;

    @Inject
    private Converters converters;

    private HumanNoSQLRepository personRepository;

    @SuppressWarnings("rawtypes")
    @BeforeEach
    public void setUp() {
        this.template = Mockito.mock(OracleNoSQLTemplate.class);

        OracleDocumentRepositoryProxy handler = new OracleDocumentRepositoryProxy<>(template,
                HumanNoSQLRepository.class, converters, entitiesMetadata);

        when(template.insert(any(Human.class))).thenReturn(new Human());
        when(template.insert(any(Human.class), any(Duration.class))).thenReturn(new Human());
        when(template.update(any(Human.class))).thenReturn(new Human());
        this.personRepository = (HumanNoSQLRepository) Proxy.newProxyInstance(HumanNoSQLRepository.class.getClassLoader(),
                new Class[]{HumanNoSQLRepository.class},
                handler);
    }


    @Test
    public void shouldFindAll() {
        personRepository.findAllQuery();
        verify(template).sql("select * from Person");
    }

    @Test
    public void shouldFindByNameSQL() {
        ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
        personRepository.findByName("Ada");
        verify(template).sql(eq("select * from Person where content.name= ?"), captor.capture());

        Object[] value = captor.getValue();
        Assertions.assertThat(value).hasSize(1).contains("Ada");
    }

    @Test
    public void shouldSaveUsingInsert() {
        Human human = Human.of("Ada", 10);
        personRepository.save(human);
        verify(template).insert(eq(human));
    }


    @Test
    public void shouldSaveUsingUpdate() {
        Human human = Human.of("Ada-2", 10);
        when(template.find(Human.class, "Ada-2")).thenReturn(Optional.of(human));
        personRepository.save(human);
        verify(template).update(eq(human));
    }

    @Test
    public void shouldDelete(){
        personRepository.deleteById("id");
        verify(template).delete(Human.class, "id");
    }


    @Test
    public void shouldDeleteEntity(){
        Human human = Human.of("Ada", 10);
        personRepository.delete(human);
        verify(template).delete(Human.class, human.getName());
    }

    @Test
    public void shouldDeleteAll() {
        ArgumentCaptor<Class<?>> queryCaptor = ArgumentCaptor.forClass(Class.class);

        personRepository.deleteAll();
        verify(template).deleteAll(queryCaptor.capture());

        Class<?> query = queryCaptor.getValue();
        Assertions.assertThat(query).isEqualTo(Human.class);
    }


    interface HumanNoSQLRepository extends OracleNoSQLRepository<Human, String> {

        @SQL("select * from Person")
        List<Human> findAllQuery();

        @SQL("select * from Person where content.name= ?")
        List<Human> findByName(@Param("") String name);
    }
}

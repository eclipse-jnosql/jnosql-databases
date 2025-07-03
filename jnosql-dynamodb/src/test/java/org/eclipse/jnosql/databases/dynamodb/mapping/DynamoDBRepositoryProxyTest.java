/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *
 * Maximillian Arruda
 */

package org.eclipse.jnosql.databases.dynamodb.mapping;

import ee.jakarta.tck.nosql.entities.Person;
import jakarta.data.repository.Param;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.document.spi.DocumentExtension;
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
@AddPackages(value = {Converters.class, EntityConverter.class, PartiQL.class})
@AddPackages(MockProducer.class)
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, DocumentExtension.class, DynamoDBExtension.class})
class DynamoDBRepositoryProxyTest {

    private DynamoDBTemplate template;

    @Inject
    private EntitiesMetadata entitiesMetadata;

    @Inject
    private Converters converters;

    private HumanNoSQLRepository personRepository;

    @SuppressWarnings("rawtypes")
    @BeforeEach
    void setUp() {
        this.template = Mockito.mock(DynamoDBTemplate.class);

        DynamoDBRepositoryProxy handler = new DynamoDBRepositoryProxy<>(template,
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
        verify(template).partiQL(eq("select * from Person"), eq(Human.class), eq(new Object[0]));
    }

    @Test
    public void shouldFindByNameSQL() {
        ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
        personRepository.findByName("Ada");
        verify(template).partiQL(eq("select * from Person where name= ?"), eq(Human.class), captor.capture());

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
    public void shouldDelete() {
        personRepository.deleteById("id");
        verify(template).delete(Human.class, "id");
    }


    @Test
    public void shouldDeleteEntity() {
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

    interface HumanNoSQLRepository extends DynamoDBRepository<Human, String> {

        @PartiQL("select * from Person")
        List<Human> findAllQuery();

        @PartiQL("select * from Person where name= ?")
        List<Human> findByName(@Param("") String name);
    }

}

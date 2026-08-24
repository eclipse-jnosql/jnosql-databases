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
package org.eclipse.jnosql.databases.hazelcast.mapping;

import jakarta.data.repository.Param;
import jakarta.inject.Inject;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.keyvalue.AbstractKeyValueTemplate;
import org.eclipse.jnosql.mapping.keyvalue.spi.KeyValueExtension;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableAutoWeld
@AddPackages(value = {Converters.class, AbstractKeyValueTemplate.class, Query.class})
@AddPackages(MockProducer.class)
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class,
        KeyValueExtension.class, HazelcastExtension.class})
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class HazelcastRepositoryProxyTest {

    @Mock
    private HazelcastTemplate template;


    @Inject
    private EntitiesMetadata entitiesMetadata;

    private HumanRepository humanRepository;


    @SuppressWarnings("rawtypes")
    @BeforeEach
    public void setUp() {

        Collection<Object> people = asList(new Human("Poliana", 25), new Human("Otavio", 28));

        when(template.sql(anyString())).thenReturn(people);
        HazelcastRepositoryProxy handler = new HazelcastRepositoryProxy<>(template, HumanRepository.class, entitiesMetadata);

        when(template.sql(anyString(), any(Map.class))).thenReturn(people);

        humanRepository = (HumanRepository) Proxy.newProxyInstance(HumanRepository.class.getClassLoader(),
                new Class[]{HumanRepository.class},
                handler);
    }

    @Test
    public void shouldFindAll() {
        List<Human> people = humanRepository.findActive();
        verify(template).sql("active");
        assertNotNull(people);
        assertTrue(people.stream().allMatch(Human.class::isInstance));
    }

    @Test
    public void shouldFindByAgeAndInteger() {
        Set<Human> people = humanRepository.findByAgeAndInteger("Ada", 10);
        Map<String, Object> params = new HashMap<>();
        params.put("age", 10);
        params.put("name", "Ada");
        verify(template).sql("name = :name AND age = :age", params);
        assertNotNull(people);
        assertTrue(people.stream().allMatch(Human.class::isInstance));
    }

    @Test
    public void shouldSaveUsingInsert() {
        Human human = Human.of("Ada", 10);
        humanRepository.save(human);
    }


    @Test
    public void shouldSaveUsingUpdate() {
        Human human = Human.of("Ada-2", 10);
        when(template.find(Human.class, "Ada-2")).thenReturn(Optional.of(human));
        humanRepository.save(human);
    }

    @Test
    public void shouldDelete(){
        humanRepository.deleteById("id");
    }


    @Test
    public void shouldDeleteEntity(){
        Human human = Human.of("Ada", 10);
        humanRepository.delete(human);
    }


    interface HumanRepository extends HazelcastRepository<Human, String> {

        @Query("active")
        List<Human> findActive();

        @Query("name = :name AND age = :age")
        Set<Human> findByAgeAndInteger(@Param("name") String name,@Param("age") Integer age);
    }
}
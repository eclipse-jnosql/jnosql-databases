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
package org.eclipse.jnosql.databases.arangodb.mapping;

import jakarta.data.repository.Param;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;
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
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class, DocumentTemplate.class, AQL.class})
@AddPackages(MockProducer.class)
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, DocumentExtension.class, ArangoDBExtension.class})
public class ArangoDBDocumentRepositoryProxyTest {

    private ArangoDBTemplate template;
    @Inject
    private EntitiesMetadata entitiesMetadata;

    @Inject
    private Converters converters;

    private HumanRepository humanRepository;

    @SuppressWarnings("rawtypes")
    @BeforeEach
    public void setUp() {
        this.template = Mockito.mock(ArangoDBTemplate.class);

        ArangoDBDocumentRepositoryProxy handler = new ArangoDBDocumentRepositoryProxy<>(template,
                HumanRepository.class, converters, entitiesMetadata);

        when(template.insert(any(Human.class))).thenReturn(new Human());
        when(template.insert(any(Human.class), any(Duration.class))).thenReturn(new Human());
        when(template.update(any(Human.class))).thenReturn(new Human());
        this.humanRepository = (HumanRepository) Proxy.newProxyInstance(HumanRepository.class.getClassLoader(),
                new Class[]{HumanRepository.class},
                handler);
    }


    @Test
    public void shouldFindAll() {
        humanRepository.findAllQuery();
        verify(template).aql("FOR p IN Person RETURN p", emptyMap());
    }

    @Test
    public void shouldFindByNameAQL() {
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        humanRepository.findByName("Ada");
        verify(template).aql(eq("FOR p IN Person FILTER p.name = @name RETURN p"), captor.capture());

        Map value = captor.getValue();
        assertEquals("Ada", value.get("name"));
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

    @Test
    public void shouldDeleteAll() {
        ArgumentCaptor<Class<?>> queryCaptor = ArgumentCaptor.forClass(Class.class);

        humanRepository.deleteAll();
        verify(template).deleteAll(queryCaptor.capture());

        Class<?> query = queryCaptor.getValue();
        Assertions.assertThat(query).isEqualTo(Human.class);
    }


    interface HumanRepository extends ArangoDBRepository<Human, String> {

        @AQL("FOR p IN Person RETURN p")
        List<Human> findAllQuery();

        @AQL("FOR p IN Person FILTER p.name = @name RETURN p")
        List<Human> findByName(@Param("name") String name);
    }
}

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
package org.eclipse.jnosql.databases.orientdb.mapping;

import ee.jakarta.tck.nosql.entities.Person;
import jakarta.data.repository.Param;
import jakarta.inject.Inject;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;
import org.eclipse.jnosql.mapping.document.spi.DocumentExtension;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.repository.LifecycleEventHandler;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;


@EnableAutoWeld
@AddPackages(value = {Converters.class,
        EntityConverter.class, DocumentTemplate.class, SQL.class})
@AddPackages(MockProducer.class)
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class,
        DocumentExtension.class, OrientDBExtension.class})
public class OrientDBDocumentRepositoryProxyTest {

    private OrientDBTemplate template;

    @Inject
    private EntitiesMetadata entitiesMetadata;

    @Inject
    private Converters converters;

    @Inject
    private LifecycleEventHandler lifecycleEventHandler;

    private HumanRepository humanRepository;


    @BeforeEach
    public void setUp() {
        this.template = Mockito.mock(OrientDBTemplate.class);
        OrientDBDocumentRepositoryProxy handler = new OrientDBDocumentRepositoryProxy(template,
                HumanRepository.class, converters, entitiesMetadata, lifecycleEventHandler);

        when(template.insert(any(Person.class))).thenReturn(new Person());
        when(template.insert(any(Person.class), any(Duration.class))).thenReturn(new Person());
        when(template.update(any(Person.class))).thenReturn(new Person());
        this.humanRepository = (HumanRepository) Proxy.newProxyInstance(HumanRepository.class.getClassLoader(),
                new Class[]{HumanRepository.class},
                handler);
    }

    @Test
    public void shouldFindAll() {
        humanRepository.findAllQuery();
        verify(template).sql("select * from Person");
    }

    @Test
    public void shouldFindByNameSQL() {
        humanRepository.findByName("Ada");
        verify(template).sql(Mockito.eq("select * from Person where name = ?"), Mockito.any(Object.class));
    }

    @Test
    public void shouldFindByNameSQL2() {
        humanRepository.findByAge(10);
        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(template).sql(Mockito.eq("select * from Person where age = :age"), argumentCaptor.capture());
        Map value = argumentCaptor.getValue();
        assertThat(value.get("age")).isEqualTo(10);
    }

    @Test
    public void shouldSaveUsingInsert() {
        var person = new Person();
        person.setName("Ada");
        person.setAge(10);
        humanRepository.save(person);
        verify(template).insert(eq(person));
    }


    @Test
    public void shouldSaveUsingUpdate() {
        var person = new Person();
        person.setName("Ada-2");
        person.setAge(10);
        person.setId("10");
        when(template.find(Person.class, "10")).thenReturn(Optional.of(person));
        humanRepository.save(person);
        verify(template).update(eq(person));
    }

    @Test
    public void shouldDelete(){
        humanRepository.deleteById("id");
        verify(template).delete(Person.class, "id");
    }


    @Test
    public void shouldDeleteEntity(){

        var person = new Person();
        person.setName("Ada");
        person.setAge(10);
        person.setId("10");
        humanRepository.delete(person);
        verify(template).delete(Person.class, person.getId());
    }

    interface HumanRepository extends OrientDBCrudRepository<Person, String> {

        @SQL("select * from Person")
        List<Person> findAllQuery();

        @SQL("select * from Person where name = ?")
        List<Person> findByName(String name);

        @SQL("select * from Person where age = :age")
        List<Person> findByAge(@Param("age") Integer age);
    }
}
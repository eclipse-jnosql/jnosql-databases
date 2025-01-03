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

import jakarta.inject.Inject;
import jakarta.nosql.tck.entities.Person;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;
import org.eclipse.jnosql.mapping.document.spi.DocumentExtension;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.core.spi.EntityMetadataExtension;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@EnableAutoWeld
@AddPackages(value = {Converters.class,
        EntityConverter.class, DocumentTemplate.class, SQL.class})
@AddPackages(MockProducer.class)
@AddPackages(Reflections.class)
@AddExtensions({EntityMetadataExtension.class,
        DocumentExtension.class, OrientDBExtension.class})
public class OrientDBDocumentRepositoryProxyTest {

    private OrientDBTemplate template;

    @Inject
    private EntitiesMetadata entitiesMetadata;

    @Inject
    private Converters converters;

    private PersonRepository personRepository;


    @BeforeEach
    public void setUp() {
        this.template = Mockito.mock(OrientDBTemplate.class);
        OrientDBDocumentRepositoryProxy handler = new OrientDBDocumentRepositoryProxy(template,
                PersonRepository.class, converters, entitiesMetadata);

        when(template.insert(any(Person.class))).thenReturn(new Person());
        when(template.insert(any(Person.class), any(Duration.class))).thenReturn(new Person());
        when(template.update(any(Person.class))).thenReturn(new Person());
        this.personRepository = (PersonRepository) Proxy.newProxyInstance(PersonRepository.class.getClassLoader(),
                new Class[]{PersonRepository.class},
                handler);
    }

    @Test
    public void shouldFindAll() {
        personRepository.findAllQuery();
        verify(template).sql("select * from Person");
    }

    @Test
    public void shouldFindByNameSQL() {
        personRepository.findByName("Ada");
        verify(template).sql(Mockito.eq("select * from Person where name = ?"), Mockito.any(Object.class));
    }

    @Test
    public void shouldFindByNameSQL2() {
        personRepository.findByAge(10);
        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(template).sql(Mockito.eq("select * from Person where age = :age"), argumentCaptor.capture());
        Map value = argumentCaptor.getValue();
        assertEquals(10, value.get("age"));
    }

    @Test
    public void shouldSaveUsingInsert() {
        var person = new Person();
        person.setName("Ada");
        person.setAge(10);
        personRepository.save(person);
        verify(template).insert(eq(person));
    }


    @Test
    public void shouldSaveUsingUpdate() {
        var person = new Person();
        person.setName("Ada-2");
        person.setAge(10);
        person.setId(10L);
        when(template.find(Person.class, 10L)).thenReturn(Optional.of(person));
        personRepository.save(person);
        verify(template).update(eq(person));
    }

    @Test
    public void shouldDelete(){
        personRepository.deleteById("id");
        verify(template).delete(Person.class, "id");
    }


    @Test
    public void shouldDeleteEntity(){

        var person = new Person();
        person.setName("Ada");
        person.setAge(10);
        person.setId(10L);
        personRepository.delete(person);
        verify(template).delete(Person.class, person.getId());
    }

    interface PersonRepository extends OrientDBCrudRepository<Person, String> {

        @SQL("select * from Person")
        List<Person> findAllQuery();

        @SQL("select * from Person where name = ?")
        List<Person> findByName(String name);

        @SQL("select * from Person where age = :age")
        List<Person> findByAge(@Param("age") Integer age);
    }
}
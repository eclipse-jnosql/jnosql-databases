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
package org.eclipse.jnosql.databases.neo4j.mapping;

import jakarta.data.repository.Param;
import jakarta.inject.Inject;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.core.spi.EntityMetadataExtension;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.reflection.Reflections;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableAutoWeld
@AddPackages(value = {Converters.class, Neo4JRepository.class, EntityConverter.class})
@AddPackages(Reflections.class)
@AddExtensions({EntityMetadataExtension.class, Neo4JExtension.class})
public class Neo4jRepositoryProxyTest {

    private Neo4JTemplate template;

    @Inject
    private Converters converters;

    @Inject
    private EntitiesMetadata entitiesMetadata;

    private HumanRepository personRepository;

    @SuppressWarnings("rawtypes")
    @BeforeEach
    public void setUp() {
        this.template = Mockito.mock(Neo4JTemplate.class);
        Neo4JRepositoryProxy handler = new Neo4JRepositoryProxy<>(template,
                HumanRepository.class, converters, entitiesMetadata);

        when(template.insert(any(Contact.class))).thenReturn(new Contact());
        when(template.insert(any(Contact.class), any(Duration.class))).thenReturn(new Contact());
        when(template.update(any(Contact.class))).thenReturn(new Contact());
        this.personRepository = (HumanRepository) Proxy.newProxyInstance(HumanRepository.class.getClassLoader(),
                new Class[]{HumanRepository.class},
                handler);
    }


    @Test
    public void shouldFindByName() {
        personRepository.findByName("Ada");
        verify(template).cypher("MATCH (p:Person) WHERE p.name = $1 RETURN p", Map.of("name", "Ada"));
    }

    @Test
    public void shouldDeleteByName() {
        personRepository.deleteByName("Ada");
        verify(template).cypher("MATCH (p:Person {name: $name}) DELETE p", Collections.singletonMap("name", "Ada"));
    }

    @Test
    public void shouldFindAll() {
        personRepository.findAllQuery();
        verify(template).cypher("MATCH (p:Person) RETURN p", Collections.emptyMap());
    }

    @Test
    public void shouldFindByNameCQL() {
        personRepository.findByName("Ada");
        verify(template).cypher("MATCH (p:Person) WHERE p.name = $1 RETURN p", Collections.singletonMap("name", "Ada"));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void shouldFindByName2CQL() {
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);

        personRepository.findByName2("Ada");
        verify(template).cypher(Mockito.eq("MATCH (p:Person) WHERE p.name = $name RETURN p"), captor.capture());
        Map map = captor.getValue();
        assertEquals("Ada", map.get("name"));
    }

    @Test
    public void shouldSaveUsingInsert() {
        Contact contact = new Contact("Ada", 10);
        personRepository.save(contact);
        verify(template).insert(eq(contact));
    }

    @Test
    public void shouldSaveUsingUpdate() {
        Contact contact = new Contact("Ada-2", 10);
        when(template.find(Contact.class, "Ada-2")).thenReturn(Optional.of(contact));
        personRepository.save(contact);
        verify(template).update(eq(contact));
    }

    @Test
    public void shouldDelete(){
        personRepository.deleteById("id");
        verify(template).delete(Contact.class, "id");
    }


    @Test
    public void shouldDeleteEntity(){
        Contact contact = new Contact("Ada", 10);
        personRepository.delete(contact);
        verify(template).delete(Contact.class, contact.getName());
    }

    interface HumanRepository extends Neo4JRepository<Contact, String> {

        @Cypher("MATCH (p:Person {name: $name}) DELETE p")
        void deleteByName(@Param("name") String name);

        @Cypher("MATCH (p:Person) RETURN p")
        List<Contact> findAllQuery();

        @Cypher("MATCH (p:Person) WHERE p.name = $1 RETURN p")
        List<Contact> findByName(@Param("name") String name);

        @Cypher("MATCH (p:Person) WHERE p.name = $name RETURN p")
        List<Contact> findByName2(@Param("name") String name);
    }


}
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
package org.eclipse.jnosql.databases.elasticsearch.mapping;

import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.util.ObjectBuilder;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.databases.elasticsearch.communication.ElasticsearchDocumentManager;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;
import org.eclipse.jnosql.mapping.document.spi.DocumentExtension;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.semistructured.EventPersistManager;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


@EnableAutoWeld
@AddPackages(value = {Converters.class,
        EntityConverter.class, DocumentTemplate.class, ElasticsearchTemplate.class})
@AddPackages(Human.class)
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class,
        DocumentExtension.class})
public class DefaultElasticsearchTemplateTest {

    @Inject
    private EntityConverter converter;

    @Inject
    private EventPersistManager persistManager;

    @Inject
    private EntitiesMetadata entities;

    @Inject
    private Converters converters;

    private ElasticsearchDocumentManager manager;

    private DefaultElasticsearchTemplate template;


    @BeforeEach
    public void setup() {
        manager = Mockito.mock(ElasticsearchDocumentManager.class);
        Instance instance = Mockito.mock(Instance.class);
        when(instance.get()).thenReturn(manager);
        template = new DefaultElasticsearchTemplate(instance, converter, persistManager, entities, converters);

        CommunicationEntity entity = CommunicationEntity.of("Human");
        entity.add(Element.of("name", "Ada"));
        entity.add(Element.of("age", 10));
        when(manager.search(Mockito.any(SearchRequest.class)))
                .thenReturn(Stream.of(entity));
    }

    @Test
    public void shouldFindQuery() {
        String searchText = "bike";
        Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn  =
                q -> q.query(t -> t.match(MatchQuery.of(m -> m.field("field").query(2))));
        SearchRequest request = fn.apply(new SearchRequest.Builder()).build();
        List<Human> people = template.<Human>search(request).collect(Collectors.toList());

        assertThat(people).contains(new Human("Ada", 10));
        Mockito.verify(manager).search(Mockito.eq(request));
    }

    @Test
    public void shouldGetConverter() {
        assertThat(template.converter()).isNotNull();
        assertThat(template.converter()).isEqualTo(converter);
    }

    @Test
    public void shouldGetManager() {
        assertThat(template.manager()).isNotNull();
        assertThat(template.manager()).isEqualTo(manager);
    }


    @Test
    public void shouldGetPersistManager() {
        assertThat(template.eventManager()).isNotNull();
        assertThat(template.eventManager()).isEqualTo(persistManager);
    }


    @Test
    public void shouldGetClassMappings() {
        assertThat(template.entities()).isNotNull();
        assertThat(template.entities()).isEqualTo(entities);
    }

    @Test
    public void shouldGetConverters() {
        assertThat(template.converters()).isNotNull();
        assertThat(template.converters()).isEqualTo(converters);
    }
}
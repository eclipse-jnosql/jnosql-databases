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

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.databases.arangodb.communication.ArangoDBDocumentManager;
import org.eclipse.jnosql.databases.arangodb.communication.Human;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.when;


@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class, DocumentTemplate.class, AQL.class})
@AddPackages(MockProducer.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, DocumentExtension.class, ArangoDBExtension.class})
@ExtendWith(MockitoExtension.class)
@AddPackages(Reflections.class)
public class DefaultArangoDBTemplateTest {

    @Inject
    private EntityConverter converter;

    @Inject
    private EventPersistManager persistManager;

    @Inject
    private EntitiesMetadata entities;
    @Inject
    private Converters converters;

    private ArangoDBDocumentManager manager;

    private ArangoDBTemplate template;


    @BeforeEach
    public void setup() {
        manager = Mockito.mock(ArangoDBDocumentManager.class);
        Instance instance = Mockito.mock(Instance.class);
        when(instance.get()).thenReturn(manager);
        template = new DefaultArangoDBTemplate(instance, converter, persistManager, entities, converters);

        CommunicationEntity entity = CommunicationEntity.of("Person");
        entity.add(Element.of("_key", "Ada"));
        entity.add(Element.of("age", 10));

    }

    @Test
    public void shouldFindAQL() {
        Map<String, Object> params = Collections.singletonMap("name", "Ada");
        template.aql("FOR p IN Person FILTER p.name = @name RETURN p", params);
        Mockito.verify(manager).aql("FOR p IN Person FILTER p.name = @name RETURN p", params);
    }

    @Test
    public void shouldFindAQLWithTypeAndParameters() {
        Map<String, Object> params = Collections.singletonMap("name", "Ada");
        template.aql("FOR p IN Person FILTER p.name = @name RETURN p", params, String.class);
        Mockito.verify(manager).aql("FOR p IN Person FILTER p.name = @name RETURN p", params, String.class);
    }

    @Test
    public void shouldFindAQLWithType() {
        template.aql("FOR p IN Person FILTER p.name = @name RETURN p", String.class);
        Mockito.verify(manager).aql("FOR p IN Person FILTER p.name = @name RETURN p", String.class);
    }

    @Test
    public void shouldDeleteAll(){
        ArgumentCaptor<DeleteQuery> argumentCaptor = ArgumentCaptor.forClass(DeleteQuery.class);
        template.deleteAll(Human.class);
        Mockito.verify(manager).delete(argumentCaptor.capture());
        DeleteQuery query = argumentCaptor.getValue();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(query.name()).isEqualTo("Human");
            soft.assertThat(query.columns()).isEmpty();
            soft.assertThat(query.condition()).isEmpty();
        });

    }


}

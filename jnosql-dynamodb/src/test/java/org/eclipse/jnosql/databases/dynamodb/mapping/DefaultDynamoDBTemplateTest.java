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

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.databases.dynamodb.communication.DynamoDBDatabaseManager;
import org.eclipse.jnosql.mapping.core.Converters;
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

import java.util.List;

import static org.mockito.Mockito.when;

@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class, PartiQL.class})
@AddPackages(MockProducer.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, DocumentExtension.class, DynamoDBExtension.class})
@ExtendWith(MockitoExtension.class)
@AddPackages(Reflections.class)
class DefaultDynamoDBTemplateTest {

    @Inject
    private EntityConverter converter;

    @Inject
    private EventPersistManager persistManager;

    @Inject
    private EntitiesMetadata entities;

    @Inject
    private Converters converters;

    private DynamoDBDatabaseManager manager;

    private DynamoDBTemplate template;

    @BeforeEach
    void setup() {
        manager = Mockito.mock(DynamoDBDatabaseManager.class);
        Instance instance = Mockito.mock(Instance.class);
        when(instance.get()).thenReturn(manager);
        template = new DefaultDynamoDBTemplate(instance, converter, persistManager, entities, converters);
    }

    @Test
    void shouldFindSQL() {
        template.partiQL("select from human", Human.class);
        Mockito.verify(manager).partiQL("select from human", "Human");
    }

    @Test
    void shouldFindSQLWithTypeAndParameters() {
        template.partiQL("select from database where content.name = ?", Human.class,  List.of("Ada"), String.class);
        Mockito.verify(manager).partiQL("select from database where content.name = ?", "Human", List.of("Ada"), String.class);
    }

    @Test
    void shouldDeleteAll(){
        ArgumentCaptor<DeleteQuery> argumentCaptor = ArgumentCaptor.forClass(DeleteQuery.class);
        template.deleteAll(Human.class);
        Mockito.verify(manager).delete(argumentCaptor.capture());
        var query = argumentCaptor.getValue();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(query.name()).isEqualTo("Human");
            soft.assertThat(query.columns()).isEmpty();
            soft.assertThat(query.condition()).isEmpty();
        });

    }


}

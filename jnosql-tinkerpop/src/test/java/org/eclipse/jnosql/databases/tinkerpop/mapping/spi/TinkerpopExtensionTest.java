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
package org.eclipse.jnosql.databases.tinkerpop.mapping.spi;

import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.tinkerpop.mapping.TinkerpopTemplate;
import org.eclipse.jnosql.databases.tinkerpop.cdi.mock.MockGraphProducer;
import org.eclipse.jnosql.databases.tinkerpop.mapping.entities.HumanRepository;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.DatabaseType;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.graph.GraphTemplate;
import org.eclipse.jnosql.mapping.graph.spi.GraphExtension;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;


@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class, TinkerpopTemplate.class, GraphTemplate.class})
@AddPackages(MockGraphProducer.class)
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, TinkerpopExtension.class, GraphExtension.class})
class TinkerpopExtensionTest {

    @Inject
    @Database(value = DatabaseType.GRAPH)
    private HumanRepository repository;

    @Inject
    private HumanRepository repository2;

    @Inject
    @Database(value = DatabaseType.GRAPH)
    private HumanRepository repositoryMock;

    @Inject
    @Database(value = DatabaseType.GRAPH, provider = "graphRepositoryMock")
    private TinkerpopTemplate templateMock;

    @Inject
    private TinkerpopTemplate template;


    @Test
    void shouldInitiate() {
        assertNotNull(repository);
    }

    @Test
    void shouldUseMock() {
        assertNotNull(repositoryMock);
    }

    @Test
    void shouldInjectTemplate() {
        assertNotNull(templateMock);
        assertNotNull(template);
        assertNotNull(repository2);
    }

    @Test
    void shouldInjectRepository() {
        assertNotNull(repository);
        assertNotNull(repositoryMock);
    }
}

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
package org.eclipse.jnosql.databases.tinkerpop.mapping.configuration;

import jakarta.inject.Inject;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.jnosql.databases.tinkerpop.communication.DatabaseConfigurationAdapter;
import org.eclipse.jnosql.databases.tinkerpop.mapping.TinkerpopTemplate;
import org.eclipse.jnosql.databases.tinkerpop.mapping.spi.TinkerpopExtension;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.mapping.core.config.MappingConfigurations.GRAPH_PROVIDER;

@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class, TinkerpopTemplate.class})
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, TinkerpopExtension.class})
class GraphSupplierTest {

    @Inject
    private GraphSupplier supplier;

    @BeforeEach
    public void beforeEach(){
        System.clearProperty(GRAPH_PROVIDER.get());
    }

    @Test
    void shouldGetGraph() {
        System.setProperty(DatabaseConfigurationAdapter.TINKERPOP_PROVIDER, GraphConfigurationMock.class.getName());
        Graph graph = supplier.get();
        Assertions.assertNotNull(graph);
        assertThat(graph).isInstanceOf(GraphConfigurationMock.GraphMock.class);
    }


    @Test
    void shouldUseDefaultConfigurationWhenProviderIsWrong() {
        System.setProperty(GRAPH_PROVIDER.get(), Integer.class.getName());
        Graph graph = supplier.get();
        Assertions.assertNotNull(graph);
        assertThat(graph).isInstanceOf(GraphConfigurationMock2.GraphMock.class);
    }

    @Test
    void shouldUseDefaultConfiguration() {
        Graph graph = supplier.get();
        Assertions.assertNotNull(graph);
        assertThat(graph).isInstanceOf(GraphConfigurationMock2.GraphMock.class);
    }

    @Test
    void shouldClose() throws Exception {
        Graph graph = Mockito.mock(Graph.class);
        supplier.close(graph);
        Mockito.verify(graph).close();
    }
}

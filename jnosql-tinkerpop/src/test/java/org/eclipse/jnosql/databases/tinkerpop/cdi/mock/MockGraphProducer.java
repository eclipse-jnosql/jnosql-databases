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
package org.eclipse.jnosql.databases.tinkerpop.cdi.mock;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.interceptor.Interceptor;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.DatabaseType;
import org.mockito.Mockito;

import java.util.Collections;

import static java.util.Collections.singleton;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ApplicationScoped
@Alternative
@Priority(Interceptor.Priority.APPLICATION)
public class MockGraphProducer {

    @Produces
    @ApplicationScoped
    @Database(value = DatabaseType.GRAPH, provider = "graphRepositoryMock")
    public Graph getGraphMock() {
        Graph graphMock = mock(Graph.class);
        Vertex vertex = mock(Vertex.class);
        when(vertex.label()).thenReturn("Person");
        when(vertex.id()).thenReturn("10");
        when(graphMock.vertices("10")).thenReturn(Collections.emptyIterator());
        when(vertex.keys()).thenReturn(singleton("name"));
        when(vertex.value("name")).thenReturn("nameMock");
        when(graphMock.addVertex(Mockito.anyString())).thenReturn(vertex);
        when(graphMock.vertices(Mockito.any())).thenReturn(Collections.emptyIterator());
        return graphMock;
    }

}

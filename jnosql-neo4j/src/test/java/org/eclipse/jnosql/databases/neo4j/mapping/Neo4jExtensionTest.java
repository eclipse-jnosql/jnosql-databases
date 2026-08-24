/*
 *  Copyright (c) 2025 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License 2.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *   and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.databases.neo4j.mapping;

import jakarta.inject.Inject;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@EnableAutoWeld
@AddPackages(value = {Converters.class, Neo4JRepository.class, EntityConverter.class, GraphTemplate.class})
@AddExtensions({ReflectionEntityMetadataExtension.class, Neo4JExtension.class, GraphExtension.class})
@AddPackages(Reflections.class)
public class Neo4jExtensionTest {

    @Inject
    private MusicRepository repository;

    @Inject
    @Database(value = DatabaseType.GRAPH)
    private MusicStoreRepository repository2;

    @Inject
    @Database(value = DatabaseType.GRAPH)
    private MusicStoreRepository repository3;

    @Test
    public void shouldCreteNeo4j() {
        Assertions.assertNotNull(repository);
    }

    @Test
    public void shouldCreteGraph() {
        Assertions.assertNotNull(repository2);
        Assertions.assertNotNull(repository3);
    }
}
/*
 *  Copyright (c) 2025 Contributors to the Eclipse Foundation
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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(repository).isNotNull();
    }

    @Test
    public void shouldCreteGraph() {
        assertThat(repository2).isNotNull();
        assertThat(repository3).isNotNull();
    }
}
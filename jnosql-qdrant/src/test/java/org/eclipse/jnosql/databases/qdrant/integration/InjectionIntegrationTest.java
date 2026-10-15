/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.qdrant.integration;

import jakarta.data.Limit;
import jakarta.inject.Inject;
import jakarta.nosql.Template;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.DatabaseType;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.vector.DenseVector;
import org.eclipse.jnosql.mapping.vector.Vector;
import org.eclipse.jnosql.mapping.vector.VectorTemplate;
import org.eclipse.jnosql.mapping.vector.spi.VectorExtension;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, VectorTemplate.class})
@AddPackages(Article.class)
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, VectorExtension.class})
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
@DisplayName("Qdrant CDI injection")
class InjectionIntegrationTest {

    static {
        IntegrationSupport.configure();
    }

    @Nested
    @DisplayName("When injecting Template")
    class WhenInjectingTemplate {

        @Inject
        private Template template;

        @Test
        @DisplayName("Should resolve the standard Template")
        void shouldResolveTemplate() {
            assertThat(template).isNotNull();
        }
    }

    @Nested
    @DisplayName("When injecting VectorTemplate")
    class WhenInjectingVectorTemplate {

        @Inject
        private VectorTemplate vectorTemplate;

        @Test
        @DisplayName("Should resolve the VectorTemplate")
        void shouldResolveVectorTemplate() {
            assertThat(vectorTemplate).isNotNull();
        }
    }

    @Nested
    @DisplayName("When injecting a qualified Template")
    class WhenInjectingQualifiedTemplate {

        @Inject
        @Database(DatabaseType.VECTOR)
        private Template template;

        @Test
        @DisplayName("Should resolve the Template of the vector database")
        void shouldResolveQualifiedTemplate() {
            assertThat(template).isNotNull().isInstanceOf(VectorTemplate.class);
        }
    }
}

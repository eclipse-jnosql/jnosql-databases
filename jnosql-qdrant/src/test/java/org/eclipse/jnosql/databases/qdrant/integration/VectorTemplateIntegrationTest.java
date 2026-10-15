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
@DisplayName("Qdrant through VectorTemplate")
class VectorTemplateIntegrationTest {

    static {
        IntegrationSupport.configure();
    }

    private static final Vector QUERY = DenseVector.of(1f, 0f, 0f);

    @Inject
    private VectorTemplate vectorTemplate;

    @BeforeEach
    void seed() {
        vectorTemplate.delete(Article.class).execute();
        vectorTemplate.delete(Movie.class).execute();
        vectorTemplate.insert(IntegrationSupport.ARTICLES);
        vectorTemplate.insert(IntegrationSupport.MOVIES);
    }

    @Nested
    @DisplayName("When selecting without a vector")
    class WhenSelectingWithoutVector {

        @Test
        @DisplayName("Should keep the regular query capability")
        void shouldSelect() {
            assertThat(IntegrationSupport.ids(vectorTemplate.select(Article.class).where("category").eq("java").result()))
                    .containsExactlyInAnyOrder("a", "b");
        }
    }

    @Nested
    @DisplayName("When searching by vector")
    class WhenSearchingByVector {

        @Test
        @DisplayName("Should return nearest entities first")
        void shouldSearchByVector() {
            var result = vectorTemplate.search(Article.class).vector(QUERY).limit(Limit.of(10)).result();

            assertThat(IntegrationSupport.ids(result)).containsExactly("a", "b", "c");
            assertThat(result.get(0).embedding()).isNotNull();
        }

        @Test
        @DisplayName("Should combine vector and one predicate")
        void shouldSearchWithPredicate() {
            var result = vectorTemplate.search(Article.class).vector(QUERY)
                    .where("category").eq("java").limit(Limit.of(10)).result();

            assertThat(IntegrationSupport.ids(result)).containsExactly("a", "b");
        }

        @Test
        @DisplayName("Should combine vector and several predicates")
        void shouldSearchWithPredicates() {
            var result = vectorTemplate.search(Article.class).vector(QUERY)
                    .where("category").eq("java").and("year").gte(2025).limit(Limit.of(10)).result();

            assertThat(IntegrationSupport.ids(result)).containsExactly("a");
        }
    }

    @Nested
    @DisplayName("When mapping Vector attributes")
    class WhenMappingVectorAttributes {

        @Test
        @DisplayName("Should return a Vector attribute as a DenseVector")
        void shouldReturnDenseVector() {
            var first = vectorTemplate.search(Article.class).vector(QUERY).limit(Limit.of(2)).result().get(1);

            assertThat(first.embedding()).isInstanceOf(DenseVector.class);
            assertThat(((DenseVector) first.embedding()).values()).containsExactly(0.9f, 0.1f, 0f);
        }
    }

    @Nested
    @DisplayName("When mapping DenseVector attributes")
    class WhenMappingDenseVectorAttributes {

        @Test
        @DisplayName("Should search entities with a DenseVector attribute")
        void shouldSearchMovies() {
            var result = vectorTemplate.search(Movie.class).vector(QUERY).limit(Limit.of(2)).result();

            assertThat(IntegrationSupport.ids(result)).containsExactly("a", "b");
            assertThat(result.get(1).embedding()).isNotNull();
            assertThat(result.get(1).embedding().values()).containsExactly(0.8f, 0.2f, 0f);
        }
    }
}

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
package org.eclipse.jnosql.databases.milvus.integration;

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
import java.util.UUID;

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
@DisplayName("Milvus through Jakarta NoSQL Template")
class TemplateIntegrationTest {

    static {
        IntegrationSupport.configure();
    }

    @Inject
    private Template template;

    @BeforeEach
    void seed() {
        template.delete(Article.class).execute();
        template.delete(Movie.class).execute();
        template.delete(UserProfile.class).execute();
        template.insert(IntegrationSupport.ARTICLES);
        template.insert(IntegrationSupport.MOVIES);
    }

    @Nested
    @DisplayName("When inserting")
    class WhenInserting {

        @Test
        @DisplayName("Should insert and find by id")
        void shouldInsertAndFind() {
            var article = new Article("n", "New", "java", 2026, DenseVector.of(0f, 0f, 1f));
            template.insert(article);

            assertThat(template.find(Article.class, "n")).isPresent();
        }

        @Test
        @DisplayName("Should insert multiple entities")
        void shouldInsertMultiple() {
            template.insert(List.of(new Article("x", "X", "go", 2020, DenseVector.of(0f, 0f, 1f)),
                    new Article("y", "Y", "go", 2021, DenseVector.of(0f, 1f, 1f))));

            assertThat(template.select(Article.class).where("category").eq("go").result()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("When updating")
    class WhenUpdating {

        @Test
        @DisplayName("Should update an article")
        void shouldUpdate() {
            template.update(new Article("a", "Jakarta EE 11", "java", 2025, DenseVector.of(1f, 0f, 0f)));

            assertThat(template.find(Article.class, "a").orElseThrow().title()).isEqualTo("Jakarta EE 11");
        }
    }

    @Nested
    @DisplayName("When deleting")
    class WhenDeleting {

        @Test
        @DisplayName("Should delete by id")
        void shouldDeleteById() {
            template.delete(Article.class, "a");

            assertThat(template.find(Article.class, "a")).isEmpty();
        }

        @Test
        @DisplayName("Should delete by query")
        void shouldDeleteByQuery() {
            template.delete(Article.class).where("category").eq("java").execute();

            assertThat(IntegrationSupport.ids(template.select(Article.class).result()))
                    .containsExactly("c");
        }
    }

    @Nested
    @DisplayName("When selecting")
    class WhenSelecting {

        @Test
        @DisplayName("Should select all entities")
        void shouldSelectAll() {
            assertThat(template.select(Article.class).result()).hasSize(3);
        }

        @Test
        @DisplayName("Should filter by a regular attribute")
        void shouldFilter() {
            assertThat(IntegrationSupport.ids(template.select(Article.class).where("category").eq("java").result()))
                    .containsExactlyInAnyOrder("a", "b");
        }

        @Test
        @DisplayName("Should filter by several attributes")
        void shouldFilterSeveral() {
            assertThat(IntegrationSupport.ids(template.select(Movie.class).where("genre").eq("action").and("year").gte(2025).result()))
                    .containsExactly("a");
        }

        @Test
        @DisplayName("Should return every stored entity")
        void shouldCount() {
            assertThat(template.select(Article.class).result()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("When mapping Vector attributes")
    class WhenMappingVectorAttributes {

        @Test
        @DisplayName("Should restore a Vector attribute as a DenseVector")
        void shouldRoundTrip() {
            Vector vector = template.find(Article.class, "b").orElseThrow().embedding();

            assertThat(vector).isInstanceOf(DenseVector.class);
            assertThat(((DenseVector) vector).values()).containsExactly(0.9f, 0.1f, 0f);
        }
    }

    @Nested
    @DisplayName("When mapping DenseVector attributes")
    class WhenMappingDenseVectorAttributes {

        @Test
        @DisplayName("Should restore a DenseVector attribute directly")
        void shouldRoundTrip() {
            DenseVector vector = template.find(Movie.class, "b").orElseThrow().embedding();

            assertThat(vector).isNotNull();
            assertThat(vector.values()).containsExactly(0.8f, 0.2f, 0f);
        }
    }

    @Nested
    @DisplayName("When using UUID attributes")
    class WhenUsingUuid {

        @BeforeEach
        void insert() {
            template.insert(IntegrationSupport.PROFILE);
        }

        @Test
        @DisplayName("Should find by UUID id and restore UUID attributes and vector")
        void shouldFindByUuid() {
            var profile = template.find(UserProfile.class, IntegrationSupport.PROFILE_ID).orElseThrow();

            assertThat(profile.id()).isEqualTo(IntegrationSupport.PROFILE_ID);
            assertThat(profile.externalId()).isEqualTo(IntegrationSupport.PROFILE_EXTERNAL_ID);
            assertThat(profile.embedding().values()).containsExactly(1f, 0f, 0f);
        }

        @Test
        @DisplayName("Should update by UUID id")
        void shouldUpdate() {
            template.update(new UserProfile(IntegrationSupport.PROFILE_ID, "Grace",
                    IntegrationSupport.PROFILE_EXTERNAL_ID, DenseVector.of(1f, 0f, 0f)));

            assertThat(template.find(UserProfile.class, IntegrationSupport.PROFILE_ID).orElseThrow().name())
                    .isEqualTo("Grace");
        }

        @Test
        @DisplayName("Should select by a UUID attribute")
        void shouldSelectByUuidAttribute() {
            var result = template.select(UserProfile.class)
                    .where("externalId").eq(IntegrationSupport.PROFILE_EXTERNAL_ID).result();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should delete by UUID id")
        void shouldDelete() {
            template.delete(UserProfile.class, IntegrationSupport.PROFILE_ID);

            assertThat(template.find(UserProfile.class, IntegrationSupport.PROFILE_ID)).isEmpty();
        }
    }
}

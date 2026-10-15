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
@DisplayName("Qdrant through Jakarta Data repositories")
class RepositoryIntegrationTest {

    static {
        IntegrationSupport.configure();
    }

    @Inject
    private Template template;

    @Inject
    private ArticleRepository articleRepository;

    @Inject
    private MovieRepository movieRepository;

    @BeforeEach
    void seed() {
        template.delete(Article.class).execute();
        template.delete(Movie.class).execute();
        template.insert(IntegrationSupport.ARTICLES);
        template.insert(IntegrationSupport.MOVIES);
    }

    @Nested
    @DisplayName("When saving")
    class WhenSaving {

        @Test
        @DisplayName("Should save and find by id")
        void shouldSaveAndFind() {
            articleRepository.save(new Article("r", "Repo", "java", 2026, DenseVector.of(0f, 0f, 1f)));

            assertThat(articleRepository.findById("r")).isPresent();
        }

        @Test
        @DisplayName("Should save multiple entities")
        void shouldSaveMultiple() {
            articleRepository.saveAll(List.of(new Article("x", "X", "go", 2020, DenseVector.of(0f, 0f, 1f)),
                    new Article("y", "Y", "go", 2021, DenseVector.of(0f, 1f, 1f))));

            assertThat(articleRepository.findByCategory("go")).hasSize(2);
        }
    }

    @Nested
    @DisplayName("When querying")
    class WhenQuerying {

        @Test
        @DisplayName("Should run a derived query on Article")
        void shouldFindByCategory() {
            assertThat(articleRepository.findByCategory("java")).extracting(Article::id)
                    .containsExactlyInAnyOrder("a", "b");
        }

        @Test
        @DisplayName("Should run a derived query on Movie")
        void shouldFindByGenre() {
            assertThat(movieRepository.findByGenre("action")).extracting(Movie::id)
                    .containsExactlyInAnyOrder("a", "b");
        }

        @Test
        @DisplayName("Should count all entities")
        void shouldCount() {
            assertThat(articleRepository.findAll().count()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should report whether an entity exists")
        void shouldExist() {
            assertThat(articleRepository.findById("a")).isPresent();
            assertThat(articleRepository.findById("zzz")).isEmpty();
        }
    }

    @Nested
    @DisplayName("When deleting")
    class WhenDeleting {

        @Test
        @DisplayName("Should delete by id")
        void shouldDeleteById() {
            articleRepository.deleteById("a");

            assertThat(articleRepository.findById("a")).isEmpty();
        }

        @Test
        @DisplayName("Should delete an entity")
        void shouldDeleteEntity() {
            articleRepository.delete(IntegrationSupport.ARTICLES.get(1));

            assertThat(articleRepository.findById("b")).isEmpty();
        }
    }

    @Nested
    @DisplayName("When mapping vector attributes")
    class WhenMappingVectorAttributes {

        @Test
        @DisplayName("Should restore a Vector attribute as a DenseVector")
        void shouldRestoreVector() {
            Vector vector = articleRepository.findById("b").orElseThrow().embedding();

            assertThat(vector).isInstanceOf(DenseVector.class);
            assertThat(((DenseVector) vector).values()).containsExactly(0.9f, 0.1f, 0f);
        }

        @Test
        @DisplayName("Should restore a DenseVector attribute directly")
        void shouldRestoreDenseVector() {
            DenseVector vector = movieRepository.findById("b").orElseThrow().embedding();

            assertThat(vector).isNotNull();
            assertThat(vector.values()).containsExactly(0.8f, 0.2f, 0f);
        }
    }
}

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
import org.eclipse.jnosql.databases.qdrant.communication.QdrantConfigurations;
import org.eclipse.jnosql.databases.qdrant.communication.VectorDatabase;
import org.eclipse.jnosql.databases.qdrant.communication.QdrantDistance;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.DatabaseType;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.core.config.MappingConfigurations;
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
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, VectorTemplate.class})
@AddPackages(Article.class)
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, VectorExtension.class})
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class VectorIntegrationTest {

    static {
        VectorDatabase.INSTANCE.settings().toMap().forEach((key, value) -> System.setProperty(key, value.toString()));
        System.setProperty(QdrantConfigurations.DISTANCE.get(), QdrantDistance.DOT.name());
        System.setProperty(MappingConfigurations.VECTOR_DATABASE.get(), "library");
    }

    private static final Vector QUERY = DenseVector.of(1f, 0f, 0f);

    @Inject
    private Template template;

    @Inject
    private VectorTemplate vectorTemplate;

    @Inject
    @Database(DatabaseType.VECTOR)
    private ArticleRepository articleRepository;

    @Inject
    @Database(DatabaseType.VECTOR)
    private MovieRepository movieRepository;

    private final Article articleA = new Article("a", "Jakarta EE", "java", 2025, DenseVector.of(1f, 0f, 0f));
    private final Article articleB = new Article("b", "Records", "java", 2024, DenseVector.of(0.9f, 0.1f, 0f));
    private final Article articleC = new Article("c", "Indexes", "database", 2025, DenseVector.of(0f, 1f, 0f));

    private final Movie movieA = new Movie("a", "Heat", "action", 2025, DenseVector.of(1f, 0f, 0f));
    private final Movie movieB = new Movie("b", "Speed", "action", 2024, DenseVector.of(0.8f, 0.2f, 0f));
    private final Movie movieC = new Movie("c", "Amelie", "drama", 2025, DenseVector.of(0f, 1f, 0f));

    @BeforeEach
    void cleanAndSeed() {
        template.delete(Article.class).execute();
        template.delete(Movie.class).execute();
        template.insert(List.of(articleA, articleB, articleC));
        template.insert(List.of(movieA, movieB, movieC));
    }

    private static List<String> ids(List<?> articles) {
        return articles.stream().map(article -> ((Article) article).id()).toList();
    }

    @Nested
    @DisplayName("When using Jakarta NoSQL Template")
    class WhenUsingTemplate {

        @Test
        @DisplayName("Should insert and find by id")
        void shouldInsertAndFind() {
            var article = new Article("n", "New", "java", 2026, DenseVector.of(0f, 0f, 1f));
            template.insert(article);

            assertThat(template.find(Article.class, "n")).contains(article);
        }

        @Test
        @DisplayName("Should update an article")
        void shouldUpdate() {
            template.update(new Article("a", "Jakarta EE 11", "java", 2025, DenseVector.of(1f, 0f, 0f)));

            assertThat(template.find(Article.class, "a").orElseThrow().title()).isEqualTo("Jakarta EE 11");
        }

        @Test
        @DisplayName("Should delete by id")
        void shouldDelete() {
            template.delete(Article.class, "a");

            assertThat(template.find(Article.class, "a")).isEmpty();
        }

        @Test
        @DisplayName("Should select without vector similarity")
        void shouldSelect() {
            var result = template.select(Article.class).where("category").eq("java").result();

            assertThat(ids(result)).containsExactlyInAnyOrder("a", "b");
        }
    }

    @Nested
    @DisplayName("When using VectorTemplate")
    class WhenUsingVectorTemplate {

        @Test
        @DisplayName("Should keep the regular query capability")
        void shouldSelectWithoutVector() {
            var result = vectorTemplate.select(Article.class).where("category").eq("java").result();

            assertThat(ids(result)).containsExactlyInAnyOrder("a", "b");
        }

        @Test
        @DisplayName("Should search by vector only, nearest first")
        void shouldSearchByVector() {
            var result = vectorTemplate.search(Article.class).vector(QUERY).limit(Limit.of(10)).result();

            assertThat(ids(result)).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("Should combine vector and one predicate")
        void shouldSearchWithPredicate() {
            var result = vectorTemplate.search(Article.class).vector(QUERY)
                    .where("category").eq("java").limit(Limit.of(10)).result();

            assertThat(ids(result)).containsExactly("a", "b");
        }

        @Test
        @DisplayName("Should combine vector and several predicates")
        void shouldSearchWithPredicates() {
            var result = vectorTemplate.search(Article.class).vector(QUERY)
                    .where("category").eq("java").and("year").gte(2025).limit(Limit.of(10)).result();

            assertThat(ids(result)).containsExactly("a");
        }

        @Test
        @DisplayName("Should search entities with a DenseVector attribute")
        void shouldSearchMovies() {
            var result = vectorTemplate.search(Movie.class).vector(QUERY).limit(Limit.of(2)).result();

            assertThat(result).extracting(Movie::id).containsExactly("a", "b");
            assertThat(result.get(1).embedding()).isNotNull();
            assertThat(result.get(1).embedding().values()).containsExactly(0.8f, 0.2f, 0f);
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

        @Test
        @DisplayName("Should populate the Vector attribute in search results")
        void shouldPopulateOnSearch() {
            var first = vectorTemplate.search(Article.class).vector(QUERY).limit(Limit.of(1)).result().get(0);

            assertThat(first.embedding()).isInstanceOf(DenseVector.class);
            assertThat(((DenseVector) first.embedding()).values()).containsExactly(1f, 0f, 0f);
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
    @DisplayName("When using Repository")
    class WhenUsingRepository {

        @Test
        @DisplayName("Should save and find by id")
        void shouldSaveAndFind() {
            var article = new Article("r", "Repo", "java", 2026, DenseVector.of(0f, 0f, 1f));
            articleRepository.save(article);

            assertThat(articleRepository.findById("r")).isPresent();
        }

        @Test
        @DisplayName("Should run a derived query")
        void shouldFindByCategory() {
            assertThat(ids(articleRepository.findByCategory("java"))).containsExactlyInAnyOrder("a", "b");
        }

        @Test
        @DisplayName("Should delete and count")
        void shouldDeleteAndCount() {
            articleRepository.deleteById("a");

            assertSoftly(soft -> {
                soft.assertThat(articleRepository.findById("a")).isEmpty();
                soft.assertThat(articleRepository.findById("b")).isPresent();
            });
        }

        @Test
        @DisplayName("Should persist and retrieve an entity with a DenseVector attribute")
        void shouldPersistMovie() {
            var movie = movieRepository.findById("a").orElseThrow();

            assertThat(movie.embedding().values()).containsExactly(1f, 0f, 0f);
        }
    }
}

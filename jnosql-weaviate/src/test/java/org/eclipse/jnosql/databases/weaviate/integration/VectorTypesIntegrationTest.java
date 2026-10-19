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
 */
package org.eclipse.jnosql.databases.weaviate.integration;

import jakarta.data.Limit;
import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.weaviate.communication.VectorDatabase;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.vector.DenseVector;
import org.eclipse.jnosql.mapping.vector.MultiVector;
import org.eclipse.jnosql.mapping.vector.SparseVector;
import org.eclipse.jnosql.mapping.vector.Vector;
import org.eclipse.jnosql.mapping.vector.VectorTemplate;
import org.eclipse.jnosql.mapping.vector.spi.VectorExtension;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, VectorTemplate.class})
@AddPackages(Article.class)
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, VectorExtension.class})
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
@DisplayName("Weaviate vector representations through VectorTemplate")
class VectorTypesIntegrationTest {

    static {
        IntegrationSupport.configure();
    }

    private static final List<String> COLLECTIONS = List.of("Article", "Movie", "ScientificArticle", "MediaItem");

    private static final SparseVector SPARSE = SparseVector.of(10,
            SparseVector.element(2, 7.0F), SparseVector.element(8, 3.0F));

    private static final MultiVector MULTI = MultiVector.of(
            DenseVector.of(0.2F, 0.8F, 0.1F),
            DenseVector.of(0.7F, 0.1F, 0.4F),
            DenseVector.of(0.3F, 0.6F, 0.9F));

    private static final MultiVector MULTI_OTHER = MultiVector.of(
            DenseVector.of(0F, 1F, 0F),
            DenseVector.of(0F, 0.9F, 0.1F));

    private static final DenseVector DENSE = DenseVector.of(0.12F, 0.45F, 0.78F);

    private static final DenseVector DENSE_OTHER = DenseVector.of(-0.5F, 0.1F, 0.1F);

    @Inject
    private VectorTemplate template;

    @BeforeEach
    @AfterEach
    void dropCollections() throws Exception {
        try (var client = VectorDatabase.INSTANCE.client()) {
            for (String collection : COLLECTIONS) {
                if (client.collections.exists(collection)) {
                    client.collections.delete(collection);
                }
            }
        }
    }

    @Nested
    @DisplayName("When an Article declares a generic Vector attribute")
    class WhenTheArticleDeclaresVector {

        @Test
        @DisplayName("Should preserve a DenseVector runtime value")
        void shouldPreserveDenseVector() {
            template.insert(new Article("a", "Jakarta", "java", 2025, DENSE));
            template.insert(new Article("b", "Records", "java", 2024, DENSE_OTHER));

            var found = List.of(template.find(Article.class, "a").orElseThrow());
            assertThat(found.get(0).embedding()).isInstanceOf(DenseVector.class);
            var dense = (DenseVector) found.get(0).embedding();
            assertThat(dense.dimensions()).isEqualTo(3);
            assertThat(dense.values()).containsExactly(0.12F, 0.45F, 0.78F);

            var result = template.search(Article.class).vector(DENSE).limit(Limit.of(10)).result();
            assertThat(IntegrationSupport.ids(result)).containsExactly("a", "b");
            assertThat(result.get(0).embedding()).isInstanceOf(DenseVector.class);
        }

        @Test
        @DisplayName("Should reject a SparseVector runtime value because Weaviate has no native sparse vector")
        void shouldRejectSparseVector() {
            var article = new Article("a", "Jakarta", "java", 2025, SPARSE);
            assertThatThrownBy(() -> template.insert(article))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("SparseVector");
        }

        @Test
        @DisplayName("Should preserve a MultiVector runtime value")
        void shouldPreserveMultiVector() {
            template.insert(new Article("a", "Jakarta", "java", 2025, MULTI));
            template.insert(new Article("b", "Records", "java", 2024, MULTI_OTHER));

            var found = List.of(template.find(Article.class, "a").orElseThrow());
            assertThat(found.get(0).embedding()).isInstanceOf(MultiVector.class);
            assertMulti((MultiVector) found.get(0).embedding());

            var result = template.search(Article.class)
                    .vector(MultiVector.of(DenseVector.of(1F, 0F, 0F))).limit(Limit.of(10)).result();
            assertThat(IntegrationSupport.ids(result)).containsExactly("a", "b");
            assertThat(result.get(0).embedding()).isInstanceOf(MultiVector.class);
        }
    }

    @Nested
    @DisplayName("When a Movie declares a DenseVector attribute")
    class WhenTheMovieDeclaresDenseVector {

        @Test
        @DisplayName("Should insert, read and search the DenseVector")
        void shouldPreserveDenseVector() {
            template.insert(new Movie("a", "Heat", "action", 2025, DENSE));
            template.insert(new Movie("b", "Speed", "action", 2024, DENSE_OTHER));

            var found = List.of(template.find(Movie.class, "a").orElseThrow());
            assertThat(found.get(0).embedding()).isInstanceOf(DenseVector.class);
            assertThat(found.get(0).embedding().dimensions()).isEqualTo(3);
            assertThat(found.get(0).embedding().values()).containsExactly(0.12F, 0.45F, 0.78F);

            var result = template.search(Movie.class).vector(DENSE).limit(Limit.of(10)).result();
            assertThat(IntegrationSupport.ids(result)).containsExactly("a", "b");
        }
    }

    @Nested
    @DisplayName("When a ScientificArticle declares a SparseVector attribute")
    class WhenTheScientificArticleDeclaresSparseVector {

        @Test
        @DisplayName("Should reject the SparseVector on insert and on search")
        void shouldRejectSparseVector() {
            var entity = new ScientificArticle("a", "Quantum", SPARSE);
            assertThatThrownBy(() -> template.insert(entity))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("SparseVector");
            var search = template.search(ScientificArticle.class).vector(SPARSE).limit(Limit.of(10));
            assertThatThrownBy(search::result).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("When a MediaItem declares a MultiVector attribute")
    class WhenTheMediaItemDeclaresMultiVector {

        @Test
        @DisplayName("Should insert, read and search the MultiVector")
        void shouldPreserveMultiVector() {
            template.insert(new MediaItem("a", "Trailer", MULTI));
            template.insert(new MediaItem("b", "Teaser", MULTI_OTHER));

            var found = List.of(template.find(MediaItem.class, "a").orElseThrow());
            assertThat(found.get(0).embedding()).isInstanceOf(MultiVector.class);
            assertMulti(found.get(0).embedding());

            var result = template.search(MediaItem.class)
                    .vector(MultiVector.of(DenseVector.of(1F, 0F, 0F))).limit(Limit.of(10)).result();
            assertThat(result).extracting(MediaItem::id).containsExactly("a", "b");
            assertMulti(result.get(0).embedding());
        }
    }

    private static void assertMulti(MultiVector multi) {
        assertThat(multi.size()).isEqualTo(3);
        assertThat(multi.dimensions()).isEqualTo(3);
        assertThat(multi.vectors()).hasSize(3);
        assertThat(multi.vectors().get(0).values()).containsExactly(0.2F, 0.8F, 0.1F);
        assertThat(multi.vectors().get(1).values()).containsExactly(0.7F, 0.1F, 0.4F);
        assertThat(multi.vectors().get(2).values()).containsExactly(0.3F, 0.6F, 0.9F);
    }
}

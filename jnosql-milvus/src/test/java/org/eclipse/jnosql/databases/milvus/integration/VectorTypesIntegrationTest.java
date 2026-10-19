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
package org.eclipse.jnosql.databases.milvus.integration;

import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import jakarta.data.Limit;
import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.milvus.communication.VectorDatabase;
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
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, VectorTemplate.class})
@AddPackages(Article.class)
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, VectorExtension.class})
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
@DisplayName("Milvus vector representations through VectorTemplate")
class VectorTypesIntegrationTest {

    static {
        IntegrationSupport.configure();
    }

    private static final String DATABASE = "library";

    private static final List<String> COLLECTIONS = List.of("Article", "Movie", "ScientificArticle", "MediaItem");

    private static final SparseVector SPARSE = SparseVector.of(10,
            SparseVector.element(2, 7.0F), SparseVector.element(8, 3.0F));

    private static final SparseVector SPARSE_WEAK = SparseVector.of(10, SparseVector.element(2, 1.0F));

    private static final SparseVector SPARSE_OTHER = SparseVector.of(10, SparseVector.element(5, 4.0F));

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
    void dropCollections() {
        var client = VectorDatabase.INSTANCE.client();
        try {
            if (!client.listDatabases().getDatabaseNames().contains(DATABASE)) {
                return;
            }
            for (String collection : COLLECTIONS) {
                var has = HasCollectionReq.builder().databaseName(DATABASE).collectionName(collection).build();
                if (Boolean.TRUE.equals(client.hasCollection(has))) {
                    client.dropCollection(DropCollectionReq.builder()
                            .databaseName(DATABASE).collectionName(collection).build());
                }
            }
        } finally {
            client.close();
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
        @DisplayName("Should preserve a SparseVector runtime value")
        void shouldPreserveSparseVector() {
            template.insert(new Article("a", "Jakarta", "java", 2025, SPARSE));
            template.insert(new Article("b", "Records", "java", 2024, SPARSE_WEAK));
            template.insert(new Article("c", "Indexes", "database", 2025, SPARSE_OTHER));

            var found = List.of(template.find(Article.class, "a").orElseThrow());
            assertThat(found.get(0).embedding()).isInstanceOf(SparseVector.class);
            var sparse = (SparseVector) found.get(0).embedding();
            assertThat(sparse.dimensions()).isEqualTo(10);
            assertThat(sparse.indices()).containsExactly(2, 8);
            assertThat(sparse.values()).containsExactly(7.0F, 3.0F);

            var result = template.search(Article.class).vector(SPARSE_WEAK).limit(Limit.of(10)).result();
            assertThat(IntegrationSupport.ids(result)).containsExactly("a", "b");
            assertThat(result.get(0).embedding()).isInstanceOf(SparseVector.class);
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
        @DisplayName("Should insert, read and search the SparseVector")
        void shouldPreserveSparseVector() {
            template.insert(new ScientificArticle("a", "Quantum", SPARSE));
            template.insert(new ScientificArticle("b", "Gravity", SPARSE_WEAK));
            template.insert(new ScientificArticle("c", "Optics", SPARSE_OTHER));

            var found = List.of(template.find(ScientificArticle.class, "a").orElseThrow());
            var sparse = found.get(0).embedding();
            assertThat(sparse).isInstanceOf(SparseVector.class);
            assertThat(sparse.dimensions()).isEqualTo(10);
            assertThat(sparse.indices()).containsExactly(2, 8);
            assertThat(sparse.values()).containsExactly(7.0F, 3.0F);

            var result = template.search(ScientificArticle.class).vector(SPARSE_WEAK).limit(Limit.of(10)).result();
            assertThat(result).extracting(ScientificArticle::id).containsExactly("a", "b");
            assertThat(result.get(1).embedding().dimensions()).isEqualTo(10);
            assertThat(result.get(1).embedding().indices()).containsExactly(2);
            assertThat(result.get(1).embedding().values()).containsExactly(1.0F);
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

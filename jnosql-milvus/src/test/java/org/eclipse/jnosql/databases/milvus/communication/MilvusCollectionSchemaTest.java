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
package org.eclipse.jnosql.databases.milvus.communication;

import io.milvus.v2.common.DataType;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.mapping.vector.DenseVector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.response.DescribeCollectionResp;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MilvusCollectionSchemaTest {

    private final MilvusCollectionSchema schema = new MilvusCollectionSchema(DataType.VarChar, "embedding", 3,
            Map.of("title", DataType.VarChar, "year", DataType.Int64, "rating", DataType.Double));

    private CommunicationEntity entity(Object id, String field, Object value, int dimension) {
        var entity = CommunicationEntity.of("c");
        entity.add("_id", id);
        entity.add(field, value);
        entity.add("embedding", DenseVector.of(new float[dimension]));
        return entity;
    }

    private void validate(CommunicationEntity entity) {
        schema.validate("c", entity, MilvusRowConverter.vectorElement(entity),
                MilvusRowConverter.values(MilvusRowConverter.vectorElement(entity)).length);
    }

    @Nested
    @DisplayName("When validating an entity against the collection schema")
    class WhenTheValidation {

        @Test
        @DisplayName("Should accept a compatible entity, new fields and integers in a double field")
        void shouldAccept() {
            assertThatCode(() -> validate(entity("a", "title", "Dune", 3))).doesNotThrowAnyException();
            assertThatCode(() -> validate(entity("a", "other", "Dune", 3))).doesNotThrowAnyException();
            assertThatCode(() -> validate(entity("a", "rating", 4, 3))).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject a different vector dimension")
        void shouldRejectDimension() {
            assertThatThrownBy(() -> validate(entity("a", "title", "Dune", 2)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should reject a different field type")
        void shouldRejectType() {
            assertThatThrownBy(() -> validate(entity("a", "year", "1965", 3)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should reject a different id representation")
        void shouldRejectId() {
            assertThatThrownBy(() -> validate(entity(1L, "title", "Dune", 3)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should reject a different vector field name")
        void shouldRejectVectorName() {
            var entity = CommunicationEntity.of("c");
            entity.add("_id", "a");
            entity.add("vector", DenseVector.of(1f, 0f, 0f));
            Element vector = MilvusRowConverter.vectorElement(entity);

            assertThatThrownBy(() -> schema.validate("c", entity, vector, 3))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("When describing a collection")
    class WhenDescribing {

        private DescribeCollectionResp describe(AddFieldReq... fields) {
            var schema = CreateCollectionReq.CollectionSchema.builder().build();
            schema.addField(AddFieldReq.builder().fieldName("_id").dataType(DataType.VarChar).maxLength(64)
                    .isPrimaryKey(true).build());
            for (AddFieldReq field : fields) {
                schema.addField(field);
            }
            return DescribeCollectionResp.builder().collectionName("c").collectionSchema(schema).build();
        }

        private AddFieldReq dense() {
            return AddFieldReq.builder().fieldName("dense").dataType(DataType.FloatVector).dimension(3).build();
        }

        private AddFieldReq sparse() {
            return AddFieldReq.builder().fieldName("sparse").dataType(DataType.SparseFloatVector).build();
        }

        @Test
        @DisplayName("Should keep a consistent name, dimension and kind when the sparse field comes first")
        void shouldHandleSparseThenDense() {
            var result = MilvusCollectionSchema.of(describe(sparse(), dense()));

            assertThat(result.vectorField()).isEqualTo("dense");
            assertThat(result.dimension()).isEqualTo(3);
            assertThat(result.kind()).isEqualTo(MilvusVectors.Kind.DENSE);
        }

        @Test
        @DisplayName("Should keep a consistent name, dimension and kind when the dense field comes first")
        void shouldHandleDenseThenSparse() {
            var result = MilvusCollectionSchema.of(describe(dense(), sparse()));

            assertThat(result.vectorField()).isEqualTo("sparse");
            assertThat(result.dimension()).isZero();
            assertThat(result.kind()).isEqualTo(MilvusVectors.Kind.SPARSE);
        }
    }
}

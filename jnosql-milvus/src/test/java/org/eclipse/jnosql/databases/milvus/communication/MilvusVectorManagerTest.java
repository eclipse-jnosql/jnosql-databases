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

import io.milvus.v2.client.MilvusClientV2;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.mapping.vector.DenseVector;
import org.eclipse.jnosql.mapping.vector.VectorSelectQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;

class MilvusVectorManagerTest {

    private final MilvusClientV2 client = Mockito.mock(MilvusClientV2.class);

    private final MilvusVectorManager manager = new MilvusVectorManager(client, "library",
            MilvusCollectionSettings.DEFAULT);

    private CommunicationEntity entity() {
        var entity = CommunicationEntity.of("c");
        entity.add("_id", "a");
        entity.add("embedding", DenseVector.of(1f));
        return entity;
    }

    @Test
    @DisplayName("Should expose the database name and keep the client open on close")
    void shouldExposeName() {
        manager.close();

        assertThat(manager.name()).isEqualTo("library");
        Mockito.verifyNoInteractions(client);
    }

    @Nested
    @DisplayName("When the operation is not supported")
    class WhenTheOperationIsUnsupported {

        @Test
        @DisplayName("Should reject a time to live")
        void shouldRejectTtl() {
            assertThatThrownBy(() -> manager.insert(entity(), Duration.ofSeconds(1)))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> manager.insert(List.of(entity()), Duration.ofSeconds(1)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Should reject sorting")
        void shouldRejectSort() {
            assertThatThrownBy(() -> manager.select(select().from("c").orderBy("year").asc().build()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Should reject a sparse vector search")
        void shouldRejectNonDenseSearch() {
            VectorSelectQuery query = Mockito.mock(VectorSelectQuery.class);
            Mockito.when(query.vector()).thenReturn(Mockito.mock(org.eclipse.jnosql.mapping.vector.Vector.class));

            assertThatThrownBy(() -> manager.search(query)).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("When the arguments are invalid")
    class WhenTheValidation {

        @Test
        @DisplayName("Should reject null arguments")
        void shouldRejectNulls() {
            assertThatNullPointerException().isThrownBy(() -> manager.insert((CommunicationEntity) null));
            assertThatNullPointerException().isThrownBy(() -> manager.insert((Iterable<CommunicationEntity>) null));
            assertThatNullPointerException().isThrownBy(() -> manager.update((CommunicationEntity) null));
            assertThatNullPointerException().isThrownBy(() -> manager.delete(null));
            assertThatNullPointerException().isThrownBy(() -> manager.select(null));
            assertThatNullPointerException().isThrownBy(() -> manager.count((String) null));
            assertThatNullPointerException().isThrownBy(() -> manager.search(null));
        }

        @Test
        @DisplayName("Should require the _id to update")
        void shouldRequireId() {
            var entity = CommunicationEntity.of("c");
            entity.add("embedding", DenseVector.of(1f));

            assertThatThrownBy(() -> manager.update(entity)).isInstanceOf(UnsupportedOperationException.class);
        }
    }
}

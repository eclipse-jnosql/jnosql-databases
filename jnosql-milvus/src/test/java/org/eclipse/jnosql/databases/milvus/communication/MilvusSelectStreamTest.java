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

import io.milvus.orm.iterator.QueryIterator;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.QueryIteratorReq;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.mapping.vector.DenseVector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
@DisplayName("Milvus select stream lifecycle")
class MilvusSelectStreamTest {

    private final MilvusClientV2 client = Mockito.spy(VectorDatabase.INSTANCE.client());

    private final MilvusVectorManager manager = new MilvusVectorManager(client, "library",
            MilvusCollectionSettings.DEFAULT);

    private final String collection = "stream_" + UUID.randomUUID().toString().replace("-", "");

    private QueryIterator iterator;

    private void seed(int size) {
        VectorDatabase.INSTANCE.get("library");
        var entities = new java.util.ArrayList<CommunicationEntity>();
        for (int index = 0; index < size; index++) {
            var entity = CommunicationEntity.of(collection);
            entity.add("_id", "id" + index);
            entity.add("embedding", DenseVector.of(1f, 0f));
            entities.add(entity);
        }
        manager.insert(entities);
        Mockito.doAnswer(invocation -> {
            iterator = Mockito.spy((QueryIterator) invocation.callRealMethod());
            return iterator;
        }).when(client).queryIterator(Mockito.any(QueryIteratorReq.class));
    }

    @Nested
    @DisplayName("When the stream is consumed")
    class WhenTheStreamIsConsumed {

        @Test
        @DisplayName("Should not open an iterator before the terminal operation")
        void shouldStayLazy() {
            seed(3);

            try (var stream = manager.select(select().from(collection).build())) {
                Mockito.verify(client, Mockito.never()).queryIterator(Mockito.any(QueryIteratorReq.class));
                assertThat(stream.findFirst()).isPresent();
            }
        }

        @Test
        @DisplayName("Should close the iterator when a partially consumed stream is closed")
        void shouldCloseWhenPartiallyConsumed() {
            seed(3);

            try (var stream = manager.select(select().from(collection).build())) {
                assertThat(stream.findFirst()).isPresent();
            }

            Mockito.verify(iterator, Mockito.times(1)).close();
        }

        @Test
        @DisplayName("Should close the iterator once when the stream is exhausted and then closed")
        void shouldCloseOnceWhenExhausted() {
            seed(3);

            try (var stream = manager.select(select().from(collection).build())) {
                assertThat(stream.toList()).hasSize(3);
            }

            Mockito.verify(iterator, Mockito.times(1)).close();
        }

        @Test
        @DisplayName("Should not open an iterator when the stream is closed before being consumed")
        void shouldNotOpenWhenClosedEarly() {
            seed(1);

            manager.select(select().from(collection).build()).close();

            Mockito.verify(client, Mockito.never()).queryIterator(Mockito.any(QueryIteratorReq.class));
        }
    }

    @Nested
    @DisplayName("When the iteration fails")
    class WhenTheIterationFails {

        @Test
        @DisplayName("Should close the iterator and propagate the failure")
        void shouldCloseOnFailure() {
            seed(3);
            ArgumentCaptor<QueryIteratorReq> captor = ArgumentCaptor.forClass(QueryIteratorReq.class);
            Mockito.doAnswer(invocation -> {
                iterator = Mockito.spy((QueryIterator) invocation.callRealMethod());
                Mockito.doThrow(new IllegalStateException("boom")).when(iterator).next();
                return iterator;
            }).when(client).queryIterator(captor.capture());

            try (var stream = manager.select(select().from(collection).build())) {
                assertThatThrownBy(stream::toList).isInstanceOf(IllegalStateException.class).hasMessage("boom");
            }

            Mockito.verify(iterator, Mockito.times(1)).close();
        }
    }
}

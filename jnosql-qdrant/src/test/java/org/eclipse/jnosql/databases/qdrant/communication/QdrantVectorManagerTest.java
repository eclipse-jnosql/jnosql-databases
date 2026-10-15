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
package org.eclipse.jnosql.databases.qdrant.communication;

import io.qdrant.client.QdrantClient;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.mapping.vector.VectorManager;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QdrantVectorManagerTest {

    private final QdrantClient client = Mockito.mock(QdrantClient.class);

    private final QdrantVectorManager manager =
            new QdrantVectorManager(client, "library", QdrantCollectionSettings.EMPTY);

    private final CommunicationEntity entity = CommunicationEntity.of("documents");

    @Nested
    @DisplayName("When using the manager as a vector manager")
    class WhenTheManagerIsUsed {

        @Test
        @DisplayName("Should be a VectorManager")
        void shouldBeVectorManager() {
            assertThat(manager).isInstanceOf(VectorManager.class);
        }

        @Test
        @DisplayName("Should return the database name")
        void shouldReturnName() {
            assertThat(manager.name()).isEqualTo("library");
        }
    }

    @Nested
    @DisplayName("When closing the manager")
    class WhenTheClosing {

        @Test
        @DisplayName("Should not close the client owned by the factory")
        void shouldNotCloseClient() {
            manager.close();

            Mockito.verifyNoInteractions(client);
        }
    }

    @Nested
    @DisplayName("When the arguments are invalid")
    class WhenTheArgumentsAreInvalid {

        @Test
        @DisplayName("Should reject null arguments")
        void shouldRejectNulls() {
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThatThrownBy(() -> manager.insert((CommunicationEntity) null))
                        .isInstanceOf(NullPointerException.class);
                soft.assertThatThrownBy(() -> manager.insert((Iterable<CommunicationEntity>) null))
                        .isInstanceOf(NullPointerException.class);
                soft.assertThatThrownBy(() -> manager.update((CommunicationEntity) null))
                        .isInstanceOf(NullPointerException.class);
                soft.assertThatThrownBy(() -> manager.update((Iterable<CommunicationEntity>) null))
                        .isInstanceOf(NullPointerException.class);
                soft.assertThatThrownBy(() -> manager.delete(null)).isInstanceOf(NullPointerException.class);
                soft.assertThatThrownBy(() -> manager.select(null)).isInstanceOf(NullPointerException.class);
                soft.assertThatThrownBy(() -> manager.count((String) null)).isInstanceOf(NullPointerException.class);
                soft.assertThatThrownBy(() -> manager.search(null)).isInstanceOf(NullPointerException.class);
            });
        }

        @Test
        @DisplayName("Should not support inserting with TTL")
        void shouldNotInsertWithTtl() {
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThatThrownBy(() -> manager.insert(entity, Duration.ofSeconds(1)))
                        .isInstanceOf(UnsupportedOperationException.class);
                soft.assertThatThrownBy(() -> manager.insert(List.of(entity), Duration.ofSeconds(1)))
                        .isInstanceOf(UnsupportedOperationException.class);
            });
        }

        @Test
        @DisplayName("Should require the _id to update")
        void shouldRequireIdToUpdate() {
            assertThatThrownBy(() -> manager.update(entity)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Should require exactly one vector to insert")
        void shouldRequireOneVector() {
            assertThatThrownBy(() -> manager.insert(entity)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}

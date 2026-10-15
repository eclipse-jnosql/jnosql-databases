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
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.mapping.vector.VectorManager;
import org.eclipse.jnosql.mapping.vector.VectorSelectQuery;
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
    @DisplayName("When executing operations not implemented yet")
    class WhenTheOperationIsUnsupported {

        @Test
        @DisplayName("Should not support inserting an entity")
        void shouldNotInsert() {
            assertUnsupported(() -> manager.insert(entity));
        }

        @Test
        @DisplayName("Should not support inserting an entity with TTL")
        void shouldNotInsertWithTtl() {
            assertUnsupported(() -> manager.insert(entity, Duration.ofSeconds(1)));
        }

        @Test
        @DisplayName("Should not support inserting entities")
        void shouldNotInsertEntities() {
            assertUnsupported(() -> manager.insert(List.of(entity)));
        }

        @Test
        @DisplayName("Should not support inserting entities with TTL")
        void shouldNotInsertEntitiesWithTtl() {
            assertUnsupported(() -> manager.insert(List.of(entity), Duration.ofSeconds(1)));
        }

        @Test
        @DisplayName("Should not support updating an entity")
        void shouldNotUpdate() {
            assertUnsupported(() -> manager.update(entity));
        }

        @Test
        @DisplayName("Should not support updating entities")
        void shouldNotUpdateEntities() {
            assertUnsupported(() -> manager.update(List.of(entity)));
        }

        @Test
        @DisplayName("Should not support deleting")
        void shouldNotDelete() {
            assertUnsupported(() -> manager.delete(Mockito.mock(DeleteQuery.class)));
        }

        @Test
        @DisplayName("Should not support selecting")
        void shouldNotSelect() {
            assertUnsupported(() -> manager.select(Mockito.mock(SelectQuery.class)));
        }

        @Test
        @DisplayName("Should not support counting")
        void shouldNotCount() {
            assertUnsupported(() -> manager.count("documents"));
        }

        @Test
        @DisplayName("Should not support searching")
        void shouldNotSearch() {
            assertUnsupported(() -> manager.search(Mockito.mock(VectorSelectQuery.class)));
        }

        private void assertUnsupported(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
            assertThatThrownBy(callable)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessage("not supported yet");
        }
    }
}

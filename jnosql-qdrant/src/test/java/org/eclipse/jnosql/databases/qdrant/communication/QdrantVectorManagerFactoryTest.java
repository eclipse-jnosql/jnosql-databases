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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class QdrantVectorManagerFactoryTest {

    private final QdrantClient client = Mockito.mock(QdrantClient.class);

    private final QdrantVectorManagerFactory factory =
            new QdrantVectorManagerFactory(client, QdrantCollectionSettings.EMPTY);

    @Nested
    @DisplayName("When creating a vector manager")
    class WhenTheCreation {

        @Test
        @DisplayName("Should create a manager named after the database")
        void shouldCreateManager() {
            var manager = (QdrantVectorManager) factory.apply("library");

            assertSoftly(softly -> {
                softly.assertThat(manager).as("manager").isNotNull();
                softly.assertThat(manager.name()).as("name").isEqualTo("library");
            });
        }

        @Test
        @DisplayName("Should hand the collection settings to the manager")
        void shouldPassCollectionSettings() {
            var settings = new QdrantCollectionSettings(QdrantDistance.COSINE, 128L);
            var manager = (QdrantVectorManager) new QdrantVectorManagerFactory(client, settings).apply("library");

            assertThat(manager.collectionSettings()).isEqualTo(settings);
        }

        @Test
        @DisplayName("Should reject a null database")
        void shouldRejectNullDatabase() {
            assertThatNullPointerException().isThrownBy(() -> factory.apply(null))
                    .withMessage("database is required");
        }
    }

    @Nested
    @DisplayName("When closing the factory")
    class WhenTheClosing {

        @Test
        @DisplayName("Should close the client")
        void shouldCloseClient() {
            factory.close();

            Mockito.verify(client).close();
        }

        @Test
        @DisplayName("Should keep the client open when a manager is closed")
        void shouldKeepClientOpenWhenManagerCloses() {
            factory.apply("library").close();

            Mockito.verify(client, Mockito.never()).close();
        }
    }
}

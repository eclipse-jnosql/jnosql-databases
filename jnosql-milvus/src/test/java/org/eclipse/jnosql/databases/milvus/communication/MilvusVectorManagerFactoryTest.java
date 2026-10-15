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
import io.milvus.v2.service.database.request.CreateDatabaseReq;
import io.milvus.v2.service.database.response.ListDatabasesResp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class MilvusVectorManagerFactoryTest {

    private final MilvusClientV2 client = Mockito.mock(MilvusClientV2.class);

    private final MilvusVectorManagerFactory factory = new MilvusConfiguration().get(client);

    private void databases(String... names) {
        ListDatabasesResp response = Mockito.mock(ListDatabasesResp.class);
        Mockito.when(response.getDatabaseNames()).thenReturn(List.of(names));
        Mockito.when(client.listDatabases()).thenReturn(response);
    }

    @Nested
    @DisplayName("When creating a vector manager")
    class WhenTheCreation {

        @Test
        @DisplayName("Should create a manager named after an existing database")
        void shouldCreateManager() {
            databases("library");

            var manager = (MilvusVectorManager) factory.apply("library");

            assertSoftly(soft -> {
                soft.assertThat(manager.name()).isEqualTo("library");
                soft.assertThat(manager.collectionSettings()).isEqualTo(MilvusCollectionSettings.DEFAULT);
            });
            Mockito.verify(client, Mockito.never()).createDatabase(Mockito.any());
        }

        @Test
        @DisplayName("Should create the database when it does not exist")
        void shouldCreateDatabase() {
            databases("default");

            factory.apply("library");

            ArgumentCaptor<CreateDatabaseReq> request = ArgumentCaptor.forClass(CreateDatabaseReq.class);
            Mockito.verify(client).createDatabase(request.capture());
            assertThat(request.getValue().getDatabaseName()).isEqualTo("library");
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
            databases("library");

            factory.apply("library").close();

            Mockito.verify(client, Mockito.never()).close();
        }
    }
}

/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License 2.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *   and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 *   You may elect to redistribute this code under either of these licenses.
 */
package org.eclipse.jnosql.databases.oracle.communication;

import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultOracleNoSQLDocumentManagerTest {

    @Test
    void shouldPreserveExistingTypedId() {
        var entity = CommunicationEntity.of("CatalogItem");
        entity.add("_id", 54L);

        DefaultOracleNoSQLDocumentManager.addPrimaryKeyIdWhenMissing(entity, "CatalogItem:54");

        assertThat(entity.find("_id").orElseThrow().get())
                .isEqualTo(54L)
                .isInstanceOf(Long.class);
    }

    @Test
    void shouldRemoveOnlyFirstPrimaryKeyPrefix() {
        var entity = CommunicationEntity.of("CatalogItem");

        DefaultOracleNoSQLDocumentManager.addPrimaryKeyIdWhenMissing(entity, "CatalogItem:part:42");

        assertThat(entity.find("_id").orElseThrow().get()).isEqualTo("part:42");
    }

    @Test
    void shouldUseEntirePrimaryKeyWithoutPrefix() {
        var entity = CommunicationEntity.of("CatalogItem");

        DefaultOracleNoSQLDocumentManager.addPrimaryKeyIdWhenMissing(entity, "standalone");

        assertThat(entity.find("_id").orElseThrow().get()).isEqualTo("standalone");
    }
}

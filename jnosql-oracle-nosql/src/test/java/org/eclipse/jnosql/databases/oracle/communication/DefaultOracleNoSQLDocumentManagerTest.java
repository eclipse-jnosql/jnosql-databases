/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 */
package org.eclipse.jnosql.databases.oracle.communication;

import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultOracleNoSQLDocumentManagerTest {

    @Test
    void shouldPreserveTypedJsonId() {
        var entity = CommunicationEntity.of("person");
        entity.add("_id", 54L);

        DefaultOracleNoSQLDocumentManager.addPrimaryKeyIdWhenMissing(entity, "person:54");

        assertThat(entity.find("_id")).hasValueSatisfying(id ->
                assertThat(id.get()).isInstanceOf(Long.class).isEqualTo(54L));
    }

    @ParameterizedTest
    @CsvSource({
            "person:alpha:beta, alpha:beta",
            "raw-id, raw-id"
    })
    void shouldExtractFallbackIdFromPrimaryKey(String primaryKey, String expectedId) {
        var entity = CommunicationEntity.of("person");

        DefaultOracleNoSQLDocumentManager.addPrimaryKeyIdWhenMissing(entity, primaryKey);

        assertThat(entity.find("_id")).hasValueSatisfying(id ->
                assertThat(id.get()).isEqualTo(expectedId));
    }
}

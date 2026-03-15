/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.oracle.mapping;

import org.eclipse.jnosql.databases.oracle.communication.OracleNoSQLDocumentManager;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.semistructured.EventPersistManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for OracleNoSQLTemplateBuilder")
class OracleNoSQLTemplateBuilderTest {

    @Mock
    private Converters converters;

    @Mock
    private EntitiesMetadata entities;

    @Mock
    private OracleNoSQLDocumentManager manager;

    @Mock
    private EntityConverter converter;

    @Mock
    private EventPersistManager eventManager;

    @Test
    @DisplayName("shouldCreateOracleNoSQLTemplateUsingBuilder")
    void shouldCreateOracleNoSQLTemplateUsingBuilder() {
        OracleNoSQLTemplate template = OracleNoSQLTemplateBuilder.builder()
                .withConverters(converters)
                .withEntities(entities)
                .withManager(manager)
                .withEntityConverter(converter)
                .withEventPersistManager(eventManager)
                .build();

        assertThat(template).isNotNull();
    }

    @Test
    @DisplayName("shouldThrowExceptionWhenConvertersIsNull")
    void shouldThrowExceptionWhenConvertersIsNull() {
        assertThrows(NullPointerException.class, () ->
                OracleNoSQLTemplateBuilder.builder().withConverters(null));
    }

    @Test
    @DisplayName("shouldThrowExceptionWhenEntitiesIsNull")
    void shouldThrowExceptionWhenEntitiesIsNull() {
        assertThrows(NullPointerException.class, () ->
                OracleNoSQLTemplateBuilder.builder().withConverters(converters).withEntities(null));
    }

    @Test
    @DisplayName("shouldThrowExceptionWhenManagerIsNull")
    void shouldThrowExceptionWhenManagerIsNull() {
        assertThrows(NullPointerException.class, () ->
                OracleNoSQLTemplateBuilder.builder()
                        .withConverters(converters)
                        .withEntities(entities)
                        .withManager((OracleNoSQLDocumentManager) null));
    }

    @Test
    @DisplayName("shouldThrowExceptionWhenConverterIsNull")
    void shouldThrowExceptionWhenConverterIsNull() {
        assertThrows(NullPointerException.class, () ->
                OracleNoSQLTemplateBuilder.builder()
                        .withConverters(converters)
                        .withEntities(entities)
                        .withManager(manager)
                        .withEntityConverter(null));
    }

    @Test
    @DisplayName("shouldThrowExceptionWhenEventManagerIsNull")
    void shouldThrowExceptionWhenEventManagerIsNull() {
        assertThrows(NullPointerException.class, () ->
                OracleNoSQLTemplateBuilder.builder()
                        .withConverters(converters)
                        .withEntities(entities)
                        .withManager(manager)
                        .withEntityConverter(converter)
                        .withEventPersistManager(null));
    }
}

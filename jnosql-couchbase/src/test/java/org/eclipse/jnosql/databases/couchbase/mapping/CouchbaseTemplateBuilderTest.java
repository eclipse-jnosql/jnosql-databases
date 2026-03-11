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
package org.eclipse.jnosql.databases.couchbase.mapping;

import org.eclipse.jnosql.databases.couchbase.communication.CouchbaseDocumentManager;
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
@DisplayName("Unit tests for CouchbaseTemplateBuilder")
class CouchbaseTemplateBuilderTest {

    @Mock
    private Converters converters;

    @Mock
    private EntitiesMetadata entities;

    @Mock
    private CouchbaseDocumentManager manager;

    @Mock
    private EntityConverter converter;

    @Mock
    private EventPersistManager eventManager;

    @Test
    @DisplayName("shouldCreateCouchbaseTemplateUsingBuilder")
    void shouldCreateCouchbaseTemplateUsingBuilder() {
        CouchbaseTemplate template = CouchbaseTemplateBuilder.builder()
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
                CouchbaseTemplateBuilder.builder().withConverters(null));
    }
}

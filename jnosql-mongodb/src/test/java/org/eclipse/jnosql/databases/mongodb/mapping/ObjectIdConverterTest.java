/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.mongodb.mapping;

import jakarta.nosql.AttributeConverter;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectIdConverterTest {

    private AttributeConverter<String, ObjectId> converter;

    @BeforeEach
    void setUp() {
        this.converter = new ObjectIdConverter();
    }

    @Test
    void shouldReturnNullWhenAttributeIsNull() {
        assertThat(this.converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void shouldReturnNullWhenDataIsNull() {
        assertThat(this.converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void shouldConvertToEntity() {
        ObjectId id = new ObjectId();
        String entityAttribute = this.converter.convertToEntityAttribute(id);
        assertThat(entityAttribute).isNotNull();
        assertThat(entityAttribute).isEqualTo(id.toString());
    }

    @Test
    void shouldConvertToDatabase() {
        ObjectId objectId = new ObjectId();
        String entityAttribute = objectId.toString();
        ObjectId id = this.converter.convertToDatabaseColumn(entityAttribute);
        assertThat(id).isNotNull();
        assertThat(id).isEqualTo(objectId);
    }
}
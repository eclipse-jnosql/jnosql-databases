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

import java.util.Objects;

/**
 * An implementation of AttributeConverter where it converts the {@link ObjectId}
 * from/to {@link String}
 */
public class ObjectIdConverter implements AttributeConverter<String, ObjectId> {

    @Override
    public ObjectId convertToDatabaseColumn(String attribute) {
        if(Objects.nonNull(attribute)) {
            return new ObjectId(attribute);
        }
        return null;
    }

    @Override
    public String convertToEntityAttribute(ObjectId dbData) {
        if(Objects.nonNull(dbData)) {
           return dbData.toString();
        }
        return null;
    }
}

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
 * Converts between a {@link String} entity attribute and a MongoDB {@link ObjectId}, so an id can be kept as a
 * string in the entity while MongoDB stores a native {@code ObjectId}. It does not handle {@link java.util.UUID}.
 *
 * <pre>{@code
 * @Entity
 * public class Music {
 *     @Id
 *     @Convert(ObjectIdConverter.class)
 *     private String id;
 * }
 * }</pre>
 *
 * <p>A {@code null} value converts to {@code null} in both directions. Converting to the database requires a
 * 24-character hexadecimal string, otherwise {@link IllegalArgumentException} is thrown.</p>
 */
public class ObjectIdConverter implements AttributeConverter<String, ObjectId> {

    /**
     * Converts the hexadecimal string to an {@link ObjectId}.
     *
     * @param attribute the 24-character hexadecimal string, or {@code null}
     * @return the {@link ObjectId}, or {@code null} when {@code attribute} is {@code null}
     * @throws IllegalArgumentException when {@code attribute} is not a valid ObjectId
     */
    @Override
    public ObjectId convertToDatabaseColumn(String attribute) {
        if(Objects.nonNull(attribute)) {
            return new ObjectId(attribute);
        }
        return null;
    }

    /**
     * Converts the {@link ObjectId} to its hexadecimal string.
     *
     * @param dbData the {@link ObjectId}, or {@code null}
     * @return the hexadecimal string, or {@code null} when {@code dbData} is {@code null}
     */
    @Override
    public String convertToEntityAttribute(ObjectId dbData) {
        if(Objects.nonNull(dbData)) {
           return dbData.toString();
        }
        return null;
    }
}

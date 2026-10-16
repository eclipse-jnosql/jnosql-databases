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
package org.eclipse.jnosql.databases.weaviate.communication;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Maps the names and ids of the JNoSQL entities to the names and ids accepted by Weaviate.
 *
 * <ul>
 *     <li>A collection name starts with an upper case letter in Weaviate, so the first letter of the entity name
 *     is upper-cased;</li>
 *     <li>{@code id} and {@code _id} are reserved properties in Weaviate, so the JNoSQL id is stored in the
 *     {@value #ID_PROPERTY} property;</li>
 *     <li>Weaviate objects are identified by a UUID, so a text id that is a UUID is used as is and any other id
 *     becomes a name based UUID. The original text is kept in {@value #ID_PROPERTY}.</li>
 * </ul>
 */
final class WeaviateNames {

    static final String ID_FIELD = "_id";

    static final String ID_PROPERTY = "_jnosql_id";

    private static final Pattern COLLECTION = Pattern.compile("[A-Z][_0-9A-Za-z]*");

    private static final Pattern PROPERTY = Pattern.compile("[_A-Za-z][_0-9A-Za-z]*");

    private static final Pattern UUID_TEXT = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private static final Set<String> RESERVED = Set.of("id", "_id", "_additional");

    private WeaviateNames() {
    }

    static String collection(String entity) {
        Objects.requireNonNull(entity, "entity is required");
        if (entity.isEmpty()) {
            throw new IllegalArgumentException("The entity name must not be empty");
        }
        String name = entity.substring(0, 1).toUpperCase(Locale.ROOT) + entity.substring(1);
        if (!COLLECTION.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid Weaviate collection name: " + entity
                    + ". It must match " + COLLECTION.pattern());
        }
        return name;
    }

    static String property(String field) {
        Objects.requireNonNull(field, "field is required");
        if (ID_FIELD.equals(field)) {
            return ID_PROPERTY;
        }
        if (RESERVED.contains(field) || ID_PROPERTY.equals(field)) {
            throw new IllegalArgumentException("The name " + field + " is reserved by the Weaviate driver");
        }
        if (!PROPERTY.matcher(field).matches()) {
            throw new IllegalArgumentException("Invalid Weaviate property name: " + field
                    + ". It must match " + PROPERTY.pattern());
        }
        return field;
    }

    static String field(String property) {
        return ID_PROPERTY.equals(property) ? ID_FIELD : property;
    }

    static String vectorName(String field) {
        return property(field);
    }

    static String objectId(String id) {
        Objects.requireNonNull(id, "id is required");
        if (UUID_TEXT.matcher(id).matches()) {
            return id.toLowerCase(Locale.ROOT);
        }
        return UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8)).toString();
    }
}

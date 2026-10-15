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

import io.qdrant.client.PointIdFactory;
import io.qdrant.client.grpc.Common.PointId;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Maps entity identifiers to Qdrant point ids, which must be unsigned integers or UUIDs.
 * Integral non-negative numbers and UUIDs are kept as they are; any other identifier
 * is mapped to a deterministic UUID. The original identifier is stored in the payload.
 */
final class QdrantPointIds {

    static final String ID_FIELD = "_id";

    private static final int UUID_LENGTH = 36;

    private QdrantPointIds() {
    }

    static PointId of(Object id) {
        if (id instanceof UUID uuid) {
            return PointIdFactory.id(uuid);
        }
        if (id instanceof Byte || id instanceof Short || id instanceof Integer || id instanceof Long) {
            long number = ((Number) id).longValue();
            if (number >= 0) {
                return PointIdFactory.id(number);
            }
        }
        String text = id.toString();
        if (text.length() == UUID_LENGTH) {
            try {
                return PointIdFactory.id(UUID.fromString(text));
            } catch (IllegalArgumentException exception) {
                // not a UUID, it falls back to the deterministic one
            }
        }
        return PointIdFactory.id(UUID.nameUUIDFromBytes(text.getBytes(StandardCharsets.UTF_8)));
    }

    static Object value(PointId id) {
        return id.hasUuid() ? id.getUuid() : id.getNum();
    }

    static Object storedValue(Object id) {
        return id instanceof UUID ? id.toString() : id;
    }
}

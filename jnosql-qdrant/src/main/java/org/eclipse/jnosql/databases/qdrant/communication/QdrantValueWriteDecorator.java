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

import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.ValueWriter;
import org.eclipse.jnosql.communication.Value;

import java.util.List;
import java.util.UUID;

/**
 * Converts Java values into the representation accepted by the Qdrant driver,
 * such as {@link java.util.UUID} to {@link String}.
 */
final class QdrantValueWriteDecorator<T, S> implements ValueWriter<T, S> {

    @SuppressWarnings("rawtypes")
    static final ValueWriter QDRANT_VALUE_WRITER = new QdrantValueWriteDecorator();

    static Object convert(Value value) {
        return ValueUtil.convert(value, QDRANT_VALUE_WRITER);
    }

    static List<Object> convertToList(Value value) {
        return ValueUtil.convertToList(value, QDRANT_VALUE_WRITER);
    }

    @Override
    public boolean test(Class<?> type) {
        return UUID.class.equals(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public S write(T value) {
        return value instanceof UUID uuid ? (S) uuid.toString() : null;
    }
}

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
package org.eclipse.jnosql.databases.mongodb.communication;

import org.bson.types.Binary;
import org.eclipse.jnosql.communication.ValueReader;

/**
 * An implementation of {@link ValueReader} of {@link Binary}
 */
public class BinaryValueReader implements ValueReader {

    @Override
    public boolean test(Class<?> type) {
        return Binary.class.equals(type);
    }

    @Override
    public <T> T read(Class<T> valueType, Object value) {

        if (value == null) {
            return null;
        }
        if (value instanceof byte[]) {
            return (T) new Binary((byte[]) value);
        }
        return (T) new Binary(value.toString().getBytes());
    }
}

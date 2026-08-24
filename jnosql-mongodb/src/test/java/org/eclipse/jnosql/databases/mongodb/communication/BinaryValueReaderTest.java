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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BinaryValueReaderTest {

    private ValueReader valueReader;

    @BeforeEach
    void init() {
        valueReader = new BinaryValueReader();
    }

    @Test
    void shouldValidateCompatibility() {
        assertTrue(valueReader.test(Binary.class));
        assertFalse(valueReader.test(AtomicBoolean.class));
    }

    @Test
    void shouldConvert() {
        byte[] bytes = new byte[] {10, 10, 10};
        assertEquals(new Binary(bytes), valueReader.read(Binary.class, bytes));
        assertEquals(new Binary("hello".getBytes()), valueReader.read(Binary.class, "hello"));
    }
}
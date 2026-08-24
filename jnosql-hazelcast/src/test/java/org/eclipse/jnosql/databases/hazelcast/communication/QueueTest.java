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

package org.eclipse.jnosql.databases.hazelcast.communication;


import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;
import org.eclipse.jnosql.databases.hazelcast.communication.model.LineBank;
import org.eclipse.jnosql.databases.hazelcast.communication.util.KeyValueEntityManagerFactoryUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QueueTest {


    private BucketManagerFactory keyValueEntityManagerFactory;

    private Queue<LineBank> lineBank;

    @BeforeEach
    public void init() {
        keyValueEntityManagerFactory =  KeyValueEntityManagerFactoryUtils.get();
        lineBank = keyValueEntityManagerFactory.getQueue("physical-bank", LineBank.class);
    }

    @Test
    public void shouldPushInTheLine() {
        assertTrue(lineBank.add(new LineBank("Otavio", 25)));
        assertEquals(1, lineBank.size());
        LineBank otavio = lineBank.poll();
        assertEquals(otavio.getPerson().name(), "Otavio");
        assertNull(lineBank.poll());
        assertTrue(lineBank.isEmpty());
    }

    @Test
    public void shouldPeekInTheLine() {
        lineBank.add(new LineBank("Otavio", 25));
        LineBank otavio = lineBank.peek();
        assertNotNull(otavio);
        assertNotNull(lineBank.peek());
        LineBank otavio2 = lineBank.remove();
        assertEquals(otavio.getPerson().name(), otavio2.getPerson().name());
        boolean happendException = false;
        try {
            lineBank.remove();
        }catch(NoSuchElementException e) {
            happendException = true;
        }
        assertTrue(happendException);
    }

    @Test
    public void shouldElementInTheLine() {
        lineBank.add(new LineBank("Otavio", 25));
        assertNotNull(lineBank.element());
        assertNotNull(lineBank.element());
        lineBank.remove(new LineBank("Otavio", 25));
        boolean happendException = false;
        try {
            lineBank.element();
        }catch(NoSuchElementException e) {
            happendException = true;
        }
        assertTrue(happendException);
    }
    @SuppressWarnings("unused")
    @Test
    public void shouldIterate() {
        lineBank.add(new LineBank("Otavio", 25));
        lineBank.add(new LineBank("Gama", 26));
        int count = 0;
        for (LineBank line: lineBank) {
            count++;
        }
        assertEquals(2, count);
        lineBank.remove();
        lineBank.remove();
        count = 0;
        for (LineBank line: lineBank) {
            count++;
        }
        assertEquals(0, count);
    }
    @AfterEach
    public void dispose() {
        lineBank.clear();
    }
}

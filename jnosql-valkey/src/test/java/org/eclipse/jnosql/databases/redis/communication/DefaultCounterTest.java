/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
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
 *   Otavio Santana
 */
package org.eclipse.jnosql.databases.redis.communication;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;

import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.junit.jupiter.api.Assertions.assertEquals;


@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class DefaultCounterTest {

    private RedisBucketManagerFactory keyValueEntityManagerFactory;
    private Counter counter;

    @BeforeEach
    public void init() {
        keyValueEntityManagerFactory = KeyValueDatabase.INSTANCE.get();
        counter = keyValueEntityManagerFactory.getCounter("counter-redis");
        counter.delete();
    }

    @Test
    public void shouldIncrement() {
        assertEquals(1D, counter.increment());
        assertEquals(10D, counter.increment(9));
    }

    @Test
    public void shouldDecrement() {
        counter.increment(10.15);
        assertEquals(9.15D, counter.decrement());
        assertEquals(0.15D, counter.decrement(9));
    }

    @Test
    public void shouldGet() {
        counter.increment(10.15);
        assertEquals(10.15D, counter.get().doubleValue());
    }

    @Test
    public void shouldShouldExpires() throws InterruptedException {
        counter.increment(10.15);
        counter.expire(Duration.ofSeconds(1));
        Thread.sleep(2_000L);
        assertEquals(0D, counter.get().doubleValue());
    }

    @Test
    public void shouldPersist() throws InterruptedException {
        counter.increment(10.15);
        counter.expire(Duration.ofSeconds(1));
        counter.persist();
        Thread.sleep(2_000L);
        assertEquals(10.15D, counter.get().doubleValue());
    }

    @Test
    public void shouldDelete() {
        counter.increment(10.15);
        counter.delete();
        assertEquals(0D, counter.get().doubleValue());
    }

    @AfterEach
    public void removeCounter(){
        counter.delete();
    }

}
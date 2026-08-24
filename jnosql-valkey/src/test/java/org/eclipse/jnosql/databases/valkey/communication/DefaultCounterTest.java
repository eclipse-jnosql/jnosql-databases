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
package org.eclipse.jnosql.databases.valkey.communication;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;

import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.assertj.core.api.Assertions.assertThat;


@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class DefaultCounterTest {

    private ValkeyBucketManagerFactory keyValueEntityManagerFactory;
    private Counter counter;

    @BeforeEach
    public void init() {
        keyValueEntityManagerFactory = KeyValueDatabase.INSTANCE.get();
        counter = keyValueEntityManagerFactory.getCounter("counter-redis");
        counter.delete();
    }

    @Test
    public void shouldIncrement() {
        assertThat(counter.increment()).isEqualTo(1D);
        assertThat(counter.increment(9)).isEqualTo(10D);
    }

    @Test
    public void shouldDecrement() {
        counter.increment(10.15);
        assertThat(counter.decrement()).isEqualTo(9.15D);
        assertThat(counter.decrement(9)).isEqualTo(0.15D);
    }

    @Test
    public void shouldGet() {
        counter.increment(10.15);
        assertThat(counter.get().doubleValue()).isEqualTo(10.15D);
    }

    @Test
    public void shouldShouldExpires() throws InterruptedException {
        counter.increment(10.15);
        counter.expire(Duration.ofSeconds(1));
        Thread.sleep(2_000L);
        assertThat(counter.get().doubleValue()).isEqualTo(0D);
    }

    @Test
    public void shouldPersist() throws InterruptedException {
        counter.increment(10.15);
        counter.expire(Duration.ofSeconds(1));
        counter.persist();
        Thread.sleep(2_000L);
        assertThat(counter.get().doubleValue()).isEqualTo(10.15D);
    }

    @Test
    public void shouldDelete() {
        counter.increment(10.15);
        counter.delete();
        assertThat(counter.get().doubleValue()).isEqualTo(0D);
    }

    @AfterEach
    public void removeCounter(){
        counter.delete();
    }

}
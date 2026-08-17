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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class DefaultSortedSetTest {

    private static final String BRAZIL = "Brazil";
    private static final String USA = "USA";
    private static final String ENGLAND = "England";

    private RedisBucketManagerFactory keyValueEntityManagerFactory;
    private SortedSet sortedSet;

    @BeforeEach
    public void init() {
        keyValueEntityManagerFactory = KeyValueDatabase.INSTANCE.get();
        sortedSet = keyValueEntityManagerFactory.getSortedSet("world-cup-2018");
        sortedSet.clear();
    }

    @Test
    public void shouldAdd() {
        sortedSet.add(BRAZIL, 10);
    }

    @Test
    public void shouldSize() {
        assertThat(sortedSet.isEmpty()).isTrue();
        sortedSet.add(BRAZIL, 10);
        assertThat(Integer.valueOf(1)).isEqualTo(Integer.valueOf(sortedSet.size()));
    }

    @Test
    public void shouldCheckIfEmpty() {

        assertThat(sortedSet.isEmpty()).isTrue();
        sortedSet.add(BRAZIL, 1);
        sortedSet.add(USA, 2);
        sortedSet.add(ENGLAND, 3);
        assertThat(sortedSet.isEmpty()).isFalse();

    }

    @Test
    public void souldDelete() {
        sortedSet.add(BRAZIL, 1);
        sortedSet.add(USA, 2);
        sortedSet.add(ENGLAND, 3);
        assertThat(sortedSet.isEmpty()).isFalse();
        sortedSet.delete();
        assertThat(sortedSet.isEmpty()).isTrue();
    }

    @Test
    public void souldClear() {
        sortedSet.add(BRAZIL, 1);
        sortedSet.add(USA, 2);
        sortedSet.add(ENGLAND, 3);
        assertThat(sortedSet.isEmpty()).isFalse();
        sortedSet.clear();
        assertThat(sortedSet.isEmpty()).isTrue();
    }

    @Test
    public void shouldIncrement() {
        sortedSet.add(BRAZIL, 10);
        Number points = sortedSet.increment(BRAZIL, 2);
        assertThat(Long.valueOf(points.longValue())).isEqualTo(Long.valueOf(12));
    }

    @Test
    public void shouldDecrement() {
        sortedSet.add(BRAZIL, 10);
        Number points = sortedSet.decrement(BRAZIL, 2);
        assertThat(Long.valueOf(points.longValue())).isEqualTo(Long.valueOf(8));
    }

    @Test
    public void shouldRemoveMember() {
        sortedSet.add(BRAZIL, 10);
        sortedSet.remove(BRAZIL);
        assertThat(sortedSet.size()).isEqualTo(0);
    }

    @Test
    public void shouldShouldExpires() throws InterruptedException {
        sortedSet.add(BRAZIL, 10);
        sortedSet.expire(Duration.ofSeconds(1));
        Thread.sleep(2_000L);
        assertThat(sortedSet.size()).isEqualTo(0);
    }

    @Test
    public void shouldPersist() throws InterruptedException {
        sortedSet.add(BRAZIL, 10);
        sortedSet.expire(Duration.ofSeconds(1));
        sortedSet.persist();
        Thread.sleep(2_000L);
        assertThat(sortedSet.size()).isEqualTo(1);
    }

    @Test
    public void shouldRange() {
        sortedSet.add(BRAZIL, 1);
        sortedSet.add(USA, 2);
        sortedSet.add(ENGLAND, 3);

        assertThat(sortedSet.range(2, 3)).contains(Ranking.of(ENGLAND, 3.0));
    }

    @Test
    public void shoulgetRanges() {
        Ranking brazil = Ranking.of(BRAZIL, 1.0);
        Ranking usa = Ranking.of(USA, 2.0);
        Ranking england = Ranking.of(ENGLAND, 3.0);
        sortedSet.add(brazil);
        sortedSet.add(usa);
        sortedSet.add(england);

        assertThat(sortedSet.getRanking()).contains(brazil, usa, england);
    }

    @Test
    public void shouldRevRange() {
        sortedSet.add(BRAZIL, 1);
        sortedSet.add(USA, 2);
        sortedSet.add(ENGLAND, 3);

        assertThat(sortedSet.revRange(2, 3)).contains(Ranking.of(BRAZIL, 1.0));
    }

    @Test
    public void shoulgetRevRanges() {
        Ranking brazil = Ranking.of(BRAZIL, 1.0);
        Ranking usa = Ranking.of(USA, 2.0);
        Ranking england = Ranking.of(ENGLAND, 3.0);
        sortedSet.add(brazil);
        sortedSet.add(usa);
        sortedSet.add(england);

        assertThat(sortedSet.getRevRanking()).contains(england, usa, brazil);
    }

    @AfterEach
    public void remove() {
        sortedSet.clear();
    }

}
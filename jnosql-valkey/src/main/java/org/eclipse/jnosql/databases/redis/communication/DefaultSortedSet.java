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


import redis.clients.jedis.UnifiedJedis;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

/**
 * The default {@link SortedSet} implementation
 */
class DefaultSortedSet implements SortedSet {


    private static final int LAST_ELEMENT = -1;
    private String key;

    private UnifiedJedis jedis;

    DefaultSortedSet(UnifiedJedis jedis, String keyspace) {
        Objects.requireNonNull(jedis, "jedis is required");
        Objects.requireNonNull(keyspace, "keyspace is required");
        this.key = keyspace;
        this.jedis = jedis;
    }

    @Override
    public void add(String member, Number value) throws NullPointerException {
        Objects.requireNonNull(member, "member is required");
        Objects.requireNonNull(value, "value is required");
        jedis.zadd(key, value.doubleValue(), member);
    }

    @Override
    public void add(Ranking ranking) throws NullPointerException {
        Objects.requireNonNull(ranking, "ranking is required");
        jedis.zadd(key, ranking.getPoints().doubleValue(), ranking.getMember());
    }

    @Override
    public Number increment(String member, Number value) throws NullPointerException {
        Objects.requireNonNull(member, "member is required");
        Objects.requireNonNull(value, "value is required");
        return jedis.zincrby(key, value.doubleValue(), member);
    }

    @Override
    public Number decrement(String member, Number value) throws NullPointerException {
        Objects.requireNonNull(member, "member is required");
        Objects.requireNonNull(value, "value is required");
        return increment(member, -value.longValue());
    }

    @Override
    public void remove(String member) throws NullPointerException {
        jedis.zrem(key, member);
    }

    @Override
    public int size() {
        return (int) jedis.zcard(key);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void delete() {
        jedis.del(key);
    }


    @Override
    public void expire(Duration ttl) throws NullPointerException {
        Objects.requireNonNull(ttl, "ttl is required");
        jedis.expire(key, (int) ttl.getSeconds());
    }

    @Override
    public void persist() {
        jedis.persist(key);
    }

    @Override
    public List<Ranking> range(long start, long end) {
        return jedis.zrangeWithScores(key, start, end).stream()
                .map(t -> new DefaultRanking(t.getElement(), t.getScore()))
                .collect(toList());
    }

    @Override
    public List<Ranking> revRange(long start, long end) {
        return jedis.zrevrangeWithScores(key, start, end).stream()
                .map(t -> new DefaultRanking(t.getElement(), t.getScore()))
                .collect(toList());
    }

    @Override
    public List<Ranking> getRanking() {
        return range(0, LAST_ELEMENT);
    }

    @Override
    public List<Ranking> getRevRanking() {
        return revRange(0, LAST_ELEMENT);
    }

    @Override
    public void clear() {
        jedis.del(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DefaultSortedSet that)) {
            return false;
        }
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key);
    }

    @Override
    public String toString() {
        return "SortedSet{" + "key='" + key + '\'' +
                '}';
    }
}

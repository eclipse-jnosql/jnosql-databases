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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;


/**
 * The default {@link Counter}
 */
class DefaultCounter implements Counter {

    private static final Predicate<String> IS_EMPTY = String::isEmpty;
    private static final Predicate<String> IS_NOT_EMPTY = IS_EMPTY.negate();

    private final String key;

    private final UnifiedJedis jedis;

    DefaultCounter(String key, UnifiedJedis jedis) {
        this.key = key;
        this.jedis = jedis;
    }

    @Override
    public Number get() {
        return Optional.ofNullable(jedis.get(key))
                .filter(IS_NOT_EMPTY)
                .map(Double::valueOf)
                .orElse(0D);
    }

    @Override
    public Number increment() {
        return increment(1);
    }

    @Override
    public Number increment(Number value) throws NullPointerException {
        Objects.requireNonNull(value, "value is required");
        return jedis.incrByFloat(key, value.doubleValue());
    }

    @Override
    public Number decrement() {
        return increment(-1);
    }

    @Override
    public Number decrement(Number value) {
        Objects.requireNonNull(value, "value is required");
        return jedis.incrByFloat(key, -value.doubleValue());
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DefaultCounter that)) {
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
        return "Counter{" + "key='" + key + '\'' +
                '}';
    }
}

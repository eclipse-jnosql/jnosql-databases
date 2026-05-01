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

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;

class RedisQueue<T> extends RedisCollection<T> implements Queue<T> {

    RedisQueue(UnifiedJedis jedis, Class<T> clazz, String keyWithNameSpace) {
        super(jedis, clazz, keyWithNameSpace);
    }

    @Override
    public void clear() {
        jedis.del(keyWithNameSpace);
    }

    @Override
    public boolean add(T e) {
        Objects.requireNonNull(e);
        if(isString){
            jedis.rpush(keyWithNameSpace, e.toString());
        } else {
            jedis.rpush(keyWithNameSpace, JSONB.toJson(e));
        }
        return true;
    }

    @Override
    public boolean offer(T e) {
        return add(e);
    }

    @Override
    public T remove() {
        T value = poll();
        if (value == null) {
            throw new NoSuchElementException("No element in Redis Queue");
        }
        return value;
    }

    @Override
    public T poll() {
        String value = jedis.lpop(keyWithNameSpace);
        if (value != null && !value.isEmpty()) {
            if(isString){
                return (T) value;
            } else {
                return JSONB.fromJson(value, clazz);
            }

        }
        return null;
    }

    @Override
    public T element() {
        T value = peek();
        if (value == null) {
            throw new NoSuchElementException("No element in Redis Queue");
        }
        return value;
    }

    @Override
    public T peek() {
        int index = size();
        if (index == 0) {
            return null;
        }
        if(isString) {
            return (T) jedis.lindex(keyWithNameSpace, (long) index - 1);
        } else {
            return JSONB.fromJson(jedis.lindex(keyWithNameSpace, (long) index - 1), clazz);
        }


    }

}

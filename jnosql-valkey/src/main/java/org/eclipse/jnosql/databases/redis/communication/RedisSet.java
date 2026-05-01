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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

class RedisSet<T> extends RedisCollection<T> implements Set<T> {

    RedisSet(UnifiedJedis jedis, Class<T> clazz, String keyWithNameSpace) {
        super(jedis, clazz, keyWithNameSpace);
    }

    @Override
    public boolean add(T e) {
        Objects.requireNonNull(e);
        if (isString) {
            jedis.sadd(keyWithNameSpace, e.toString());
        } else {
            jedis.sadd(keyWithNameSpace, JSONB.toJson(e));
        }
        return true;
    }

    @Override
    public void clear() {
        jedis.del(keyWithNameSpace);
    }

    @Override
    public int size() {
        return (int) jedis.scard(keyWithNameSpace);
    }

    @Override
    protected int indexOf(Object o) {
        Objects.requireNonNull(o);

        String find = serialize(o);
        Set<String> values = jedis.smembers(keyWithNameSpace);
        int index = 0;
        for (String value : values) {
            if (value.contains(find)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    @Override
    protected T remove(int index) {
        throw new UnsupportedOperationException("Remove with index is not supported on Redis Set");
    }

    @Override
    public boolean remove(Object o) {
        if (!clazz.isInstance(o)) {
            throw new ClassCastException("The object required is " + clazz.getName());
        }
        String find = serialize(o);
        Set<String> values = jedis.smembers(keyWithNameSpace);
        for (String value : values) {
            if (value.contains(find)) {
                jedis.srem(keyWithNameSpace, value);
                return true;
            }
        }
        return false;
    }

    @Override
    protected List<T> toArrayList() {
        Set<String> redisValues = jedis.smembers(keyWithNameSpace);
        List<T> list = new ArrayList<>();
        for (String redisValue : redisValues) {
            if (isString) {
                list.add((T) redisValue);
            } else {
                list.add(JSONB.fromJson(redisValue, clazz));
            }

        }
        return list;
    }

}

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

import jakarta.json.bind.Jsonb;
import org.eclipse.jnosql.communication.driver.JsonbSupplier;
import redis.clients.jedis.UnifiedJedis;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

class RedisMap<K, V> implements Map<K, V> {


    protected static final Jsonb JSONB = JsonbSupplier.getInstance().get();

    private final Class<K> keyClass;

    private final Class<V> valueClass;

    private final String nameSpace;

    private final UnifiedJedis jedis;

    private final boolean isKeyString;

    private final boolean isValueString;

    RedisMap(UnifiedJedis jedis, Class<K> keyValue, Class<V> valueClass, String keyWithNameSpace) {
        this.keyClass = keyValue;
        this.valueClass = valueClass;
        this.nameSpace = keyWithNameSpace;
        this.jedis = jedis;
        this.isKeyString = String.class.equals(keyClass);
        this.isValueString = String.class.equals(valueClass);
    }

    @Override
    public int size() {
        return jedis.hgetAll(nameSpace).size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        requireNonNull(key, "key is required");
        if (isKeyString) {
            return jedis.hexists(nameSpace, key.toString());
        } else {
            return jedis.hexists(nameSpace, JSONB.toJson(key));
        }
    }

    @Override
    public boolean containsValue(Object value) {
        requireNonNull(value);
        String valueString;
        if (isValueString) {
            valueString = value.toString();
        } else {
            valueString = JSONB.toJson(value);
        }

        Map<String, String> map = createRedisMap();
        return map.containsValue(valueString);
    }

    @Override
    public V get(Object key) {
        requireNonNull(key, "Key is required");

        String value = jedis.hget(nameSpace, JSONB.toJson(key));
        if (isKeyString) {
            value = jedis.hget(nameSpace, key.toString());
        } else {
            value = jedis.hget(nameSpace, JSONB.toJson(key));
        }
        if (value != null && !value.isEmpty()) {
            if (isValueString) {
                return (V) value;
            } else {
                return JSONB.fromJson(value, valueClass);
            }

        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        requireNonNull(value, "Value is required");
        requireNonNull(value, "Key is required");

        String keyJson;
        if(isKeyString) {
            keyJson = key.toString();
        } else {
             keyJson = JSONB.toJson(key);
        }
        String valueJSON;

        if(isValueString) {
            valueJSON = value.toString();
        } else {
            valueJSON = JSONB.toJson(value);
        }
        jedis.hset(nameSpace, keyJson, valueJSON);
        return value;
    }

    @Override
    public V remove(Object key) {
        requireNonNull(key, "Key is required");
        V value = get(key);
        if (value != null) {
            if (isKeyString) {
                jedis.hdel(nameSpace, key.toString());
            } else {
                jedis.hdel(nameSpace, JSONB.toJson(key));
            }

            return value;
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        requireNonNull(map, "map is required");

        for (K key : map.keySet()) {
            V value = map.get(key);
            if (value != null) {
                put(key, value);
            }
        }
    }

    @Override
    public void clear() {
        jedis.del(nameSpace);
    }

    @Override
    public Set<K> keySet() {
        return createHashMap().keySet();
    }

    @Override
    public Collection<V> values() {
        return createHashMap().values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return createHashMap().entrySet();
    }

    private Map<String, String> createRedisMap() {
        return jedis.hgetAll(nameSpace);
    }

    private Map<K, V> createHashMap() {
        Map<K, V> values = new HashMap<>();
        Map<String, String> redisMap = createRedisMap();
        final Function<String, K> keyFunction = k -> {
            if(isKeyString) {
                return (K) k;
            } else {
                return JSONB.fromJson(k, keyClass);
            }
        };
        final Function<String, V> valueFunction = k -> {
            if(isValueString) {
                return (V) redisMap.get(k);
            } else {
                return JSONB.fromJson(redisMap.get(k), valueClass);
            }
        };
        return redisMap.keySet().stream().collect(Collectors
                .toMap(keyFunction, valueFunction));
    }


    @Override
    public String toString() {
        return "RedisMap{" + "keyClass=" + keyClass +
                ", valueClass=" + valueClass +
                ", nameSpace='" + nameSpace + '\'' +
                ", jedis=" + jedis +
                ", JsonB=" + JSONB +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nameSpace);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (RedisMap.class.isInstance(obj)) {
            RedisMap otherRedis = RedisMap.class.cast(obj);
            return Objects.equals(otherRedis.nameSpace, nameSpace);
        }
        return false;
    }

}

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

import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.impl.predicates.SqlPredicate;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.keyvalue.KeyValueEntity;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * The default implementation of hazelcast bucket manager
 */
class DefaultHazelcastBucketManager implements HazelcastBucketManager {

    private final IMap map;

    private final String bucket;
    DefaultHazelcastBucketManager(IMap map, String bucket) {
        this.map = map;
        this.bucket = bucket;
    }

    @Override
    public String name() {
        return bucket;
    }

    @Override
    public <K, V> void put(K key, V value) {
        map.put(key, value);
    }

    @Override
    public void put(KeyValueEntity entity) throws NullPointerException {
        map.put(entity.key(), entity.value());
    }

    @Override
    public void put(KeyValueEntity entity, Duration ttl) {
        map.put(entity.key(), entity.value(), ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void put(Iterable<KeyValueEntity> entities) throws NullPointerException {
        StreamSupport.stream(entities.spliterator(), false).forEach(this::put);
    }

    @Override
    public void put(Iterable<KeyValueEntity> entities, Duration ttl) throws NullPointerException, UnsupportedOperationException {
        StreamSupport.stream(entities.spliterator(), false).forEach(kv -> this.put(kv, ttl));
    }

    @Override
    public <K> Optional<Value> get(K key) throws NullPointerException {
        Object value = map.get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(Value.of(value));
    }

    @Override
    public <K> Iterable<Value> get(Iterable<K> keys) throws NullPointerException {
        return StreamSupport.stream(keys.spliterator(), false).map((Function<K, Object>) map::get).filter(Objects::nonNull)
                .map(Value::of).collect(toList());
    }

    @Override
    public <K> void delete(K key) {
        map.remove(key);
    }

    @Override
    public <K> void delete(Iterable<K> keys) {
        StreamSupport.stream(keys.spliterator(), false).forEach(this::delete);
    }

    @Override
    public void close() {
    }

    @Override
    public Collection<Value> sql(String query) throws NullPointerException {
        requireNonNull(query, "sql is required");
        return sql(new SqlPredicate(query));
    }

    @Override
    public Collection<Value> sql(String query, Map<String, Object> params) throws NullPointerException {
        requireNonNull(query, "sql is required");
        requireNonNull(params, "params is required");
        return sql(new SqlPredicate(bind(query, params)));
    }

    @Override
    public <K, V> Collection<Value> sql(Predicate<K, V> predicate) throws NullPointerException {
        requireNonNull(predicate, "predicate is required");
        Collection<V> values = map.values(predicate);
        return values.stream().map(Value::of).collect(toList());
    }

    static String bind(String query, Map<String, Object> params) {
        StringBuilder bound = new StringBuilder();
        Set<String> used = new HashSet<>();
        boolean quoted = false;
        for (int index = 0; index < query.length(); index++) {
            char character = query.charAt(index);
            if (character == '\'') {
                bound.append(character);
                if (quoted && index + 1 < query.length() && query.charAt(index + 1) == '\'') {
                    bound.append(query.charAt(++index));
                } else {
                    quoted = !quoted;
                }
                continue;
            }
            if (!quoted && character == ':' && index + 1 < query.length()
                    && isParameterStart(query.charAt(index + 1))) {
                int end = index + 2;
                while (end < query.length() && isParameterPart(query.charAt(end))) {
                    end++;
                }
                String name = query.substring(index + 1, end);
                if (!params.containsKey(name)) {
                    throw new IllegalArgumentException("Missing Hazelcast query parameter: " + name);
                }
                used.add(name);
                bound.append(literal(params.get(name)));
                index = end - 1;
                continue;
            }
            bound.append(character);
        }
        if (!used.containsAll(params.keySet())) {
            Set<String> unused = new HashSet<>(params.keySet());
            unused.removeAll(used);
            throw new IllegalArgumentException("Unused Hazelcast query parameters: " + unused);
        }
        return bound.toString();
    }

    private static boolean isParameterStart(char character) {
        return Character.isLetter(character) || character == '_';
    }

    private static boolean isParameterPart(char character) {
        return Character.isLetterOrDigit(character) || character == '_';
    }

    private static String literal(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return "'" + value.toString().replace("'", "''") + "'";
    }
}

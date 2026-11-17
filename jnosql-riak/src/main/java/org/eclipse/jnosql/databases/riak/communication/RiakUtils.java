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
package org.eclipse.jnosql.databases.riak.communication;

import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.api.commands.kv.StoreValue.Builder;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;

import java.time.Duration;
import java.util.Objects;

/**
 * Helpers that build Riak client commands for a {@link Namespace}. Keys are converted with {@code toString()},
 * so a {@link java.util.UUID} key is stored as its canonical text form.
 */
public final class RiakUtils {

    private RiakUtils() {
    }

/**
 * Creates a command that stores a value under the key.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @param key the key
 * @param value the value to store
 * @param namespace the bucket namespace
 * @param ttl the expiration in whole seconds; a zero duration means no expiration
 * @return the store command
 * @throws NullPointerException when key, value or namespace is null, or when ttl is null
 * @throws ArithmeticException when the ttl in seconds does not fit an {@code int}
 */
    public static <K, V> StoreValue createStoreValue(K key, V value, Namespace namespace, Duration ttl) {

        Objects.requireNonNull(value, "Value is required");
        Objects.requireNonNull(key, "key is required");

        Location location = createLocation(namespace, key);
        Builder builder = new StoreValue.Builder(value).withLocation(location);

        if (!ttl.isZero()) {
            builder = builder.withTimeout(Math.toIntExact(ttl.getSeconds()));
        }


        return builder.build();
    }

/**
 * Creates the Riak location of a key.
 *
 * @param <K> the key type
 * @param namespace the bucket namespace
 * @param key the key, converted with {@code toString()}
 * @return the location
 * @throws NullPointerException when namespace or key is null
 */
    public static <K> Location createLocation(Namespace namespace, K key) {

        Objects.requireNonNull(namespace, "Namespace is required");
        Objects.requireNonNull(key, "key is required");

        return new Location(namespace, key.toString());
    }

/**
 * Creates a command that fetches the value of a key.
 *
 * @param <K> the key type
 * @param namespace the bucket namespace
 * @param key the key, converted with {@code toString()}
 * @return the fetch command
 * @throws NullPointerException when namespace or key is null
 */
    public static <K> FetchValue createFetchValue(Namespace namespace, K key) {

        Location location = createLocation(namespace, key);
        return new FetchValue.Builder(location).build();
    }

/**
 * Creates a command that deletes a key.
 *
 * @param <K> the key type
 * @param namespace the bucket namespace
 * @param key the key, converted with {@code toString()}
 * @return the delete command
 * @throws NullPointerException when namespace or key is null
 */
    public static <K> DeleteValue createDeleteValue(Namespace namespace, K key) {

        Location location = createLocation(namespace, key);
        return new DeleteValue.Builder(location).build();
    }


}

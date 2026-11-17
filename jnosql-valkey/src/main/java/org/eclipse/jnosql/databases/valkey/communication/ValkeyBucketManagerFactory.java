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

import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;

/**
 * The Valkey implementation of {@link BucketManagerFactory}. Besides {@link ValkeyBucketManager}, it exposes
 * Valkey data structures: lists, sets, queues and maps through the inherited methods, and {@link SortedSet} and
 * {@link Counter} through the methods declared here.
 *
 * <p>The factory owns the Valkey connection pool. Closing it with {@link #close()} releases the connections
 * used by every manager and structure it created.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * ValkeyBucketManagerFactory factory = new ValkeyConfiguration().apply(Settings.settings());
 * BucketManager users = factory.apply("users");
 * Counter visits = factory.getCounter("visits");
 * visits.increment();
 * }</pre>
 *
 * @see ValkeyConfiguration
 */
public interface ValkeyBucketManagerFactory extends BucketManagerFactory {


    /**
     * Returns the {@link SortedSet} stored at a Valkey key. The sorted set is created in Valkey on its first write.
     *
     * <p>Example:</p>
     * <pre>{@code
     * SortedSet ranking = factory.getSortedSet("game:ranking");
     * }</pre>
     *
     * @param key the Valkey key of the sorted set
     * @return a view of the sorted set stored at {@code key}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    SortedSet getSortedSet(String key) throws NullPointerException;
    /**
     * Returns the {@link Counter} stored at a Valkey key.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Counter visits = factory.getCounter("visits");
     * Number total = visits.increment();
     * }</pre>
     *
     * @param key the Valkey key of the counter
     * @return a view of the counter stored at {@code key}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    Counter getCounter(String key) throws NullPointerException;

}

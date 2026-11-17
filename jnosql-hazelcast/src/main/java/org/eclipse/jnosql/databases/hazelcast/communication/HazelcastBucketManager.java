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

import com.hazelcast.query.Predicate;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.keyvalue.BucketManager;

import java.util.Collection;
import java.util.Map;

/**
 * The Hazelcast implementation of {@link BucketManager}. Besides the key-value operations, it can query the
 * values of the bucket with Hazelcast predicates.
 *
 * <p>Example:</p>
 * <pre>{@code
 * HazelcastBucketManager bucket = factory.apply("movies");
 * Collection<Value> matches = bucket.sql("name = :name", Map.of("name", "Matrix"));
 * }</pre>
 *
 * @see HazelcastBucketManagerFactory
 */
public interface HazelcastBucketManager extends BucketManager {


    /**
     * Returns the values of the bucket that match a Hazelcast SQL predicate expression, such as
     * {@code "name = 'Matrix'"}. The expression is a predicate on the stored values, not a full {@code SELECT}.
     *
     * @param query the predicate expression
     * @return the matching values, empty when none matches
     * @throws NullPointerException if {@code query} is {@code null}
     */
    Collection<Value> sql(String query);

    /**
     * Returns the values of the bucket that match a predicate expression with named parameters, such as
     * {@code "name = :name"}. Parameter values are inserted as literals; a parameter used in the expression
     * but absent from the map is rejected.
     *
     * @param query  the predicate expression
     * @param params the values to bind, by parameter name
     * @return the matching values, empty when none matches
     * @throws NullPointerException     if {@code query} is {@code null}
     * @throws IllegalArgumentException if the expression uses a parameter that is not in {@code params}
     */
    Collection<Value> sql(String query, Map<String, Object> params);

    /**
     * Returns the values of the bucket that match a Hazelcast {@link Predicate}.
     *
     * @param predicate the predicate to apply to the entries
     * @param <K>       the key type
     * @param <V>       the value type
     * @return the matching values, empty when none matches
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
    <K, V> Collection<Value> sql(Predicate<K, V> predicate);

}

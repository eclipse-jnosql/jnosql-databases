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


import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * The hazelcast implementation of {@link BucketManagerFactory}
 */
public interface HazelcastBucketManagerFactory extends BucketManagerFactory {

    /**
     * Return the {@link HazelcastBucketManager} instance
     * @param bucket the function argument
     * @return the {@link HazelcastBucketManager} instance
     * @throws NullPointerException when the bucket is null
     */
    @Override
    HazelcastBucketManager apply(String bucket);
    /**
     * Creates a {@link List} from bucket name
     *
     * @param bucketName a bucket name
     * @param <T>        the value type
     * @return a {@link List} instance
     * @throws UnsupportedOperationException when the database does not have to it
     * @throws NullPointerException          when the bucke name is null
     */
    <T> List<T> getList(String bucketName) throws UnsupportedOperationException, NullPointerException;

    /**
     * Creates a {@link Set} from bucket name
     *
     * @param bucketName a bucket name
     * @param <T>        the value type
     * @return a {@link Set} instance
     * @throws UnsupportedOperationException when the database does not have to it
     * @throws NullPointerException          when the bucket name is null
     */
    <T> Set<T> getSet(String bucketName) throws UnsupportedOperationException, NullPointerException;

    /**
     * Creates a {@link Queue} from bucket name
     *
     * @param bucketName a bucket name
     * @param <T>        the value type
     * @return a {@link Queue} instance
     * @throws UnsupportedOperationException when the database does not have to it
     * @throws NullPointerException          when the bucket name is null
     */
    <T> Queue<T> getQueue(String bucketName) throws UnsupportedOperationException, NullPointerException;

    /**
     * Creates a {@link  Map} from bucket name
     *
     * @param bucketName the bucket name
     * @param <K>        the key type
     * @param <V>        the value type
     * @return a {@link Map} instance
     * @throws UnsupportedOperationException when the database does not have to it
     * @throws NullPointerException          when the bucket name is null
     */
    <K, V> Map<K, V> getMap(String bucketName) throws
            UnsupportedOperationException, NullPointerException;
}

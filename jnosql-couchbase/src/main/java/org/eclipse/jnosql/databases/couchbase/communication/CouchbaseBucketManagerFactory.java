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
package org.eclipse.jnosql.databases.couchbase.communication;

import org.eclipse.jnosql.communication.keyvalue.BucketManager;
import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * A Couchbase specialization of BucketManagerFactory.
 */
public interface CouchbaseBucketManagerFactory extends BucketManagerFactory {


    /**
     * Create a BucketManager using a given collection instead of the default
     *
     * @param bucketName the bucket name
     * @param collection the collection name
     * @return a BucketManager instance
     * @throws NullPointerException when there is null parameter
     */
    BucketManager getBucketManager(String bucketName, String collection);

    /**
     * Creates a {@link Queue} from bucket name
     *
     * @param bucketName a bucket name
     * @param type       the value class
     * @param key        key to the queue
     * @param <T>        the value type
     * @return a {@link Queue} instance
     * @throws UnsupportedOperationException when the database does not have to it
     * @throws NullPointerException          when either bucketName or class are null
     */
    <T> Queue<T> getQueue(String bucketName, String key, Class<T> type);

    /**
     * Creates a {@link Set} from bucket name
     *
     * @param bucketName a bucket name
     * @param type       the valeu class
     * @param key        key to the set
     * @param <T>        the value type
     * @return a {@link Set} instance
     * @throws UnsupportedOperationException when the database does not have to it
     * @throws NullPointerException          when either bucketName or class are null
     */
    <T> Set<T> getSet(String bucketName, String key, Class<T> type);


    /**
     * Creates a {@link List} from bucket name
     *
     * @param bucketName a bucket name
     * @param type       the valeu class
     * @param key        key to the List
     * @param <T>        the value type
     * @return a {@link List} instance
     * @throws UnsupportedOperationException when the database does not have to it
     * @throws NullPointerException          when either bucketName or class are null
     */
    <T> List<T> getList(String bucketName, String key, Class<T> type);


    /**
     * Creates a {@link  Map} from bucket name
     *
     * @param bucketName the bucket name
     * @param key        key to the Map
     * @param keyType    the key class
     * @param valueType  the value class
     * @param <K>        the key type
     * @param <V>        the value type
     * @return a {@link Map} instance
     * @throws UnsupportedOperationException when the database does not have to it
     * @throws NullPointerException          when either bucketName or class are null
     */
    <K, V> Map<K, V> getMap(String bucketName, String key, Class<K> keyType, Class<V> valueType);
}

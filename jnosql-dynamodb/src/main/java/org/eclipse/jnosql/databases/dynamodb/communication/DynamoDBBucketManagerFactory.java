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
package org.eclipse.jnosql.databases.dynamodb.communication;

import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * The DynamoDB implementation of {@link BucketManagerFactory}. Each bucket is a DynamoDB table with a string
 * {@code key} and a string {@code value} attribute that holds the value as JSON.
 *
 * <p>Only {@code apply(bucketName)} and {@link #getBucketManager(String, Long, Long)} are supported to obtain a
 * manager. {@code getList}, {@code getSet}, {@code getQueue} and {@code getMap} throw
 * {@link UnsupportedOperationException}. Closing the factory closes the DynamoDB client.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * DynamoDBBucketManagerFactory factory = new DynamoDBKeyValueConfiguration().apply(settings);
 * DynamoDBBucketManager users = factory.getBucketManager("users", 10L, 5L);
 * }</pre>
 *
 * @see DynamoDBKeyValueConfiguration
 */
public class DynamoDBBucketManagerFactory implements BucketManagerFactory {

    private DynamoDbClient client;

    DynamoDBBucketManagerFactory(DynamoDbClient client) {
        this.client = client;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The table is created with 5 read and 5 write capacity units when it does not exist.</p>
     *
     * @see #getBucketManager(String, Long, Long)
     */
    @Override
    public DynamoDBBucketManager apply(String bucketName) {

        return getBucketManager(bucketName, null, null);
    }

/**
 * Returns the manager of a table, creating the table first when it does not exist.
 *
 * @param bucketName the table name
 * @param readCapacityUnits the read capacity units of a new table; the default of 5 is used when {@code null}
 *                          or not positive
 * @param writeCapacityUnit the write capacity units of a new table; the default of 5 is used when {@code null}
 *                          or not positive
 * @return the manager of the table
 * @throws RuntimeException if DynamoDB rejects the table listing
 */
    public DynamoDBBucketManager getBucketManager(String bucketName, Long readCapacityUnits, Long writeCapacityUnit) {

        DynamoTableUtils.manageTables(bucketName, client, readCapacityUnits, writeCapacityUnit);
        return new DynamoDBBucketManager(client, bucketName);
    }

    @Override
    public <T> List<T> getList(String bucketName, Class<T> clazz) {
        throw new UnsupportedOperationException("The DynamoDB does not support getMap method");
    }

    @Override
    public <T> Set<T> getSet(String bucketName, Class<T> clazz) {
        throw new UnsupportedOperationException("The DynamoDB does not support getMap method");
    }

    @Override
    public <T> Queue<T> getQueue(String bucketName, Class<T> clazz) {
        throw new UnsupportedOperationException("The DynamoDB does not support getMap method");
    }

    @Override
    public <K, V> Map<K, V> getMap(String bucketName, Class<K> keyValue, Class<V> valueValue) {
        throw new UnsupportedOperationException("The DynamoDB does not support getMap method");
    }

    @Override
    public void close() {
        client.close();
    }
}

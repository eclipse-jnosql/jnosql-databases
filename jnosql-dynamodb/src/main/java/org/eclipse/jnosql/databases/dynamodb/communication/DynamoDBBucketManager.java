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
 *   Alessandro Moscatelli
 */
package org.eclipse.jnosql.databases.dynamodb.communication;

import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.driver.ValueJSON;
import org.eclipse.jnosql.communication.keyvalue.BucketManager;
import org.eclipse.jnosql.communication.keyvalue.KeyValueEntity;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provides dynamo dbbucket manager support.
 */
public class DynamoDBBucketManager implements BucketManager {


    private DynamoDbClient client;
    private String tableName;
    private static final Function<AttributeValue, String> TO_JSON = AttributeValue::s;

/**
 * Returns the dynamo dbbucket manager.
 *
 * @param client the client
 * @param tableName the table name
 */
    public DynamoDBBucketManager(DynamoDbClient client, String tableName) {
        this.client = client;
        this.tableName = tableName;
    }

    @Override
    public String name() {
        return tableName;
    }

    @Override
    public <K, V> void put(K key, V value) throws NullPointerException {
        client.putItem(PutItemRequest.builder().tableName(tableName).item(DynamoDBUtils.createAttributeValues(key, value)).build());
    }

    @Override
    public void put(KeyValueEntity entity) throws NullPointerException {
        put(entity.key(), entity.value());
    }

    @Override
    public void put(KeyValueEntity entity, Duration ttl)
            throws NullPointerException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void put(Iterable<KeyValueEntity> entities) throws NullPointerException {
        client.batchWriteItem(BatchWriteItemRequest.builder().requestItems(DynamoDBUtils.createMapWriteRequest(entities, tableName)).build());
    }

    @Override
    public  void put(Iterable<KeyValueEntity> entities, Duration ttl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K> Optional<Value> get(K key) throws NullPointerException {

        Objects.requireNonNull(key, "key is required");

        if (key.toString().isEmpty()) {
            throw new IllegalArgumentException("The Key is irregular");
        }

        GetItemResponse getItemResponse = client.getItem(DynamoDBUtils.createGetItemRequest(key, tableName));
        Map<String, AttributeValue> item = getItemResponse.item();
        AttributeValue attributeValue = item.get(ConfigurationAmazonEntity.VALUE);

        return Optional.ofNullable(attributeValue)
                .map(TO_JSON)
                .map(ValueJSON::of);
    }

    @Override
    public <K> Iterable<Value> get(Iterable<K> keys) throws NullPointerException {
        
        return client.batchGetItem(DynamoDBUtils.createBatchGetItemRequest(keys, tableName))
                .responses()
                .values()
                .stream()
                .flatMap(Collection::stream)
                .map(v -> v.get(ConfigurationAmazonEntity.VALUE))
                .map(TO_JSON)
                .map(ValueJSON::of)
                .collect(Collectors.toList());
    }

    @Override
    public <K> void delete(K key) throws NullPointerException {
        client.deleteItem(DeleteItemRequest.builder().tableName(tableName).key(DynamoDBUtils.createKeyAttributeValues(key)).build());
    }

    @Override
    public <K> void delete(Iterable<K> keys) throws NullPointerException {
        keys.forEach(this::delete);
    }

    @Override
    public void close() {
        client.close();
    }
}

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
 *   Alessandro Moscatelli
 */
package org.eclipse.jnosql.databases.dynamodb.communication;

import jakarta.json.bind.Jsonb;
import org.eclipse.jnosql.communication.driver.JsonbSupplier;
import org.eclipse.jnosql.communication.keyvalue.KeyValueEntity;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.eclipse.jnosql.databases.dynamodb.communication.ConfigurationAmazonEntity.KEY;
import static org.eclipse.jnosql.databases.dynamodb.communication.ConfigurationAmazonEntity.VALUE;

public class DynamoDBUtils {

    private static final AttributeValue.Builder attributeValueBuilder = AttributeValue.builder();
    private static final Jsonb JSONB = JsonbSupplier.getInstance().get();

    private DynamoDBUtils() {
    }

    public static <K, V> Map<String, AttributeValue> createAttributeValues(K key, V value) {

        Map<String, AttributeValue> createAttributeValues = createKeyAttributeValues(key);
        String valueAsJson = JSONB.toJson(value);

        AttributeValue valueAttributeValue = attributeValueBuilder.s(valueAsJson).build();
        createAttributeValues.put(VALUE, valueAttributeValue);
        return createAttributeValues;
    }

    public static <K, V> Map<String, AttributeValue> createKeyAttributeValues(K key) {
        Map<String, AttributeValue> map = new HashMap<>();
        AttributeValue keyAttributeValue = attributeValueBuilder.s(key.toString()).build();
        map.put(KEY, keyAttributeValue);

        return map;
    }

    public static <K, V> Collection<Map<String, AttributeValue>> createKeyAttributeValues(Iterable<K> keys) {
        return StreamSupport.stream(keys.spliterator(), false).map(
                k -> Collections.singletonMap(KEY, attributeValueBuilder.s(k.toString()).build())
        ).collect(Collectors.toList());
    }

    public static <K, V> Map<String, AttributeValue> createAttributeValues(KeyValueEntity entity) {
        return createAttributeValues(entity.key(), entity.value());
    }

    public static <K> Collection<Map<String, AttributeValue>> createAttributeValues(Iterable<KeyValueEntity> entities) {

        return StreamSupport.stream(entities.spliterator(), false)
                .map(DynamoDBUtils::createAttributeValues)
                .collect(Collectors.toList());
    }

    private static Map<String, List<WriteRequest>> createMapWriteRequest(Collection<Map<String, AttributeValue>> map, String tableName) {

        PutRequest.Builder putRequestBuilder = PutRequest.builder();
        WriteRequest.Builder writeRequestBuilder = WriteRequest.builder();

        return Collections.singletonMap(
                tableName,
                map
                        .stream()
                        .map(m -> putRequestBuilder.item(m).build())
                        .map(p -> writeRequestBuilder.putRequest(p).build()).collect(Collectors.toList())
        );
    }

    public static <K> Map<String, List<WriteRequest>> createMapWriteRequest(Iterable<KeyValueEntity> entities, String tableName) {
        Collection<Map<String, AttributeValue>> attributeValues = createAttributeValues(entities);
        createMapWriteRequest(attributeValues, tableName);
        return createMapWriteRequest(attributeValues, tableName);
    }

    public static <K> Map<String, AttributeValue> create(Iterable<K> keys) {

        Map<String, AttributeValue> map = StreamSupport.stream(keys.spliterator(), false)
                .map(Object::toString)
                .collect(Collectors.toMap(Function.identity(), k -> attributeValueBuilder.s(k).build()));

        return Collections.unmodifiableMap(map);
    }

    private static <K> Map<String, KeysAndAttributes> createKeysAndAttribute(Iterable<K> keys, String tableName) {

        KeysAndAttributes.Builder keysAndAttributesBuilder = KeysAndAttributes.builder();

        return Collections.singletonMap(
                tableName,
                keysAndAttributesBuilder.keys(createKeyAttributeValues(keys)).build()
        );
    }

    public static <K> BatchGetItemRequest createBatchGetItemRequest(Iterable<K> keys, String tableName) {
        BatchGetItemRequest.Builder batchGetItemRequestBuilder = BatchGetItemRequest.builder();
        return batchGetItemRequestBuilder.requestItems(createKeysAndAttribute(keys, tableName)).build();
    }

    public static <K> GetItemRequest createGetItemRequest(K key, String tableName) {
        GetItemRequest.Builder getItemRequest = GetItemRequest.builder();
        return getItemRequest.tableName(tableName).key(createKeyAttributeValues(key)).build();
    }

    public static String replaceInvalidCharactersForKey(String attributeName) {
        if (attributeName == null || attributeName.isEmpty())
            return attributeName;
        StringBuilder sb = new StringBuilder();
        for (char c : attributeName.trim().toCharArray()) {
            if (isEnglishLetterOrDigit(c) || c == '_') {
                sb.append(c);
            } else {
                sb.append("_").append((int) c);
            }
        }
        return sb.toString();
    }

    private static boolean isEnglishLetterOrDigit(char c) {
        return (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9');
    }
}

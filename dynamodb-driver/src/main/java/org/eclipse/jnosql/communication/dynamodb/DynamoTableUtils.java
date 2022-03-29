/*
 *   Copyright (c) 2022 Otávio Santana and others
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
package org.eclipse.jnosql.communication.dynamodb;

import java.util.ArrayList;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DynamoTableUtils {

    private static final long READ_CAPACITY_UNITS = 5L;

    private DynamoTableUtils() {
    }

    public static KeySchemaElement createKeyElementSchema(Map<String, KeyType> keys) {

        KeySchemaElement.Builder keySchemaElementBuilder = KeySchemaElement.builder();

        keys
                .entrySet()
                .forEach(
                        es -> {
                            keySchemaElementBuilder.attributeName(es.getKey());
                            keySchemaElementBuilder.keyType(es.getValue());
                        });

        return keySchemaElementBuilder.build();
    }

    public static AttributeDefinition[] createAttributeDefinition(Map<String, ScalarAttributeType> attributes) {
        return attributes
                .entrySet()
                .stream()
                .map(
                        es -> {
                            AttributeDefinition.Builder attributeDefinitionBuilder = AttributeDefinition.builder();
                            attributeDefinitionBuilder.attributeName(es.getKey());
                            attributeDefinitionBuilder.attributeType(es.getValue());
                            return attributeDefinitionBuilder.build();
                        }
                ).toArray(
                        AttributeDefinition[]::new
                );
    }

    public static ProvisionedThroughput createProvisionedThroughput(Long readCapacityUnits, Long writeCapacityUnit) {

        ProvisionedThroughput.Builder provisionedThroughputBuilder = ProvisionedThroughput.builder();

        if (readCapacityUnits != null && readCapacityUnits.longValue() > 0) {
            provisionedThroughputBuilder.readCapacityUnits(readCapacityUnits);
        } else {
            provisionedThroughputBuilder.readCapacityUnits(READ_CAPACITY_UNITS);
        }

        if (writeCapacityUnit != null && writeCapacityUnit.longValue() > 0) {
            provisionedThroughputBuilder.writeCapacityUnits(writeCapacityUnit);
        } else {
            provisionedThroughputBuilder.writeCapacityUnits(READ_CAPACITY_UNITS);
        }

        return provisionedThroughputBuilder.build();
    }

    public static Map<String, KeyType> createKeyDefinition() {
        return Collections.singletonMap(ConfigurationAmazonEntity.KEY, KeyType.HASH);
    }

    public static Map<String, ScalarAttributeType> createAttributesType() {
        return Collections.singletonMap(ConfigurationAmazonEntity.KEY, ScalarAttributeType.S);
    }

    public static boolean existTable(String tableName, DynamoDbClient client) {
        String lastName = null;
        boolean hasTable = false;

        while (!hasTable) {
            try {
                ListTablesRequest.Builder builder = ListTablesRequest.builder();
                if (Objects.nonNull(lastName)) {
                    builder.exclusiveStartTableName(lastName);
                }
                ListTablesRequest request = builder.build();
                ListTablesResponse response = client.listTables(request);

                List<String> tableNames = new ArrayList(response.tableNames());

                if (tableNames.isEmpty()) {
                    break;
                } else {
                    if (tableNames.contains(tableName)) {
                        hasTable = true;
                        break;
                    }
                    Collections.reverse(tableNames);
                    lastName = tableNames.stream().findFirst().get();
                }
            } catch (DynamoDbException e) {
                throw new RuntimeException(e);
            }
        }

        return hasTable;
    }

    public static void manageTables(String tableName, DynamoDbClient client, Long readCapacityUnits, Long writeCapacityUnit) {
        if (!existTable(tableName, client)) {
            createTable(tableName, client, readCapacityUnits, writeCapacityUnit);
        }
    }

    private static void createTable(String tableName, DynamoDbClient client, Long readCapacityUnits, Long writeCapacityUnit) {

        Map<String, KeyType> keyDefinition = createKeyDefinition();
        Map<String, ScalarAttributeType> attributeDefinition = createAttributesType();

        client.createTable(CreateTableRequest.builder()
                .tableName(tableName)
                .provisionedThroughput(createProvisionedThroughput(readCapacityUnits, writeCapacityUnit))
                .keySchema(createKeyElementSchema(keyDefinition))
                .attributeDefinitions(createAttributeDefinition(attributeDefinition))
                .build());

        client.waiter().waitUntilTableExists(t -> t.tableName(tableName));

    }
}

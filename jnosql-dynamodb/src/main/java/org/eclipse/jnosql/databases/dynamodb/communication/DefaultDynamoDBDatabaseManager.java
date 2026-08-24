/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 * and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *
 * Maximillian Arruda
 */

package org.eclipse.jnosql.databases.dynamodb.communication;

import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTimeToLiveRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTimeToLiveResponse;
import software.amazon.awssdk.services.dynamodb.model.ExecuteStatementRequest;
import software.amazon.awssdk.services.dynamodb.model.ExecuteStatementResponse;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.Select;
import software.amazon.awssdk.services.dynamodb.model.TimeToLiveStatus;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;
import static org.eclipse.jnosql.databases.dynamodb.communication.DynamoDBConverter.entityAttributeName;
import static org.eclipse.jnosql.databases.dynamodb.communication.DynamoDBConverter.toAttributeValue;
import static org.eclipse.jnosql.databases.dynamodb.communication.DynamoDBConverter.toCommunicationEntity;
import static org.eclipse.jnosql.databases.dynamodb.communication.DynamoDBConverter.toItem;
import static org.eclipse.jnosql.databases.dynamodb.communication.DynamoDBConverter.toItemUpdate;

public class DefaultDynamoDBDatabaseManager implements DynamoDBDatabaseManager {

    private final String database;

    private final Settings settings;

    private final DynamoDbClient dynamoDbClient;

    private final ConcurrentHashMap<String, Supplier<String>> ttlAttributeNamesByTable = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, DescribeTableResponse> tables = new ConcurrentHashMap<>();

    public DefaultDynamoDBDatabaseManager(String database, DynamoDbClient dynamoDbClient, Settings settings) {
        this.settings = settings;
        this.database = database;
        this.dynamoDbClient = dynamoDbClient;
    }

    private String resolveEntityNameAttributeName(String entityName) {
        return this.settings.get(DynamoDBConfigurations.ENTITY_PARTITION_KEY, String.class).orElse(entityName);
    }

    public DynamoDbClient dynamoDbClient() {
        return dynamoDbClient;
    }

    @Override
    public String name() {
        return database;
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity documentEntity) {
        requireNonNull(documentEntity, "documentEntity is required");
        dynamoDbClient().putItem(PutItemRequest.builder()
                .tableName(createTableIfNeeded(documentEntity.name()).table().tableName())
                .item(toItem(this::resolveEntityNameAttributeName, documentEntity))
                .build());
        return documentEntity;
    }

    private Supplier<String> getTTLAttributeName(String tableName) {
        return this.ttlAttributeNamesByTable.computeIfAbsent(tableName, this::getTTLAttributeNameSupplier);
    }

    private Supplier<String> getTTLAttributeNameSupplier(String tableName) {
        createTableIfNeeded(tableName);
        DescribeTimeToLiveResponse describeTimeToLiveResponse = dynamoDbClient().describeTimeToLive(DescribeTimeToLiveRequest.builder()
                .tableName(tableName).build());
        if (TimeToLiveStatus.ENABLED.equals(describeTimeToLiveResponse.timeToLiveDescription().timeToLiveStatus())) {
            var ttlAttributeName = describeTimeToLiveResponse.timeToLiveDescription().attributeName();
            return () -> ttlAttributeName;
        }
        return () -> tableName + " don't support TTL operations. Check if TTL support is enabled for this table.";
    }

    private DescribeTableResponse createTableIfNeeded(String tableName) {
        return this.tables.computeIfAbsent(tableName, this::resolveTable);
    }

    private DescribeTableResponse resolveTable(String tableName) {
        try {
            return getDescribeTableResponse(tableName);
        } catch (ResourceNotFoundException ex) {
            if (!shouldCreateTables())
                throw ex;
            return createTable(tableName);
        }
    }

    private DescribeTableResponse getDescribeTableResponse(String tableName) {
        return dynamoDbClient().describeTable(DescribeTableRequest.builder()
                .tableName(tableName)
                .build());
    }

    private DescribeTableResponse createTable(String tableName) {
        try (var waiter = dynamoDbClient().waiter()) {
            dynamoDbClient().createTable(CreateTableRequest.builder()
                    .tableName(tableName)
                    .keySchema(defaultKeySchemaFor())
                    .attributeDefinitions(defaultAttributeDefinitionsFor())
                    .provisionedThroughput(defaultProvisionedThroughputFor())
                    .build());

            var tableRequest = DescribeTableRequest.builder().tableName(tableName).build();
            var waiterResponse = waiter.waitUntilTableExists(tableRequest);
            return waiterResponse.matched().response().orElseThrow();
        }
    }

    private ProvisionedThroughput defaultProvisionedThroughputFor() {
        return DynamoTableUtils.createProvisionedThroughput(null, null);
    }

    private Collection<AttributeDefinition> defaultAttributeDefinitionsFor() {
        return List.of(
                AttributeDefinition.builder().attributeName(getEntityAttributeName()).attributeType(ScalarAttributeType.S).build(),
                AttributeDefinition.builder().attributeName(DynamoDBConverter.ID).attributeType(ScalarAttributeType.S).build()
        );
    }

    private Collection<KeySchemaElement> defaultKeySchemaFor() {
        return List.of(
                KeySchemaElement.builder().attributeName(getEntityAttributeName()).keyType(KeyType.HASH).build(),
                KeySchemaElement.builder().attributeName(DynamoDBConverter.ID).keyType(KeyType.RANGE).build()
        );
    }

    private boolean shouldCreateTables() {
        return this.settings
                .get(DynamoDBConfigurations.CREATE_TABLES, Boolean.class)
                .orElse(false);
    }

    private String getEntityAttributeName() {
        return entityAttributeName(this::resolveEntityNameAttributeName);
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity documentEntity, Duration ttl) {
        requireNonNull(documentEntity, "documentEntity is required");
        requireNonNull(ttl, "ttl is required");
        documentEntity.add(getTTLAttributeName(documentEntity.name()).get(), Instant.now().plus(ttl).truncatedTo(ChronoUnit.SECONDS));
        return insert(documentEntity);
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities) {
        requireNonNull(entities, "entities are required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::insert)
                .toList();
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities, Duration ttl) {
        requireNonNull(entities, "entities is required");
        requireNonNull(ttl, "ttl is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(e -> this.insert(e, ttl))
                .toList();
    }

    @Override
    public CommunicationEntity update(CommunicationEntity documentEntity) {
        requireNonNull(documentEntity, "entity is required");
        Map<String, AttributeValue> itemKey = getItemKey(documentEntity);
        Map<String, AttributeValueUpdate> attributeUpdates = asItemToUpdate(documentEntity);
        itemKey.keySet().forEach(attributeUpdates::remove);
        dynamoDbClient().updateItem(UpdateItemRequest.builder()
                .tableName(createTableIfNeeded(documentEntity.name()).table().tableName())
                .key(itemKey)
                .attributeUpdates(attributeUpdates)
                .build());
        return documentEntity;
    }

    private Map<String, AttributeValue> getItemKey(CommunicationEntity documentEntity) {
        DescribeTableResponse describeTableResponse = this.tables.computeIfAbsent(documentEntity.name(), this::getDescribeTableResponse);
        Map<String, AttributeValue> itemKey = describeTableResponse
                .table()
                .keySchema()
                .stream()
                .map(attribute -> Map.of(attribute.attributeName(),
                        toAttributeValue(documentEntity.find(attribute.attributeName(), Object.class).orElse(null))))
                .reduce(new HashMap<>(), (a, b) -> {
                    a.putAll(b);
                    return a;
                });
        itemKey.put(getEntityAttributeName(), toAttributeValue(documentEntity.name()));
        return itemKey;
    }

    private Map<String, AttributeValueUpdate> asItemToUpdate(CommunicationEntity documentEntity) {
        return toItemUpdate(this::resolveEntityNameAttributeName, documentEntity);
    }

    @Override
    public Iterable<CommunicationEntity> update(Iterable<CommunicationEntity> entities) {
        requireNonNull(entities, "entities is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::update)
                .toList();
    }

    @Override
    public void delete(DeleteQuery deleteQuery) {
        Objects.requireNonNull(deleteQuery, "deleteQuery is required");

        List<String> primaryKeys = getDescribeTableResponse(deleteQuery.name())
                .table()
                .keySchema()
                .stream()
                .map(KeySchemaElement::attributeName).toList();


        var selectQueryBuilder = SelectQuery.builder()
                .select(primaryKeys.toArray(new String[0]))
                .from(deleteQuery.name());

        deleteQuery.condition().ifPresent(selectQueryBuilder::where);

        select(selectQueryBuilder.build()).forEach(
                documentEntity ->
                        dynamoDbClient().deleteItem(DeleteItemRequest.builder()
                                .tableName(deleteQuery.name())
                                .key(getItemKey(documentEntity))
                                .build()));
    }

    @Override
    public Stream<CommunicationEntity> select(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        DynamoDBQuery dynamoDBQuery = DynamoDBQuery
                .builderOf(query.name(), getEntityAttributeName(), query)
                .get();

        ScanRequest.Builder selectRequest = ScanRequest.builder()
                .consistentRead(true)
                .tableName(dynamoDBQuery.table())
                .projectionExpression(dynamoDBQuery.projectionExpression())
                .filterExpression(dynamoDBQuery.filterExpression())
                .expressionAttributeNames(dynamoDBQuery.expressionAttributeNames())
                .expressionAttributeValues(dynamoDBQuery.expressionAttributeValues())
                .select(dynamoDBQuery.projectionExpression() != null ? Select.SPECIFIC_ATTRIBUTES : Select.ALL_ATTRIBUTES);

        return StreamSupport
                .stream(dynamoDbClient().scanPaginator(selectRequest.build()).spliterator(), false)
                .flatMap(scanResponse -> scanResponse.items().stream()
                        .map(item -> toCommunicationEntity(this::resolveEntityNameAttributeName, item)));
    }

    @Override
    public long count(String tableName) {
        Objects.requireNonNull(tableName, "tableName is required");
        try {
            return getDescribeTableResponse(tableName)
                    .table()
                    .itemCount();
        } catch (ResourceNotFoundException ex) {
            return 0;
        }
    }

    @Override
    public void close() {
        this.dynamoDbClient.close();
    }

    @Override
    public Stream<CommunicationEntity> partiQL(String query, Object... params) {
        Objects.requireNonNull(query, "query is required");
        List<AttributeValue> parameters = Stream.of(params).map(DynamoDBConverter::toAttributeValue).toList();
        ExecuteStatementResponse executeStatementResponse = dynamoDbClient()
                .executeStatement(ExecuteStatementRequest.builder()
                        .statement(query)
                        .parameters(parameters)
                        .build());
        List<CommunicationEntity> result = new LinkedList<>();
        executeStatementResponse.items().forEach(item -> result.add(toCommunicationEntity(this::resolveEntityNameAttributeName, item)));
        while (executeStatementResponse.nextToken() != null) {
            executeStatementResponse = dynamoDbClient()
                    .executeStatement(ExecuteStatementRequest.builder()
                            .statement(query)
                            .parameters(parameters)
                            .nextToken(executeStatementResponse.nextToken())
                            .build());
            executeStatementResponse.items().forEach(item -> result.add(toCommunicationEntity(this::resolveEntityNameAttributeName, item)));
        }
        return result.stream();
    }
}

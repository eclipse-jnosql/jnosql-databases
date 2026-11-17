/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.function.Supplier;

/**
 * A DynamoDB scan request derived from a {@link SelectQuery}: the table, an optional projection and a filter
 * with its placeholder maps.
 *
 * @param table the table name
 * @param projectionExpression the comma-separated projection aliases, or {@code null} to return every attribute
 * @param filterExpression the filter expression; it always restricts the scan to the entity name
 * @param expressionAttributeNames the attribute name aliases used by the expressions
 * @param expressionAttributeValues the attribute values bound to the filter placeholders
 */
public record DynamoDBQuery(String table,
                            String projectionExpression,
                            String filterExpression,
                            Map<String,String> expressionAttributeNames,
                            Map<String, AttributeValue> expressionAttributeValues) {

    /**
     * Creates a supplier that converts the select query into a {@link DynamoDBQuery} when invoked. The filter
     * always includes a condition that matches the entity name against the partition key attribute.
     *
     * @param table the table to scan
     * @param partitionKey the attribute that holds the entity name
     * @param query the select query whose condition and columns become the filter and the projection
     * @return a {@link Supplier} of the converted query
     */
    public static Supplier<DynamoDBQuery> builderOf(String table,
                                                    String partitionKey,
                                                    SelectQuery query) {
        return new DynamoDBQuerySelectBuilder(table, partitionKey, query);
    }
}

/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

class DynamoDBQuerySelectBuilder extends DynamoDBQueryBuilder {

    private final String table;

    private final SelectQuery selectQuery;

    public DynamoDBQuerySelectBuilder(String table,
                                      SelectQuery selectQuery) {
        this.table = table;
        this.selectQuery = selectQuery;
    }

    @Override
    public DynamoDBQuery get() {

        var filterExpression = new StringBuilder();
        var expressionAttributeNames = new HashMap<String, String>();
        var expressionAttributeValues = new HashMap<String, AttributeValue>();

        this.selectQuery.condition().ifPresent(c -> {
            super.condition(c,
                    filterExpression,
                    expressionAttributeNames,
                    expressionAttributeValues);
        });

        return new DynamoDBQuery(
                table,
                projectionExpression(expressionAttributeNames),
                filterExpression.toString(),
                expressionAttributeNames,
                expressionAttributeValues);
    }

    String projectionExpression(HashMap<String, String> expressionAttributeNames) {
        var columns = selectQuery.columns();
        if (columns.isEmpty()) {
            return null;
        }
        List<String> projectionAttributes = new ArrayList<>(columns.size());
        columns.forEach(column -> {
            var alias = "#%s".formatted(column);
            expressionAttributeNames.computeIfAbsent(alias, k -> column);
            projectionAttributes.add(alias);
        });
        return projectionAttributes.stream().collect(Collectors.joining(","));
    }


}

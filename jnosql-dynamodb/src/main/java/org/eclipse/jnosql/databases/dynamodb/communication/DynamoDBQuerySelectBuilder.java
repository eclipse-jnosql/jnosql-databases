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

import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;

class DynamoDBQuerySelectBuilder extends DynamoDBQueryBuilder {

    private final String table;

    private final String partitionKey;

    private final SelectQuery selectQuery;

    public DynamoDBQuerySelectBuilder(String table,
                                      String partitionKey,
                                      SelectQuery selectQuery) {
        this.table = table;
        this.partitionKey = partitionKey;
        this.selectQuery = selectQuery;
    }

    @Override
    public DynamoDBQuery get() {

        var filterExpression = new StringBuilder();
        var expressionAttributeNames = new HashMap<String, String>();
        var expressionAttributeValues = new HashMap<String, AttributeValue>();

        super.condition(
                CriteriaCondition.eq(Element.of(partitionKey, selectQuery.name())),
                filterExpression, expressionAttributeNames, expressionAttributeValues);

        this.selectQuery.condition().ifPresent(c -> {
            filterExpression.append(" AND ");
            super.condition(c,
                    filterExpression,
                    expressionAttributeNames,
                    expressionAttributeValues);
        });


        return new DynamoDBQuery(
                table,
                projectionExpression(),
                filterExpression.toString(),
                expressionAttributeNames,
                expressionAttributeValues);
    }

    String projectionExpression() {
        var columns = selectQuery.columns();
        if (columns.isEmpty()) {
            return null;
        }
        return String.join(", ", columns);
    }


}

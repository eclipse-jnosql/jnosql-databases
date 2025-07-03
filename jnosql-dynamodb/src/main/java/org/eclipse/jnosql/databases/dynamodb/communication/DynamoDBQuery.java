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

import java.util.Map;
import java.util.function.Supplier;

public record DynamoDBQuery(String table,
                            String projectionExpression,
                            String filterExpression,
                            Map<String,String> expressionAttributeNames,
                            Map<String, AttributeValue> expressionAttributeValues) {

    public static Supplier<DynamoDBQuery> builderOf(String table,
                                                    SelectQuery query) {
        return new DynamoDBQuerySelectBuilder(table, query);
    }
}

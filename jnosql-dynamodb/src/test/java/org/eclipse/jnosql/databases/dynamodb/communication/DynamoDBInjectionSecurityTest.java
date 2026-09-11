/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Apache License v2.0 which accompanies this distribution.
 */
package org.eclipse.jnosql.databases.dynamodb.communication;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;

class DynamoDBInjectionSecurityTest {

    @Test
    void shouldUseInternalExpressionPlaceholders() {
        String fieldPayload = "name) OR attribute_exists(secret) OR (name";
        String valuePayload = "' OR '1'='1";
        var query = select(fieldPayload).from("people")
                .where(fieldPayload).eq(valuePayload)
                .build();

        DynamoDBQuery generated = DynamoDBQuery.builderOf("people", "entity", query).get();

        assertThat(generated.filterExpression()).isEqualTo("#n0 = :v0 AND #n1 = :v1");
        assertThat(generated.projectionExpression()).isEqualTo("#n2");
        assertThat(generated.expressionAttributeNames())
                .containsEntry("#n0", "entity")
                .containsEntry("#n1", fieldPayload)
                .containsEntry("#n2", fieldPayload);
        assertThat(generated.expressionAttributeValues().get(":v1").s()).isEqualTo(valuePayload);
    }
}

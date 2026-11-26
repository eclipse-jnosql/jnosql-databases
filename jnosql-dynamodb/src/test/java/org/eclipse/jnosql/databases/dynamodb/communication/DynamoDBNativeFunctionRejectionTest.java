/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
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

import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;
import org.eclipse.jnosql.communication.semistructured.NativeFunctionQueries;
import org.eclipse.jnosql.communication.semistructured.DefaultSelectQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.paginators.ScanIterable;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.Stream;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("DynamoDB rejects native functions without any SDK request")
class DynamoDBNativeFunctionRejectionTest {

    private DynamoDbClient client;
    private DefaultDynamoDBDatabaseManager manager;

    @BeforeEach
    void setUp() {
        client = mock(DynamoDbClient.class);
        manager = new DefaultDynamoDBDatabaseManager("database", client, Settings.builder().build());
    }

    private static CriteriaCondition function(String name, Object... extra) {
        var arguments = new ArrayList<FunctionArgument>();
        arguments.add(new FunctionArgument.AttributePath("name"));
        for (Object value : extra) {
            arguments.add(new FunctionArgument.ValueArgument(Value.of(value)));
        }
        return CriteriaCondition.of(new CriteriaOperand.Function(NativeFunction.of(name,
                arguments.toArray(FunctionArgument[]::new))), Condition.EQUALS,
                new CriteriaOperand.Constant(Value.of("x")));
    }

    private static CriteriaCondition nativeOnTheRight() {
        return CriteriaCondition.of(new CriteriaOperand.Attribute("name"), Condition.EQUALS,
                new CriteriaOperand.Function(NativeFunction.of("UPPER",
                        new FunctionArgument.AttributePath("other"))));
    }

    private static CriteriaCondition nested() {
        return CriteriaCondition.of(new CriteriaOperand.Function(NativeFunction.of("UPPER",
                new FunctionArgument.NativeArgument(NativeFunction.of("LEFT",
                        new FunctionArgument.AttributePath("name"), new FunctionArgument.ValueArgument(Value.of(3)))))),
                Condition.EQUALS, new CriteriaOperand.Constant(Value.of("OTA")));
    }

    static Stream<Arguments> conditions() {
        var plain = CriteriaCondition.eq(Element.of("age", 1));
        return Stream.of(
                Arguments.of("ABS", function("ABS")),
                Arguments.of("LENGTH", function("LENGTH")),
                Arguments.of("LOWER", function("LOWER")),
                Arguments.of("UPPER", function("UPPER")),
                Arguments.of("LEFT", function("LEFT", 3)),
                Arguments.of("RIGHT", function("RIGHT", 3)),
                Arguments.of("native on the right", nativeOnTheRight()),
                Arguments.of("nested", nested()),
                Arguments.of("AND group", CriteriaCondition.and(plain, function("UPPER"))),
                Arguments.of("OR group", CriteriaCondition.or(function("UPPER"), plain)),
                Arguments.of("NOT", function("UPPER").negate()),
                Arguments.of("deep group", CriteriaCondition.and(plain,
                        CriteriaCondition.or(plain, function("LOWER").negate()))));
    }

    @Nested
    @DisplayName("Given a native function predicate")
    class Rejected {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.dynamodb.communication.DynamoDBNativeFunctionRejectionTest#conditions")
        void shouldRejectSelect(String name, CriteriaCondition condition) {
            var query = new DefaultSelectQuery(0, 0, "people", List.of(), List.of(), condition, false);

            assertSoftly(soft -> soft.assertThatThrownBy(() -> manager.select(query))
                    .isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("Native functions"));
            verifyNoInteractions(client);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.dynamodb.communication.DynamoDBNativeFunctionRejectionTest#conditions")
        @DisplayName("A rejected delete issues neither a describe, a scan nor a delete request")
        void shouldRejectDelete(String name, CriteriaCondition condition) {
            var query = NativeFunctionQueries.delete("people", condition, List.of());

            assertSoftly(soft -> soft.assertThatThrownBy(() -> manager.delete(query))
                    .isInstanceOf(UnsupportedOperationException.class));
            assertSoftly(soft -> soft.assertThatThrownBy(() -> manager.deleteAndCount(query))
                    .isInstanceOf(UnsupportedOperationException.class));
            verifyNoInteractions(client);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.eclipse.jnosql.databases.dynamodb.communication.DynamoDBNativeFunctionRejectionTest#conditions")
        @DisplayName("The filter expression builder rejects before building an expression")
        void shouldRejectInBuilder(String name, CriteriaCondition condition) {
            var query = new DefaultSelectQuery(0, 0, "people", List.of(), List.of(), condition, false);

            assertSoftly(soft -> soft.assertThatThrownBy(() -> DynamoDBQuery.builderOf("people", "entity", query).get())
                    .isInstanceOf(UnsupportedOperationException.class));
        }
    }

    @Nested
    @DisplayName("Given an ordinary predicate whose value came from an evaluable function")
    class Ordinary {

        @Test
        @DisplayName("The evaluated value is bound through an expression attribute value")
        void shouldKeepOrdinaryScan() {
            var description = TableDescription.builder().tableName("people").build();
            when(client.describeTable(any(DescribeTableRequest.class)))
                    .thenReturn(DescribeTableResponse.builder().table(description).build());
            var iterable = mock(ScanIterable.class);
            when(iterable.spliterator()).thenReturn(Spliterators.emptySpliterator());
            when(client.scanPaginator(any(ScanRequest.class))).thenReturn(iterable);
            var query = new DefaultSelectQuery(0, 0, "people", List.of(), List.of(),
                    CriteriaCondition.eq(Element.of("name", "OTAVIO")), false);

            manager.select(query).toList();

            var captor = ArgumentCaptor.forClass(ScanRequest.class);
            verify(client).scanPaginator(captor.capture());
            var request = captor.getValue();
            assertSoftly(soft -> {
                soft.assertThat(request.filterExpression()).isEqualTo("#n0 = :v0 AND #n1 = :v1");
                soft.assertThat(request.expressionAttributeNames()).containsEntry("#n1", "name");
                soft.assertThat(request.expressionAttributeValues().get(":v1").s()).isEqualTo("OTAVIO");
            });
        }
    }
}

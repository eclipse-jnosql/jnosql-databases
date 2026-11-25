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
package org.eclipse.jnosql.databases.couchbase.communication;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryResult;
import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;
import org.eclipse.jnosql.communication.semistructured.DefaultDeleteQuery;
import org.eclipse.jnosql.communication.semistructured.DefaultSelectQuery;
import org.eclipse.jnosql.communication.semistructured.DefaultUpdateQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Couchbase native functions reaching the mocked cluster")
class NativeFunctionManagerTest {

    private Cluster cluster;
    private DefaultCouchbaseDocumentManager manager;

    @BeforeEach
    void setUp() {
        cluster = mock(Cluster.class);
        Bucket bucket = mock(Bucket.class, RETURNS_DEEP_STUBS);
        when(bucket.defaultScope().name()).thenReturn("_default");
        when(cluster.bucket("bucket")).thenReturn(bucket);
        QueryResult result = mock(QueryResult.class);
        when(result.rowsAsObject()).thenReturn(List.of());
        when(cluster.query(anyString())).thenReturn(result);
        when(cluster.query(anyString(), any(QueryOptions.class))).thenReturn(result);
        manager = new DefaultCouchbaseDocumentManager(cluster, "bucket");
    }

    private static FunctionArgument attribute(String name) {
        return new FunctionArgument.AttributePath(name);
    }

    private static CriteriaOperand function(String name, FunctionArgument... arguments) {
        return new CriteriaOperand.Function(NativeFunction.of(name, arguments));
    }

    private static CriteriaOperand constant(Object value) {
        return new CriteriaOperand.Constant(Value.of(value));
    }

    private static CriteriaCondition compare(CriteriaOperand left, CriteriaOperand right) {
        return CriteriaCondition.of(left, Condition.EQUALS, right);
    }

    private static DefaultSelectQuery select(CriteriaCondition condition) {
        return new DefaultSelectQuery(0, 0, "person", List.of(), List.of(), condition, false);
    }

    private String statement;
    private Object parameters;

    private void captureWithParameters() {
        var invocation = mockingDetails(cluster).getInvocations().stream()
                .filter(call -> call.getMethod().getName().equals("query")).findFirst().orElseThrow();
        statement = invocation.getArgument(0, String.class).replaceAll("\\s+", " ");
        parameters = invocation.getRawArguments().length > 1
                ? ((QueryOptions.Built) invocation.getArgument(1, QueryOptions.class).build()).namedParameters()
                : "{}";
    }

    static Stream<Arguments> functions() {
        return Stream.of(
                Arguments.of(compare(function("ABS", attribute("balance")), constant(7)),
                        "ABS(`balance`) = $p0", "{\"$p0\":7}"),
                Arguments.of(compare(function("LENGTH", attribute("name")), constant(3)),
                        "ARRAY_LENGTH(SPLIT(`name`, ''))", "{\"$p0\":3}"),
                Arguments.of(compare(function("LOWER", attribute("name")), constant("ota")),
                        "LOWER(`name`) = $p0", "{\"$p0\":\"ota\"}"),
                Arguments.of(compare(function("UPPER", attribute("name")), constant("OTA")),
                        "UPPER(`name`) = $p0", "{\"$p0\":\"OTA\"}"),
                Arguments.of(compare(function("LEFT", attribute("name"), FunctionArgument.literal(3)),
                        constant("Ota")), "i0 < $p0", "{\"$p0\":3,\"$p1\":\"Ota\"}"),
                Arguments.of(compare(function("RIGHT", attribute("name"), FunctionArgument.literal(3)),
                        constant("vio")), "SPLIT(`name`, '')", "{\"$p0\":3,\"$p1\":\"vio\"}"),
                Arguments.of(compare(new CriteriaOperand.Attribute("name"), function("UPPER", attribute("other"))),
                        "UPPER(`other`)", "{}"));
    }

    @Nested
    @DisplayName("Given native function predicates")
    class Supported {

        @ParameterizedTest(name = "{1}")
        @MethodSource("org.eclipse.jnosql.databases.couchbase.communication.NativeFunctionManagerTest#functions")
        void shouldSelect(CriteriaCondition condition, String fragment, String params) {
            manager.select(select(condition)).toList();

            captureWithParameters();
            assertSoftly(soft -> {
                soft.assertThat(statement).startsWithIgnoringCase("SELECT").contains(fragment);
                soft.assertThat(parameters.toString()).isEqualTo(params);
            });
        }

        @ParameterizedTest(name = "{1}")
        @MethodSource("org.eclipse.jnosql.databases.couchbase.communication.NativeFunctionManagerTest#functions")
        void shouldCount(CriteriaCondition condition, String fragment, String params) {
            manager.count(new DefaultSelectQuery(0, 0, "person", List.of(), List.of(), condition, true));

            captureWithParameters();
            assertSoftly(soft -> {
                soft.assertThat(statement).contains("COUNT").contains(fragment);
                soft.assertThat(parameters.toString()).isEqualTo(params);
            });
        }

        @ParameterizedTest(name = "{1}")
        @MethodSource("org.eclipse.jnosql.databases.couchbase.communication.NativeFunctionManagerTest#functions")
        @DisplayName("DELETE selects the matching ids in the database before removing them")
        void shouldDelete(CriteriaCondition condition, String fragment, String params) {
            manager.delete(new DefaultDeleteQuery("person", condition, List.of()));

            captureWithParameters();
            assertSoftly(soft -> {
                soft.assertThat(statement).startsWithIgnoringCase("SELECT").contains(fragment);
                soft.assertThat(parameters.toString()).isEqualTo(params);
            });
        }

        @ParameterizedTest(name = "{1}")
        @CsvSource(delimiter = '|', value = {
                "UPPER|UPPER(d.`name`) = $p1|{\"$p0\":\"x\",\"$p1\":\"A\"}",
                "LOWER|LOWER(d.`name`) = $p1|{\"$p0\":\"x\",\"$p1\":\"A\"}",
                "ABS|ABS(d.`name`) = $p1|{\"$p0\":\"x\",\"$p1\":\"A\"}"})
        @DisplayName("UPDATE qualifies the attribute with the document alias and shifts the parameters")
        void shouldUpdate(String name, String fragment, String params) {
            var condition = compare(function(name, attribute("name")), constant("A"));

            manager.update(new DefaultUpdateQuery("person", List.of(Element.of("status", "x")), condition));

            captureWithParameters();
            assertSoftly(soft -> {
                soft.assertThat(statement).startsWithIgnoringCase("UPDATE").contains(fragment);
                soft.assertThat(parameters.toString()).isEqualTo(params);
            });
        }

        @Test
        @DisplayName("An injection payload stays a named parameter")
        void shouldBindInjectionPayload() {
            var payload = "x' OR 1=1 --\\`";

            manager.select(select(compare(function("UPPER", attribute("name")), constant(payload)))).toList();

            captureWithParameters();
            assertSoftly(soft -> {
                soft.assertThat(statement).doesNotContain("OR 1=1");
                soft.assertThat(parameters.toString()).contains("OR 1=1");
            });
        }
    }

    @Nested
    @DisplayName("Given unsupported native function predicates")
    class Unsupported {

        static Stream<CriteriaCondition> unsupported() {
            return Stream.of(
                    compare(function("SOUNDEX", attribute("name")), constant("A")),
                    compare(function("LEFT", attribute("name"), attribute("size")), constant("A")),
                    CriteriaCondition.of(function("LENGTH", attribute("name")), Condition.LIKE, constant("1%")));
        }

        @ParameterizedTest
        @MethodSource("unsupported")
        void shouldRejectSelectWithoutQuery(CriteriaCondition condition) {
            assertThatThrownBy(() -> manager.select(select(condition)))
                    .isInstanceOf(UnsupportedOperationException.class);
            verifyNoQuery();
        }

        @ParameterizedTest
        @MethodSource("unsupported")
        void shouldRejectCountWithoutQuery(CriteriaCondition condition) {
            assertThatThrownBy(() -> manager.count(select(condition)))
                    .isInstanceOf(UnsupportedOperationException.class);
            verifyNoQuery();
        }

        @ParameterizedTest
        @MethodSource("unsupported")
        @DisplayName("An unsupported delete never becomes an unrestricted delete")
        void shouldRejectDeleteWithoutQuery(CriteriaCondition condition) {
            assertThatThrownBy(() -> manager.delete(new DefaultDeleteQuery("person", condition, List.of())))
                    .isInstanceOf(UnsupportedOperationException.class);
            verifyNoQuery();
        }

        @ParameterizedTest
        @MethodSource("unsupported")
        @DisplayName("An unsupported update never becomes an unrestricted update")
        void shouldRejectUpdateWithoutQuery(CriteriaCondition condition) {
            assertThatThrownBy(() -> manager.update(new DefaultUpdateQuery("person",
                    List.of(Element.of("status", "x")), condition)))
                    .isInstanceOf(UnsupportedOperationException.class);
            verifyNoQuery();
        }

        private void verifyNoQuery() {
            verify(cluster).bucket("bucket");
            verifyNoMoreInteractions(cluster);
        }
    }
}

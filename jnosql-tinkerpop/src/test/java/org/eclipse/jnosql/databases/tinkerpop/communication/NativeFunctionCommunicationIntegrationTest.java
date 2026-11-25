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
package org.eclipse.jnosql.databases.tinkerpop.communication;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;
import org.eclipse.jnosql.communication.semistructured.DefaultDeleteQuery;
import org.eclipse.jnosql.communication.semistructured.DefaultSelectQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.databases.tinkerpop.cdi.TestGraphSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

/**
 * Executes the native function traversals on real TinkerPop graph providers. The traversal is evaluated by the
 * Gremlin engine, so the in-memory TinkerGraph and the embedded Neo4jGraph verify the {@code toLower()} and
 * {@code toUpper()} steps of the configured TinkerPop version, and the ArangoDB provider verifies them on top of a
 * remote database. Remote Gremlin Server providers are not covered.
 */
@DisplayName("TinkerPop native functions through the communication API")
abstract class NativeFunctionCommunicationIntegrationTest {

    private static final String PERSON = "Person";
    private static final String ID = "code";

    @EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
    static class ArangoDBTest extends NativeFunctionCommunicationIntegrationTest {
        @Override
        Graph graph() {
            return TestGraphSupplier.ARANGODB.get();
        }
    }

    @EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
    static class Neo4jTest extends NativeFunctionCommunicationIntegrationTest {
        @Override
        Graph graph() {
            return TestGraphSupplier.NEO4J.get();
        }
    }

    static class TinkerGraphTest extends NativeFunctionCommunicationIntegrationTest {
        @Override
        Graph graph() {
            return TestGraphSupplier.TINKER_GRAPH.get();
        }
    }

    private TinkerpopGraphDatabaseManager manager;

    abstract Graph graph();

    @BeforeEach
    void insertDataset() {
        manager = TinkerpopGraphDatabaseManager.of(graph());
        clear();
        person(1, "Otavio", "OTAVIO");
        person(2, "otavio", "JAVA");
        person(3, "JAVA", "java");
        person(4, "Jakarta", "JAKARTA");
        person(5, "Abc", "ABC");
    }

    @AfterEach
    void close() {
        manager.close();
    }

    private void clear() {
        manager.delete(new DefaultDeleteQuery(PERSON, null, List.of()));
    }

    private void person(int id, String name, String otherName) {
        var entity = CommunicationEntity.of(PERSON);
        entity.add(ID, id);
        if (name != null) {
            entity.add("name", name);
        }
        if (otherName != null) {
            entity.add("otherName", otherName);
        }
        manager.insert(entity);
    }

    private static FunctionArgument attribute(String name) {
        return new FunctionArgument.AttributePath(name);
    }

    private static FunctionArgument nested(String name, FunctionArgument... arguments) {
        return new FunctionArgument.NativeArgument(NativeFunction.of(name, arguments));
    }

    private static CriteriaOperand function(String name, FunctionArgument... arguments) {
        return new CriteriaOperand.Function(NativeFunction.of(name, arguments));
    }

    private static CriteriaOperand constant(Object value) {
        return new CriteriaOperand.Constant(Value.of(value));
    }

    private static CriteriaCondition compare(CriteriaOperand left, Condition condition, CriteriaOperand right) {
        return CriteriaCondition.of(left, condition, right);
    }

    private Set<Integer> people(CriteriaCondition condition) {
        var query = new DefaultSelectQuery(0, 0, PERSON, List.of(), List.of(), condition, false);
        try (Stream<CommunicationEntity> result = manager.select(query)) {
            return result.map(e -> e.find(ID).orElseThrow().get(Integer.class)).collect(Collectors.toSet());
        }
    }

    private static Set<Integer> ids(Integer... ids) {
        return Set.of(ids);
    }

    static Stream<Arguments> predicates() {
        return Stream.of(
                Arguments.of("UPPER(name) = 'OTAVIO'",
                        compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("OTAVIO")),
                        ids(1, 2)),
                Arguments.of("LOWER(name) = 'java'",
                        compare(function("LOWER", attribute("name")), Condition.EQUALS, constant("java")), ids(3)),
                Arguments.of("UPPER(LOWER(name)) = 'OTAVIO'",
                        compare(function("UPPER", nested("LOWER", attribute("name"))), Condition.EQUALS,
                                constant("OTAVIO")), ids(1, 2)),
                Arguments.of("LOWER(UPPER(otherName)) = 'java'",
                        compare(function("LOWER", nested("UPPER", attribute("otherName"))), Condition.EQUALS,
                                constant("java")), ids(2, 3)),
                Arguments.of("UPPER(name) = UPPER('otavio')",
                        compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("OTAVIO")),
                        ids(1, 2)),
                Arguments.of("UPPER(name) IN ('JAVA', 'ABC')",
                        compare(function("UPPER", attribute("name")), Condition.IN,
                                constant(List.of("JAVA", "ABC"))), ids(3, 5)),
                Arguments.of("UPPER(name) > 'JAVA'",
                        compare(function("UPPER", attribute("name")), Condition.GREATER_THAN, constant("JAVA")),
                        ids(1, 2)),
                Arguments.of("UPPER(name) BETWEEN 'ABC' AND 'JAKARTA'",
                        compare(function("UPPER", attribute("name")), Condition.BETWEEN,
                                constant(List.of("ABC", "JAKARTA"))), ids(4, 5)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("predicates")
    @DisplayName("Given SELECT with native functions")
    void shouldReturnExactIds(String label, CriteriaCondition condition, Set<Integer> expected) {
        assertThat(people(condition)).as(label).isEqualTo(expected);
    }

    @Test
    @DisplayName("COUNT uses the same predicate")
    void shouldCount() {
        var condition = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("OTAVIO"));

        var count = manager.count(new DefaultSelectQuery(0, 0, PERSON, List.of(), List.of(), condition, true));

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("AND, OR and NOT keep their grouping")
    void shouldKeepBooleanGrouping() {
        var upper = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("OTAVIO"));
        var lower = compare(function("LOWER", attribute("otherName")), Condition.EQUALS, constant("java"));
        var java = CriteriaCondition.eq("name", "JAVA");
        var first = CriteriaCondition.eq(ID, 1);
        var fifth = CriteriaCondition.eq(ID, 5);

        assertSoftly(soft -> {
            soft.assertThat(people(upper.or(lower.and(java)))).isEqualTo(ids(1, 2, 3));
            soft.assertThat(people(upper.and(first.or(fifth)))).isEqualTo(ids(1));
            soft.assertThat(people(upper.or(first.and(fifth)))).isEqualTo(ids(1, 2));
            soft.assertThat(people(upper.negate())).isEqualTo(ids(3, 4, 5));
            soft.assertThat(people(upper.and(first.negate()))).isEqualTo(ids(2));
        });
    }

    @Test
    @DisplayName("A native predicate can be combined with an ordinary predicate")
    void shouldCombineWithOrdinaryPredicates() {
        var upper = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("OTAVIO"));

        assertThat(people(upper.and(CriteriaCondition.gt(ID, 1)))).isEqualTo(ids(2));
    }

    @Test
    @DisplayName("A vertex without the property never matches")
    void shouldNotMatchMissingProperty() {
        clear();
        person(1, null, null);
        person(2, "a", null);

        assertSoftly(soft -> {
            soft.assertThat(people(compare(function("UPPER", attribute("name")), Condition.EQUALS,
                    constant("A")))).isEqualTo(ids(2));
            soft.assertThat(people(compare(function("LOWER", attribute("name")), Condition.LESSER_THAN,
                    constant("z")))).isEqualTo(ids(2));
        });
    }

    @Test
    @DisplayName("Case conversion covers non-ASCII letters and keeps the empty string")
    void shouldConvertUnicodeCase() {
        clear();
        person(1, "éa", null);
        person(2, "", null);

        assertSoftly(soft -> {
            soft.assertThat(people(compare(function("UPPER", attribute("name")), Condition.EQUALS,
                    constant("ÉA")))).isEqualTo(ids(1));
            soft.assertThat(people(compare(function("LOWER", attribute("name")), Condition.EQUALS,
                    constant("éa")))).isEqualTo(ids(1));
            soft.assertThat(people(compare(function("UPPER", attribute("name")), Condition.EQUALS,
                    constant("")))).isEqualTo(ids(2));
        });
    }

    @Test
    @DisplayName("Constants that look like Gremlin syntax are values")
    void shouldTreatPayloadsAsValues() {
        for (String payload : List.of("g.V().drop()", "' OR 1=1 //", "\\", "\"quote\"", "OTAVIO'); g.V().drop(); ('")) {
            var condition = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant(payload));
            assertThat(people(condition)).as(payload).isEmpty();
        }
        assertThat(manager.count(PERSON)).isEqualTo(5);
    }

    @Test
    @DisplayName("DELETE removes only the matching vertices")
    void shouldDeleteOnlyMatchingVertices() {
        var condition = compare(function("UPPER", attribute("name")), Condition.EQUALS, constant("OTAVIO"));

        manager.delete(new DefaultDeleteQuery(PERSON, condition, List.of()));

        assertThat(people(null)).isEqualTo(ids(3, 4, 5));
    }

    @Test
    @DisplayName("An evaluated function reaches the driver as an ordinary value")
    void shouldTreatEvaluatedValueAsOrdinaryConstant() {
        var evaluated = Value.of("OTAVIO");

        assertThat(people(CriteriaCondition.eq(Element.of("otherName", evaluated)))).isEqualTo(ids(1));
    }

}

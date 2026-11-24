/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Apache License v2.0 which accompanies this distribution.
 */
package org.eclipse.jnosql.databases.tinkerpop.communication;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tinkerpop traversal boolean composition")
class TraversalExecutorBooleanTest {

    private static TinkerGraph graph;

    @BeforeAll
    static void setUp() {
        graph = TinkerGraph.open();
        for (int number = 1; number <= 8; number++) {
            int bits = number - 1;
            graph.addVertex(org.apache.tinkerpop.gremlin.structure.T.label, "truth",
                    "number", number,
                    "a", (bits & 4) != 0,
                    "b", (bits & 2) != 0,
                    "c", (bits & 1) != 0);
        }
    }

    @AfterAll
    static void tearDown() throws Exception {
        graph.close();
    }

    private static CriteriaCondition is(String name) {
        return CriteriaCondition.eq(Element.of(name, true));
    }

    private static List<Integer> numbers(CriteriaCondition condition) {
        return graph.traversal().V().hasLabel("truth")
                .filter(TraversalExecutor.getPredicate(condition))
                .toList().stream()
                .map(v -> ((Vertex) v).<Integer>value("number"))
                .sorted()
                .toList();
    }

    @Nested
    @DisplayName("truth table")
    class TruthTable {

        @Test
        @DisplayName("should evaluate AND(A, B, C)")
        void shouldEvaluateAnd() {
            assertThat(numbers(CriteriaCondition.and(is("a"), is("b"), is("c")))).containsExactly(8);
        }

        @Test
        @DisplayName("should evaluate OR(A, B, C)")
        void shouldEvaluateOr() {
            assertThat(numbers(CriteriaCondition.or(is("a"), is("b"), is("c"))))
                    .containsExactly(2, 3, 4, 5, 6, 7, 8);
        }

        @Test
        @DisplayName("should evaluate OR(AND(A, B), C)")
        void shouldEvaluateOrOfAnd() {
            assertThat(numbers(CriteriaCondition.or(CriteriaCondition.and(is("a"), is("b")), is("c"))))
                    .containsExactly(2, 4, 6, 7, 8);
        }

        @Test
        @DisplayName("should evaluate AND(OR(A, B), C)")
        void shouldEvaluateAndOfOr() {
            assertThat(numbers(CriteriaCondition.and(CriteriaCondition.or(is("a"), is("b")), is("c"))))
                    .containsExactly(4, 6, 8);
        }

        @Test
        @DisplayName("should evaluate OR(A, AND(B, C))")
        void shouldEvaluateOrWithTrailingAnd() {
            assertThat(numbers(CriteriaCondition.or(is("a"), CriteriaCondition.and(is("b"), is("c")))))
                    .containsExactly(4, 5, 6, 7, 8);
        }

        @Test
        @DisplayName("should evaluate AND(NOT(A), B)")
        void shouldEvaluateAndOfNot() {
            assertThat(numbers(CriteriaCondition.and(is("a").negate(), is("b")))).containsExactly(3, 4);
        }

        @Test
        @DisplayName("should evaluate NOT(OR(A, B))")
        void shouldEvaluateNotOr() {
            assertThat(numbers(CriteriaCondition.or(is("a"), is("b")).negate())).containsExactly(1, 2);
        }

        @Test
        @DisplayName("should evaluate NOT(AND(A, B))")
        void shouldEvaluateNotAnd() {
            assertThat(numbers(CriteriaCondition.and(is("a"), is("b")).negate()))
                    .containsExactly(1, 2, 3, 4, 5, 6);
        }
    }

    @Nested
    @DisplayName("equivalent nesting")
    class EquivalentNesting {

        @Test
        @DisplayName("should treat AND(AND(A, B), C) like AND(A, B, C)")
        void shouldTreatNestedAndLikeFlat() {
            assertThat(numbers(CriteriaCondition.and(CriteriaCondition.and(is("a"), is("b")), is("c"))))
                    .isEqualTo(numbers(CriteriaCondition.and(is("a"), is("b"), is("c"))));
        }

        @Test
        @DisplayName("should treat OR(OR(A, B), C) like OR(A, B, C)")
        void shouldTreatNestedOrLikeFlat() {
            assertThat(numbers(CriteriaCondition.or(CriteriaCondition.or(is("a"), is("b")), is("c"))))
                    .isEqualTo(numbers(CriteriaCondition.or(is("a"), is("b"), is("c"))));
        }
    }
}

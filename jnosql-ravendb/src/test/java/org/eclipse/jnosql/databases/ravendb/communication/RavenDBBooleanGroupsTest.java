/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Apache License v2.0 which accompanies this distribution.
 */
package org.eclipse.jnosql.databases.ravendb.communication;

import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.eclipse.jnosql.communication.semistructured.DeleteQuery.delete;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
@DisplayName("RavenDB nested boolean conditions")
class RavenDBBooleanGroupsTest {

    private static final String COLLECTION = "TruthTable";
    private static DatabaseManager manager;
    private static final List<String> GENERATED = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        manager = DocumentConfigurationUtils.INSTANCE.get().apply("database");
        manager.delete(delete().from(COLLECTION).build());
        for (int number = 1; number <= 8; number++) {
            int bits = number - 1;
            var entity = manager.insert(CommunicationEntity.of(COLLECTION, List.of(
                    Element.of("number", number),
                    Element.of("a", (bits & 4) != 0),
                    Element.of("b", (bits & 2) != 0),
                    Element.of("c", (bits & 1) != 0))));
            GENERATED.add(entity.find("_id").orElseThrow().get(String.class));
        }
    }

    @AfterAll
    static void tearDown() {
        manager.delete(delete().from(COLLECTION).build());
    }

    private static CriteriaCondition is(String name) {
        return CriteriaCondition.eq(Element.of(name, true));
    }

    private static List<Integer> numbers(CriteriaCondition condition) {
        SelectQuery query = mock(SelectQuery.class);
        when(query.name()).thenReturn(COLLECTION);
        when(query.condition()).thenReturn(Optional.of(condition));
        when(query.sorts()).thenReturn(List.of());
        return manager.select(query)
                .map(e -> e.find("number").orElseThrow().get(Integer.class))
                .sorted()
                .toList();
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

    @Test
    @DisplayName("should keep the other predicate when an id is inside an AND")
    void shouldKeepPredicateWithIdInsideAnd() {
        var idOfSeven = CriteriaCondition.eq(Element.of("_id", GENERATED.get(6)));

        assertThat(numbers(CriteriaCondition.and(idOfSeven, is("c")))).isEmpty();
        assertThat(numbers(CriteriaCondition.and(idOfSeven, is("b")))).containsExactly(7);
    }

    @Test
    @DisplayName("should keep the other predicate when an id is inside an OR")
    void shouldKeepPredicateWithIdInsideOr() {
        var idOfOne = CriteriaCondition.eq(Element.of("_id", GENERATED.get(0)));

        assertThat(numbers(CriteriaCondition.or(idOfOne, is("c")))).containsExactly(1, 2, 4, 6, 8);
    }
}

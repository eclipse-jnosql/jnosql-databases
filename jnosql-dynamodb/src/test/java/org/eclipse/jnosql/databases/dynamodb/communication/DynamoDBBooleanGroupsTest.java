/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Apache License v2.0 which accompanies this distribution.
 */
package org.eclipse.jnosql.databases.dynamodb.communication;

import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("DynamoDB nested boolean filter expressions")
class DynamoDBBooleanGroupsTest {

    private static CriteriaCondition eq(String name, Object value) {
        return CriteriaCondition.eq(Element.of(name, value));
    }

    private static DynamoDBQuery build(CriteriaCondition condition) {
        SelectQuery query = mock(SelectQuery.class);
        when(query.condition()).thenReturn(Optional.of(condition));
        when(query.columns()).thenReturn(List.of());
        return DynamoDBQuery.builderOf("people", query).get();
    }

    @Nested
    @DisplayName("grouping")
    class Grouping {

        @Test
        @DisplayName("should keep an OR group inside an AND")
        void shouldKeepOrGroupInsideAnd() {
            DynamoDBQuery generated = build(
                    CriteriaCondition.and(CriteriaCondition.or(eq("a", 1), eq("b", 2)), eq("c", 3)));

            assertThat(generated.filterExpression()).isEqualTo("(#n0 = :v0 OR #n1 = :v1) AND #n2 = :v2");
        }

        @Test
        @DisplayName("should keep AND groups inside an OR")
        void shouldKeepAndGroupsInsideOr() {
            DynamoDBQuery generated = build(CriteriaCondition.or(
                    CriteriaCondition.and(eq("a", 1), eq("b", 2)),
                    CriteriaCondition.and(eq("c", 3), eq("d", 4))));

            assertThat(generated.filterExpression())
                    .isEqualTo("(#n0 = :v0 AND #n1 = :v1) OR (#n2 = :v2 AND #n3 = :v3)");
        }

        @Test
        @DisplayName("should negate the whole compound operand")
        void shouldNegateWholeCompound() {
            DynamoDBQuery generated = build(CriteriaCondition.or(eq("a", 1), eq("b", 2)).negate());

            assertThat(generated.filterExpression()).isEqualTo(" NOT (#n0 = :v0 OR #n1 = :v1)");
        }

        @Test
        @DisplayName("should translate redundantly nested AND like a flat AND")
        void shouldTranslateRedundantNesting() {
            DynamoDBQuery generated = build(
                    CriteriaCondition.and(CriteriaCondition.and(eq("a", 1), eq("b", 2)), eq("c", 3)));

            assertThat(generated.filterExpression()).isEqualTo("(#n0 = :v0 AND #n1 = :v1) AND #n2 = :v2");
        }
    }

    @Nested
    @DisplayName("parameter binding")
    class Binding {

        @Test
        @DisplayName("should not overwrite values of repeated fields")
        void shouldNotOverwriteRepeatedFieldValues() {
            DynamoDBQuery generated = build(CriteriaCondition.or(eq("age", 10), eq("age", 20), eq("age", 30)));

            assertSoftly(softly -> {
                softly.assertThat(generated.filterExpression())
                        .isEqualTo("#n0 = :v0 OR #n1 = :v1 OR #n2 = :v2");
                softly.assertThat(generated.expressionAttributeNames()).hasSize(3);
                softly.assertThat(generated.expressionAttributeValues().get(":v0").n()).isEqualTo("10");
                softly.assertThat(generated.expressionAttributeValues().get(":v1").n()).isEqualTo("20");
                softly.assertThat(generated.expressionAttributeValues().get(":v2").n()).isEqualTo("30");
            });
        }

        @Test
        @DisplayName("should keep BETWEEN as one predicate inside a group")
        void shouldKeepBetweenAsOnePredicate() {
            DynamoDBQuery generated = build(CriteriaCondition.or(
                    CriteriaCondition.between(Element.of("age", List.of(10, 20))), eq("active", true)));

            assertSoftly(softly -> {
                softly.assertThat(generated.filterExpression())
                        .isEqualTo("#n0 BETWEEN :v0 AND :v1 OR #n1 = :v2");
                softly.assertThat(generated.expressionAttributeValues()).hasSize(3);
            });
        }
    }
}

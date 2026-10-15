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
package org.eclipse.jnosql.databases.milvus.communication;

import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;

class MilvusFilterConverterTest {

    private MilvusFilterConverter.Filter filter(CriteriaCondition condition) {
        return MilvusFilterConverter.convert(Optional.of(condition)).orElseThrow();
    }

    private CriteriaCondition condition(org.eclipse.jnosql.communication.semistructured.SelectQuery query) {
        return query.condition().orElseThrow();
    }

    @Test
    @DisplayName("Should return nothing when there is no condition")
    void shouldReturnEmpty() {
        assertThat(MilvusFilterConverter.convert(Optional.empty())).isEmpty();
    }

    @Nested
    @DisplayName("When translating a comparison")
    class WhenTheComparisonIsTranslated {

        @Test
        @DisplayName("Should translate EQ with a template value")
        void shouldTranslateEquals() {
            var result = filter(condition(select().from("c").where("title").eq("Dune").build()));

            assertThat(result.expression()).isEqualTo("title == {p0}");
            assertThat(result.parameters()).containsEntry("p0", "Dune");
        }

        @Test
        @DisplayName("Should translate EQ null as a null check")
        void shouldTranslateNull() {
            var result = filter(CriteriaCondition.eq(Element.of("title", null)));

            assertThat(result.expression()).isEqualTo("title is null");
            assertThat(result.parameters()).isEmpty();
        }

        @Test
        @DisplayName("Should translate GT, GTE, LT and LTE")
        void shouldTranslateRanges() {
            assertThat(filter(condition(select().from("c").where("y").gt(1).build())).expression())
                    .isEqualTo("y > {p0}");
            assertThat(filter(condition(select().from("c").where("y").gte(1).build())).expression())
                    .isEqualTo("y >= {p0}");
            assertThat(filter(condition(select().from("c").where("y").lt(1).build())).expression())
                    .isEqualTo("y < {p0}");
            assertThat(filter(condition(select().from("c").where("y").lte(1).build())).expression())
                    .isEqualTo("y <= {p0}");
        }

        @Test
        @DisplayName("Should widen integral values to Long and Float to Double")
        void shouldNormalizeNumbers() {
            assertThat(filter(condition(select().from("c").where("y").eq(1).build())).parameters())
                    .containsEntry("p0", 1L);
            assertThat(filter(condition(select().from("c").where("y").eq(1.5f).build())).parameters())
                    .containsEntry("p0", 1.5d);
        }

        @Test
        @DisplayName("Should translate a UUID as text")
        void shouldTranslateUuid() {
            UUID id = UUID.randomUUID();

            assertThat(filter(condition(select().from("c").where("_id").eq(id).build())).parameters())
                    .containsEntry("p0", id.toString());
        }

        @Test
        @DisplayName("Should translate BETWEEN")
        void shouldTranslateBetween() {
            var result = filter(condition(select().from("c").where("y").between(1, 5).build()));

            assertThat(result.expression()).isEqualTo("y >= {p0} and y <= {p1}");
            assertThat(result.parameters()).containsEntry("p0", 1L).containsEntry("p1", 5L);
        }

        @Test
        @DisplayName("Should translate IN")
        void shouldTranslateIn() {
            var result = filter(condition(select().from("c").where("t").in(List.of("a", "b")).build()));

            assertThat(result.expression()).isEqualTo("t in {p0}");
            assertThat(result.parameters()).containsEntry("p0", List.of("a", "b"));
        }
    }

    @Nested
    @DisplayName("When translating a logical condition")
    class WhenTheLogicIsTranslated {

        @Test
        @DisplayName("Should translate AND recursively")
        void shouldTranslateAnd() {
            var result = filter(condition(select().from("c").where("a").eq("x").and("b").gt(1).build()));

            assertThat(result.expression()).isEqualTo("(a == {p0}) and (b > {p1})");
        }

        @Test
        @DisplayName("Should translate OR recursively")
        void shouldTranslateOr() {
            var result = filter(condition(select().from("c").where("a").eq("x").or("b").gt(1).build()));

            assertThat(result.expression()).isEqualTo("(a == {p0}) or (b > {p1})");
        }

        @Test
        @DisplayName("Should translate NOT")
        void shouldTranslateNot() {
            var result = filter(condition(select().from("c").where("a").not().eq("x").build()));

            assertThat(result.expression()).isEqualTo("not (a == {p0})");
        }
    }

    @Nested
    @DisplayName("When the condition is not safe to translate")
    class WhenTheConditionIsRejected {

        @Test
        @DisplayName("Should reject LIKE")
        void shouldRejectLike() {
            assertThatThrownBy(() -> filter(condition(select().from("c").where("a").like("x%").build())))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Should reject a field name that is not an identifier")
        void shouldRejectFieldName() {
            assertThatThrownBy(() -> filter(condition(select().from("c").where("a == 1 or b").eq("x").build())))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should reject an empty IN")
        void shouldRejectEmptyIn() {
            assertThatThrownBy(() -> filter(condition(select().from("c").where("a").in(List.of()).build())))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should reject a value type that Milvus cannot compare")
        void shouldRejectValueType() {
            assertThatThrownBy(() -> filter(condition(select().from("c").where("a").eq(new Object()).build())))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Should keep injection attempts in the template values")
        void shouldKeepValuesOutOfTheExpression() {
            var result = filter(condition(select().from("c").where("a").eq("x' or 1 == 1").build()));

            assertThat(result.expression()).doesNotContain("1 == 1");
            assertThat(result.parameters()).containsEntry("p0", "x' or 1 == 1");
        }
    }
}

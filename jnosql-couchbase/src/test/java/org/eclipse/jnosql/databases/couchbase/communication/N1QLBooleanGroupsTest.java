/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Apache License v2.0 which accompanies this distribution.
 */
package org.eclipse.jnosql.databases.couchbase.communication;

import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.communication.semistructured.UpdateQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class N1QLBooleanGroupsTest {

    private static CriteriaCondition eq(String name, Object value) {
        return CriteriaCondition.eq(Element.of(name, value));
    }

    private static N1QLQuery select(CriteriaCondition condition, boolean count) {
        SelectQuery query = mock(SelectQuery.class);
        when(query.name()).thenReturn("person");
        when(query.condition()).thenReturn(Optional.of(condition));
        when(query.sorts()).thenReturn(List.of());
        when(query.columns()).thenReturn(List.of());
        return new N1QLSelectQueryBuilder(query, "bucket", "scope", count).get();
    }

    private static N1QLQuery update(CriteriaCondition condition) {
        UpdateQuery query = mock(UpdateQuery.class);
        when(query.name()).thenReturn("person");
        when(query.sets()).thenReturn(List.of(Element.of("status", "x")));
        when(query.where()).thenReturn(Optional.of(condition));
        return new N1QLUpdateQueryBuilder(query, "bucket", "scope").get();
    }

    private static String normalized(String n1ql) {
        return n1ql.replaceAll("\\s+", " ").replace("( ", "(").replace(" )", ")");
    }

    @Nested
    @DisplayName("SELECT")
    class Select {

        @Test
        @DisplayName("should keep an OR group inside an AND")
        void shouldKeepOrGroupInsideAnd() {
            N1QLQuery generated = select(
                    CriteriaCondition.and(CriteriaCondition.or(eq("a", 1), eq("b", 2)), eq("c", 3)), false);

            assertThat(normalized(generated.query()))
                    .contains("WHERE (`a` = $p0 OR `b` = $p1) AND `c` = $p2");
        }

        @Test
        @DisplayName("should keep AND groups inside an OR")
        void shouldKeepAndGroupsInsideOr() {
            N1QLQuery generated = select(CriteriaCondition.or(
                    CriteriaCondition.and(eq("a", 1), eq("b", 2)),
                    CriteriaCondition.and(eq("c", 3), eq("d", 4))), false);

            assertThat(normalized(generated.query()))
                    .contains("WHERE (`a` = $p0 AND `b` = $p1) OR (`c` = $p2 AND `d` = $p3)");
        }

        @Test
        @DisplayName("should negate the whole compound operand")
        void shouldNegateWholeCompound() {
            N1QLQuery generated = select(CriteriaCondition.or(eq("a", 1), eq("b", 2)).negate(), false);

            assertThat(normalized(generated.query())).contains("WHERE NOT (`a` = $p0 OR `b` = $p1)");
        }

        @Test
        @DisplayName("should keep a standalone id equality as a direct lookup")
        void shouldKeepStandaloneIdLookup() {
            N1QLQuery generated = select(eq(EntityConverter.ID_FIELD, "1"), false);

            assertSoftly(softly -> {
                softly.assertThat(generated.ids()).containsExactly("1");
                softly.assertThat(generated.hasOnlyIds()).isTrue();
            });
        }

        @Test
        @DisplayName("should not extract the id out of an AND")
        void shouldNotExtractIdFromAnd() {
            N1QLQuery generated = select(CriteriaCondition.and(eq(EntityConverter.ID_FIELD, "1"), eq("active", true)),
                    false);

            assertSoftly(softly -> {
                softly.assertThat(generated.ids()).isEmpty();
                softly.assertThat(generated.hasOnlyIds()).isFalse();
                softly.assertThat(normalized(generated.query())).contains("`_id` = $p0 AND `active` = $p1");
            });
        }

        @Test
        @DisplayName("should not extract the id out of an OR")
        void shouldNotExtractIdFromOr() {
            N1QLQuery generated = select(CriteriaCondition.or(eq(EntityConverter.ID_FIELD, "1"), eq("active", true)),
                    false);

            assertSoftly(softly -> {
                softly.assertThat(generated.ids()).isEmpty();
                softly.assertThat(normalized(generated.query())).contains("`_id` = $p0 OR `active` = $p1");
            });
        }

        @Test
        @DisplayName("should not extract the id out of a negation")
        void shouldNotExtractIdFromNot() {
            N1QLQuery generated = select(eq(EntityConverter.ID_FIELD, "1").negate(), false);

            assertSoftly(softly -> {
                softly.assertThat(generated.ids()).isEmpty();
                softly.assertThat(normalized(generated.query())).contains("NOT (`_id` = $p0)");
            });
        }

        @Test
        @DisplayName("should translate an id predicate natively when counting")
        void shouldTranslateIdNativelyWhenCounting() {
            N1QLQuery generated = select(eq(EntityConverter.ID_FIELD, "1"), true);

            assertSoftly(softly -> {
                softly.assertThat(generated.ids()).isEmpty();
                softly.assertThat(normalized(generated.query())).contains("WHERE `_id` = $p0");
            });
        }
    }

    @Nested
    @DisplayName("UPDATE")
    class Update {

        @Test
        @DisplayName("should keep the same grouping as SELECT")
        void shouldKeepSameGroupingAsSelect() {
            CriteriaCondition condition = CriteriaCondition.and(CriteriaCondition.or(eq("a", 1), eq("b", 2)), eq("c", 3));

            N1QLQuery generated = update(condition);

            assertThat(normalized(generated.query()))
                    .contains("WHERE (d.`a` = $p1 OR d.`b` = $p2) AND d.`c` = $p3");
        }

        @Test
        @DisplayName("should negate the whole compound operand")
        void shouldNegateWholeCompound() {
            N1QLQuery generated = update(CriteriaCondition.and(eq("a", 1), eq("b", 2)).negate());

            assertThat(normalized(generated.query())).contains("WHERE NOT (d.`a` = $p1 AND d.`b` = $p2)");
        }

        @Test
        @DisplayName("should keep distinct bindings for repeated fields")
        void shouldKeepDistinctBindings() {
            N1QLQuery generated = update(CriteriaCondition.or(eq("age", 10), eq("age", 20)));

            assertSoftly(softly -> {
                softly.assertThat(generated.params().getInt("$p1")).isEqualTo(10);
                softly.assertThat(generated.params().getInt("$p2")).isEqualTo(20);
            });
        }
    }
}

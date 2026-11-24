/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License 2.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *   and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 *   You may elect to redistribute this code under either of these licenses.
 */
package org.eclipse.jnosql.databases.oracle.communication;

import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.DefaultSelectQuery;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Oracle NoSQL boolean condition translation")
class BooleanGroupQueryBuilderTest {

    private static final String TABLE = "entities";

    @Nested
    @DisplayName("Id fast path")
    class IdFastPath {

        @Test
        @DisplayName("should load by key for a standalone id equality")
        void shouldLoadByKeyForStandaloneIdEquality() {
            var query = select(CriteriaCondition.eq(Element.of("_id", "1")));

            assertThat(query.ids()).containsExactly("1");
        }

        @Test
        @DisplayName("should load by key for a standalone id IN")
        void shouldLoadByKeyForStandaloneIdIn() {
            var query = select(CriteriaCondition.in(Element.of("_id", List.of("1", "2"))));

            assertThat(query.ids()).containsExactly("1", "2");
        }

        @Test
        @DisplayName("should keep AND of ids as a native intersection")
        void shouldKeepAndOfIdsNative() {
            var query = select(CriteriaCondition.and(
                    CriteriaCondition.eq(Element.of("_id", "1")),
                    CriteriaCondition.eq(Element.of("_id", "2"))));

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(query.ids()).isEmpty();
                soft.assertThat(normalize(query.query())).contains(".id = ? AND");
                soft.assertThat(query.params()).hasSize(2);
            });
        }

        @Test
        @DisplayName("should keep AND of id IN lists as a native intersection")
        void shouldKeepAndOfIdInNative() {
            var query = select(CriteriaCondition.and(
                    CriteriaCondition.in(Element.of("_id", List.of("1", "2"))),
                    CriteriaCondition.in(Element.of("_id", List.of("2", "3")))));

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(query.ids()).isEmpty();
                soft.assertThat(query.params()).hasSize(2);
            });
        }

        @Test
        @DisplayName("should keep OR of ids native")
        void shouldKeepOrOfIdsNative() {
            var query = select(CriteriaCondition.or(
                    CriteriaCondition.eq(Element.of("_id", "1")),
                    CriteriaCondition.eq(Element.of("_id", "2"))));

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(query.ids()).isEmpty();
                soft.assertThat(normalize(query.query())).contains(" OR ");
            });
        }

        @Test
        @DisplayName("should keep the non-id predicate when combined with an id in select")
        void shouldKeepNonIdPredicateInSelect() {
            var query = select(CriteriaCondition.and(
                    CriteriaCondition.eq(Element.of("_id", "1")),
                    CriteriaCondition.eq(Element.of("active", true))));

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(query.ids()).isEmpty();
                soft.assertThat(query.params()).hasSize(2);
            });
        }

        @Test
        @DisplayName("should keep the non-id predicate when combined with an id in delete")
        void shouldKeepNonIdPredicateInDelete() {
            var query = new DeleteBuilder(delete(CriteriaCondition.and(
                    CriteriaCondition.eq(Element.of("_id", "1")),
                    CriteriaCondition.eq(Element.of("active", true)))), TABLE).get();

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(query.ids()).isEmpty();
                soft.assertThat(query.params()).hasSize(2);
            });
        }

        @Test
        @DisplayName("should keep a negated id native")
        void shouldKeepNegatedIdNative() {
            var query = select(CriteriaCondition.eq(Element.of("_id", "1")).negate());

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(query.ids()).isEmpty();
                soft.assertThat(normalize(query.query())).contains("NOT");
                soft.assertThat(query.params()).hasSize(1);
            });
        }
    }

    @Nested
    @DisplayName("Nested groups")
    class NestedGroups {

        @Test
        @DisplayName("should keep native parentheses for mixed groups")
        void shouldKeepParenthesesForMixedGroups() {
            var query = select(CriteriaCondition.and(
                    CriteriaCondition.or(
                            CriteriaCondition.eq(Element.of("a", true)),
                            CriteriaCondition.eq(Element.of("b", true))),
                    CriteriaCondition.eq(Element.of("c", true))));

            assertThat(normalize(query.query())).contains("(( entities.content.a = ? OR entities.content.b = ? ) AND entities.content.c = ? )");
        }
    }

    private static OracleQuery select(CriteriaCondition condition) {
        return new SelectBuilder(new DefaultSelectQuery(0, 0, "e", List.of(), List.of(), condition, false), TABLE).get();
    }

    private static DeleteQuery delete(CriteriaCondition condition) {
        return new DeleteQuery() {
            @Override
            public String name() {
                return "e";
            }

            @Override
            public Optional<CriteriaCondition> condition() {
                return Optional.of(condition);
            }

            @Override
            public List<String> columns() {
                return List.of();
            }
        };
    }

    private static String normalize(String query) {
        return query.replaceAll("\\s+", " ").trim();
    }
}

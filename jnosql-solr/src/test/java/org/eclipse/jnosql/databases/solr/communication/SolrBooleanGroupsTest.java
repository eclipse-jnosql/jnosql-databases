/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Apache License v2.0 which accompanies this distribution.
 */
package org.eclipse.jnosql.databases.solr.communication;

import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Solr nested boolean conditions")
class SolrBooleanGroupsTest {

    private static CriteriaCondition is(String name, String value) {
        return CriteriaCondition.eq(Element.of(name, value));
    }

    private static SelectQuery query(String collection, CriteriaCondition condition) {
        SelectQuery query = mock(SelectQuery.class);
        when(query.name()).thenReturn(collection);
        when(query.condition()).thenReturn(Optional.of(condition));
        return query;
    }

    @Nested
    @DisplayName("native query translation")
    class Translation {

        @Test
        @DisplayName("should keep the entity restriction for a top-level OR")
        void shouldKeepEntityRestrictionForTopLevelOr() {
            var solr = DocumentQueryConverter.convert(query("person", CriteriaCondition.or(is("a", "1"), is("b", "2"))));

            assertThat(solr).isEqualTo("_entity:person AND (a:1 OR b:2)");
        }

        @Test
        @DisplayName("should keep an OR group inside an AND")
        void shouldKeepOrGroupInsideAnd() {
            var solr = DocumentQueryConverter.convert(query("person",
                    CriteriaCondition.and(CriteriaCondition.or(is("a", "1"), is("b", "2")), is("c", "3"))));

            assertThat(solr).isEqualTo("_entity:person AND ((a:1 OR b:2) AND c:3)");
        }

        @Test
        @DisplayName("should negate the whole compound operand as a self-contained clause")
        void shouldNegateWholeCompound() {
            var solr = DocumentQueryConverter.convert(query("person",
                    CriteriaCondition.or(is("a", "1"), is("b", "2")).negate()));

            assertThat(solr).isEqualTo("_entity:person AND (*:* AND NOT (a:1 OR b:2))");
        }
    }

    @Nested
    @EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
    @DisplayName("truth table against Solr")
    class TruthTable {

        private static final String COLLECTION = "truthtable" + System.currentTimeMillis();
        private static SolrDocumentManager manager;

        @BeforeAll
        static void setUp() {
            manager = DocumentDatabase.INSTANCE.get();
            for (int id = 1; id <= 8; id++) {
                int bits = id - 1;
                manager.insert(CommunicationEntity.of(COLLECTION, List.of(
                        Element.of("_id", "tt" + id),
                        Element.of("a", ((bits & 4) != 0) ? "yes" : "no"),
                        Element.of("b", ((bits & 2) != 0) ? "yes" : "no"),
                        Element.of("c", ((bits & 1) != 0) ? "yes" : "no"))));
            }
        }

        private List<String> ids(CriteriaCondition condition) {
            return manager.select(query(COLLECTION, condition))
                    .map(e -> e.find("_id").orElseThrow().get(String.class))
                    .map(id -> id.substring(2))
                    .sorted()
                    .toList();
        }

        private CriteriaCondition a() {
            return is("a", "yes");
        }

        private CriteriaCondition b() {
            return is("b", "yes");
        }

        private CriteriaCondition c() {
            return is("c", "yes");
        }

        @Test
        @DisplayName("should evaluate OR(AND(A, B), C)")
        void shouldEvaluateOrOfAnd() {
            assertThat(ids(CriteriaCondition.or(CriteriaCondition.and(a(), b()), c())))
                    .containsExactly("2", "4", "6", "7", "8");
        }

        @Test
        @DisplayName("should evaluate AND(OR(A, B), C)")
        void shouldEvaluateAndOfOr() {
            assertThat(ids(CriteriaCondition.and(CriteriaCondition.or(a(), b()), c())))
                    .containsExactly("4", "6", "8");
        }

        @Test
        @DisplayName("should evaluate AND(NOT(A), B)")
        void shouldEvaluateAndOfNot() {
            assertThat(ids(CriteriaCondition.and(a().negate(), b()))).containsExactly("3", "4");
        }

        @Test
        @DisplayName("should evaluate NOT(OR(A, B))")
        void shouldEvaluateNotOr() {
            assertThat(ids(CriteriaCondition.or(a(), b()).negate())).containsExactly("1", "2");
        }

        @Test
        @DisplayName("should evaluate NOT(AND(A, B))")
        void shouldEvaluateNotAnd() {
            assertThat(ids(CriteriaCondition.and(a(), b()).negate()))
                    .containsExactly("1", "2", "3", "4", "5", "6");
        }

        @Test
        @DisplayName("should evaluate OR(A, NOT(B)) with a negation inside an OR")
        void shouldEvaluateNegationInsideOr() {
            assertThat(ids(CriteriaCondition.or(a(), b().negate())))
                    .containsExactly("1", "2", "5", "6", "7", "8");
        }
    }
}

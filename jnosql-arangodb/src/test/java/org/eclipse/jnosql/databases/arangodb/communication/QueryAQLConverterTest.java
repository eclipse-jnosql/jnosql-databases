/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
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
 *   Michele Rastelli
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.arangodb.communication;

import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.communication.semistructured.UpdateQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QueryAQLConverterTest {

    @Test
    public void shouldRunEqualsQuery() {
        SelectQuery query = select().from("collection")
                .where("name").eq("value").build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertEquals("value", values.get("name"));
        assertEquals("FOR c IN collection FILTER  c.name == @name RETURN c", aql);

    }

    @Test
    public void shouldRunEqualsQueryAnd() {
        SelectQuery query = select().from("collection")
                .where("name").eq("value")
                .and("age").lte(10)
                .build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertEquals("value", values.get("name"));
        assertEquals("FOR c IN collection FILTER  c.name == @name AND  c.age <= @age RETURN c", aql);

    }

    @Test
    public void shouldRunEqualsQueryOr() {
        SelectQuery query = select().from("collection")
                .where("name").eq("value")
                .or("age").lte(10)
                .build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertEquals("value", values.get("name"));
        assertEquals("FOR c IN collection FILTER  c.name == @name OR  c.age <= @age RETURN c", aql);

    }

    @Test
    public void shouldRunEqualsQuerySort() {
        SelectQuery query = select().from("collection")
                .where("name").eq("value")
                .orderBy("name").asc().build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertEquals("value", values.get("name"));
        assertEquals("FOR c IN collection FILTER  c.name == @name SORT  c.name ASC RETURN c", aql);
    }

    @Test
    public void shouldRunEqualsQuerySort2() {
        SelectQuery query = select().from("collection")
                .where("name").eq("value")
                .orderBy("name").asc()
                .orderBy("age").desc().build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertEquals("value", values.get("name"));
        assertEquals("FOR c IN collection FILTER  c.name == @name SORT  c.name ASC , c.age DESC RETURN c", aql);
    }


    @Test
    public void shouldRunEqualsQueryLimit() {
        SelectQuery query = select().from("collection")
                .where("name").eq("value")
                .limit(5).build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertEquals("value", values.get("name"));
        assertEquals("FOR c IN collection FILTER  c.name == @name LIMIT 5 RETURN c", aql);

    }

    @Test
    public void shouldRunEqualsQueryLimit2() {
        SelectQuery query = select().from("collection")
                .where("name").eq("value")
                .skip(1).limit(5).build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertEquals("value", values.get("name"));
        assertEquals("FOR c IN collection FILTER  c.name == @name LIMIT 1, 5 RETURN c", aql);
    }

    @Test
    public void shouldRunEqualsQueryNot() {
        SelectQuery query = select().from("collection")
                .where("name").not().eq("value").build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertEquals("value", values.get("name"));
        assertEquals("FOR c IN collection FILTER  NOT ( c.name == @name) RETURN c", aql);

    }


    @Test
    public void shouldNegate() {
        SelectQuery query = select().from("collection")
                .where("city").not().eq("Assis")
                .and("name").eq("Otavio")
                .or("name").not().eq("Lucas").build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertEquals(3, values.size());
        assertEquals("Assis", values.get("city"));
        assertEquals("Otavio", values.get("name"));
        assertEquals("Lucas", values.get("name_1"));
        assertEquals("FOR c IN collection FILTER ( NOT ( c.city == @city) AND  c.name == @name) OR  NOT ( c.name == @name_1) RETURN c", aql);

    }

    @Test
    public void shouldRunEqualsCountQuery() {
        SelectQuery query = select().from("collection")
                .where("name").eq("value").build();

        AQLQueryResult convert = QueryAQLConverter.count(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertEquals("value", values.get("name"));
        assertEquals("RETURN LENGTH(FOR c IN collection FILTER  c.name == @name RETURN c)", aql);

    }

    @Test
    public void shouldRunUpdateQuery() {

        UpdateQuery query = mock(UpdateQuery.class);
        when(query.name()).thenReturn("collection");
        when(query.set()).then( i->
                List.of(
                        Element.of("name", "John"),
                        Element.of("surname", "Doe")
                )
        );
        when(query.condition()).thenReturn(Optional.of(
                        CriteriaCondition.eq(Element.of("id", 1))));


        AQLQueryResult convert = QueryAQLConverter.update(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();

        assertSoftly(softly->{
            softly.assertThat(values.get("id")).isEqualTo(1);
            softly.assertThat(values.get("name")).isEqualTo("John");
            softly.assertThat(values.get("surname")).isEqualTo("Doe");
            softly.assertThat(aql.replaceAll("  "," "))
                    .isEqualTo(
                            "FOR c IN collection FILTER c.id == @id " +
                                    "UPDATE c WITH { name: @name, surname: @surname } IN collection " +
                                    "RETURN NEW");
        });

    }

    @Test
    void shouldBindInjectionPayloadAsValue() {
        String payload = "' OR true RETURN secret";
        SelectQuery query = select().from("collection")
                .where("name").eq(payload)
                .build();

        AQLQueryResult generated = QueryAQLConverter.select(query);

        assertThat(generated.query()).doesNotContain(payload).contains("c.name == @name");
        assertThat(generated.values()).containsEntry("name", payload);
    }

    @Test
    void shouldRejectExecutableIdentifierSyntax() {
        SelectQuery query = select().from("collection")
                .where("name) OR true").eq("safe")
                .build();

        assertThatThrownBy(() -> QueryAQLConverter.select(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid AQL identifier");
    }

    @Test
    void shouldRejectExecutableCollectionSyntax() {
        assertThatThrownBy(() -> QueryAQLConverter.validateIdentifier("collection) RETURN secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid AQL identifier");
    }



    @Nested
    @DisplayName("Nested boolean groups")
    class NestedBooleanGroups {

        private CriteriaCondition eq(String name, Object value) {
            return CriteriaCondition.eq(Element.of(name, value));
        }

        private AQLQueryResult run(CriteriaCondition condition) {
            SelectQuery query = mock(SelectQuery.class);
            when(query.name()).thenReturn("collection");
            when(query.condition()).thenReturn(Optional.of(condition));
            when(query.sorts()).thenReturn(List.of());
            return QueryAQLConverter.select(query);
        }

        private String filter(CriteriaCondition condition) {
            return run(condition).query();
        }

        @Test
        @DisplayName("should keep an OR group inside an AND")
        void shouldKeepOrGroupInsideAnd() {
            CriteriaCondition condition = CriteriaCondition.and(
                    CriteriaCondition.or(eq("a", 1), eq("b", 2)), eq("c", 3));

            assertThat(filter(condition)).isEqualTo(
                    "FOR c IN collection FILTER ( c.a == @a OR  c.b == @b) AND  c.c == @c RETURN c");
        }

        @Test
        @DisplayName("should keep an AND group inside an OR")
        void shouldKeepAndGroupInsideOr() {
            CriteriaCondition condition = CriteriaCondition.or(
                    CriteriaCondition.and(eq("a", 1), eq("b", 2)),
                    CriteriaCondition.and(eq("c", 3), eq("d", 4)));

            assertThat(filter(condition)).isEqualTo(
                    "FOR c IN collection FILTER ( c.a == @a AND  c.b == @b) OR ( c.c == @c AND  c.d == @d) RETURN c");
        }

        @Test
        @DisplayName("should negate the whole BETWEEN predicate")
        void shouldNegateWholeBetween() {
            CriteriaCondition condition = CriteriaCondition.between(Element.of("age", List.of(10, 20))).negate();

            assertThat(filter(condition)).isEqualTo(
                    "FOR c IN collection FILTER  NOT ( c.age >= @age AND  c.age <= @age_1) RETURN c");
        }

        @Test
        @DisplayName("should group BETWEEN inside an OR")
        void shouldGroupBetweenInsideOr() {
            CriteriaCondition condition = CriteriaCondition.or(
                    CriteriaCondition.between(Element.of("age", List.of(10, 20))), eq("active", true));

            assertThat(filter(condition)).isEqualTo(
                    "FOR c IN collection FILTER ( c.age >= @age AND  c.age <= @age_1) OR  c.active == @active RETURN c");
        }

        @Test
        @DisplayName("should bind distinct parameters for repeated fields")
        void shouldBindDistinctParametersForRepeatedFields() {
            CriteriaCondition condition = CriteriaCondition.or(eq("age", 10), eq("age", 20), eq("age", 30));

            AQLQueryResult result = run(condition);

            assertSoftly(softly -> {
                softly.assertThat(result.values()).containsOnlyKeys("age", "age_1", "age_2");
                softly.assertThat(result.values().values()).containsExactlyInAnyOrder(10, 20, 30);
                softly.assertThat(result.query()).contains("@age", "@age_1", "@age_2");
            });
        }

        @Test
        @DisplayName("should translate redundantly nested AND like a flat AND")
        void shouldTranslateRedundantNestingLikeFlat() {
            CriteriaCondition nested = CriteriaCondition.and(
                    CriteriaCondition.and(eq("a", 1), eq("b", 2)), eq("c", 3));

            assertThat(filter(nested)).isEqualTo(
                    "FOR c IN collection FILTER ( c.a == @a AND  c.b == @b) AND  c.c == @c RETURN c");
        }
    }

}

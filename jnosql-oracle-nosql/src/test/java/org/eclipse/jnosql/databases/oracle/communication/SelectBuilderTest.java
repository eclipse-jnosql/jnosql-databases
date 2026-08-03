/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 */
package org.eclipse.jnosql.databases.oracle.communication;

import jakarta.data.Sort;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.semistructured.DeleteQuery.delete;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;

class SelectBuilderTest {

    @ParameterizedTest
    @CsvSource({
            "2B, 2b",
            "2g, 2g",
            "6A, 6a"
    })
    void shouldTranslateIgnoreCaseEquality(String value, String expectedBind) {
        var condition = CriteriaCondition.ignoreCase(CriteriaCondition.eq("hexadecimal", value));
        var query = SelectQuery.builder()
                .from("AsciiCharacter")
                .where(condition)
                .build();

        var oracleQuery = new SelectBuilder(query, "entity_data").get();

        assertThat(normalize(oracleQuery.query()))
                .contains("entity_data.entity= 'AsciiCharacter' AND")
                .contains("lower( entity_data.content.hexadecimal ) = ?");
        assertThat(oracleQuery.params()).hasSize(1);
        assertThat(oracleQuery.params().get(0).asString().getValue()).isEqualTo(expectedBind);
        assertThat(oracleQuery.ids()).isEmpty();
    }

    @Test
    void shouldTranslateIgnoreCaseBetweenAndPreserveSurroundingQuery() {
        var condition = CriteriaCondition.ignoreCase(
                        CriteriaCondition.between("hexadecimal", List.of("4c", "5A")))
                .and(CriteriaCondition.not(
                        CriteriaCondition.in("hexadecimal", Set.of("5"))));
        var query = SelectQuery.builder()
                .from("AsciiCharacter")
                .where(condition)
                .sort(Sort.asc("hexadecimal"))
                .build();

        var oracleQuery = new SelectBuilder(query, "entity_data").get();

        assertThat(normalize(oracleQuery.query())).isEqualTo(
                "select * from entity_data WHERE entity_data.entity= 'AsciiCharacter' AND "
                        + "(lower( entity_data.content.hexadecimal ) BETWEEN ? AND ? AND "
                        + "NOT entity_data.content.hexadecimal IN ?[] ) "
                        + "ORDER BY entity_data.content.hexadecimal ASC");
        assertThat(oracleQuery.params()).hasSize(3);
        assertThat(oracleQuery.params().get(0).asString().getValue()).isEqualTo("4c");
        assertThat(oracleQuery.params().get(1).asString().getValue()).isEqualTo("5a");
        assertThat(oracleQuery.params().get(2).asArray().get(0).asString().getValue()).isEqualTo("5");
        assertThat(oracleQuery.ids()).isEmpty();
    }

    @Test
    void shouldUseIdFastPathForEqualsQuery() {
        var query = select().from("person")
                .where("_id").eq("id-1")
                .build();

        var oracleQuery = new SelectBuilder(query, "people").get();

        assertThat(oracleQuery.ids()).containsExactly("id-1");
        assertThat(oracleQuery.params()).isEmpty();
        assertThat(oracleQuery.query()).doesNotContain("content._id");
    }

    @Test
    void shouldUseIdFastPathForInQuery() {
        var query = select().from("person")
                .where("_id").in(List.of("id-1", "id-2"))
                .build();

        var oracleQuery = new SelectBuilder(query, "people").get();

        assertThat(oracleQuery.ids()).containsExactly("id-1", "id-2");
        assertThat(oracleQuery.params()).isEmpty();
        assertThat(oracleQuery.query()).doesNotContain("content._id");
    }

    @Test
    void shouldKeepMixedIdPredicateInSql() {
        var query = select().from("person")
                .where("_id").eq("id-1")
                .and("scope").eq("admin")
                .build();

        var oracleQuery = new SelectBuilder(query, "people").get();

        assertThat(oracleQuery.ids()).isEmpty();
        assertThat(oracleQuery.params()).hasSize(2);
        assertThat(oracleQuery.params().get(0).asString().getValue()).isEqualTo("person:id-1");
        assertThat(oracleQuery.params().get(1).asString().getValue()).isEqualTo("admin");
        assertThat(oracleQuery.query())
                .contains("people.id")
                .contains("people.content.scope");
    }

    @Test
    void shouldWrapCompositeOrConditionsToPreserveEntityFilter() {
        var query = select().from("person")
                .where("age").not().gt(42)
                .or("name").not().eq("Ada")
                .build();

        var oracleQuery = new SelectBuilder(query, "people").get();

        assertThat(oracleQuery.query())
                .contains("people.entity= 'person' AND (")
                .contains(" OR ")
                .endsWith(")");
    }

    @Test
    void shouldConvertCharacterPredicateToStringValue() {
        var query = select().from("person")
                .where("middleInitial").eq('Q')
                .build();

        var oracleQuery = new SelectBuilder(query, "people").get();

        assertThat(oracleQuery.params()).singleElement().satisfies(value -> {
            assertThat(value.isString()).isTrue();
            assertThat(value.asString().getValue()).isEqualTo("Q");
        });
    }

    private static String normalize(String query) {
        return query.replaceAll("\\s+", " ").trim();
    }

    @Test
    void shouldUseTypedJsonIdForRelationalPredicates() {
        var queries = List.of(
                select().from("naturalNumber").where("_id").lt(54L).build(),
                select().from("naturalNumber").where("_id").lte(54L).build(),
                select().from("naturalNumber").where("_id").gt(54L).build(),
                select().from("naturalNumber").where("_id").gte(54L).build());

        assertThat(queries).allSatisfy(query -> {
            var oracleQuery = new SelectBuilder(query, "entities").get();

            assertThat(oracleQuery.query())
                    .contains("entities.content.\"_id\"")
                    .doesNotContain("entities.id");
            assertThat(oracleQuery.params()).singleElement()
                    .satisfies(value -> assertThat(value.asLong().getValue()).isEqualTo(54L));
        });
    }

    @Test
    void shouldUseTypedJsonIdForBetweenInEverySqlBuilder() {
        var selectQuery = select().from("naturalNumber")
                .where("_id").between(49L, 54L)
                .build();
        var deleteQuery = delete().from("naturalNumber")
                .where("_id").between(49L, 54L)
                .build();

        var oracleQueries = List.of(
                new SelectBuilder(selectQuery, "entities").get(),
                new SelectCountBuilder(selectQuery, "entities").get(),
                new DeleteBuilder(deleteQuery, "entities").get());

        assertThat(oracleQueries).allSatisfy(oracleQuery -> {
            assertThat(oracleQuery.query())
                    .contains("entities.content.\"_id\"")
                    .contains("BETWEEN ? AND ?")
                    .doesNotContain("entities.id");
            assertThat(oracleQuery.params()).hasSize(2);
            assertThat(oracleQuery.params().get(0).asLong().getValue()).isEqualTo(49L);
            assertThat(oracleQuery.params().get(1).asLong().getValue()).isEqualTo(54L);
        });
    }

    @Test
    void shouldUseTypedJsonIdForIgnoreCaseBetweenQuery() {
        var condition = CriteriaCondition.ignoreCase(
                CriteriaCondition.between("_id", List.of("Alpha", "Zulu")));
        var query = SelectQuery.builder().from("person")
                .where(condition)
                .build();

        var oracleQuery = new SelectBuilder(query, "people").get();

        assertThat(oracleQuery.ids()).isEmpty();
        assertThat(oracleQuery.query())
                .contains("lower(")
                .contains("people.content.\"_id\"")
                .contains("BETWEEN ? AND ?")
                .doesNotContain("people.id");
        assertThat(oracleQuery.params())
                .extracting(value -> value.asString().getValue())
                .containsExactly("alpha", "zulu");
    }

    @Test
    void shouldUseTypedJsonIdForSorting() {
        var query = select().from("naturalNumber")
                .orderBy("_id").asc()
                .build();

        var oracleQuery = new SelectBuilder(query, "entities").get();

        assertThat(oracleQuery.query())
                .contains("ORDER BY")
                .contains("entities.content.\"_id\"")
                .contains("ASC")
                .doesNotContain("ORDER BY  entities.id");
    }

    @Test
    void shouldKeepPrimaryKeyIdForProjection() {
        var query = select("_id").from("naturalNumber").build();

        var oracleQuery = new SelectBuilder(query, "entities").get();

        assertThat(oracleQuery.query())
                .contains("select id, entity, entities.id")
                .doesNotContain("entities.content.\"_id\"");
    }

    @Test
    void shouldKeepPrefixedPrimaryKeyIdForDirectInPredicate() {
        var query = select().from("naturalNumber")
                .where("_id").in(List.of(54L, 55L))
                .build();

        var oracleQuery = new SelectCountBuilder(query, "entities").get();

        assertThat(oracleQuery.query())
                .contains("entities.id")
                .contains("IN")
                .doesNotContain("entities.content.\"_id\"");
        assertThat(oracleQuery.params()).singleElement().satisfies(value -> {
            assertThat(value.asArray().get(0).asString().getValue()).isEqualTo("naturalNumber:54");
            assertThat(value.asArray().get(1).asString().getValue()).isEqualTo("naturalNumber:55");
        });
    }
}

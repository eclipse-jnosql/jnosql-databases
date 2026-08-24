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

import jakarta.data.Sort;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
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
        var query = SelectQuery.builder().from("AsciiCharacter")
                .where(condition)
                .build();

        var oracleQuery = new SelectBuilder(query, "entity_data").get();

        assertThat(oracleQuery.query())
                .contains("entity_data.entity= 'AsciiCharacter'")
                .containsPattern("lower\\(\\s*entity_data\\.content\\.hexadecimal\\s*\\)\\s*=\\s*\\?");
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
        var query = SelectQuery.builder().from("AsciiCharacter")
                .where(condition)
                .sort(Sort.asc("hexadecimal"))
                .build();

        var oracleQuery = new SelectBuilder(query, "entity_data").get();

        assertThat(oracleQuery.query())
                .contains("entity_data.entity= 'AsciiCharacter' AND (")
                .containsPattern("lower\\(\\s*entity_data\\.content\\.hexadecimal\\s*\\)"
                        + "\\s+BETWEEN\\s+\\?\\s+AND\\s+\\?")
                .containsPattern("NOT\\s+entity_data\\.content\\.hexadecimal\\s+IN\\s+\\?\\[\\]")
                .containsPattern("ORDER\\s+BY\\s+entity_data\\.content\\.hexadecimal\\s+ASC");
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
    void shouldUseTypedJsonIdForRelationalQuery() {
        var query = select().from("person")
                .where("_id").gte(54L)
                .build();

        var oracleQuery = new SelectBuilder(query, "people").get();

        assertThat(oracleQuery.ids()).isEmpty();
        assertThat(oracleQuery.query())
                .contains("people.content.\"_id\"")
                .doesNotContain("people.id");
        assertThat(oracleQuery.params()).singleElement().satisfies(value -> {
            assertThat(value.isLong()).isTrue();
            assertThat(value.toJson()).isEqualTo("54");
        });
    }

    @Test
    void shouldUseTypedJsonIdForBetweenQuery() {
        var query = select().from("person")
                .where("_id").between(52L, 57L)
                .build();

        var oracleQuery = new SelectBuilder(query, "people").get();

        assertThat(oracleQuery.ids()).isEmpty();
        assertThat(oracleQuery.query())
                .contains("people.content.\"_id\"")
                .contains("BETWEEN ? AND ?")
                .doesNotContain("people.id");
        assertThat(oracleQuery.params())
                .extracting(value -> value.toJson())
                .containsExactly("52", "57");
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
    void shouldUseTypedJsonIdForOrdering() {
        var query = select().from("person")
                .orderBy("_id").desc()
                .build();

        var oracleQuery = new SelectBuilder(query, "people").get();

        assertThat(oracleQuery.query())
                .contains("ORDER BY  people.content.\"_id\"  DESC")
                .doesNotContain("people.id");
        assertThat(oracleQuery.params()).isEmpty();
        assertThat(oracleQuery.ids()).isEmpty();
    }

    @Test
    void shouldKeepIdProjectionOnPrimaryKey() {
        var query = select("_id").from("person").build();

        var oracleQuery = new SelectBuilder(query, "people").get();

        assertThat(oracleQuery.query())
                .contains("id, entity, people.id")
                .doesNotContain("people.content.\"_id\"");
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
        var query = select().from("asciiCharacter")
                .where("thisCharacter").eq('j')
                .build();

        var oracleQuery = new SelectBuilder(query, "characters").get();

        assertThat(oracleQuery.params()).singleElement()
                .satisfies(value -> assertThat(value.asString().getValue()).isEqualTo("j"));
    }
}

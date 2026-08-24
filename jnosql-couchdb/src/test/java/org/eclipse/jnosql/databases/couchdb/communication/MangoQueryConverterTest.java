/*
 *
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
 *
 */
package org.eclipse.jnosql.databases.couchdb.communication;

import jakarta.json.JsonObject;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.Arrays;

import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;
import static org.assertj.core.api.Assertions.assertThat;


class MangoQueryConverterTest {

    private MangoQueryConverter converter = new MangoQueryConverter();

    @ParameterizedTest
    @JsonSource("select_all.json")
    public void shouldReturnSelectFromAll(JsonObject expected) {
        var query = select().from("person").build();
        JsonObject jsonObject = converter.apply(query);
        assertThat(jsonObject).isEqualTo(expected);
    }

    @ParameterizedTest
    @JsonSource("select_fields.json")
    public void shouldReturnSelectFieldsFromAll(JsonObject expected) {
        var query = select("_id", "_rev").from("person").build();
        JsonObject jsonObject = converter.apply(query);
        assertThat(jsonObject).isEqualTo(expected);
    }

    @ParameterizedTest
    @JsonSource("select_fields_skip_start.json")
    public void shouldReturnSelectFieldsLimitSkip(JsonObject expected) {
        var query = select("_id", "_rev").from("person").limit(10).skip(2).build();
        JsonObject jsonObject = converter.apply(query);
        assertThat(jsonObject).isEqualTo(expected);
    }

    @ParameterizedTest
    @JsonSource("select_from_order.json")
    public void shouldReturnSelectFromOrder(JsonObject expected) {
        var query = select().from("person").orderBy("year").asc()
                .orderBy("name").desc().build();
        JsonObject jsonObject = converter.apply(query);
        assertThat(jsonObject).isEqualTo(expected);
    }

    @ParameterizedTest
    @JsonSource("select_from_gt_order.json")
    public void shouldSelectFromGtAge(JsonObject expected) {
        var query = select().from("person").where("age").gt(10).build();
        JsonObject jsonObject = converter.apply(query);
        assertThat(jsonObject).isEqualTo(expected);
    }

    @ParameterizedTest
    @JsonSource("select_from_gte_order.json")
    public void shouldSelectFromGteAge(JsonObject expected) {
        var query = select().from("person").where("age").gte(10).build();
        JsonObject jsonObject = converter.apply(query);
        assertThat(jsonObject).isEqualTo(expected);
    }

    @ParameterizedTest
    @JsonSource("select_from_lt_order.json")
    public void shouldSelectFromLtAge(JsonObject expected) {
        var query = select().from("person").where("age").lt(10).build();
        JsonObject jsonObject = converter.apply(query);
        assertThat(jsonObject).isEqualTo(expected);
    }

    @ParameterizedTest
    @JsonSource("select_from_lte_order.json")
    public void shouldSelectFromLteAge(JsonObject expected) {
        var query = select().from("person").where("age").lte(10).build();
        JsonObject jsonObject = converter.apply(query);
        assertThat(jsonObject).isEqualTo(expected);
    }

    @ParameterizedTest
    @JsonSource("select_from_in_order.json")
    public void shouldSelectFromInAge(JsonObject expected) {
        var query = select().from("person").where("age").in(Arrays.asList(10, 12)).build();
        JsonObject jsonObject = converter.apply(query);
        assertThat(jsonObject).isEqualTo(expected);
    }

    @ParameterizedTest
    @JsonSource("select_from_not_order.json")
    public void shouldSelectFromNotAge(JsonObject expected) {
        var query = select().from("person").where("age").not().lt(10).build();
        JsonObject jsonObject = converter.apply(query);
        assertThat(jsonObject).isEqualTo(expected);
    }

    @ParameterizedTest
    @JsonSource("select_from_and_order.json")
    public void shouldSelectFromAndAge(JsonObject expected) {
        var query = select().from("person")
                .where("name").eq("Poliana")
                .and("name").eq("Ada").build();
        JsonObject jsonObject = converter.apply(query);
        assertThat(jsonObject).isEqualTo(expected);
    }

    @ParameterizedTest
    @JsonSource("select_from_or_order.json")
    public void shouldSelectFromOrAge(JsonObject expected) {
        var query = select().from("person")
                .where("name").eq("Poliana")
                .or("name").eq("Ada").build();
        JsonObject jsonObject = converter.apply(query);
        assertThat(jsonObject).isEqualTo(expected);
    }

    @ParameterizedTest
    @JsonSource("select_all.json")
    public void shouldReturnSelectFromAllBookmark(JsonObject expected) {
        var query = select().from("person").build();
        JsonObject jsonObject = converter.apply(CouchDBDocumentQuery.of(query));
        assertThat(jsonObject).isEqualTo(expected);
    }

    @ParameterizedTest
    @JsonSource("select_all_bookmark.json")
    public void shouldReturnSelectFromAllBookmark2(JsonObject expected) {
        var query = select().from("person").build();
        JsonObject jsonObject = converter.apply(CouchDBDocumentQuery.of(query, "bookmark"));
        assertThat(jsonObject).isEqualTo(expected);
    }


}
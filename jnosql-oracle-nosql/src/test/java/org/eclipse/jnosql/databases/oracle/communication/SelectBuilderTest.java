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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;

class SelectBuilderTest {

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
}

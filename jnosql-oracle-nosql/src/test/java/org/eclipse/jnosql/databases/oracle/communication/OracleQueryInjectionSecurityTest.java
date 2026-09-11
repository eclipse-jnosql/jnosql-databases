/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Apache License v2.0 which accompanies this distribution.
 */
package org.eclipse.jnosql.databases.oracle.communication;

import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OracleQueryInjectionSecurityTest {

    @Test
    void shouldBindEntityAndRegexPayloadAsValues() {
        String entityPayload = "Person' OR '1'='1";
        String valuePayload = "\") OR 1 = 1 --";
        var query = SelectQuery.builder().from(entityPayload)
                .where(CriteriaCondition.contains(Element.of("name", valuePayload)))
                .build();

        OracleQuery generated = new SelectBuilder(query, "entities").get();

        assertThat(generated.query())
                .contains("entities.entity= ?")
                .contains("regex_like( entities.content.name , ?)")
                .doesNotContain(entityPayload, valuePayload);
        assertThat(generated.entity().asString().getValue()).isEqualTo(entityPayload);
        assertThat(generated.params())
                .extracting(value -> value.asString().getValue())
                .containsExactly(OracleNoSqlLikeConverter.INSTANCE.contains(valuePayload));
    }

    @Test
    void shouldRejectExecutableFieldSyntax() {
        var query = SelectQuery.builder().from("Person")
                .where(CriteriaCondition.eq("name) OR 1 = 1 --", "safe"))
                .build();

        assertThatThrownBy(() -> new SelectBuilder(query, "entities").get())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Oracle NoSQL identifier");
    }

    @Test
    void shouldRejectExecutableTableSyntax() {
        var query = SelectQuery.builder().from("Person").build();

        assertThatThrownBy(() -> new SelectBuilder(query, "entities; DROP TABLE secrets").get())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Oracle NoSQL identifier");
    }
}

/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Apache License v2.0 which accompanies this distribution.
 */
package org.eclipse.jnosql.databases.couchbase.communication;

import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.UpdateQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class N1QLInjectionSecurityTest {

    @Test
    void shouldBindPayloadAndQuoteIdentifiers() {
        String payload = "' OR TRUE --";
        var query = select("name` FROM secret --").from("person` WHERE TRUE --")
                .where("name` = 'admin' OR `name").eq(payload)
                .build();

        N1QLQuery generated = new N1QLSelectQueryBuilder(query, "bucket", "scope", false).get();

        assertThat(generated.query()).contains("`person`` WHERE TRUE --`")
                .contains("`name`` = 'admin' OR ``name`")
                .contains("= $p0")
                .doesNotContain(payload);
        assertThat(generated.params().getString("$p0")).isEqualTo(payload);
    }

    @Test
    void shouldUseDistinctBindingsForBetweenValues() {
        var query = select().from("person")
                .where("age").between(18, 65)
                .build();

        N1QLQuery generated = new N1QLSelectQueryBuilder(query, "bucket", "scope", false).get();

        assertThat(generated.query()).contains("`age`  BETWEEN $p0 AND $p1");
        assertThat(generated.params().getInt("$p0")).isEqualTo(18);
        assertThat(generated.params().getInt("$p1")).isEqualTo(65);
    }

    @Test
    void shouldQuoteDottedBucketNameAtomically() {
        var query = select().from("person").build();

        N1QLQuery generated = new N1QLSelectQueryBuilder(query, "tenant.data", "scope", false).get();

        assertThat(generated.query()).contains("FROM `tenant.data`.`scope`.`person`".toLowerCase());
    }

    @Test
    void shouldBuildValidUpdateBetweenPredicate() {
        UpdateQuery query = mock(UpdateQuery.class);
        when(query.name()).thenReturn("person");
        when(query.set()).thenReturn(List.of(Element.of("status", "adult")));
        when(query.condition()).thenReturn(Optional.of(CriteriaCondition.between("age", List.of(18, 65))));

        N1QLQuery generated = new N1QLUpdateQueryBuilder(query, "bucket", "scope").get();

        assertThat(generated.query()).contains("d.`age` BETWEEN $p1 AND $p2");
        assertThat(generated.params().getString("$p0")).isEqualTo("adult");
        assertThat(generated.params().getInt("$p1")).isEqualTo(18);
        assertThat(generated.params().getInt("$p2")).isEqualTo(65);
    }
}

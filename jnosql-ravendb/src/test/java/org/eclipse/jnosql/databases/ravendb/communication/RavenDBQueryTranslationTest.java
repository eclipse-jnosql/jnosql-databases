/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Apache License v2.0 which accompanies this distribution.
 */
package org.eclipse.jnosql.databases.ravendb.communication;

import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("RavenDB native query translation")
class RavenDBQueryTranslationTest {

    private static DocumentStore store;
    private static IDocumentSession session;

    @BeforeAll
    static void setUp() {
        store = new DocumentStore("http://localhost:1", "database");
        store.initialize();
        session = store.openSession();
    }

    @AfterAll
    static void tearDown() {
        session.close();
        store.close();
    }

    private static CriteriaCondition eq(String name, Object value) {
        return CriteriaCondition.eq(Element.of(name, value));
    }

    private static DocumentQueryConverter.QueryResult convert(CriteriaCondition condition) {
        SelectQuery query = mock(SelectQuery.class);
        when(query.name()).thenReturn("people");
        when(query.condition()).thenReturn(Optional.of(condition));
        when(query.sorts()).thenReturn(List.of());
        return DocumentQueryConverter.createQuery(session, query);
    }

    private static String rql(CriteriaCondition condition) {
        return convert(condition).getRavenQuery().orElseThrow().toString();
    }

    @Test
    @DisplayName("should enclose an OR group inside an AND")
    void shouldEncloseOrGroupInsideAnd() {
        assertThat(rql(CriteriaCondition.and(CriteriaCondition.or(eq("a", 1), eq("b", 2)), eq("c", 3))))
                .contains("(a = $p0 or b = $p1) and c = $p2");
    }

    @Test
    @DisplayName("should enclose AND groups inside an OR")
    void shouldEncloseAndGroupsInsideOr() {
        assertThat(rql(CriteriaCondition.or(
                CriteriaCondition.and(eq("a", 1), eq("b", 2)),
                CriteriaCondition.and(eq("c", 3), eq("d", 4)))))
                .contains("(a = $p0 and b = $p1) or (c = $p2 and d = $p3)");
    }

    @Test
    @DisplayName("should negate the whole group")
    void shouldNegateWholeGroup() {
        assertThat(rql(CriteriaCondition.or(eq("a", 1), eq("b", 2)).negate()))
                .contains("not (a = $p0 or b = $p1)");
    }

    @Test
    @DisplayName("should not use the direct id lookup for an id inside a compound condition")
    void shouldKeepIdInsideCompoundCondition() {
        var result = convert(CriteriaCondition.and(eq("_id", "people/1"), eq("active", true)));

        assertSoftly(softly -> {
            softly.assertThat(result.getIds()).isEmpty();
            softly.assertThat(result.getRavenQuery().orElseThrow().toString())
                    .contains("id() = $p0 and active = $p1");
        });
    }

    @Test
    @DisplayName("should keep the direct id lookup for a standalone id")
    void shouldKeepStandaloneIdLookup() {
        var result = convert(eq("_id", "people/1"));

        assertSoftly(softly -> {
            softly.assertThat(result.getIds()).containsExactly("people/1");
            softly.assertThat(result.getRavenQuery()).isEmpty();
        });
    }
}

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

import org.eclipse.jnosql.communication.Params;
import org.eclipse.jnosql.communication.query.data.DeleteProvider;
import org.eclipse.jnosql.communication.query.data.SelectProvider;
import org.eclipse.jnosql.communication.semistructured.CommunicationObserverParser;
import org.eclipse.jnosql.communication.semistructured.DeleteQueryParams;
import org.eclipse.jnosql.communication.semistructured.DeleteQueryParser;
import org.eclipse.jnosql.communication.semistructured.QueryParams;
import org.eclipse.jnosql.communication.semistructured.SelectQueryParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;

class QueryValueNormalizationTest {

    @Test
    void shouldNormalizeEnumScalarValue() {
        var query = select().from("Person")
                .where("kind").eq(NumberKind.PRIME)
                .build();

        var generated = new SelectBuilder(query, "entities").get();

        assertThat(generated.params()).singleElement()
                .satisfies(value -> assertThat(value.asString().getValue()).isEqualTo("PRIME"));
    }

    @Test
    void shouldUnwrapNamedInParametersForCount() {
        QueryParams queryParams = parseSelect("FROM Person WHERE age IN (:first, :second)",
                params -> {
                    params.bind("first", 21);
                    params.bind("second", 42);
                });

        var generated = new SelectCountBuilder(queryParams.query(), "entities").get();

        assertThat(generated.params()).singleElement()
                .satisfies(value -> {
                    assertThat(value.isArray()).isTrue();
                    assertThat(value.asArray().get(0).asInteger().getValue()).isEqualTo(21);
                    assertThat(value.asArray().get(1).asInteger().getValue()).isEqualTo(42);
                });
    }

    @Test
    void shouldUnwrapNamedBetweenParametersForDelete() {
        DeleteQueryParams queryParams = delete("DELETE FROM Person WHERE age BETWEEN :minimum AND :maximum",
                params -> {
                    params.bind("minimum", 21);
                    params.bind("maximum", 42);
                });

        var generated = new DeleteBuilder(queryParams.query(), "entities").get();

        assertThat(generated.params()).hasSize(2);
        assertThat(generated.params().get(0).asInteger().getValue()).isEqualTo(21);
        assertThat(generated.params().get(1).asInteger().getValue()).isEqualTo(42);
    }

    @Test
    void shouldKeepIdPrefixForMixedInQuery() {
        var query = select().from("Person")
                .where("_id").in(List.of("id-1", "id-2"))
                .and("scope").eq("admin")
                .build();

        var generated = new SelectBuilder(query, "entities").get();

        assertThat(generated.ids()).isEmpty();
        assertThat(generated.params()).hasSize(2);
        assertThat(generated.params().get(0).asArray().get(0).asString().getValue()).isEqualTo("Person:id-1");
        assertThat(generated.params().get(0).asArray().get(1).asString().getValue()).isEqualTo("Person:id-2");
    }

    private static QueryParams parseSelect(String jdql, Consumer<Params> binder) {
        var parsed = SelectProvider.INSTANCE.apply(jdql, "Person");
        QueryParams queryParams = new SelectQueryParser()
                .apply(parsed, CommunicationObserverParser.EMPTY);
        binder.accept(queryParams.params());
        return queryParams;
    }

    private static DeleteQueryParams delete(String jdql, Consumer<Params> binder) {
        var parsed = DeleteProvider.INSTANCE.apply(jdql);
        DeleteQueryParams queryParams = new DeleteQueryParser()
                .apply(parsed, CommunicationObserverParser.EMPTY);
        binder.accept(queryParams.params());
        return queryParams;
    }

    private enum NumberKind {
        PRIME
    }
}

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

import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.DefaultSelectQuery;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class NullPredicateQueryBuilderTest {

    private static final String ENTITY = "asciiCharacter";
    private static final String FIELD = "hexadecimal";
    private static final String TABLE = "entities";

    @ParameterizedTest(name = "{0}")
    @MethodSource("queryCases")
    void shouldTranslateNullEqualityWithoutBindParameter(QueryCase queryCase) {
        var generated = queryCase.build(CriteriaCondition.eq(nullElement()));

        assertThat(generated.query()).contains("content." + FIELD, " IS NULL")
                .doesNotContain("?");
        assertThat(generated.params()).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("queryCases")
    void shouldTranslateNegatedNullEqualityWithoutBindParameter(QueryCase queryCase) {
        var generated = queryCase.build(CriteriaCondition.eq(nullElement()).negate());

        assertThat(generated.query()).contains("content." + FIELD, " IS NOT NULL")
                .doesNotContain("?");
        assertThat(generated.params()).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("queryCases")
    void shouldKeepNullStringAsBindParameter(QueryCase queryCase) {
        var generated = queryCase.build(CriteriaCondition.eq(Element.of(FIELD, "null")));

        assertThat(generated.query()).contains("content." + FIELD, " = ", "?")
                .doesNotContain("IS NULL", "IS NOT NULL");
        assertThat(generated.params()).singleElement()
                .satisfies(parameter -> assertThat(parameter.asString().getValue()).isEqualTo("null"));
    }

    private static Stream<QueryCase> queryCases() {
        return Stream.of(
                new QueryCase("select", condition -> new SelectBuilder(selectQuery(condition), TABLE).get()),
                new QueryCase("count", condition -> new SelectCountBuilder(selectQuery(condition), TABLE).get()),
                new QueryCase("delete", condition -> new DeleteBuilder(deleteQuery(condition), TABLE).get()));
    }

    private static SelectQuery selectQuery(CriteriaCondition condition) {
        return new DefaultSelectQuery(0, 0, ENTITY, List.of(), List.of(), condition, false);
    }

    private static DeleteQuery deleteQuery(CriteriaCondition condition) {
        return new DeleteQuery() {
            @Override
            public String name() {
                return ENTITY;
            }

            @Override
            public Optional<CriteriaCondition> condition() {
                return Optional.of(condition);
            }

            @Override
            public List<String> columns() {
                return List.of();
            }
        };
    }

    private static Element nullElement() {
        return Element.of(FIELD, Value.ofNull());
    }

    private record QueryCase(String name, Function<CriteriaCondition, OracleQuery> builder) {

        private OracleQuery build(CriteriaCondition condition) {
            return builder.apply(condition);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}

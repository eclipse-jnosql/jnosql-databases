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
    private static final String ID = "_id";
    private static final String TABLE = "entities";
    private static final String JSON_PATH = TABLE + ".content." + FIELD;
    private static final String ID_PATH = TABLE + ".id";

    @ParameterizedTest(name = "{0}")
    @MethodSource("queryCases")
    void shouldTranslateNullEqualityWithoutBindParameter(QueryCase queryCase) {
        var generated = queryCase.build(CriteriaCondition.eq(nullElement(FIELD)));

        assertThat(normalize(generated.query()))
                .contains("(" + JSON_PATH + " = null OR NOT EXISTS " + JSON_PATH
                        + " OR " + JSON_PATH + " IS NULL)")
                .doesNotContain("?");
        assertThat(generated.params()).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("queryCases")
    void shouldTranslateNegatedNullEqualityWithoutBindParameter(QueryCase queryCase) {
        var generated = queryCase.build(CriteriaCondition.eq(nullElement(FIELD)).negate());

        assertThat(normalize(generated.query()))
                .contains("(" + JSON_PATH + " != null AND EXISTS " + JSON_PATH
                        + " AND " + JSON_PATH + " IS NOT NULL)")
                .doesNotContain("?");
        assertThat(generated.params()).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("queryCases")
    void shouldKeepIdNullEqualityAsNativeSqlNullPredicate(QueryCase queryCase) {
        var generated = queryCase.build(CriteriaCondition.eq(nullElement(ID)));

        assertThat(normalize(generated.query())).contains(ID_PATH + " IS NULL")
                .doesNotContain("content." + ID, "= null", "EXISTS", "?");
        assertThat(generated.params()).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("queryCases")
    void shouldKeepNegatedIdNullEqualityAsNativeSqlNullPredicate(QueryCase queryCase) {
        var generated = queryCase.build(CriteriaCondition.eq(nullElement(ID)).negate());

        assertThat(normalize(generated.query())).contains(ID_PATH + " IS NOT NULL")
                .doesNotContain("content." + ID, "!= null", "EXISTS", "?");
        assertThat(generated.params()).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("queryCases")
    void shouldKeepNullStringAsBindParameter(QueryCase queryCase) {
        var generated = queryCase.build(CriteriaCondition.eq(Element.of(FIELD, "null")));

        assertThat(normalize(generated.query())).contains(JSON_PATH + " = ?")
                .doesNotContain("IS NULL", "IS NOT NULL", "EXISTS", "= null", "!= null");
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

    private static Element nullElement(String field) {
        return Element.of(field, Value.ofNull());
    }

    private static String normalize(String query) {
        return query.replaceAll("\\s+", " ").trim();
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

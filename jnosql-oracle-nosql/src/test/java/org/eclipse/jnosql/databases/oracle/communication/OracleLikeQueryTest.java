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

import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class OracleLikeQueryTest {

    @Test
    void shouldMatchDottedNameWithLike() {
        var query = build(CriteriaCondition.like("name", "Dr. Gaylord Lueilwitz"));

        assertThat(query.query()).contains("regex_like( people.content.name , ?)");
        var pattern = boundPattern(query);
        assertThat(pattern.matcher("Dr. Gaylord Lueilwitz").matches()).isTrue();
        assertThat(pattern.matcher("DrX Gaylord Lueilwitz").matches()).isFalse();
        assertThat(pattern.matcher("Ada Lovelace").matches()).isFalse();
    }

    @Test
    void shouldExcludeMatchingDottedNameWithNotLike() {
        var query = build(CriteriaCondition.not(CriteriaCondition.like("name", "Dr. Gaylord Lueilwitz")));

        assertThat(query.query()).contains("NOT regex_like( people.content.name , ?)");
        var pattern = boundPattern(query);
        assertThat(!pattern.matcher("Dr. Gaylord Lueilwitz").matches()).isFalse();
        assertThat(!pattern.matcher("DrX Gaylord Lueilwitz").matches()).isTrue();
        assertThat(!pattern.matcher("Ada Lovelace").matches()).isTrue();
    }

    @ParameterizedTest(name = "LIKE treats {0} as a literal regex character")
    @MethodSource("regexMetacharacters")
    void shouldMatchRegexMetacharactersLiterally(String metacharacter) {
        String term = "a" + metacharacter + "c";
        var query = build(CriteriaCondition.like("name", "%" + term + "%"));

        var pattern = boundPattern(query);
        assertThat(pattern.matcher("prefix " + term + " suffix").matches()).isTrue();
        assertThat(pattern.matcher("prefix abc suffix").matches()).isFalse();
    }

    static Stream<String> regexMetacharacters() {
        return Stream.of(".", "^", "$", "*", "+", "?", "(", ")", "[", "]", "{", "}", "\\", "|");
    }

    @ParameterizedTest(name = "LIKE {0} matches {1}: {2}")
    @MethodSource("likeWildcards")
    void shouldPreserveSqlLikeWildcards(String like, String candidate, boolean expected) {
        var pattern = boundPattern(build(CriteriaCondition.like("name", like)));

        assertThat(pattern.matcher(candidate).matches()).isEqualTo(expected);
    }

    static Stream<Arguments> likeWildcards() {
        return Stream.of(
                arguments("a_c", "abc", true),
                arguments("a_c", "ac", false),
                arguments("a_c", "abbc", false),
                arguments("a%c", "ac", true),
                arguments("a%c", "abbc", true),
                arguments("a%c", "abd", false)
        );
    }

    @ParameterizedTest(name = "{0} matches a literal term")
    @MethodSource("literalStringPredicates")
    void shouldMatchLiteralStringPredicates(String operation, CriteriaCondition condition,
                                           String matching, String nonmatching) {
        var pattern = boundPattern(build(condition));

        assertThat(pattern.matcher(matching).matches()).isTrue();
        assertThat(pattern.matcher(nonmatching).matches()).isFalse();
    }

    static Stream<Arguments> literalStringPredicates() {
        String term = "a.c\\d%_";
        return Stream.of(
                arguments("contains", CriteriaCondition.contains(Element.of("name", term)),
                        "prefix " + term + " suffix", "prefix a.c\\dXY suffix"),
                arguments("startsWith", CriteriaCondition.startsWith(Element.of("name", term)),
                        term + " suffix", "a.c\\dXY suffix"),
                arguments("endsWith", CriteriaCondition.endsWith(Element.of("name", term)),
                        "prefix " + term, "prefix a.c\\dXY")
        );
    }

    private static OracleQuery build(CriteriaCondition condition) {
        var query = SelectQuery.builder().from("Person").where(condition).build();
        return new SelectBuilder(query, "people").get();
    }

    private static Pattern boundPattern(OracleQuery query) {
        assertThat(query.params()).hasSize(1);
        // Bound values go directly to regex_like's regex parser without SQL literal unescaping.
        return Pattern.compile(query.params().get(0).asString().getValue());
    }
}

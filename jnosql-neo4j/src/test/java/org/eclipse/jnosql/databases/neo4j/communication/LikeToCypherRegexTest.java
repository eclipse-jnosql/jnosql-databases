/*
 *
 *  Copyright (c) 2025 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 *
 */
package org.eclipse.jnosql.databases.neo4j.communication;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class LikeToCypherRegexTest {


    @ParameterizedTest(name = "LIKE \"{0}\" -> regex \"{1}\"")
    @CsvSource({
            // contains / starts / ends
            "'%Ota%',         '^.*\\QOta\\E.*$'",
            "'Ota%',          '^\\QOta\\E.*$'",
            "'%Ota',          '^.*\\QOta\\E$'",
            // exact (no wildcards)
            "'Ota',           '^\\QOta\\E$'",
            // single-char wildcard
            "'Ot_',           '^\\QOt\\E.$'",
            // mixed case with both _ and %
            "'_%ta%',         '^..*\\Qta\\E.*$'"
    })
    @DisplayName("Converts SQL LIKE to anchored Cypher regex")
    void shouldConvertSqlLikeToAnchoredRegex(String like, String expectedRegex) {
        String actual = LikeToCypherRegex.INSTANCE.toCypherRegex(like);
        assertThat(actual).isEqualTo(expectedRegex);
    }

    @Test
    @DisplayName("Escapes regex metacharacters in literals")
    void shouldEscapeRegexMetacharacters() {
        // Input contains regex metas: . ^ $ ( ) [ ] { } + ? * | \
        String like = "%a.^$()[]{}+?*|\\b%";
        String regex = LikeToCypherRegex.INSTANCE.toCypherRegex(like);

        assertThat(regex)
                .startsWith("^.*")
                .endsWith(".*$")
                // The literal run should be quoted as one block
                .contains("\\Qa.^$()[]{}+?*|\\b\\E");
    }

    @Test
    @DisplayName("Returns never-matching regex for null")
    void shouldReturnNeverMatchingForNull() {
        String regex = LikeToCypherRegex.INSTANCE.toCypherRegex(null);
        assertThat(regex).isEqualTo("(?!)");
    }

    @Test
    @DisplayName("Handles empty string as exact empty match")
    void shouldHandleEmptyString() {
        String regex = LikeToCypherRegex.INSTANCE.toCypherRegex("");
        assertThat(regex).isEqualTo("^$"); // not "^\\Q\\E$"
    }

    @Test
    @DisplayName("Handles only wildcards")
    void shouldHandleOnlyWildcards() {
        assertThat(LikeToCypherRegex.INSTANCE.toCypherRegex("%")).isEqualTo("^.*$");
        assertThat(LikeToCypherRegex.INSTANCE.toCypherRegex("%%")).isEqualTo("^.*.*$");
        assertThat(LikeToCypherRegex.INSTANCE.toCypherRegex("_")).isEqualTo("^.$");
        assertThat(LikeToCypherRegex.INSTANCE.toCypherRegex("__")).isEqualTo("^..$");
    }
}
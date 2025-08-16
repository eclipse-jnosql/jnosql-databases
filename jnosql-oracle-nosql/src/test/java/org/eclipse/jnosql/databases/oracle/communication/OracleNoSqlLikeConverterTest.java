/*
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
 */
package org.eclipse.jnosql.databases.oracle.communication;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class OracleNoSqlLikeConverterTest {


    @ParameterizedTest(name = "LIKE \"{0}\" -> pattern \"{1}\"")
    @MethodSource("cases")
    @DisplayName("Converts SQL LIKE to Oracle NoSQL regex_like pattern (no anchors)")
    void shouldConvertSqlLikeToOracleNoSqlRegex(String like, String expected) {
        String actual = OracleNoSqlLikeConverter.INSTANCE.convert(like);
        assertThat(actual).isEqualTo(expected);
    }

    static Stream<Arguments> cases() {
        return Stream.of(
                // starts / ends / contains / exact
                arguments("Lu%", "Lu.*"),
                arguments("%Lu", ".*Lu"),
                arguments("%Lu%", ".*Lu.*"),
                arguments("Lu", "Lu"),

                // single-char wildcard
                arguments("Ot_", "Ot."),
                arguments("_ta", ".ta"),

                // escaping of regex metacharacters
                arguments("%a.c%", ".*a\\.c.*"),
                arguments("100% match", "100.* match"),

                // edge cases
                arguments("", ""),          // empty LIKE -> empty pattern
                arguments("%%", ".*.*"),    // only wildcards
                arguments("__", "..")
        );
    }

    @Test
    @DisplayName("Returns empty string for null input")
    void shouldReturnEmptyForNull() {
        assertThat(OracleNoSqlLikeConverter.INSTANCE.convert(null)).isEqualTo("");
    }

    @Test
    @DisplayName("Returns empty string for empty input")
    void shouldReturnEmptyForEmptyString() {
        assertThat(OracleNoSqlLikeConverter.INSTANCE.convert("")).isEqualTo("");
    }

    @ParameterizedTest(name = "contains(\"{0}\") -> \"{1}\"")
    @MethodSource("containsCases")
    @DisplayName("contains(term) escapes meta and wraps with .* … .*")
    void shouldContains(String term, String expected) {
        String actual = OracleNoSqlLikeConverter.INSTANCE.contains(term);
        assertThat(actual).isEqualTo(expected);
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> containsCases() {
        return Stream.of(
                arguments("Lu", ".*Lu.*"),
                arguments("a.c", ".*a\\.c.*"),
                arguments("price$", ".*price\\$.*"),
                arguments("(hello)", ".*\\(hello\\).*"),
                arguments("", ".*.*")
        );
    }

    @ParameterizedTest(name = "startsWith(\"{0}\") -> \"{1}\"")
    @MethodSource("startsWithCases")
    @DisplayName("startsWith(term) escapes meta and appends .*")
    void shouldStartsWith(String term, String expected) {
        String actual = OracleNoSqlLikeConverter.INSTANCE.startsWith(term);
        assertThat(actual).isEqualTo(expected);
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> startsWithCases() {
        return Stream.of(
                arguments("Lu", "Lu.*"),
                arguments("a.c", "a\\.c.*"),
                arguments("price$", "price\\$.*"),
                arguments("(hello)", "\\(hello\\).*"),
                arguments("", ".*")
        );
    }


    @ParameterizedTest(name = "endsWith(\"{0}\") -> \"{1}\"")
    @MethodSource("endsWithCases")
    @DisplayName("endsWith(term) escapes meta and prefixes .*")
    void shouldEndsWith(String term, String expected) {
        String actual = OracleNoSqlLikeConverter.INSTANCE.endsWith(term);
        assertThat(actual).isEqualTo(expected);
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> endsWithCases() {
        return Stream.of(
                arguments("Lu", ".*Lu"),
                arguments("a.c", ".*a\\.c"),
                arguments("price$", ".*price\\$"),
                arguments("(hello)", ".*\\(hello\\)"),
                arguments("", ".*")
        );
    }

    @Test
    @DisplayName("All regex metacharacters are escaped in contains/startsWith/endsWith")
    void escapesAllMetaCharacters() {
        String term = ".^$*+?()[]{}\\|";
        // Expected escaped chunk: \.\^\$\*\+\?\(\)\[\]\{\}\\\|
        String escaped = "\\.\\^\\$\\*\\+\\?\\(\\)\\[\\]\\{\\}\\\\\\|";

        assertThat(OracleNoSqlLikeConverter.INSTANCE.contains(term))
                .isEqualTo(".*" + escaped + ".*");

        assertThat(OracleNoSqlLikeConverter.INSTANCE.startsWith(term))
                .isEqualTo(escaped + ".*");

        assertThat(OracleNoSqlLikeConverter.INSTANCE.endsWith(term))
                .isEqualTo(".*" + escaped);
    }

}
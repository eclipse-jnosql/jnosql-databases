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
package org.eclipse.jnosql.communication.driver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.*;

class StringMatchTest {


    @Test
    @DisplayName("DEFAULT should return the exact input without wildcards")
    void shouldReturnExactValueForDefault() {
        String input = "Ota";
        String result = StringMatch.DEFAULT.format(input);

        assertThat(result).isEqualTo("Ota");
    }

    @Test
    @DisplayName("CONTAINS should wrap input with % on both sides")
    void shouldWrapWithWildcardsForContains() {
        String input = "Ota";
        String result = StringMatch.CONTAINS.format(input);

        assertThat(result).isEqualTo("%Ota%");
    }

    @Test
    @DisplayName("STARTS_WITH should append % to the input")
    void shouldAppendPercentForStartsWith() {
        String input = "Ota";
        String result = StringMatch.STARTS_WITH.format(input);

        assertThat(result).isEqualTo("Ota%");
    }

    @Test
    @DisplayName("ENDS_WITH should prepend % to the input")
    void shouldPrependPercentForEndsWith() {
        String input = "Ota";
        String result = StringMatch.ENDS_WITH.format(input);

        assertThat(result).isEqualTo("%Ota");
    }

    @ParameterizedTest(name = "All strategies should reject null input: {0}")
    @EnumSource(StringMatch.class)
    @DisplayName("Null input should throw NullPointerException for every strategy")
    void shouldRejectNullValuesWithNpe(StringMatch strategy) {
        assertThatThrownBy(() -> strategy.format(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value cannot be null");
    }
}
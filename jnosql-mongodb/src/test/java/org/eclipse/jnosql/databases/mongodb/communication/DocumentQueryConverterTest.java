/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
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
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.mongodb.communication;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentQueryConverterTest {

    @ParameterizedTest
    @CsvSource(textBlock = """
                 Max_;^\\QM\\E\\Qa\\E\\Qx\\E.$
            Max%;^\\QM\\E\\Qa\\E\\Qx\\E.*$
            M_x;^\\QM\\E.\\Qx\\E$
            M%x;^\\QM\\E.*\\Qx\\E$
            _ax;^.\\Qa\\E\\Qx\\E$
            %ax;^.*\\Qa\\E\\Qx\\E$
            """, delimiterString = ";")
    void shouldPrepareRegexValueSupportedByMongoDB(String rawValue, String expectedValue) {
        assertThat(DocumentQueryConversor.prepareRegexValue(rawValue))
                .as("The value should be prepared to be used in a MongoDB regex query: " +
                        "the '_' character should matches any single character, and " +
                        "the '%' character should matches any sequence of characters.")
                .isEqualTo(expectedValue);
    }

    @Test
    void shouldReturnNeverMatchingRegexWhenRawValueIsNull() {
        assertThat(DocumentQueryConversor.prepareRegexValue(null))
                .as("should return a never-matching regex when the raw value is null")
                .isEqualTo("(?!)");
    }
}
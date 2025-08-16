
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

import java.util.Objects;


/**
 * Represents strategies for matching string values in database queries,
 * typically for SQL {@code LIKE} clauses or NoSQL regex-like searches.
 * <p>
 * Each constant defines a specific way to wrap the given value
 * with wildcard symbols ({@code %}) to produce a matching pattern.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * String pattern = StringMatch.CONTAINS.format("Ota"); // "%Ota%"
 * }</pre>
 */
public enum StringMatch {

    /**
     * Exact match.
     * <p>
     * The given value will be used as-is, without adding any wildcards.
     * For SQL, this corresponds to {@code column = 'value'}.
     * </p>
     */
    DEFAULT {
        @Override
        public String apply(String value) {
            return value;
        }
    },

    /**
     * Contains match.
     * <p>
     * The given value will be wrapped with wildcards on both sides:
     * {@code %value%}. For SQL, this corresponds to
     * {@code column LIKE '%value%'}.
     * </p>
     */
    CONTAINS {
        @Override
        public String apply(String value) {
            return "%" + value + "%";
        }
    },

    /**
     * Starts-with match.
     * <p>
     * The given value will be followed by a wildcard:
     * {@code value%}. For SQL, this corresponds to
     * {@code column LIKE 'value%'}.
     * </p>
     */
    STARTS_WITH {
        @Override
        public String apply(String value) {
            return value + "%";
        }
    },

    /**
     * Ends-with match.
     * <p>
     * The given value will be preceded by a wildcard:
     * {@code %value}. For SQL, this corresponds to
     * {@code column LIKE '%value'}.
     * </p>
     */
    ENDS_WITH {
        @Override
        public String apply(String value) {
            return "%" + value;
        }
    };

    /**
     * Applies the match strategy to the given value, producing a pattern string.
     *
     * @param value the value to be transformed into a pattern
     * @return the pattern string, with wildcards applied according to the match strategy
     */
    abstract String apply(String value);

    /**
     * Formats the given value by applying the match strategy.
     * <p>
     * This method ensures the value is not {@code null} before applying the strategy.
     * </p>
     *
     * @param value the value to be transformed into a pattern
     * @return the pattern string, with wildcards applied according to the match strategy
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public String format(String value) {
        Objects.requireNonNull(value, "value cannot be null");
        return apply(value);
    }

}
/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License 2.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *   and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.databases.tinkerpop.communication;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;

/**
 * Rejects every predicate that has a {@link CriteriaOperand.Function} operand.
 * <p>
 * No standard function can be translated with the same semantics as the JNoSQL function contract:
 * <ul>
 *     <li>{@code LOWER} and {@code UPPER} map to the {@code toLower()} and {@code toUpper()} steps, which use the
 *     default locale of the JVM instead of {@code Locale.ROOT}, so the result changes with the host locale
 *     (for example, a Turkish locale maps {@code "i"} to {@code "İ"});</li>
 *     <li>{@code LENGTH}, {@code LEFT} and {@code RIGHT} map only to the {@code length()} and
 *     {@code substring()} steps, which count UTF-16 units instead of code points;</li>
 *     <li>{@code ABS} has only the {@code math()} step, which converts to a double and loses precision.</li>
 * </ul>
 * No lambda, script or client-side filtering is used as a replacement.
 */
final class TinkerpopNativeFunction {

    private static final String DATABASE = "TinkerPop";

    private TinkerpopNativeFunction() {
    }

    /**
     * Rejects a native predicate.
     *
     * @param condition the native condition
     * @return never returns normally
     * @throws UnsupportedOperationException always, before the graph is touched
     */
    static GraphTraversal<Vertex, Vertex> convert(CriteriaCondition condition) {
        throw new UnsupportedOperationException("The " + DATABASE + " driver does not support native functions: "
                + "the Gremlin steps do not follow the JNoSQL function contract (default-locale case mapping, "
                + "UTF-16 length and substring, double-based math)");
    }
}

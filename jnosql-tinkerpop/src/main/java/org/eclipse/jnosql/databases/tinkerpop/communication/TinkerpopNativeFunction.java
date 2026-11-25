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

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.function.FunctionArgument;
import org.eclipse.jnosql.communication.function.NativeFunction;
import org.eclipse.jnosql.communication.function.StandardFunction;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.CriteriaOperand;

import java.util.List;
import java.util.function.Function;

/**
 * Translates a predicate that has a {@link CriteriaOperand.Function} operand into portable Gremlin steps.
 * <p>
 * Only {@code LOWER} and {@code UPPER} applied to a stored property, directly or nested, compared with a
 * constant are translated, as the {@code toLower()} and {@code toUpper()} steps of TinkerPop 3.8 followed by
 * {@code is(P)}. Property values are read with {@code values()}, so a vertex without the property never matches.
 * <p>
 * The other functions are rejected: {@code LENGTH}, {@code LEFT} and {@code RIGHT} map only to the
 * {@code length()} and {@code substring()} steps, which count UTF-16 units instead of code points, and
 * {@code ABS} has only the {@code math()} step, which converts to a double and loses precision. No lambda,
 * script or client-side filtering is used. Steps added in TinkerPop 3.8 are required of the provider.
 */
final class TinkerpopNativeFunction {

    private static final String DATABASE = "TinkerPop";

    private TinkerpopNativeFunction() {
    }

    /**
     * Converts a native predicate.
     *
     * @param condition the native condition
     * @return the traversal that filters a vertex by the predicate
     * @throws UnsupportedOperationException when the predicate or a function cannot be translated
     */
    @SuppressWarnings("unchecked")
    static GraphTraversal<Vertex, Vertex> convert(CriteriaCondition condition) {
        Condition type = condition.condition();
        if (!(condition.leftOperand() instanceof CriteriaOperand.Function function)
                || !(condition.rightOperand() instanceof CriteriaOperand.Constant constant)) {
            throw new UnsupportedOperationException("The " + DATABASE + " driver supports a native function only "
                    + "as the left operand compared with a constant");
        }
        var traversal = values(function.function());
        Object value = ValueUtil.convert(constant.value());
        if (value == null) {
            throw new UnsupportedOperationException("The " + DATABASE + " driver does not support comparing a "
                    + "native function with null");
        }
        P<Object> predicate = switch (type) {
            case EQUALS -> P.eq(value);
            case GREATER_THAN -> P.gt(value);
            case GREATER_EQUALS_THAN -> P.gte(value);
            case LESSER_THAN -> P.lt(value);
            case LESSER_EQUALS_THAN -> P.lte(value);
            case IN -> P.within(ValueUtil.convertToList(constant.value()));
            case BETWEEN -> between(ValueUtil.convertToList(constant.value()));
            default -> throw new UnsupportedOperationException("The " + DATABASE + " driver does not support the "
                    + type + " condition with a native function operand");
        };
        // the traversal is used inside filter(), not, and and or, which only test whether it emits a value
        return (GraphTraversal<Vertex, Vertex>) (GraphTraversal<?, ?>) traversal.is(predicate);
    }

    private static P<Object> between(List<Object> values) {
        if (values.size() != 2) {
            throw new IllegalStateException("The between condition requires two parameters");
        }
        // P.between excludes the upper bound while BETWEEN is inclusive
        return P.gte(values.get(0)).and(P.lte(values.get(1)));
    }

    @SuppressWarnings("unchecked")
    private static GraphTraversal<Vertex, Object> values(NativeFunction function) {
        var standard = StandardFunction.find(function.name()).orElseThrow(() -> new UnsupportedOperationException(
                "The " + DATABASE + " driver does not support the native function " + function.name()));
        Function<GraphTraversal<Vertex, Object>, GraphTraversal<Vertex, Object>> step = switch (standard) {
            case LOWER -> traversal -> (GraphTraversal<Vertex, Object>) (GraphTraversal<?, ?>) traversal.toLower();
            case UPPER -> traversal -> (GraphTraversal<Vertex, Object>) (GraphTraversal<?, ?>) traversal.toUpper();
            default -> throw new UnsupportedOperationException("The " + DATABASE + " driver does not support native "
                    + standard.name() + ": the Gremlin step does not follow the function contract");
        };
        return step.apply(argument(standard, function.arguments().get(0)));
    }

    @SuppressWarnings("unchecked")
    private static GraphTraversal<Vertex, Object> argument(StandardFunction function, FunctionArgument argument) {
        return switch (argument) {
            case FunctionArgument.AttributePath path -> (GraphTraversal<Vertex, Object>) (GraphTraversal<?, ?>)
                    __.<Vertex>start().values(path.path());
            case FunctionArgument.NativeArgument nested -> values(nested.function());
            default -> throw new UnsupportedOperationException("The " + DATABASE + " driver supports only a stored "
                    + "property or a native function as the argument of " + function.name());
        };
    }
}

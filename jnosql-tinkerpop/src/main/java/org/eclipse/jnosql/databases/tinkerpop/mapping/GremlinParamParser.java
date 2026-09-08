/*
 *  Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.tinkerpop.mapping;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This singleton replaces JNoSQL parameters with Gremlin binding variables.
 * Thus, given the query:
 * "g.V().hasLabel(@param)" where the params is {"param":"Otavio"}
 * it returns a query such as g.V().hasLabel(jnosqlParam0), with "Otavio"
 * supplied separately through the script engine bindings.
 */
enum GremlinParamParser {
    INSTANCE;

    private final Pattern pattern = Pattern.compile("@\\w+");

    ParsedQuery parse(String query, Map<String, Object> params) {
        Objects.requireNonNull(query, "query is required");
        Objects.requireNonNull(params, "params is required");
        Matcher matcher = pattern.matcher(query);
        Map<String, Object> leftParams = new HashMap<>(params);
        Map<String, String> variables = new HashMap<>();
        Map<String, Object> bindings = new LinkedHashMap<>();
        StringBuilder gremlin = new StringBuilder();
        while (matcher.find()) {
            String param = matcher.group().substring(1);
            Object value = params.get(param);
            if (value == null) {
                throw new GremlinQueryException("The param is " + param + " is required on the query " + query);
            }
            leftParams.remove(param);
            String variable = variables.computeIfAbsent(param,
                    key -> nextVariable(query, bindings));
            bindings.put(variable, value);
            matcher.appendReplacement(gremlin, variable);
        }
        matcher.appendTail(gremlin);
        if (leftParams.isEmpty()) {
            return new ParsedQuery(gremlin.toString(), Map.copyOf(bindings));
        }

        throw new GremlinQueryException("There are params missing on the parser: " + leftParams.keySet()
                + " on the query" + query);
    }

    private String nextVariable(String query, Map<String, Object> bindings) {
        int index = bindings.size();
        String variable = "jnosqlParam" + index;
        while (query.contains(variable) || bindings.containsKey(variable)) {
            variable = "jnosqlParam" + ++index;
        }
        return variable;
    }

    record ParsedQuery(String query, Map<String, Object> bindings) {
    }
}

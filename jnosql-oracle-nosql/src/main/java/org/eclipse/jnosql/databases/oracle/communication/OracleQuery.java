/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.oracle.communication;

import oracle.nosql.driver.values.FieldValue;

import java.util.Collections;
import java.util.List;

record OracleQuery(String query, List<FieldValue> params, List<String> ids) {

    @Override
    public List<String> ids() {
        return Collections.unmodifiableList(ids);
    }

    public boolean isParameterEmpty() {
        return this.params.isEmpty();
    }

    public boolean hasOnlyIds() {
        return hasIds() && isParameterEmpty();
    }

    public boolean hasIds() {
        return !this.ids.isEmpty();
    }

    @Override
    public String toString() {
        return "OracleQuery{" +
                "query='" + query + '\'' +
                ", params=" + params +
                ", ids=" + ids +
                '}';
    }

    static OracleQuery of(StringBuilder query, List<FieldValue> params, List<String> ids) {
        return new OracleQuery(query.toString(), params, ids);
    }
}

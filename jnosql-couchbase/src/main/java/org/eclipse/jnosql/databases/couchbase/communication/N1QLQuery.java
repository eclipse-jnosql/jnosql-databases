/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.couchbase.communication;

import com.couchbase.client.java.json.JsonObject;

import java.util.Collections;
import java.util.List;

record N1QLQuery(String query, JsonObject params, List<String> ids) {

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
        return "N1QLQuery{" +
                "query='" + query + '\'' +
                ", params=" + params +
                ", ids=" + ids +
                '}';
    }

    static N1QLQuery of(StringBuilder query, JsonObject params, List<String> ids) {
        return new N1QLQuery(query.toString(), params, ids);
    }
}

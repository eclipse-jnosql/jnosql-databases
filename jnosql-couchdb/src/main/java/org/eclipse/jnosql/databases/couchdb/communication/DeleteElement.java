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
package org.eclipse.jnosql.databases.couchdb.communication;

import java.util.Map;
import java.util.Objects;

final class DeleteElement {

    private final String id;

    private final String rev;

    DeleteElement(Map<String, Object> json) {
        this.id = json.get(CouchDBConstant.ID).toString();
        this.rev = json.get(CouchDBConstant.REV).toString();
    }

    public String getId() {
        return id;
    }

    public String getRev() {
        return rev;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeleteElement that = (DeleteElement) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(rev, that.rev);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, rev);
    }

    @Override
    public String toString() {
        return "DeleteElement{" + "id='" + id + '\'' +
                ", rev='" + rev + '\'' +
                '}';
    }
}

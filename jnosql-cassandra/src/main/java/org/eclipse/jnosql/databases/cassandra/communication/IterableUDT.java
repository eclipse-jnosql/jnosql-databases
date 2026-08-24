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
package org.eclipse.jnosql.databases.cassandra.communication;

import org.eclipse.jnosql.communication.TypeSupplier;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.semistructured.Element;

import java.util.Objects;

/**
 * On Cassandra, there is the option to a UDT be part of a list. This implementation holds this option.
 */
class IterableUDT implements UDT {

    private final String name;

    private final String userType;

    private final Iterable<Iterable<Element>> columns;

    IterableUDT(String name, String userType, Iterable<Iterable<Element>> columns) {
        this.name = name;
        this.userType = userType;
        this.columns = columns;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Value value() {
        return Value.of(columns);
    }

    @Override
    public <T> T get(Class<T> clazz) throws NullPointerException, UnsupportedOperationException {
        Objects.requireNonNull(clazz, "clazz is required");
        return Value.of(columns).get(clazz);
    }

    @Override
    public <T> T get(TypeSupplier<T> typeSupplier) throws NullPointerException, UnsupportedOperationException {
        Objects.requireNonNull(typeSupplier, "typeSupplier is required");
        return Value.of(columns).get(typeSupplier);

    }

    @Override
    public Object get() {
        return columns;
    }

    @Override
    public String userType() {
        return userType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UDT udt)) {
            return false;
        }
        return Objects.equals(name, udt.name()) &&
                Objects.equals(userType, udt.userType()) &&
                Objects.equals(columns, udt.get());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, userType, columns);
    }

    @Override
    public String toString() {
        return "IterableUDT{" +
                "name='" + name + '\'' +
                ", userType='" + userType + '\'' +
                ", columns=" + columns +
                '}';
    }
}

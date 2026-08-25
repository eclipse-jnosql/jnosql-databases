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


import org.eclipse.jnosql.communication.semistructured.Element;

/**
 * Represents a user-defined type (UDT) in Cassandra. This interface does not support
 * the {@code getValue(alias)} method, which includes both the {@code get(Class)} and {@code get(TypeSupplier)} aliases.
 */
public interface UDT extends Element {

    /**
     * Retrieves the name of the user-defined type (UDT).
     *
     * @return the name of the UDT
     */
    String userType();

    /**
     * Creates a builder for constructing instances of the specified user-defined type (UDT).
     *
     * @param userType the name of the user-defined type
     * @return the {@link UDTBuilder} instance for the specified UDT
     * @throws NullPointerException if {@code userType} is {@code null}
     */
    static UDTNameBuilder builder(String userType) throws NullPointerException {
        return new UDTBuilder(userType);
    }
}
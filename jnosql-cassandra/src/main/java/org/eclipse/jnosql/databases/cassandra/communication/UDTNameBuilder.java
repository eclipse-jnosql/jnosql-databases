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

/**
 * A builder interface for creating a user-defined type (UDT) element with the specified name.
 */
public interface UDTNameBuilder {

    /**
     * Specifies the name of the UDT element.
     *
     * @param name the name of the UDT element
     * @return the {@link UDTElementBuilder} instance to continue building the UDT
     * @throws NullPointerException if {@code name} is {@code null}
     */
    UDTElementBuilder withName(String name) throws NullPointerException;
}

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
 * A builder interface for constructing Cassandra User-Defined Types (UDTs).
 */
public interface UDTElementBuilder {

    /**
     * Adds a single UDT element to the builder.
     *
     * @param udt the UDT element to be added
     * @return the builder instance
     * @throws NullPointerException if the udt or any of its elements is null
     */
    UDTFinisherBuilder addUDT(Iterable<Element> udt) throws NullPointerException;

    /**
     * Adds multiple UDTs to the builder, typically used when a UDT is part of a list.
     * For example:
     * <pre>
     *     CREATE COLUMNFAMILY IF NOT EXISTS contacts (
     *         user text PRIMARY KEY,
     *         names list&lt;frozen &lt;fullname&gt;&gt;
     *     );
     * </pre>
     *
     * @param udts the UDTs to be added
     * @return the builder instance
     * @throws NullPointerException if any of the udts or their elements is null
     */
    UDTFinisherBuilder addUDTs(Iterable<Iterable<Element>> udts) throws NullPointerException;
}
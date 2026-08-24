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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

/**
 * The UDT builder
 */
class UDTBuilder implements UDTNameBuilder, UDTElementBuilder, UDTFinisherBuilder {

    private String name;

    private final String typeName;

    private final List<Element> columns = new ArrayList<>();

    private Iterable<Iterable<Element>> udts = new ArrayList<>();


    UDTBuilder(String userType) {
        this.typeName = userType;
    }

    @Override
    public UDTBuilder withName(String name) throws NullPointerException {
        this.name = Objects.requireNonNull(name, "name is required");
        return this;
    }


    @Override
    public UDTBuilder addUDT(Iterable<Element> udt) throws NullPointerException {
        Objects.requireNonNull(udt, "udt is required");
        StreamSupport.stream(udt.spliterator(), false).forEach(this.columns::add);
        return this;
    }

    @Override
    public UDTBuilder addUDTs(Iterable<Iterable<Element>> udts) throws NullPointerException {
        Objects.requireNonNull(udts, "udts is required");
        this.udts = udts;
        return this;
    }

    @Override
    public UDT build() throws IllegalStateException {
        if (Objects.isNull(name)) {
            throw new IllegalStateException("name is required");
        }
        if (Objects.isNull(typeName)) {
            throw new IllegalStateException("typeName is required");
        }

        if (udts.spliterator().getExactSizeIfKnown() == 0) {
            return new DefaultUDT(name, typeName, columns);
        } else {
            return new IterableUDT(name, typeName, udts);
        }
    }


}


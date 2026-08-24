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
package org.eclipse.jnosql.databases.cassandra.mapping;

import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.databases.cassandra.communication.UDT;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.metadata.FieldMetadata;
import org.eclipse.jnosql.mapping.semistructured.AttributeFieldValue;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static java.util.stream.StreamSupport.stream;

class CassandraUDTType implements AttributeFieldValue {

    private final String type;

    private final Object value;

    private final FieldMetadata field;


    CassandraUDTType(String type, Object value, FieldMetadata field) {
        this.value = value;
        this.type = type;
        this.field = Objects.requireNonNull(field, "field is required");
    }

    @Override
    public Object value() {
        return value;
    }

    @Override
    public FieldMetadata field() {
        return field;
    }

    @Override
    public boolean isNotEmpty() {
        return value != null;
    }

    @Override
    public List<Element> toElements(EntityConverter converter, Converters converters) {
        if (value == null) {
            return singletonList(Element.of(field.name(), null));
        } else if (Iterable.class.isInstance(value)) {
            List<Iterable<Element>> columns = new ArrayList<>();
            stream(Iterable.class.cast(value).spliterator(), false)
                    .forEach(c -> columns.add(converter.toCommunication(c).elements()));
            return singletonList(UDT.builder(type).withName(field.name()).addUDTs(columns).build());

        } else {
            return singletonList(UDT.builder(type)
                    .withName(field.name())
                    .addUDT(converter.toCommunication(value).elements())
                    .build());
        }
    }

    @Override
    public String toString() {
        return "CassandraUDTType{" +
                "type='" + type + '\'' +
                ", value=" + value +
                ", field=" + field +
                '}';
    }
}

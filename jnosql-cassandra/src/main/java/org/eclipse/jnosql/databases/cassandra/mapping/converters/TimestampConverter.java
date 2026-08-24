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
package org.eclipse.jnosql.databases.cassandra.mapping.converters;


import jakarta.nosql.AttributeConverter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

/**
 * The converter when the Cassandra type is timestamp.
 * This attribute converter has support to:
 * <p>{@link Number}</p>
 * <p>{@link java.time.LocalDate}</p>
 * <p>{@link LocalDateTime}</p>
 * <p>{@link ZonedDateTime}</p>
 * <p>{@link Date}</p>
 * <p>{@link Calendar}</p>
 */
public class TimestampConverter implements AttributeConverter<Object, Date> {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    @Override
    public Date convertToDatabaseColumn(Object attribute) {
        if (attribute == null) {
            return null;
        }

        if (Number.class.isInstance(attribute)) {
            return new Date(Number.class.cast(attribute).longValue());
        }
        if (java.time.LocalDate.class.isInstance(attribute)) {
            return Date.from(java.time.LocalDate.class.cast(attribute).atStartOfDay(ZONE_ID).toInstant());
        }
        if (LocalDateTime.class.isInstance(attribute)) {
            return Date.from(LocalDateTime.class.cast(attribute).atZone(ZONE_ID).toInstant());
        }
        if (ZonedDateTime.class.isInstance(attribute)) {
            return Date.from(ZonedDateTime.class.cast(attribute).toInstant());
        }
        if (Date.class.isInstance(attribute)) {
            return Date.class.cast(attribute);
        }
        if (Calendar.class.isInstance(attribute)) {
            return Calendar.class.cast(attribute).getTime();
        }
        throw new IllegalArgumentException("There is not support to: " + attribute.getClass());
    }

    @Override
    public Object convertToEntityAttribute(Date dbData) {
        return dbData.getTime();
    }
}

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
 * Converts an entity attribute to a {@link Date} for a Cassandra {@code timestamp} column.
 * The attribute can be a {@link Number} (epoch milliseconds), {@link java.time.LocalDate} (start of day),
 * {@link LocalDateTime}, {@link ZonedDateTime}, {@link Date} or {@link Calendar}. Local dates and date-times are
 * interpreted in the JVM default time zone. When reading, the attribute receives the epoch milliseconds as a
 * {@code long}.
 *
 * {@snippet lang="java" :
 * @Column
 * @Convert(TimestampConverter.class)
 * private Object createdAt;
 * }
 */
public class TimestampConverter implements AttributeConverter<Object, Date> {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    /**
     * Converts the attribute to a {@link Date}.
     *
     * @param attribute a supported date or time value, or {@code null}
     * @return the {@link Date}, or {@code null} when {@code attribute} is {@code null}
     * @throws IllegalArgumentException when the attribute type is not supported
     */
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

    /**
     * Converts the stored timestamp to epoch milliseconds.
     *
     * @param dbData the stored {@link Date}
     * @return the epoch milliseconds as a {@link Long}
     * @throws NullPointerException when {@code dbData} is {@code null}
     */
    @Override
    public Object convertToEntityAttribute(Date dbData) {
        return dbData.getTime();
    }
}

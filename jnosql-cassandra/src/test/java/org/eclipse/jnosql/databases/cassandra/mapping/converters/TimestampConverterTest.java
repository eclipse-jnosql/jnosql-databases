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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimestampConverterTest {

    private ZoneId defaultZoneId = ZoneId.systemDefault();

    private AttributeConverter<Object, Date> converter;

    @BeforeEach
    public void setUp() {
        converter = new TimestampConverter();
    }

    @Test
    public void shouldConvertoNumber() {
        Date date = new Date();
        date.setHours(0);
        date.setMinutes(0);
        date.setSeconds(0);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        Number number = date.getTime();
        java.time.LocalDate localDate = converter.convertToDatabaseColumn(number).toInstant()
                .atZone(defaultZoneId).toLocalDate();

        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), localDate.getDayOfMonth());
        assertEquals(calendar.get(Calendar.YEAR), localDate.getYear());
        assertEquals(calendar.get(Calendar.MONTH) + 1, localDate.getMonthValue());
    }

    @Test
    public void shouldConvertoDate() {
        Date date = new Date();
        date.setHours(0);
        date.setMinutes(0);
        date.setSeconds(0);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        java.time.LocalDate localDate = converter.convertToDatabaseColumn(date).toInstant()
                .atZone(defaultZoneId).toLocalDate();
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), localDate.getDayOfMonth());
        assertEquals(calendar.get(Calendar.YEAR), localDate.getYear());
        assertEquals(calendar.get(Calendar.MONTH) + 1, localDate.getMonthValue());
    }

    @Test
    public void shouldConvertoCalendar() {

        Calendar calendar = Calendar.getInstance();
        java.time.LocalDate localDate = converter.convertToDatabaseColumn(calendar).toInstant()
                .atZone(defaultZoneId).toLocalDate();
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), localDate.getDayOfMonth());
        assertEquals(calendar.get(Calendar.YEAR), localDate.getYear());
        assertEquals(calendar.get(Calendar.MONTH) + 1, localDate.getMonthValue());
    }

    @Test
    public void shouldConvertoLocalDate() {

        java.time.LocalDate date = java.time.LocalDate.now();
        java.time.LocalDate localDate = converter.convertToDatabaseColumn(date).toInstant()
                .atZone(defaultZoneId).toLocalDate();
        assertEquals(date.getDayOfMonth(), localDate.getDayOfMonth());
        assertEquals(date.getYear(), localDate.getYear());
        assertEquals(date.getMonthValue(), localDate.getMonthValue());
    }


    @Test
    public void shouldConvertLocalDateTime() {

        LocalDateTime date = LocalDateTime.now();
        java.time.LocalDate localDate = converter.convertToDatabaseColumn(date).toInstant()
                .atZone(defaultZoneId).toLocalDate();
        assertEquals(date.getDayOfMonth(), localDate.getDayOfMonth());
        assertEquals(date.getYear(), localDate.getYear());
        assertEquals(date.getMonthValue(), localDate.getMonthValue());
    }

    @Test
    public void shouldConvertZonedDateTime() {

        ZonedDateTime date = ZonedDateTime.now();
        java.time.LocalDate localDate = converter.convertToDatabaseColumn(date).toInstant()
                .atZone(defaultZoneId).toLocalDate();
        assertEquals(date.getDayOfMonth(), localDate.getDayOfMonth());
        assertEquals(date.getYear(), localDate.getYear());
        assertEquals(date.getMonthValue(), localDate.getMonthValue());
    }
}
/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
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

import static org.assertj.core.api.Assertions.assertThat;


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

        assertThat(localDate.getDayOfMonth()).isEqualTo(calendar.get(Calendar.DAY_OF_MONTH));
        assertThat(localDate.getYear()).isEqualTo(calendar.get(Calendar.YEAR));
        assertThat(localDate.getMonthValue()).isEqualTo(calendar.get(Calendar.MONTH) + 1);
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
        assertThat(localDate.getDayOfMonth()).isEqualTo(calendar.get(Calendar.DAY_OF_MONTH));
        assertThat(localDate.getYear()).isEqualTo(calendar.get(Calendar.YEAR));
        assertThat(localDate.getMonthValue()).isEqualTo(calendar.get(Calendar.MONTH) + 1);
    }

    @Test
    public void shouldConvertoCalendar() {

        Calendar calendar = Calendar.getInstance();
        java.time.LocalDate localDate = converter.convertToDatabaseColumn(calendar).toInstant()
                .atZone(defaultZoneId).toLocalDate();
        assertThat(localDate.getDayOfMonth()).isEqualTo(calendar.get(Calendar.DAY_OF_MONTH));
        assertThat(localDate.getYear()).isEqualTo(calendar.get(Calendar.YEAR));
        assertThat(localDate.getMonthValue()).isEqualTo(calendar.get(Calendar.MONTH) + 1);
    }

    @Test
    public void shouldConvertoLocalDate() {

        java.time.LocalDate date = java.time.LocalDate.now();
        java.time.LocalDate localDate = converter.convertToDatabaseColumn(date).toInstant()
                .atZone(defaultZoneId).toLocalDate();
        assertThat(localDate.getDayOfMonth()).isEqualTo(date.getDayOfMonth());
        assertThat(localDate.getYear()).isEqualTo(date.getYear());
        assertThat(localDate.getMonthValue()).isEqualTo(date.getMonthValue());
    }


    @Test
    public void shouldConvertLocalDateTime() {

        LocalDateTime date = LocalDateTime.now();
        java.time.LocalDate localDate = converter.convertToDatabaseColumn(date).toInstant()
                .atZone(defaultZoneId).toLocalDate();
        assertThat(localDate.getDayOfMonth()).isEqualTo(date.getDayOfMonth());
        assertThat(localDate.getYear()).isEqualTo(date.getYear());
        assertThat(localDate.getMonthValue()).isEqualTo(date.getMonthValue());
    }

    @Test
    public void shouldConvertZonedDateTime() {

        ZonedDateTime date = ZonedDateTime.now();
        java.time.LocalDate localDate = converter.convertToDatabaseColumn(date).toInstant()
                .atZone(defaultZoneId).toLocalDate();
        assertThat(localDate.getDayOfMonth()).isEqualTo(date.getDayOfMonth());
        assertThat(localDate.getYear()).isEqualTo(date.getYear());
        assertThat(localDate.getMonthValue()).isEqualTo(date.getMonthValue());
    }
}
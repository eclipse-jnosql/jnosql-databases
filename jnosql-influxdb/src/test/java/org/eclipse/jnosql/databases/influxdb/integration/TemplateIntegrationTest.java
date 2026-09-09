/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License 2.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *   and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 *   You may elect to redistribute this code under either of these licenses.
 */
package org.eclipse.jnosql.databases.influxdb.integration;

import jakarta.inject.Inject;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.databases.influxdb.communication.InfluxDBDatabase;
import org.eclipse.jnosql.databases.influxdb.communication.InfluxDBTimeSeriesConfiguration;
import org.eclipse.jnosql.databases.influxdb.communication.InfluxDBTimeSeriesConfigurations;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.core.config.MappingConfigurations;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.timeseries.TimeSeriesTemplate;
import org.eclipse.jnosql.mapping.timeseries.spi.TimeSeriesExtension;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, TimeSeriesTemplate.class})
@AddPackages(Temperature.class)
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, TimeSeriesExtension.class})
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class TemplateIntegrationTest {

    static {
        configure();
    }

    @Inject
    private TimeSeriesTemplate template;

    @Test
    void shouldInsertAndFindById() {
        Temperature temperature = temperature(Instant.now(), "Lisbon", 21.5D);

        assertThat(template.insert(temperature)).isEqualTo(temperature);
        assertThat(template.find(Temperature.class, temperature.time())).contains(temperature);
    }

    @Test
    void shouldSelectWithOrderAndLimit() {
        String location = UUID.randomUUID().toString();
        Instant first = Instant.now().minusSeconds(2);
        Instant second = first.plusSeconds(1);
        template.insert(List.of(
                temperature(first, location, 21.5D),
                temperature(second, location, 22.5D)));

        SelectQuery query = SelectQuery.select()
                .from("temperature_mapping")
                .where("location").eq(location)
                .orderBy("_id").desc()
                .limit(1)
                .build();

        assertThat(template.<Temperature>select(query)).singleElement()
                .isEqualTo(temperature(second, location, 22.5D));
    }

    private static Temperature temperature(Instant time, String location, double value) {
        return new Temperature(time, location, value);
    }

    private static void configure() {
        InfluxDBDatabase database = InfluxDBDatabase.INSTANCE;
        System.setProperty(InfluxDBTimeSeriesConfigurations.URL.get(), database.url());
        System.setProperty(InfluxDBTimeSeriesConfigurations.TOKEN.get(), database.token());
        System.setProperty(MappingConfigurations.TIME_SERIES_PROVIDER.get(),
                InfluxDBTimeSeriesConfiguration.class.getName());
        System.setProperty(MappingConfigurations.TIME_SERIES_DATABASE.get(), InfluxDBDatabase.DATABASE);
    }
}

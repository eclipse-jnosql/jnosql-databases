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
package org.eclipse.jnosql.databases.iotdb.integration;

import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.iotdb.communication.IoTDBDatabase;
import org.eclipse.jnosql.databases.iotdb.communication.IoTDBTimeSeriesConfiguration;
import org.eclipse.jnosql.databases.iotdb.communication.IoTDBTimeSeriesConfigurations;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.DatabaseType;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, TimeSeriesTemplate.class})
@AddPackages(SensorReading.class)
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, TimeSeriesExtension.class})
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class RepositoryIntegrationTest {

    static {
        configure();
    }

    @Inject
    @Database(DatabaseType.TIME_SERIES)
    private SensorReadingRepository repository;

    @Test
    void shouldInsertFindAndRunDerivedQuery() {
        String sensor = UUID.randomUUID().toString();
        SensorReading first = new SensorReading(
                Instant.now().minusSeconds(1).truncatedTo(ChronoUnit.MILLIS), sensor, 21.5D);
        SensorReading second = new SensorReading(first.timestamp().plusSeconds(1), sensor, 22.5D);

        repository.insertAll(List.of(first, second));

        assertThat(repository.findById(first.timestamp())).contains(first);
        assertThat(repository.findBySensor(sensor)).containsExactlyInAnyOrder(first, second);
    }

    private static void configure() {
        IoTDBDatabase database = IoTDBDatabase.INSTANCE;
        System.setProperty(IoTDBTimeSeriesConfigurations.HOST.get(), database.host());
        System.setProperty(IoTDBTimeSeriesConfigurations.PORT.get(), Integer.toString(database.port()));
        System.setProperty(IoTDBTimeSeriesConfigurations.ENABLE_REDIRECTION.get(), "false");
        System.setProperty(MappingConfigurations.TIME_SERIES_PROVIDER.get(),
                IoTDBTimeSeriesConfiguration.class.getName());
        System.setProperty(MappingConfigurations.TIME_SERIES_DATABASE.get(), IoTDBDatabase.DATABASE);
    }
}

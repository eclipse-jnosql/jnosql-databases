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
package org.eclipse.jnosql.databases.questdb.integration;

import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.questdb.communication.QuestDBDatabase;
import org.eclipse.jnosql.databases.questdb.communication.QuestDBTimeSeriesConfiguration;
import org.eclipse.jnosql.databases.questdb.communication.QuestDBTimeSeriesConfigurations;
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
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

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
class TemplateIntegrationTest {

    static {
        configure();
    }

    @Inject
    private TimeSeriesTemplate template;

    @Test
    void shouldUseStandardTimeSeriesEntityWithoutQuestDBAnnotations() {
        SensorReading reading = new SensorReading(
                Instant.now().truncatedTo(ChronoUnit.MICROS), UUID.randomUUID().toString(), 21.5D);

        assertThat(template.insert(reading)).isEqualTo(reading);
        awaitFind(reading);
    }

    private void awaitFind(SensorReading reading) {
        long deadline = System.currentTimeMillis() + 10_000L;
        while (template.find(SensorReading.class, reading.timestamp()).isEmpty()) {
            if (System.currentTimeMillis() >= deadline) {
                throw new AssertionError("Timed out waiting for QuestDB row");
            }
            LockSupport.parkNanos(10_000_000L);
        }
        assertThat(template.find(SensorReading.class, reading.timestamp())).contains(reading);
    }

    private static void configure() {
        QuestDBDatabase database = QuestDBDatabase.INSTANCE;
        System.setProperty(QuestDBTimeSeriesConfigurations.URL.get(), database.url());
        System.setProperty(MappingConfigurations.TIME_SERIES_PROVIDER.get(),
                QuestDBTimeSeriesConfiguration.class.getName());
        System.setProperty(MappingConfigurations.TIME_SERIES_DATABASE.get(), QuestDBDatabase.DATABASE);
    }
}

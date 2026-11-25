/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.iotdb.function;

import jakarta.inject.Inject;
import org.apache.iotdb.isession.ITableSession;
import org.apache.iotdb.isession.SessionDataSet;
import org.eclipse.jnosql.databases.iotdb.communication.MockedIoTDBConfiguration;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, TimeSeriesTemplate.class})
@AddPackages(MockedSensorReading.class)
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, TimeSeriesExtension.class})
@DisplayName("IoTDB native functions through JDQL, mapping and a mocked client")
class NativeFunctionMappingTest {

    static {
        System.setProperty(MappingConfigurations.TIME_SERIES_PROVIDER.get(), MockedIoTDBConfiguration.class.getName());
        System.setProperty(MappingConfigurations.TIME_SERIES_DATABASE.get(), "metrics");
    }

    @Inject
    private TimeSeriesTemplate template;

    private ITableSession session;

    @BeforeEach
    void setUp() throws Exception {
        Mockito.reset(MockedIoTDBConfiguration.CLIENT);
        session = mock(ITableSession.class);
        var dataSet = mock(SessionDataSet.class);
        when(dataSet.getColumnNames()).thenReturn(List.of());
        when(dataSet.getColumnTypes()).thenReturn(List.of());
        when(dataSet.hasNext()).thenReturn(false);
        when(session.executeQueryStatement(org.mockito.ArgumentMatchers.anyString())).thenReturn(dataSet);
        when(MockedIoTDBConfiguration.CLIENT.getSession()).thenReturn(session);
    }


    @Test
    @DisplayName("Native functions stay structured and use mapped names")
    void shouldTranslateNativeFunctions() throws Exception {
        template.query("FROM MockedSensorReading WHERE UPPER(name) = UPPER(:name) AND name = UPPER(otherName)")
                .bind("name", "ota").result();

        assertThat(sqls(1).get(0)).isEqualTo("SELECT * FROM \"sensor_reading\" WHERE "
                + "((upper(\"sensor_name\") = 'OTA') AND (\"sensor_name\" = upper(\"other_name\")))");
    }

    @Test
    @DisplayName("Partial evaluation inlines the ordinary length and nested functions are preserved")
    void shouldEvaluateLengthBeforeTranslation() throws Exception {
        template.query("FROM MockedSensorReading WHERE LOWER(name) = LOWER(LEFT(:prefix, LENGTH(:prefix)))")
                .bind("prefix", "Ota").result();
        template.query("FROM MockedSensorReading WHERE LOWER(UPPER(name)) = 'ota'").result();

        var statements = sqls(2);
        assertThat(statements.get(0)).isEqualTo("SELECT * FROM \"sensor_reading\" WHERE (lower(\"sensor_name\") = 'ota')");
        assertThat(statements.get(1)).isEqualTo(
                "SELECT * FROM \"sensor_reading\" WHERE (lower(upper(\"sensor_name\")) = 'ota')");
    }

    @Test
    @DisplayName("An evaluable function is an ordinary value, rebound on every execution")
    void shouldKeepEvaluableFunctionsTransparent() throws Exception {
        var statement = template.query("FROM MockedSensorReading WHERE name = UPPER(LEFT(:name, 3))");
        statement.bind("name", "otavio").result();
        statement.bind("name", "jakarta").result();

        assertThat(sqls(2)).containsExactly("SELECT * FROM \"sensor_reading\" WHERE \"sensor_name\" = 'OTA'",
                "SELECT * FROM \"sensor_reading\" WHERE \"sensor_name\" = 'JAK'");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "FROM MockedSensorReading WHERE LEFT(name, 3) = 'Ota'",
            "FROM MockedSensorReading WHERE RIGHT(name, 2) = 'io'",
            "FROM MockedSensorReading WHERE UPPER(LEFT(name, 3)) = 'OTA'",
            "FROM MockedSensorReading WHERE UPPER(name) LIKE 'O%'"})
    @DisplayName("Unsupported native predicates are rejected before a session is borrowed")
    void shouldRejectUnsupported(String jdql) {
        assertThatThrownBy(() -> template.query(jdql).result()).isInstanceOf(UnsupportedOperationException.class);

        verifyNoInteractions(MockedIoTDBConfiguration.CLIENT, session);
    }

    @Test
    @DisplayName("A delete with a native function sends no mutation")
    void shouldRejectDelete() {
        assertThatThrownBy(() -> template.query("DELETE FROM MockedSensorReading WHERE UPPER(name) = 'A'")
                .executeUpdate()).isInstanceOf(UnsupportedOperationException.class);

        verifyNoInteractions(MockedIoTDBConfiguration.CLIENT, session);
    }

    private List<String> sqls(int invocations) throws Exception {
        var statement = ArgumentCaptor.forClass(String.class);
        verify(session, Mockito.times(invocations)).executeQueryStatement(statement.capture());
        return statement.getAllValues();
    }
}

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
package org.eclipse.jnosql.databases.questdb.function;

import jakarta.inject.Inject;
import io.questdb.client.Completion;
import io.questdb.client.Query;
import io.questdb.client.cutlass.qwp.client.QwpBindSetter;
import io.questdb.client.cutlass.qwp.client.QwpBindValues;
import org.eclipse.jnosql.databases.questdb.communication.MockedQuestDBConfiguration;
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

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, TimeSeriesTemplate.class})
@AddPackages(MockedSensorReading.class)
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, TimeSeriesExtension.class})
@DisplayName("QuestDB native functions through JDQL, mapping and a mocked client")
class NativeFunctionMappingTest {

    static {
        System.setProperty(MappingConfigurations.TIME_SERIES_PROVIDER.get(), MockedQuestDBConfiguration.class.getName());
        System.setProperty(MappingConfigurations.TIME_SERIES_DATABASE.get(), "metrics");
    }

    @Inject
    private TimeSeriesTemplate template;

    private Query query;

    @BeforeEach
    void setUp() throws Exception {
        Mockito.reset(MockedQuestDBConfiguration.CLIENT);
        query = mock(Query.class, org.mockito.Answers.RETURNS_SELF);
        var completion = mock(Completion.class);
        when(completion.await(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);
        when(query.submit()).thenReturn(completion);
        when(MockedQuestDBConfiguration.CLIENT.borrowQuery()).thenReturn(query);
    }


    @Test
    @DisplayName("Native functions stay structured, use mapped names and bind parameters")
    void shouldTranslateNativeFunctions() {
        template.prepare("FROM MockedSensorReading WHERE UPPER(name) = UPPER(:name) AND name = UPPER(otherName)")
                .bind("name", "ota").result();

        assertThat(sql(0)).isEqualTo("SELECT * FROM \"sensor_reading\" WHERE "
                + "((upper(\"sensor_name\") = $1) AND (\"sensor_name\" = upper(\"other_name\")))");
        verify(binds(0)).setVarchar(0, "OTA");
    }

    @Test
    @DisplayName("Nested native functions and partial evaluation expose the ordinary length")
    void shouldEvaluateLengthBeforeTranslation() {
        template.prepare("FROM MockedSensorReading WHERE LEFT(name, LENGTH(:prefix)) = :prefix")
                .bind("prefix", "Ota").result();
        template.prepare("FROM MockedSensorReading WHERE UPPER(LEFT(name, 3)) = 'OTA'").result();

        var first = binds(0);
        assertThat(sql(0)).isEqualTo("SELECT * FROM \"sensor_reading\" WHERE (left(\"sensor_name\", $1) = $2)");
        verify(first).setInt(0, 3);
        verify(first).setVarchar(1, "Ota");
        assertThat(sql(1)).isEqualTo(
                "SELECT * FROM \"sensor_reading\" WHERE (upper(left(\"sensor_name\", $1)) = $2)");
    }

    @Test
    @DisplayName("An evaluable function is an ordinary value, rebound on every execution")
    void shouldKeepEvaluableFunctionsTransparent() {
        var statement = template.prepare("FROM MockedSensorReading WHERE name = UPPER(LEFT(:name, 3))");
        statement.bind("name", "otavio").result();
        statement.bind("name", "jakarta").result();

        assertThat(sql(0)).isEqualTo("SELECT * FROM \"sensor_reading\" WHERE \"sensor_name\" = $1");
        verify(binds(0)).setVarchar(0, "OTA");
        verify(binds(1)).setVarchar(0, "JAK");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "FROM MockedSensorReading WHERE LEFT(name, otherName) = 'A'",
            "FROM MockedSensorReading WHERE UPPER(name) LIKE 'O%'"})
    @DisplayName("Unsupported native predicates are rejected before the client is used")
    void shouldRejectUnsupported(String jdql) {
        assertThatThrownBy(() -> template.prepare(jdql).result()).isInstanceOf(UnsupportedOperationException.class);

        verifyNoInteractions(MockedQuestDBConfiguration.CLIENT);
    }

    @Test
    @DisplayName("A delete with a native function sends no mutation")
    void shouldRejectDelete() {
        assertThatThrownBy(() -> template.prepare("DELETE FROM MockedSensorReading WHERE UPPER(name) = 'A'")
                .result()).isInstanceOf(UnsupportedOperationException.class);

        verifyNoInteractions(MockedQuestDBConfiguration.CLIENT);
    }

    private String sql(int invocation) {
        var statement = ArgumentCaptor.forClass(CharSequence.class);
        verify(query, Mockito.atLeast(invocation + 1)).sql(statement.capture());
        return statement.getAllValues().get(invocation).toString();
    }

    private QwpBindValues binds(int invocation) {
        var setter = ArgumentCaptor.forClass(QwpBindSetter.class);
        verify(query, Mockito.atLeast(invocation + 1)).binds(setter.capture());
        var values = mock(QwpBindValues.class);
        setter.getAllValues().get(invocation).apply(values);
        return values;
    }
}

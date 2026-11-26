/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.scylladb.function;

import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.scylladb.communication.MockedScyllaDBProducer;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.column.ColumnTemplate;
import org.eclipse.jnosql.mapping.column.spi.ColumnExtension;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@EnableAutoWeld
@AddPackages(value = {Database.class, Converters.class, EntityConverter.class, ColumnTemplate.class})
@AddPackages(MockedAccount.class)
@AddBeanClasses(MockedScyllaDBProducer.class)
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, ColumnExtension.class})
@DisplayName("ScyllaDB native functions through JDQL, mapping and a mocked client")
class NativeFunctionMappingTest {

    @Inject
    private ColumnTemplate template;

    @BeforeEach
    void setUp() {
        MockedScyllaDBProducer.reset();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "FROM MockedAccount WHERE UPPER(name) = 'OTAVIO'",
            "FROM MockedAccount WHERE LOWER(name) = UPPER(:name)",
            "FROM MockedAccount WHERE name = UPPER(otherName)",
            "FROM MockedAccount WHERE UPPER(firstName) = UPPER(lastName)",
            "FROM MockedAccount WHERE UPPER(LEFT(name, 3)) = 'OTA'",
            "FROM MockedAccount WHERE LEFT(name, LENGTH(:name)) = :name",
            "FROM MockedAccount WHERE ABS(balance) = 3",
            "FROM MockedAccount WHERE LENGTH(name) = 3",
            "FROM MockedAccount WHERE RIGHT(name, 2) = 'io'",
            "FROM MockedAccount WHERE (UPPER(name) = 'A' OR balance = 1) AND NOT (LOWER(name) = 'b')"})
    @DisplayName("Every native function is rejected before a native request")
    void shouldRejectNativeSelect(String jdql) {
        Mockito.clearInvocations(MockedScyllaDBProducer.SESSION);

        assertThatThrownBy(() -> template.prepare(jdql).bind("name", "Ota").result())
                .isInstanceOf(UnsupportedOperationException.class);

        verifyNoInteractions(MockedScyllaDBProducer.SESSION);
    }

    @Test
    @DisplayName("A delete with a native function sends no mutation")
    void shouldRejectNativeDelete() {
        Mockito.clearInvocations(MockedScyllaDBProducer.SESSION);

        assertThatThrownBy(() -> template.prepare("DELETE FROM MockedAccount WHERE UPPER(name) = 'OTAVIO'")
                .result()).isInstanceOf(UnsupportedOperationException.class);

        verifyNoInteractions(MockedScyllaDBProducer.SESSION);
    }

    @Test
    @DisplayName("An evaluable function is an ordinary value, rebound on every execution")
    void shouldKeepEvaluableFunctionsTransparent() {
        var query = template.prepare("FROM MockedAccount WHERE name = UPPER(LEFT(:name, 3))");
        query.bind("name", "otavio").result();
        query.bind("name", "jakarta").result();

        var captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(MockedScyllaDBProducer.SESSION, times(2)).execute(captor.capture());
        var statements = captor.getAllValues();
        assertThat(statements.get(0).getQuery()).isEqualTo("SELECT * FROM library.mockedaccount WHERE account_name='OTA'");
        assertThat(statements.get(1).getQuery()).isEqualTo("SELECT * FROM library.mockedaccount WHERE account_name='JAK'");
    }
}

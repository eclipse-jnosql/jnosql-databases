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
package org.eclipse.jnosql.databases.ravendb.function;

import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.ravendb.communication.MockedRavenProducer;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;
import org.eclipse.jnosql.mapping.document.spi.DocumentExtension;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@EnableAutoWeld
@AddPackages(value = {Database.class, Converters.class, EntityConverter.class, DocumentTemplate.class})
@AddPackages(MockedAccount.class)
@AddBeanClasses(MockedRavenProducer.class)
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, DocumentExtension.class})
@DisplayName("RavenDB native functions through JDQL, mapping and a mocked store")
class NativeFunctionMappingTest {

    @Inject
    private DocumentTemplate template;

    @BeforeEach
    void setUp() {
        MockedRavenProducer.reset();
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
    @DisplayName("Every native function is rejected before the store is used")
    void shouldRejectNativeSelect(String jdql) {
        assertThatThrownBy(() -> template.query(jdql).bind("name", "Ota").result())
                .isInstanceOf(UnsupportedOperationException.class);

        verify(MockedRavenProducer.STORE, never()).openSession();
        verifyNoInteractions(MockedRavenProducer.SESSION);
    }

    @Test
    @DisplayName("A delete with a native function never reaches the store")
    void shouldRejectNativeDelete() {
        assertThatThrownBy(() -> template.query("DELETE FROM MockedAccount WHERE UPPER(name) = 'OTAVIO'")
                .executeUpdate()).isInstanceOf(UnsupportedOperationException.class);

        verify(MockedRavenProducer.STORE, never()).openSession();
        verifyNoInteractions(MockedRavenProducer.SESSION);
    }

    @Test
    @DisplayName("An evaluable function is an ordinary value and uses the mapped field name")
    void shouldKeepEvaluableFunctionsTransparent() {
        var query = template.query("FROM MockedAccount WHERE name = UPPER(LEFT(:name, 3))");
        query.bind("name", "otavio").result();
        query.bind("name", "jakarta").result();

        assertSoftly(soft -> {
            verify(MockedRavenProducer.QUERY).whereEquals("account_name", (Object) "OTA");
            verify(MockedRavenProducer.QUERY).whereEquals("account_name", (Object) "JAK");
        });
    }
}

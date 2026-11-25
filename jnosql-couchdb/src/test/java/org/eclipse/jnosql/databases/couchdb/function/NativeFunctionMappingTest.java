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
package org.eclipse.jnosql.databases.couchdb.function;

import jakarta.inject.Inject;
import jakarta.json.Json;
import org.eclipse.jnosql.databases.couchdb.communication.MockedCouchProducer;
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

import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnableAutoWeld
@AddPackages(value = {Database.class, Converters.class, EntityConverter.class, DocumentTemplate.class})
@AddPackages(MockedAccount.class)
@AddBeanClasses(MockedCouchProducer.class)
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, DocumentExtension.class})
@DisplayName("CouchDB native functions through JDQL, mapping and a recording HTTP client")
class NativeFunctionMappingTest {

    @Inject
    private DocumentTemplate template;

    @BeforeEach
    void setUp() {
        MockedCouchProducer.CLIENT.reset();
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
    @DisplayName("Every native function is rejected before an HTTP request")
    void shouldRejectNativeSelect(String jdql) {
        assertThatThrownBy(() -> template.prepare(jdql).bind("name", "Ota").result())
                .isInstanceOf(UnsupportedOperationException.class);

        assertThat(MockedCouchProducer.CLIENT.calls()).isEmpty();
    }

    @Test
    @DisplayName("A delete with a native function sends no search and no deletion")
    void shouldRejectNativeDelete() {
        assertThatThrownBy(() -> template.prepare("DELETE FROM MockedAccount WHERE UPPER(name) = 'OTAVIO'")
                .result()).isInstanceOf(UnsupportedOperationException.class);

        assertThat(MockedCouchProducer.CLIENT.calls()).isEmpty();
    }

    @Test
    @DisplayName("An evaluable function is an ordinary value and uses the mapped field name")
    void shouldKeepEvaluableFunctionsTransparent() {
        var query = template.prepare("FROM MockedAccount WHERE name = UPPER(LEFT(:name, 3))");
        assertThatThrownBy(() -> query.bind("name", "otavio").result()).isNotInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> query.bind("name", "jakarta").result()).isNotInstanceOf(UnsupportedOperationException.class);

        var calls = MockedCouchProducer.CLIENT.calls();
        assertThat(calls).hasSize(2);
        assertThat(Json.createReader(new StringReader(calls.get(0).body())).readObject()
                .getJsonObject("selector").getString("account_name")).isEqualTo("OTA");
        assertThat(Json.createReader(new StringReader(calls.get(1).body())).readObject()
                .getJsonObject("selector").getString("account_name")).isEqualTo("JAK");
    }
}

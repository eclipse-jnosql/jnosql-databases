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
package org.eclipse.jnosql.databases.neo4j.function;

import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.neo4j.communication.MockedNeo4JProducer;
import org.eclipse.jnosql.databases.neo4j.mapping.Neo4JTemplate;
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
import org.mockito.ArgumentCaptor;
import org.neo4j.driver.Value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class, Neo4JTemplate.class})
@AddPackages(MockedAccount.class)
@AddBeanClasses(MockedNeo4JProducer.class)
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class})
@DisplayName("Neo4j native functions through JDQL, mapping and a mocked driver session")
class NativeFunctionMappingTest {

    @Inject
    private Neo4JTemplate template;

    @BeforeEach
    void setUp() {
        MockedNeo4JProducer.reset();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "FROM MockedAccount WHERE LEFT(name, 3) = 'Ota'",
            "FROM MockedAccount WHERE RIGHT(name, 2) = 'io'",
            "FROM MockedAccount WHERE UPPER(LEFT(name, 3)) = 'OTA'",
            "FROM MockedAccount WHERE LEFT(name, LENGTH(:name)) = :name",
            "FROM MockedAccount WHERE (LEFT(name, 1) = 'A' OR balance = 1) AND NOT (LOWER(name) = 'b')"})
    @DisplayName("Unsupported functions are rejected before a transaction starts")
    void shouldRejectUnsupported(String jdql) {
        assertThatThrownBy(() -> template.prepare(jdql).bind("name", "Ota").result())
                .isInstanceOf(UnsupportedOperationException.class);

        verifyNoInteractions(MockedNeo4JProducer.SESSION, MockedNeo4JProducer.TRANSACTION);
    }

    @Test
    @DisplayName("A delete with an unsupported function sends no mutation")
    void shouldRejectUnsupportedDelete() {
        assertThatThrownBy(() -> template.prepare("DELETE FROM MockedAccount WHERE RIGHT(name, 2) = 'io'")
                .result()).isInstanceOf(UnsupportedOperationException.class);

        verifyNoInteractions(MockedNeo4JProducer.SESSION, MockedNeo4JProducer.TRANSACTION);
    }

    @Test
    @DisplayName("Native functions stay structured, use mapped names and bind parameters")
    void shouldTranslateNativeFunctions() {
        template.prepare("FROM MockedAccount WHERE UPPER(name) = UPPER(:name) AND name = UPPER(otherName)")
                .bind("name", "ota").result();

        var cypher = ArgumentCaptor.forClass(String.class);
        var params = ArgumentCaptor.forClass(Value.class);
        verify(MockedNeo4JProducer.TRANSACTION).run(cypher.capture(), params.capture());
        assertThat(cypher.getValue()).isEqualTo("MATCH (e:MockedAccount) WHERE ((toUpper(e.account_name) = $nf) "
                + "AND (e.account_name = toUpper(e.other_name))) RETURN e");
        assertThat(params.getValue().get("nf").asString()).isEqualTo("OTA");
    }

    @Test
    @DisplayName("An evaluable function is an ordinary value, rebound on every execution")
    void shouldKeepEvaluableFunctionsTransparent() {
        var query = template.prepare("FROM MockedAccount WHERE name = UPPER(LEFT(:name, 3))");
        query.bind("name", "otavio").result();
        query.bind("name", "jakarta").result();

        var params = ArgumentCaptor.forClass(Value.class);
        verify(MockedNeo4JProducer.TRANSACTION, times(2)).run(org.mockito.ArgumentMatchers.anyString(),
                params.capture());
        assertThat(params.getAllValues().get(0).get("account_name").asString()).isEqualTo("OTA");
        assertThat(params.getAllValues().get(1).get("account_name").asString()).isEqualTo("JAK");
    }
}

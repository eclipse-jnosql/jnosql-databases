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
package org.eclipse.jnosql.databases.arangodb.function;

import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.arangodb.communication.MockedArangoProducer;
import org.eclipse.jnosql.databases.arangodb.mapping.AQL;
import org.eclipse.jnosql.databases.arangodb.mapping.ArangoDBExtension;
import org.eclipse.jnosql.databases.arangodb.mapping.ArangoDBTemplate;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class, DocumentTemplate.class, AQL.class})
@AddPackages(MockedAccount.class)
@AddBeanClasses(MockedArangoProducer.class)
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, DocumentExtension.class, ArangoDBExtension.class})
@DisplayName("ArangoDB native functions through JDQL, mapping and a mocked driver")
class NativeFunctionMappingTest {

    @Inject
    private ArangoDBTemplate template;

    @Inject
    private MockedAccountRepository repository;

    @BeforeEach
    void setUp() {
        MockedArangoProducer.reset();
    }

    private record Request(String aql, Map<String, Object> variables) {
    }

    @SuppressWarnings("unchecked")
    private static List<Request> requests() {
        return MockedArangoProducer.requests().stream()
                .map(call -> new Request(call.getArgument(0, String.class), (Map<String, Object>) call.getArgument(2, Map.class)))
                .toList();
    }

    @Test
    @DisplayName("A native function uses the mapped field and the evaluated value is an ordinary bind variable")
    void shouldMapAttributeAndBindEvaluatedValue() {
        template.prepare("FROM MockedAccount WHERE UPPER(name) = UPPER(:name)").bind("name", "otavio").result();

        assertSoftly(soft -> soft.assertThat(requests()).singleElement().satisfies(request -> {
            soft.assertThat(request.aql()).contains("UPPER(m.account_name)").doesNotContain("m.name");
            soft.assertThat(request.variables()).isEqualTo(Map.of("nf", "OTAVIO"));
        }));
    }

    @Test
    @DisplayName("Partial evaluation reduces the length argument to the number 3")
    void shouldPartiallyEvaluate() {
        template.prepare("FROM MockedAccount WHERE LEFT(name, LENGTH(:prefix)) = :prefix")
                .bind("prefix", "Ota").result();

        assertSoftly(soft -> soft.assertThat(requests()).singleElement().satisfies(request -> {
            soft.assertThat(request.aql()).contains("LEFT(m.account_name, @nf)");
            soft.assertThat(request.variables()).isEqualTo(Map.of("nf", 3, "nf_1", "Ota"));
        }));
    }

    @Test
    @DisplayName("Nested functions and a function on the right keep their structure")
    void shouldTranslateNestedAndRightSide() {
        template.prepare("FROM MockedAccount WHERE UPPER(LEFT(name, 3)) = 'OTA'").result();
        template.prepare("FROM MockedAccount WHERE name = UPPER(otherName)").result();

        assertSoftly(soft -> {
            soft.assertThat(requests().get(0).aql()).contains("UPPER(").contains("LEFT(m.account_name, @nf)");
            soft.assertThat(requests().get(1).aql()).contains("m.account_name == (IS_STRING(m.other_name)");
        });
    }

    @Test
    @DisplayName("Rebinding the same query does not reuse stale values")
    void shouldRebind() {
        var query = template.prepare("FROM MockedAccount WHERE LEFT(name, LENGTH(:prefix)) = :prefix");
        query.bind("prefix", "Ota").result();
        query.bind("prefix", "Jakarta").result();

        assertSoftly(soft -> {
            soft.assertThat(requests().get(0).variables()).isEqualTo(Map.of("nf", 3, "nf_1", "Ota"));
            soft.assertThat(requests().get(1).variables()).isEqualTo(Map.of("nf", 7, "nf_1", "Jakarta"));
        });
    }

    @Test
    void shouldExecuteRepositoryRepeatedly() {
        repository.sameUpperName("otavio");
        repository.sameUpperName("java");
        repository.startingWith("Ja");

        assertSoftly(soft -> {
            soft.assertThat(requests().get(0).variables()).isEqualTo(Map.of("nf", "OTAVIO"));
            soft.assertThat(requests().get(1).variables()).isEqualTo(Map.of("nf", "JAVA"));
            soft.assertThat(requests().get(2).variables()).isEqualTo(Map.of("nf", 2, "nf_1", "Ja"));
        });
    }

    @Test
    @DisplayName("DELETE and UPDATE keep the native predicate")
    void shouldDeleteAndUpdateWithNativeFunction() {
        template.prepare("DELETE FROM MockedAccount WHERE UPPER(name) = 'OTAVIO'").result();
        template.prepare("UPDATE MockedAccount SET balance = 7 WHERE LOWER(name) = 'java'").result();

        assertSoftly(soft -> {
            soft.assertThat(requests().get(0).aql()).contains("UPPER(m.account_name)").endsWith("REMOVE m IN MockedAccount");
            soft.assertThat(requests().get(1).aql()).contains("LOWER(m.account_name)")
                    .contains("UPDATE m WITH { balance_amount: @balance_amount }");
        });
    }

    @Test
    @DisplayName("An unsupported native function is rejected before any query")
    void shouldRejectUnsupported() {
        assertThatThrownBy(() -> repository.unsupportedLength()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> template.prepare("DELETE FROM MockedAccount WHERE LEFT(name, balance) = 'x'")
                .result()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> template.prepare("UPDATE MockedAccount SET balance = 1 WHERE LEFT(name, balance) = 'x'")
                .result()).isInstanceOf(UnsupportedOperationException.class);

        assertSoftly(soft -> soft.assertThat(requests()).isEmpty());
    }
}

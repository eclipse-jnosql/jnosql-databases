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
package org.eclipse.jnosql.databases.orientdb.function;

import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.orientdb.communication.MockedOrientProducer;
import org.eclipse.jnosql.databases.orientdb.mapping.OrientDBExtension;
import org.eclipse.jnosql.databases.orientdb.mapping.OrientDBTemplate;
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
import org.mockito.invocation.Invocation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class, DocumentTemplate.class, OrientDBTemplate.class})
@AddPackages(MockedAccount.class)
@AddBeanClasses(MockedOrientProducer.class)
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, DocumentExtension.class, OrientDBExtension.class})
@DisplayName("OrientDB native functions through JDQL, mapping and a mocked driver")
class NativeFunctionMappingTest {

    @Inject
    private OrientDBTemplate template;

    @Inject
    private MockedAccountRepository repository;

    @BeforeEach
    void setUp() {
        MockedOrientProducer.reset();
    }

    private static String sql(Invocation call) {
        return call.getArgument(0, String.class);
    }

    private static List<Object> params(Invocation call) {
        return java.util.Arrays.asList(call.getArguments()).subList(1, call.getArguments().length);
    }

    @Test
    @DisplayName("A native function uses the mapped field and the evaluated value is an ordinary parameter")
    void shouldMapAttributeAndBindEvaluatedValue() {
        template.prepare("FROM MockedAccount WHERE UPPER(name) = UPPER(:name)").bind("name", "otavio").result();

        assertSoftly(soft -> soft.assertThat(MockedOrientProducer.commands()).singleElement().satisfies(call -> {
            soft.assertThat(sql(call)).contains("account_name.toUpperCase() = ?").doesNotContain("(name.toUpperCase");
            soft.assertThat(params(call)).containsExactly("OTAVIO");
        }));
    }

    @Test
    @DisplayName("Nested functions, a function on the right and ABS on a mapped field")
    void shouldTranslateNestedRightSideAndAbs() {
        template.prepare("FROM MockedAccount WHERE UPPER(LOWER(name)) = 'OTA'").result();
        template.prepare("FROM MockedAccount WHERE name = UPPER(otherName)").result();
        template.prepare("FROM MockedAccount WHERE ABS(balance) = 4").result();

        var calls = MockedOrientProducer.commands();
        assertSoftly(soft -> {
            soft.assertThat(sql(calls.get(0))).contains("account_name.toLowerCase().toUpperCase() = ?");
            soft.assertThat(sql(calls.get(1))).contains("account_name = other_name.toUpperCase()");
            soft.assertThat(sql(calls.get(2))).contains("abs(balance_amount) = ?");
            soft.assertThat(params(calls.get(2))).containsExactly(4);
        });
    }

    @Test
    @DisplayName("Rebinding the same query does not reuse stale values")
    void shouldRebind() {
        var query = template.prepare("FROM MockedAccount WHERE UPPER(name) = UPPER(:name)");
        query.bind("name", "first").result();
        query.bind("name", "second").result();
        repository.sameUpperName("third");

        var calls = MockedOrientProducer.commands();
        assertSoftly(soft -> {
            soft.assertThat(params(calls.get(0))).containsExactly("FIRST");
            soft.assertThat(params(calls.get(1))).containsExactly("SECOND");
            soft.assertThat(params(calls.get(2))).containsExactly("THIRD");
        });
    }

    @Test
    @DisplayName("Unsupported functions are rejected before any connection is used")
    void shouldRejectUnsupported() {
        assertThatThrownBy(() -> repository.unsupportedLength()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> template.prepare("FROM MockedAccount WHERE LEFT(name, 3) = 'Ota'").result())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> template.prepare("DELETE FROM MockedAccount WHERE RIGHT(name, 3) = 'avio'")
                .result()).isInstanceOf(UnsupportedOperationException.class);

        assertSoftly(soft -> {
            soft.assertThat(MockedOrientProducer.commands()).isEmpty();
            soft.assertThat(MockedOrientProducer.acquisitions()).isZero();
        });
    }

    @Test
    @DisplayName("An ordinary evaluated function stays a plain value")
    void shouldKeepEvaluableFunctionTransparent() {
        template.prepare("FROM MockedAccount WHERE name = UPPER(LEFT(:name, 3))").bind("name", "otavio").result();

        assertSoftly(soft -> soft.assertThat(MockedOrientProducer.commands()).singleElement().satisfies(call -> {
            soft.assertThat(sql(call)).doesNotContain("toUpperCase").contains("account_name = ?");
            soft.assertThat(params(call)).containsExactly("OTA");
        }));
    }
}

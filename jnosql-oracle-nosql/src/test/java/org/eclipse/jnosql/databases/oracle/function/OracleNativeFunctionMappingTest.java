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
package org.eclipse.jnosql.databases.oracle.function;

import jakarta.inject.Inject;
import oracle.nosql.driver.ops.PrepareRequest;
import oracle.nosql.driver.values.FieldValue;
import org.eclipse.jnosql.databases.oracle.communication.Database;
import org.eclipse.jnosql.databases.oracle.communication.MockedHandleProducer;
import org.eclipse.jnosql.databases.oracle.mapping.OracleNoSQLTemplate;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;
import org.eclipse.jnosql.mapping.document.spi.DocumentExtension;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.jboss.weld.junit5.auto.EnableAlternatives;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, DocumentTemplate.class})
@AddPackages(MockedAccount.class)
@AddBeanClasses(MockedHandleProducer.class)
@EnableAlternatives(MockedHandleProducer.class)
@AddPackages(OracleNoSQLTemplate.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, DocumentExtension.class})
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@DisplayName("Oracle NoSQL native functions through JDQL, mapping and a mocked client")
class OracleNativeFunctionMappingTest {

    private static final String NAME = "library.content.account_name";

    @Inject
    private OracleNoSQLTemplate template;

    @Inject
    private MockedAccountRepository repository;

    @BeforeEach
    void setUp() {
        MockedHandleProducer.reset();
    }

    private List<String> statements() {
        var requests = ArgumentCaptor.forClass(PrepareRequest.class);
        verify(MockedHandleProducer.HANDLE, atLeastOnce()).prepare(requests.capture());
        return requests.getAllValues().stream().map(r -> r.getStatement().replaceAll("\\s+", " ")).toList();
    }

    private List<String> variables() {
        var values = ArgumentCaptor.forClass(FieldValue.class);
        verify(MockedHandleProducer.STATEMENT, atLeastOnce()).setVariable(any(Integer.class), values.capture());
        return values.getAllValues().stream().map(FieldValue::getString).toList();
    }

    @Test
    @DisplayName("A native function uses the mapped column and the evaluated value is an ordinary value")
    void shouldMapAttributeAndBindEvaluatedValue() {
        template.query("FROM MockedAccount WHERE UPPER(name) = UPPER(:name)").bind("name", "otavio").result();

        assertSoftly(soft -> {
            soft.assertThat(statements()).singleElement().asString()
                    .contains("upper(" + NAME + ")").doesNotContain("UPPER(name");
            soft.assertThat(variables()).containsExactly("MockedAccount", "OTAVIO");
        });
    }

    @Test
    @DisplayName("Partial evaluation reduces the length argument to the number 3")
    void shouldPartiallyEvaluate() {
        template.query("FROM MockedAccount WHERE LEFT(name, LENGTH(:prefix)) = :prefix")
                .bind("prefix", "Ota").result();

        assertSoftly(soft -> {
            soft.assertThat(statements()).singleElement().asString().contains("substring(" + NAME + ", 0, 3)");
            soft.assertThat(variables()).containsExactly("MockedAccount", "Ota");
        });
    }

    @Test
    @DisplayName("Nested native functions and a native function on the right keep their structure")
    void shouldTranslateNestedAndRightSide() {
        template.query("FROM MockedAccount WHERE UPPER(LEFT(name, 3)) = 'OTA'").result();
        template.query("FROM MockedAccount WHERE name = UPPER(otherName)").result();

        assertThat(statements()).hasSize(2)
                .first().asString().contains("upper(CASE WHEN").contains("substring(" + NAME + ", 0, 3)");
        assertThat(statements().get(1)).contains(NAME + " = CASE WHEN library.content.other_name");
    }

    @Test
    @DisplayName("Rebinding the same query does not reuse stale values")
    void shouldRebind() {
        var query = template.query("FROM MockedAccount WHERE LEFT(name, LENGTH(:prefix)) = :prefix");
        query.bind("prefix", "Ota").result();
        query.bind("prefix", "Jakarta").result();

        assertSoftly(soft -> {
            soft.assertThat(statements().get(0)).contains(", 0, 3)");
            soft.assertThat(statements().get(1)).contains(", 0, 7)");
            soft.assertThat(variables()).containsExactly("MockedAccount", "Ota", "MockedAccount", "Jakarta");
        });
    }

    @Test
    void shouldExecuteRepositoryRepeatedly() {
        repository.sameUpperName("otavio");
        repository.sameUpperName("java");
        repository.startingWith("Ja");

        assertSoftly(soft -> {
            soft.assertThat(variables()).containsExactly("MockedAccount", "OTAVIO", "MockedAccount", "JAVA",
                    "MockedAccount", "Ja");
            soft.assertThat(statements().get(2)).contains(", 0, 2)");
        });
    }

    @Test
    @DisplayName("DELETE with a native function is a restricted delete")
    void shouldDeleteWithNativeFunction() {
        template.query("DELETE FROM MockedAccount WHERE UPPER(name) = 'OTAVIO'").executeUpdate();

        assertThat(statements()).singleElement().asString().startsWith("DELETE").contains("upper(" + NAME + ")");
    }

    @Test
    @DisplayName("An unsupported native function is rejected before any request")
    void shouldRejectUnsupportedSelect() {
        assertThatThrownBy(() -> repository.unsupportedLength()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> template.query("DELETE FROM MockedAccount WHERE LEFT(name, balance) = 'x'")
                .executeUpdate()).isInstanceOf(UnsupportedOperationException.class);

        verify(MockedHandleProducer.HANDLE, never()).prepare(any(PrepareRequest.class));
        verifyNoInteractions(MockedHandleProducer.STATEMENT);
    }
}

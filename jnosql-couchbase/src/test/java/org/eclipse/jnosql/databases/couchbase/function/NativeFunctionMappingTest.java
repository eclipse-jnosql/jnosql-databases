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
package org.eclipse.jnosql.databases.couchbase.function;

import com.couchbase.client.java.query.QueryOptions;
import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.couchbase.communication.MockedClusterProducer;
import org.eclipse.jnosql.databases.couchbase.mapping.CouchbaseExtension;
import org.eclipse.jnosql.databases.couchbase.mapping.CouchbaseTemplate;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;
import org.eclipse.jnosql.mapping.document.spi.DocumentExtension;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.jboss.weld.junit5.auto.EnableAlternatives;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mockingDetails;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, DocumentTemplate.class})
@AddPackages(MockedAccount.class)
@AddBeanClasses(MockedClusterProducer.class)
@EnableAlternatives(MockedClusterProducer.class)
@AddPackages(CouchbaseTemplate.class)
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, DocumentExtension.class,
        CouchbaseExtension.class})
@DisplayName("Couchbase native functions through JDQL, mapping and a mocked cluster")
class NativeFunctionMappingTest {

    @Inject
    private CouchbaseTemplate template;

    @Inject
    private MockedAccountRepository repository;

    @BeforeEach
    void setUp() {
        MockedClusterProducer.reset();
    }

    private record Request(String statement, String parameters) {
    }

    private static List<Request> requests() {
        return mockingDetails(MockedClusterProducer.CLUSTER).getInvocations().stream()
                .filter(call -> call.getMethod().getName().equals("query"))
                .map(call -> new Request(call.getArgument(0, String.class).replaceAll("\\s+", " "),
                        call.getRawArguments().length > 1
                                ? ((QueryOptions.Built) call.getArgument(1, QueryOptions.class).build())
                                .namedParameters().toString() : "{}"))
                .toList();
    }

    @Test
    @DisplayName("A native function uses the mapped field and the evaluated value is an ordinary parameter")
    void shouldMapAttributeAndBindEvaluatedValue() {
        template.prepare("FROM MockedAccount WHERE UPPER(name) = UPPER(:name)").bind("name", "otavio").result();

        assertSoftly(soft -> {
            soft.assertThat(requests()).singleElement().satisfies(request -> {
                soft.assertThat(request.statement()).contains("UPPER(`account_name`) = $p0")
                        .doesNotContain("`name`");
                soft.assertThat(request.parameters()).isEqualTo("{\"$p0\":\"OTAVIO\"}");
            });
        });
    }

    @Test
    @DisplayName("Partial evaluation reduces the length argument to the number 3")
    void shouldPartiallyEvaluate() {
        template.prepare("FROM MockedAccount WHERE LEFT(name, LENGTH(:prefix)) = :prefix")
                .bind("prefix", "Ota").result();

        assertSoftly(soft -> soft.assertThat(requests()).singleElement().satisfies(request -> {
            soft.assertThat(request.statement()).contains("SPLIT(`account_name`, '')");
            soft.assertThat(request.parameters()).isEqualTo("{\"$p0\":3,\"$p1\":\"Ota\"}");
        }));
    }

    @Test
    @DisplayName("Nested native functions and a native function on the right keep their structure")
    void shouldTranslateNestedAndRightSide() {
        template.prepare("FROM MockedAccount WHERE UPPER(LEFT(name, 3)) = 'OTA'").result();
        template.prepare("FROM MockedAccount WHERE name = UPPER(otherName)").result();

        assertSoftly(soft -> {
            soft.assertThat(requests().get(0).statement()).contains("UPPER(").contains("SUBSTR(`account_name`");
            soft.assertThat(requests().get(1).statement()).contains("`account_name` = ")
                    .contains("UPPER(`other_name`)");
        });
    }

    @Test
    @DisplayName("Rebinding the same query does not reuse stale values")
    void shouldRebind() {
        var query = template.prepare("FROM MockedAccount WHERE LEFT(name, LENGTH(:prefix)) = :prefix");
        query.bind("prefix", "Ota").result();
        query.bind("prefix", "Jakarta").result();

        assertSoftly(soft -> {
            soft.assertThat(requests().get(0).parameters()).isEqualTo("{\"$p0\":3,\"$p1\":\"Ota\"}");
            soft.assertThat(requests().get(1).parameters()).isEqualTo("{\"$p0\":7,\"$p1\":\"Jakarta\"}");
        });
    }

    @Test
    void shouldExecuteRepositoryRepeatedly() {
        repository.sameUpperName("otavio");
        repository.sameUpperName("java");
        repository.startingWith("Ja");

        assertSoftly(soft -> {
            soft.assertThat(requests().get(0).parameters()).isEqualTo("{\"$p0\":\"OTAVIO\"}");
            soft.assertThat(requests().get(1).parameters()).isEqualTo("{\"$p0\":\"JAVA\"}");
            soft.assertThat(requests().get(2).parameters()).isEqualTo("{\"$p0\":2,\"$p1\":\"Ja\"}");
        });
    }

    @Test
    @DisplayName("DELETE with a native function selects the ids with the native predicate")
    void shouldDeleteWithNativeFunction() {
        template.prepare("DELETE FROM MockedAccount WHERE UPPER(name) = 'OTAVIO'").result();

        assertSoftly(soft -> soft.assertThat(requests()).singleElement().satisfies(request ->
                soft.assertThat(request.statement()).containsIgnoringCase("select")
                        .contains("UPPER(`account_name`) = $p0")));
    }

    @Test
    @DisplayName("UPDATE with a native function restricts the update")
    void shouldUpdateWithNativeFunction() {
        template.prepare("UPDATE MockedAccount SET balance = 7 WHERE LOWER(name) = 'java'").result();

        assertSoftly(soft -> soft.assertThat(requests()).singleElement().satisfies(request ->
                soft.assertThat(request.statement()).startsWith("UPDATE")
                        .contains("LOWER(d.`account_name`) = $p1")));
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

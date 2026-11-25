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
package org.eclipse.jnosql.databases.mongodb.function;

import com.mongodb.MongoClientSettings;
import jakarta.inject.Inject;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.eclipse.jnosql.databases.mongodb.communication.MockedMongoProducer;
import org.eclipse.jnosql.databases.mongodb.mapping.MongoDBTemplate;
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
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, DocumentTemplate.class, MongoDBTemplate.class})
@AddPackages(MockedAccount.class)
@AddBeanClasses(MockedMongoProducer.class)
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, DocumentExtension.class})
@DisplayName("MongoDB native functions through JDQL, mapping and a mocked client")
class NativeFunctionMappingTest {

    @Inject
    private MongoDBTemplate template;

    @Inject
    private MockedAccountRepository repository;

    @BeforeEach
    void setUp() {
        MockedMongoProducer.reset();
    }

    private static List<String> filters(java.util.function.Consumer<ArgumentCaptor<Bson>> verification) {
        var captor = ArgumentCaptor.forClass(Bson.class);
        verification.accept(captor);
        return captor.getAllValues().stream()
                .map(bson -> bson.toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry())
                        .toJson()).toList();
    }

    private static List<String> found() {
        return filters(captor -> verify(MockedMongoProducer.COLLECTION, atLeastOnce()).find(captor.capture()));
    }

    @Test
    @DisplayName("A native function uses the mapped field and the evaluated value is an ordinary literal")
    void shouldMapAttributeAndBindEvaluatedValue() {
        template.query("FROM MockedAccount WHERE UPPER(name) = UPPER(:name)").bind("name", "otavio").result();

        assertSoftly(soft -> soft.assertThat(found()).singleElement().asString()
                .contains("$toUpper").contains("\"$account_name\"").contains("{\"$literal\": \"OTAVIO\"}")
                .doesNotContain("\"$name\""));
    }

    @Test
    @DisplayName("Partial evaluation reduces the length argument to the number 3")
    void shouldPartiallyEvaluate() {
        template.query("FROM MockedAccount WHERE LEFT(name, LENGTH(:prefix)) = :prefix")
                .bind("prefix", "Ota").result();

        assertSoftly(soft -> soft.assertThat(found()).singleElement().asString()
                .contains("\"f0a1\": {\"$literal\": 3}").contains("{\"$literal\": \"Ota\"}"));
    }

    @Test
    @DisplayName("Nested native functions and a native function on the right keep their structure")
    void shouldTranslateNestedAndRightSide() {
        template.query("FROM MockedAccount WHERE UPPER(LEFT(name, 3)) = 'OTA'").result();
        template.query("FROM MockedAccount WHERE name = UPPER(otherName)").result();

        assertSoftly(soft -> {
            soft.assertThat(found().get(0)).contains("$toUpper").contains("$substrCP").contains("{\"$let\"");
            soft.assertThat(found().get(1)).contains("\"$other_name\"").contains("\"$account_name\"");
        });
    }

    @Test
    @DisplayName("Rebinding the same query does not reuse stale values")
    void shouldRebind() {
        var query = template.query("FROM MockedAccount WHERE LEFT(name, LENGTH(:prefix)) = :prefix");
        query.bind("prefix", "Ota").result();
        query.bind("prefix", "Jakarta").result();

        assertSoftly(soft -> {
            soft.assertThat(found().get(0)).contains("{\"$literal\": 3}").contains("\"Ota\"");
            soft.assertThat(found().get(1)).contains("{\"$literal\": 7}").contains("\"Jakarta\"");
        });
    }

    @Test
    void shouldExecuteRepositoryRepeatedly() {
        repository.sameUpperName("otavio");
        repository.sameUpperName("java");
        repository.startingWith("Ja");

        assertSoftly(soft -> {
            soft.assertThat(found().get(0)).contains("\"OTAVIO\"");
            soft.assertThat(found().get(1)).contains("\"JAVA\"");
            soft.assertThat(found().get(2)).contains("{\"$literal\": 2}");
        });
    }

    @Test
    @DisplayName("DELETE with a native function is a restricted delete")
    void shouldDeleteWithNativeFunction() {
        template.query("DELETE FROM MockedAccount WHERE UPPER(name) = 'OTAVIO'").executeUpdate();

        assertSoftly(soft -> soft.assertThat(filters(captor ->
                verify(MockedMongoProducer.COLLECTION).deleteMany(captor.capture()))).singleElement().asString()
                .startsWith("{\"$expr\"").contains("$toUpper"));
    }

    @Test
    @DisplayName("UPDATE with a native function filters the documents")
    void shouldUpdateWithNativeFunction() {
        template.query("UPDATE MockedAccount SET balance = 7 WHERE LOWER(name) = 'java'").executeUpdate();

        assertSoftly(soft -> soft.assertThat(filters(captor ->
                verify(MockedMongoProducer.COLLECTION).updateMany(captor.capture(), any(Bson.class))))
                .singleElement().asString().startsWith("{\"$expr\"").contains("$toLower"));
    }

    @Test
    @DisplayName("An unsupported native function is rejected before any mutation or query")
    void shouldRejectUnsupported() {
        assertThatThrownBy(() -> repository.unsupportedLength()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> template.query("DELETE FROM MockedAccount WHERE LEFT(name, balance) = 'x'")
                .executeUpdate()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> template.query("UPDATE MockedAccount SET balance = 1 WHERE LEFT(name, balance) = 'x'")
                .executeUpdate()).isInstanceOf(UnsupportedOperationException.class);

        verify(MockedMongoProducer.COLLECTION, never()).find(any(Bson.class));
        verify(MockedMongoProducer.COLLECTION, never()).deleteMany(any(Bson.class));
        verify(MockedMongoProducer.COLLECTION, never()).updateMany(any(Bson.class), any(Bson.class));
    }
}

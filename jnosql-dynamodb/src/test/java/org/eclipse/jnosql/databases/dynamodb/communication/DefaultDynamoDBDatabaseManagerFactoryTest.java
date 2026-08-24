/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 * and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *
 * Maximillian Arruda
 */

package org.eclipse.jnosql.databases.dynamodb.communication;

import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.assertj.core.api.Assertions.assertThatCode;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class DefaultDynamoDBDatabaseManagerFactoryTest {

    private DatabaseManagerFactory databaseManagerFactory;

    @BeforeEach
    void setup() {
        this.databaseManagerFactory = DynamoDBTestUtils.CONFIG.getDocumentManagerFactory();
        assertSoftly(softly -> {
            softly.assertThat(databaseManagerFactory).isNotNull();
            softly.assertThat(databaseManagerFactory).isInstanceOf(DynamoDBDatabaseManagerFactory.class);
        });
    }
    @AfterEach
    void tearDown() {
        assertThatCode(databaseManagerFactory::close).as("DocumentManagerFactory.close() should be not throw exceptions").doesNotThrowAnyException();
    }
    @Test
    void shouldCreateDocumentManager() {
        var documentManager = databaseManagerFactory.apply("anydatabase");
        assertSoftly(softly -> softly.assertThat(documentManager).isNotNull());
    }

}

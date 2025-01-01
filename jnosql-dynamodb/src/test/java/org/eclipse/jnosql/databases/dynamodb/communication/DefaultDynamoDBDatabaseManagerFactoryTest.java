/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
        assertDoesNotThrow(databaseManagerFactory::close, "DocumentManagerFactory.close() should be not throw exceptions");
    }
    @Test
    void shouldCreateDocumentManager() {
        var documentManager = databaseManagerFactory.apply("anydatabase");
        assertSoftly(softly -> softly.assertThat(documentManager).isNotNull());
    }

}

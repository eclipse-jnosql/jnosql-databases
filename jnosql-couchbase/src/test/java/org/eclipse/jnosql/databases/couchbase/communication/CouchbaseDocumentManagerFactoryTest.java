/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.databases.couchbase.communication;

import org.eclipse.jnosql.communication.Settings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class CouchbaseDocumentManagerFactoryTest {

    @Test
    public void shouldCreateEntityManager() {
        Settings settings=Database.INSTANCE.getSettings();
        CouchbaseDocumentConfiguration configuration =  new CouchbaseDocumentConfiguration();
        CouchbaseDocumentManagerFactory factory = configuration.apply(settings);
        assertThat(factory.apply(CouchbaseUtil.BUCKET_NAME)).isNotNull();
    }

}
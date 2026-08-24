/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.couchbase.communication;

import org.eclipse.jnosql.communication.keyvalue.BucketManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;

import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class DefaultCouchbaseBucketManagerFactoryTest {

    private CouchbaseBucketManagerFactory factory;

    @BeforeEach
    public void init() {
        CouchbaseKeyValueConfiguration configuration = Database.INSTANCE.getKeyValueConfiguration();
        factory = configuration.apply(CouchbaseUtil.getSettings());
    }

    @Test
    public void shouldReturnManager() {
        BucketManager database = factory.apply(CouchbaseUtil.BUCKET_NAME);
        assertNotNull(database);
    }

    @Test
    public void shouldReturnError() {
        assertThrows(NullPointerException.class, () -> factory.apply(null));
    }


    @Test
    public void shouldReturnList() {
        List<String> names = factory.getList("jnosql", String.class);
        Assertions.assertNotNull(names);
    }

    @Test
    public void shouldReturnSet() {
        Assertions.assertNotNull(factory.getSet("jnosql", String.class));
    }

    @Test
    public void shouldReturnQueue() {
        Assertions.assertNotNull(factory.getQueue("jnosql", String.class));
    }

    @Test
    public void shouldReturnMap() {
        Assertions.assertNotNull(factory.getMap("jnosql", String.class, String.class) );
    }
}
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

import org.eclipse.jnosql.communication.keyvalue.BucketManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;

import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
        assertThat(database).isNotNull();
    }

    @Test
    public void shouldReturnError() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> factory.apply(null));
    }


    @Test
    public void shouldReturnList() {
        List<String> names = factory.getList("jnosql", String.class);
        assertThat(names).isNotNull();
    }

    @Test
    public void shouldReturnSet() {
        assertThat(factory.getSet("jnosql", String.class)).isNotNull();
    }

    @Test
    public void shouldReturnQueue() {
        assertThat(factory.getQueue("jnosql", String.class)).isNotNull();
    }

    @Test
    public void shouldReturnMap() {
        assertThat(factory.getMap("jnosql", String.class, String.class)).isNotNull();
    }
}
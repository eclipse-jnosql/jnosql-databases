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


import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.keyvalue.KeyValueConfiguration;

import static java.util.Objects.requireNonNull;

/**
 * The Couchbase implementation of {@link KeyValueConfiguration}. It connects to a cluster and returns a
 * {@link CouchbaseBucketManagerFactory}, where each Couchbase bucket is a key-value bucket.
 *
 * <p>Configure it through {@link Settings} keys from {@link CouchbaseConfigurations}:</p>
 * <pre>{@code
 * Settings settings = Settings.builder()
 *         .put(CouchbaseConfigurations.HOST, "couchbase://localhost")
 *         .put(CouchbaseConfigurations.USER, "admin")
 *         .put(CouchbaseConfigurations.PASSWORD, "password")
 *         .build();
 * CouchbaseBucketManagerFactory factory = new CouchbaseKeyValueConfiguration().apply(settings);
 * BucketManager bucket = factory.apply("my-bucket");
 * }</pre>
 *
 * @see CouchbaseConfigurations
 */
public class CouchbaseKeyValueConfiguration extends CouchbaseConfiguration
        implements KeyValueConfiguration {


    @Override
    public CouchbaseBucketManagerFactory apply(Settings settings) {
        requireNonNull(settings, "settings is required");
        CouchbaseKeyValueConfiguration configuration = new CouchbaseKeyValueConfiguration();
        configuration.update(settings);
        return new DefaultCouchbaseBucketManagerFactory(configuration.toCouchbaseSettings());
    }
}

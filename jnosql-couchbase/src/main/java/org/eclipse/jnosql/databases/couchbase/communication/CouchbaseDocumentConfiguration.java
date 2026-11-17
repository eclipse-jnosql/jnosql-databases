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
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;

import static java.util.Objects.requireNonNull;

/**
 * The Couchbase implementation of {@link DatabaseConfiguration}. It connects to a cluster and returns a
 * {@link CouchbaseDocumentManagerFactory}, where each Couchbase bucket is a database.
 *
 * <p>Example:</p>
 * <pre>{@code
 * Settings settings = Settings.builder()
 *         .put(CouchbaseConfigurations.HOST, "couchbase://localhost")
 *         .put(CouchbaseConfigurations.USER, "admin")
 *         .put(CouchbaseConfigurations.PASSWORD, "password")
 *         .build();
 * CouchbaseDocumentManagerFactory factory = new CouchbaseDocumentConfiguration().apply(settings);
 * CouchbaseDocumentManager manager = factory.apply("my-bucket");
 * }</pre>
 *
 * @see CouchbaseConfigurations
 */
public class CouchbaseDocumentConfiguration extends CouchbaseConfiguration
        implements DatabaseConfiguration {


    @Override
    public CouchbaseDocumentManagerFactory apply(Settings settings) {
        requireNonNull(settings, "settings is required");

        CouchbaseDocumentConfiguration configuration = new CouchbaseDocumentConfiguration();
        configuration.update(settings);
        return new CouchbaseDocumentManagerFactory(configuration.toCouchbaseSettings());
    }

}

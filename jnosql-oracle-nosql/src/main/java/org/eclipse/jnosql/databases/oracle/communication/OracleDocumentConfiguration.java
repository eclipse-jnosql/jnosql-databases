/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.oracle.communication;

import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;

/**
 * The Oracle NoSQL implementation of {@link DatabaseConfiguration}. It builds the connection from
 * {@link OracleNoSQLConfigurations} and returns an {@link OracleDocumentManagerFactory}.
 *
 * <p>Example:</p>
 * <pre>{@code
 * Settings settings = Settings.builder()
 *         .put(OracleNoSQLConfigurations.HOST, "http://localhost:8080")
 *         .put(OracleNoSQLConfigurations.DEPLOYMENT, "ON_PREMISES")
 *         .build();
 * OracleDocumentManagerFactory factory = new OracleDocumentConfiguration().apply(settings);
 * OracleNoSQLDocumentManager manager = factory.apply("users");
 * }</pre>
 *
 * @see OracleNoSQLConfigurations
 */
public final class OracleDocumentConfiguration implements DatabaseConfiguration {
    @Override
    public OracleDocumentManagerFactory apply(Settings settings) {
        var nosql =  NoSQLHandleConfigConfiguration.INSTANCE.apply(settings);
        return new OracleDocumentManagerFactory(nosql);
    }
}

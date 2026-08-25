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

package org.eclipse.jnosql.databases.arangodb.communication;

import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;

import static java.util.Objects.requireNonNull;

/**
 * The implementation of {@link DatabaseConfiguration}
 * that returns {@link ArangoDBDocumentManagerFactory}.
 *
 * @see ArangoDBConfiguration
 * @see ArangoDBConfigurations
 *
 */
public final class ArangoDBDocumentConfiguration extends ArangoDBConfiguration
        implements DatabaseConfiguration {

    @Override
    public ArangoDBDocumentManagerFactory apply(Settings settings) throws NullPointerException {
        requireNonNull(settings, "settings is required");

        ArangoDBBuilder arangoDBBuilder = getArangoDBBuilder(settings);
        return new ArangoDBDocumentManagerFactory(arangoDBBuilder);
    }

}

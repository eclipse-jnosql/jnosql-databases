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

package org.eclipse.jnosql.databases.ravendb.communication;

import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;

import java.util.Arrays;

import static java.util.Objects.requireNonNull;


/**
 * The RavenDB implementation to both {@link DatabaseConfiguration}
 * that returns  {@link RavenDBDocumentManagerFactory}
 *
 */
public class RavenDBDocumentConfiguration implements DatabaseConfiguration {

/**
 * The host configuration key.
 */
    public static final String HOST = "jnosql.ravendb.host";

    @Override
    public RavenDBDocumentManagerFactory apply(Settings settings) {
        requireNonNull(settings, "configurations is required");

        String[] servers = settings.prefix(Arrays.asList(HOST, Configurations.HOST.get()))
                .stream().map(Object::toString)
                .toArray(String[]::new);
        return new RavenDBDocumentManagerFactory(servers);
    }

}

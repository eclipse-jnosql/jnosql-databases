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

package org.eclipse.jnosql.databases.ravendb.communication;

import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;

import java.util.Arrays;

import static java.util.Objects.requireNonNull;


/**
 * The RavenDB implementation of {@link DatabaseConfiguration}. It returns a
 * {@link RavenDBDocumentManagerFactory} connected to the server URLs in the {@link Settings} keys that start
 * with {@link #HOST}, for example {@code jnosql.ravendb.host.1=http://localhost:8080}.
 *
 * <p>Example:</p>
 * <pre>{@code
 * Settings settings = Settings.builder()
 *         .put(RavenDBDocumentConfiguration.HOST + ".1", "http://localhost:8080")
 *         .build();
 * RavenDBDocumentManagerFactory factory = new RavenDBDocumentConfiguration().apply(settings);
 * RavenDBDocumentManager manager = factory.apply("library");
 * }</pre>
 */
public class RavenDBDocumentConfiguration implements DatabaseConfiguration {

/**
 * The prefix of the server URL keys, {@code jnosql.ravendb.host}. Append a suffix to list several servers,
 * such as {@code jnosql.ravendb.host.1}.
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

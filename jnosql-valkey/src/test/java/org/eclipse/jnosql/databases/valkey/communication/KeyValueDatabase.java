/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.valkey.communication;


import org.eclipse.jnosql.communication.Settings;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public enum KeyValueDatabase implements Supplier<ValkeyBucketManagerFactory> {
    INSTANCE;

    private final GenericContainer<?> valkey =
            new GenericContainer<>("valkey/valkey:latest")
                    .withExposedPorts(6379)
                    .waitingFor(Wait.defaultWaitStrategy());

    {
        valkey.start();
    }

    public String host() {
        return valkey.getHost();
    }

    public String port() {
        return String.valueOf(valkey.getFirstMappedPort());
    }

    @Override
    public ValkeyBucketManagerFactory get() {
        ValkeyConfiguration configuration = new ValkeyConfiguration();
        Map<String, Object> settings = new HashMap<>();

        settings.put(ValkeyConfigurations.HOST.get(), valkey.getHost());
        settings.put(ValkeyConfigurations.PORT.get(), valkey.getFirstMappedPort());
        return configuration.apply(Settings.of(settings));
    }
}
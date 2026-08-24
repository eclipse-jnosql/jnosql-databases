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
package org.eclipse.jnosql.databases.redis.communication;


import org.eclipse.jnosql.communication.Settings;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public enum KeyValueDatabase implements Supplier<RedisBucketManagerFactory> {
    INSTANCE;

    private final GenericContainer redis =
            new GenericContainer("redis:latest")
                    .withExposedPorts(6379)
                    .waitingFor(Wait.defaultWaitStrategy());

    {
        redis.start();
    }

    public String host() {
        return redis.getHost();
    }

    public String port() {
        return String.valueOf(redis.getFirstMappedPort());
    }

    @Override
    public RedisBucketManagerFactory get() {
        RedisConfiguration configuration = new RedisConfiguration();
        Map<String, Object> settings = new HashMap<>();

        settings.put(RedisConfigurations.HOST.get(), redis.getHost());
        settings.put(RedisConfigurations.PORT.get(), redis.getFirstMappedPort());
        return configuration.apply(Settings.of(settings));
    }
}
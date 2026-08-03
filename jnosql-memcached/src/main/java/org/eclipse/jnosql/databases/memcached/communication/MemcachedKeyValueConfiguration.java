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
package org.eclipse.jnosql.databases.memcached.communication;


import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.ConnectionFactoryBuilder.Locator;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.auth.AuthDescriptor;
import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.keyvalue.KeyValueConfiguration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * The memcached implementation of {@link KeyValueConfiguration} that returns
 * {@link MemcachedBucketManagerFactory}.
 *
 * @see MemcachedConfigurations
 */
public class MemcachedKeyValueConfiguration implements KeyValueConfiguration {

    @Override
    public MemcachedBucketManagerFactory apply(Settings settings) {
        requireNonNull(settings, "settings is required");
        ConnectionFactoryBuilder factoryBuilder = new ConnectionFactoryBuilder();

        settings.get(MemcachedConfigurations.DAEMON, Boolean.class)
                .ifPresent(factoryBuilder::setDaemon);

        settings.get(MemcachedConfigurations.MAX_RECONNECT_DELAY, Long.class)
                .ifPresent(factoryBuilder::setMaxReconnectDelay);

        settings.get(MemcachedConfigurations.PROTOCOL, Protocol.class)
                .ifPresent(factoryBuilder::setProtocol);

        settings.get(MemcachedConfigurations.LOCATOR, Locator.class)
                .ifPresent(factoryBuilder::setLocatorType);

        settings.get(MemcachedConfigurations.AUTH_WAIT_TIME, Long.class)
                .ifPresent(factoryBuilder::setAuthWaitTime);

        settings.get(MemcachedConfigurations.MAX_BLOCK_TIME, Long.class)
                .ifPresent(factoryBuilder::setOpQueueMaxBlockTime);

        settings.get(MemcachedConfigurations.TIMEOUT, Long.class)
                .ifPresent(factoryBuilder::setOpTimeout);

        settings.get(MemcachedConfigurations.READ_BUFFER_SIZE, Integer.class)
                .ifPresent(factoryBuilder::setReadBufferSize);

        settings.get(MemcachedConfigurations.SHOULD_OPTIMIZE, Boolean.class)
                .ifPresent(factoryBuilder::setShouldOptimize);

        settings.get(MemcachedConfigurations.TIMEOUT_THRESHOLD, Integer.class)
                .ifPresent(factoryBuilder::setTimeoutExceptionThreshold);

        settings.get(MemcachedConfigurations.USE_NAGLE_ALGORITHM, Boolean.class)
                .ifPresent(factoryBuilder::setUseNagleAlgorithm);

        settings.getSupplier(asList(MemcachedConfigurations.USER, Configurations.USER))
                .map(Object::toString)
                .ifPresent(u -> {
                    String password = ofNullable(settings.getSupplier(asList(MemcachedConfigurations.PASSWORD
                            , Configurations.PASSWORD)))
                            .map(Object::toString).orElse(null);
                    factoryBuilder.setAuthDescriptor(AuthDescriptor.typical(u, password));
                });


        List<String> hots = settings.prefixSupplier(asList(MemcachedConfigurations.HOST, Configurations.HOST))
                .stream()
                .map(Object::toString)
                .collect(Collectors.toList());

        List<InetSocketAddress> addresses = hots.isEmpty() ? Collections.emptyList() : AddrUtil.getAddresses(hots);
        ConnectionFactory connectionFactory = factoryBuilder.build();

        try {
            return new MemcachedBucketManagerFactory(new MemcachedClient(connectionFactory, addresses));
        } catch (IOException e) {
            throw new MemcachedException("There is an error when try to create da BucketManager", e);
        }
    }

    /**
     * Creates a {@link MemcachedBucketManagerFactory} from a {@link MemcachedClient}
     *
     * @param client the client
     * @return a {@link MemcachedBucketManagerFactory} instance
     */
    public MemcachedBucketManagerFactory get(MemcachedClient client) {
        Objects.requireNonNull(client, "client is required");
        return new MemcachedBucketManagerFactory(client);
    }
}


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
 * The Memcached implementation of {@link KeyValueConfiguration}. It creates a bucket manager factory backed by a
 * Memcached client.
 *
 * <p>A bucket is a key prefix: the entry for key {@code k} in bucket {@code users} is stored in Memcached under
 * {@code users:k}. The factory supports only {@code apply(bucketName)}; {@code getList}, {@code getSet},
 * {@code getQueue} and {@code getMap} throw {@link UnsupportedOperationException}.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * Settings settings = Settings.builder()
 *         .put(MemcachedConfigurations.HOST, "localhost:11211")
 *         .build();
 * BucketManagerFactory factory = new MemcachedKeyValueConfiguration().apply(settings);
 * BucketManager users = factory.apply("users");
 * users.put("ada", "Ada Lovelace", Duration.ofMinutes(10));
 * }</pre>
 *
 * @see MemcachedConfigurations
 */
public class MemcachedKeyValueConfiguration implements KeyValueConfiguration {

    /**
     * {@inheritDoc}
     *
     * <p>When no host is configured, the client is created without addresses.</p>
     *
     * @throws NullPointerException if {@code settings} is {@code null}
     * @throws MemcachedException   if the Memcached client cannot be created
     */
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
     * Creates a bucket manager factory from an existing {@link MemcachedClient}. Use it to control the client
     * setup yourself.
     *
     * <p>Example:</p>
     * <pre>{@code
     * MemcachedClient client = new MemcachedClient(new InetSocketAddress("localhost", 11211));
     * BucketManagerFactory factory = new MemcachedKeyValueConfiguration().get(client);
     * }</pre>
     *
     * @param client the Memcached client
     * @return a factory that uses {@code client}
     * @throws NullPointerException if {@code client} is {@code null}
     */
    public MemcachedBucketManagerFactory get(MemcachedClient client) {
        Objects.requireNonNull(client, "client is required");
        return new MemcachedBucketManagerFactory(client);
    }
}


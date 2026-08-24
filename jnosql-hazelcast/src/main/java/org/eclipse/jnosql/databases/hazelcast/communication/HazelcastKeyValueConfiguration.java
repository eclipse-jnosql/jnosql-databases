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
package org.eclipse.jnosql.databases.hazelcast.communication;


import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.SettingsBuilder;
import org.eclipse.jnosql.communication.keyvalue.KeyValueConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * The hazelcast implementation of {@link KeyValueConfiguration} that returns
 * {@link HazelcastBucketManagerFactory}.
 *
 * @see HazelcastConfigurations
 */
public class HazelcastKeyValueConfiguration implements KeyValueConfiguration {

    private static final String DEFAULT_INSTANCE = "hazelcast-instanceName";


    /**
     * Creates a {@link HazelcastBucketManagerFactory} from configuration map
     * @param configurations the configuration map
     * @return the HazelCastBucketManagerFactory instance
     * @throws NullPointerException when configurations is null
     */
    public HazelcastBucketManagerFactory get(Map<String, String> configurations) throws NullPointerException {
        Objects.requireNonNull(configurations, "configurations is required");
        SettingsBuilder builder = Settings.builder();
        configurations.forEach(builder::put);
        return apply(builder.build());
    }

    /**
     * Creates a {@link HazelcastBucketManagerFactory} from hazelcast config
     * @param config the {@link Config}
     * @return the HazelCastBucketManagerFactory instance
     * @throws NullPointerException when config is null
     */
    public HazelcastBucketManagerFactory get(Config config)throws NullPointerException {
        requireNonNull(config, "config is required");
        HazelcastInstance hazelcastInstance = Hazelcast.getOrCreateHazelcastInstance(config);
        return new DefaultHazelcastBucketManagerFactory(hazelcastInstance);
    }


    @Override
    public HazelcastBucketManagerFactory apply(Settings settings) {
        requireNonNull(settings, "settings is required");

        List<String> servers = settings.prefixSupplier(Arrays.asList(
                HazelcastConfigurations.HOST, Configurations.HOST))
                .stream().map(Object::toString)
                .toList();
        String instance = settings.get(HazelcastConfigurations.INSTANCE).map(Object::toString)
                .orElse(DEFAULT_INSTANCE);
        Config config = new Config(instance);

        NetworkConfig network = config.getNetworkConfig();

        settings.get(HazelcastConfigurations.PORT)
                .map(Object::toString)
                .map(Integer::parseInt)
                .ifPresent(network::setPort);

        settings.get(HazelcastConfigurations.PORT_COUNT)
                .map(Object::toString)
                .map(Integer::parseInt)
                .ifPresent(network::setPortCount);

        settings.get(HazelcastConfigurations.PORT_AUTO_INCREMENT)
                .map(Object::toString)
                .map(Boolean::parseBoolean)
                .ifPresent(network::setPortAutoIncrement);

        JoinConfig join = network.getJoin();

        settings.get(HazelcastConfigurations.MULTICAST_ENABLE)
                .map(Object::toString)
                .map(Boolean::parseBoolean)
                .ifPresent(join.getMulticastConfig()::setEnabled);

        servers.forEach(join.getTcpIpConfig()::addMember);

        join.getTcpIpConfig()
                .addMember("machine1")
                .addMember("localhost");

        settings.get(HazelcastConfigurations.TCP_IP_JOIN)
                .map(Object::toString)
                .map(Boolean::valueOf)
                .ifPresent(join.getTcpIpConfig()::setEnabled);

        HazelcastInstance hazelcastInstance = Hazelcast.getOrCreateHazelcastInstance(config);
        return new DefaultHazelcastBucketManagerFactory(hazelcastInstance);
    }
}

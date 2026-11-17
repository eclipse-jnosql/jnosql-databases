/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.jnosql.databases.valkey.communication;

import io.valkey.ClientSetInfoConfig;
import io.valkey.ConnectionPoolConfig;
import io.valkey.DefaultJedisClientConfig;
import io.valkey.HostAndPort;
import io.valkey.JedisClientConfig;
import io.valkey.JedisCluster;
import io.valkey.JedisPooled;
import io.valkey.JedisSentineled;
import io.valkey.RedisProtocol;
import io.valkey.UnifiedJedis;
import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.SettingsBuilder;
import org.eclipse.jnosql.communication.keyvalue.KeyValueConfiguration;



import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * The Valkey implementation of {@link KeyValueConfiguration}. It creates a {@link ValkeyBucketManagerFactory}
 * from {@link Settings}.
 *
 * <p>The deployment mode is chosen from the settings keys. When any key starts with
 * {@link ValkeySentinelConfigurations#SENTINEL} the factory connects through Sentinel; when any key starts
 * with {@link ValkeyClusterConfigurations#CLUSTER} it connects to a cluster; otherwise it connects to a
 * single node, using {@code localhost:6379} for the host and port that are not set.</p>
 *
 * <p>Example:</p>
 * {@snippet lang="java" :
 * Settings settings = Settings.builder()
 *         .put(ValkeyConfigurations.HOST, "localhost")
 *         .put(ValkeyConfigurations.PORT, 6379)
 *         .build();
 * ValkeyBucketManagerFactory factory = new ValkeyConfiguration().apply(settings);
 * BucketManager bucket = factory.apply("users");
 * }
 *
 * @see ValkeyConfigurations
 * @see ValkeySentinelConfigurations
 * @see ValkeyClusterConfigurations
 */
public final class ValkeyConfiguration implements KeyValueConfiguration {

    private static final int DEFAULT_PORT = 6379;
    private static final String DEFAULT_HOST = "localhost";

    /**
     * Creates a {@link ValkeyBucketManagerFactory} from a map of configuration keys, such as the values of
     * {@link ValkeyConfigurations}. It is equivalent to {@link #apply(Settings)} with the same entries.
     *
     * <p>Example:</p>
     * {@snippet lang="java" :
     * ValkeyBucketManagerFactory factory = new ValkeyConfiguration()
     *         .getManagerFactory(Map.of("jnosql.valkey.host", "localhost"));
     * }
     *
     * @param configurations the configuration keys and values
     * @return a new factory; the caller must close it
     * @throws NullPointerException if {@code configurations} is {@code null}
     * @throws IllegalArgumentException if the Sentinel or Cluster mode is selected and the required master name
     *                                  or hosts are missing
     */
    public ValkeyBucketManagerFactory getManagerFactory(Map<String, String> configurations) {
        Objects.requireNonNull(configurations, "configurations is required");
        SettingsBuilder builder = Settings.builder();
        configurations.forEach(builder::put);
        return apply(builder.build());
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code settings} is {@code null}
     * @throws IllegalArgumentException if the Sentinel or Cluster mode is selected and the required master name
     *                                  or hosts are missing
     */
    @Override
    public ValkeyBucketManagerFactory apply(Settings settings) {
        Objects.requireNonNull(settings, "settings is required");

        if (settings.keySet()
                .stream()
                .anyMatch(s -> s.startsWith(ValkeySentinelConfigurations.SENTINEL.get()))) {
            return applyForSentinel(settings);
        }

        if (settings.keySet()
                .stream()
                .anyMatch(s -> s.startsWith(ValkeyClusterConfigurations.CLUSTER.get()))) {
            return applyForCluster(settings);
        }

        var simpleJedisConfig = getJedisClientConfig(
                ValkeyConfigurations.SingleValkeyConfigurationsResolver.INSTANCE, settings);

        HostAndPort hostAndPort = getHostAndPort(settings);

        ConnectionPoolConfig connectionPoolConfig = getConnectionPoolConfig(settings);

        UnifiedJedis jedis = new JedisPooled(
                connectionPoolConfig,
                hostAndPort,
                simpleJedisConfig);

        return new DefaultValkeyBucketManagerFactory(jedis);
    }

    private HostAndPort getHostAndPort(Settings settings) {
        String localhost = settings
                .getSupplier(asList(ValkeyConfigurations.HOST, Configurations.HOST))
                .map(Object::toString).orElse(DEFAULT_HOST);

        Integer port = settings.get(ValkeyConfigurations.PORT)
                .map(Object::toString).map(Integer::parseInt)
                .orElse(DEFAULT_PORT);
        return new HostAndPort(localhost, port);
    }

    private ValkeyBucketManagerFactory applyForCluster(Settings settings) {

        Set<HostAndPort> clusterNodes = settings.get(ValkeyClusterConfigurations.CLUSTER_HOSTS)
                .map(Object::toString)
                .map(h -> h.split(","))
                .map(h -> Arrays.stream(h).map(HostAndPort::from).collect(Collectors.toSet()))
                .orElseThrow(() -> new IllegalArgumentException("The cluster nodes are required"));

        JedisClientConfig clientConfig = getJedisClientConfig(
                ValkeyClusterConfigurations.ClusterConfigurationsResolver.INSTANCE, settings);

        int maxAttempts = settings.get(ValkeyClusterConfigurations.CLUSTER_MAX_ATTEMPTS)
                .map(Object::toString).map(Integer::parseInt)
                .orElse(JedisCluster.DEFAULT_MAX_ATTEMPTS);

        Duration maxTotalRetriesDuration = settings
                .get(ValkeyClusterConfigurations.CLUSTER_MAX_TOTAL_RETRIES_DURATION)
                .map(Object::toString).map(Duration::parse)
                .orElse(Duration.ofMillis((long) clientConfig.getSocketTimeoutMillis() * maxAttempts));

        ConnectionPoolConfig poolConfig = getConnectionPoolConfig(settings);

        JedisCluster jedis = new JedisCluster(
                clusterNodes,
                clientConfig,
                maxAttempts,
                maxTotalRetriesDuration,
                poolConfig);
        return new DefaultValkeyBucketManagerFactory(jedis);
    }

    private ValkeyBucketManagerFactory applyForSentinel(Settings settings) {

        String masterName = settings.get(ValkeySentinelConfigurations.SENTINEL_MASTER_NAME)
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException("The sentinel master name is required"));

        Set<HostAndPort> hostAndPorts = settings.get(ValkeySentinelConfigurations.SENTINEL_HOSTS)
                .map(Object::toString)
                .map(h -> h.split(","))
                .map(h -> Arrays.stream(h).map(HostAndPort::from).collect(Collectors.toSet()))
                .orElseThrow(() -> new IllegalArgumentException("The sentinel hosts are required"));

        ConnectionPoolConfig connectionPoolConfig = getConnectionPoolConfig(settings);

        var masterJedisClientConfig = getJedisClientConfig(
                ValkeySentinelConfigurations.SentinelMasterConfigurationsResolver.INSTANCE,settings);

        var slaveJedisClientConfig = getJedisClientConfig(
                ValkeySentinelConfigurations.SentinelMasterConfigurationsResolver.INSTANCE, settings);

        JedisSentineled jedis = new JedisSentineled(masterName,
                masterJedisClientConfig,
                connectionPoolConfig,
                hostAndPorts,
                slaveJedisClientConfig);

        return new DefaultValkeyBucketManagerFactory(jedis);
    }

    private JedisClientConfig getJedisClientConfig(ValkeyConfigurationsResolver resolver, Settings settings) {

        DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder();

        settings.get(resolver.connectionTimeoutSupplier())
                .map(Object::toString).map(Integer::parseInt)
                .ifPresent(builder::connectionTimeoutMillis);

        settings.get(resolver.socketTimeoutSupplier())
                .map(Object::toString).map(Integer::parseInt)
                .ifPresent(builder::socketTimeoutMillis);

        settings.get(resolver.clientNameSupplier())
                .map(Object::toString)
                .ifPresent(builder::clientName);

        settings.get(resolver.userSupplier())
                .map(Object::toString)
                .ifPresent(builder::user);

        settings.get(resolver.passwordSupplier())
                .map(Object::toString)
                .ifPresent(builder::password);

        settings.get(resolver.timeoutSupplier())
                .map(Object::toString).map(Integer::parseInt)
                .ifPresent(builder::timeoutMillis);

        settings.get(resolver.sslSupplier())
                .map(Object::toString).map(Boolean::parseBoolean)
                .ifPresent(builder::ssl);

        settings.get(resolver.redisProtocolSupplier())
                .map(Object::toString).map(RedisProtocol::valueOf)
                .ifPresent(builder::protocol);

        settings.get(resolver.clientsetInfoConfigLibNameSuffixSupplier())
                .map(Object::toString)
                .ifPresentOrElse(
                        libNameSuffix -> builder.clientSetInfoConfig(new ClientSetInfoConfig(libNameSuffix)),
                        () -> settings.get(resolver.clientsetInfoConfigDisabled())
                                .map(Object::toString)
                                .map(Boolean::parseBoolean)
                                .map(disabled -> disabled ? ClientSetInfoConfig.DISABLED : ClientSetInfoConfig.DEFAULT)
                                .ifPresent(builder::clientSetInfoConfig));

        return builder.build();
    }

    private ConnectionPoolConfig getConnectionPoolConfig(Settings settings) {

        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();

        settings.get(ValkeyConfigurations.MAX_TOTAL)
                .map(Object::toString).map(Integer::parseInt)
                .ifPresent(poolConfig::setMaxTotal);

        settings.get(ValkeyConfigurations.MAX_IDLE)
                .map(Object::toString).map(Integer::parseInt)
                .ifPresent(poolConfig::setMaxIdle);

        settings.get(ValkeyConfigurations.MIN_IDLE)
                .map(Object::toString).map(Integer::parseInt)
                .ifPresent(poolConfig::setMinIdle);

        settings.get(ValkeyConfigurations.MAX_WAIT_MILLIS)
                .map(Object::toString).map(Integer::parseInt)
                .ifPresent(poolConfig::setMaxWaitMillis);

        return poolConfig;
    }

}

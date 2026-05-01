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

package org.eclipse.jnosql.databases.redis.communication;

import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.SettingsBuilder;
import org.eclipse.jnosql.communication.keyvalue.KeyValueConfiguration;
import redis.clients.jedis.ClientSetInfoConfig;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.JedisSentineled;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * The redis implementation of {@link KeyValueConfiguration} whose returns {@link RedisBucketManagerFactory}.
 *
 * @see RedisConfigurations
 */
public final class RedisConfiguration implements KeyValueConfiguration {

    private static final int DEFAULT_PORT = 6379;
    private static final String DEFAULT_HOST = "localhost";

    /**
     * Creates a {@link RedisConfiguration} from map configuration
     *
     * @param configurations the map configuration
     * @return the RedisConfiguration instance
     */
    public RedisBucketManagerFactory getManagerFactory(Map<String, String> configurations) {
        Objects.requireNonNull(configurations, "configurations is required");
        SettingsBuilder builder = Settings.builder();
        configurations.forEach(builder::put);
        return apply(builder.build());
    }

    @Override
    public RedisBucketManagerFactory apply(Settings settings) {
        Objects.requireNonNull(settings, "settings is required");

        if (settings.keySet()
                .stream()
                .anyMatch(s -> s.startsWith(RedisSentinelConfigurations.SENTINEL.get()))) {
            return applyForSentinel(settings);
        }

        if (settings.keySet()
                .stream()
                .anyMatch(s -> s.startsWith(RedisClusterConfigurations.CLUSTER.get()))) {
            return applyForCluster(settings);
        }

        var simpleJedisConfig = getJedisClientConfig(
                RedisConfigurations.SingleRedisConfigurationsResolver.INSTANCE, settings);

        HostAndPort hostAndPort = getHostAndPort(settings);

        ConnectionPoolConfig connectionPoolConfig = getConnectionPoolConfig(settings);

        UnifiedJedis jedis = new JedisPooled(
                connectionPoolConfig,
                hostAndPort,
                simpleJedisConfig);

        return new DefaultRedisBucketManagerFactory(jedis);
    }

    private HostAndPort getHostAndPort(Settings settings) {
        String localhost = settings
                .getSupplier(asList(RedisConfigurations.HOST, Configurations.HOST))
                .map(Object::toString).orElse(DEFAULT_HOST);

        Integer port = settings.get(RedisConfigurations.PORT)
                .map(Object::toString).map(Integer::parseInt)
                .orElse(DEFAULT_PORT);
        return new HostAndPort(localhost, port);
    }

    private RedisBucketManagerFactory applyForCluster(Settings settings) {

        Set<HostAndPort> clusterNodes = settings.get(RedisClusterConfigurations.CLUSTER_HOSTS)
                .map(Object::toString)
                .map(h -> h.split(","))
                .map(h -> Arrays.stream(h).map(HostAndPort::from).collect(Collectors.toSet()))
                .orElseThrow(() -> new IllegalArgumentException("The cluster nodes are required"));

        JedisClientConfig clientConfig = getJedisClientConfig(
                RedisClusterConfigurations.ClusterConfigurationsResolver.INSTANCE, settings);

        int maxAttempts = settings.get(RedisClusterConfigurations.CLUSTER_MAX_ATTEMPTS)
                .map(Object::toString).map(Integer::parseInt)
                .orElse(JedisCluster.DEFAULT_MAX_ATTEMPTS);

        Duration maxTotalRetriesDuration = settings
                .get(RedisClusterConfigurations.CLUSTER_MAX_TOTAL_RETRIES_DURATION)
                .map(Object::toString).map(Duration::parse)
                .orElse(Duration.ofMillis((long) clientConfig.getSocketTimeoutMillis() * maxAttempts));

        ConnectionPoolConfig poolConfig = getConnectionPoolConfig(settings);

        JedisCluster jedis = new JedisCluster(
                clusterNodes,
                clientConfig,
                maxAttempts,
                maxTotalRetriesDuration,
                poolConfig);
        return new DefaultRedisBucketManagerFactory(jedis);
    }

    private RedisBucketManagerFactory applyForSentinel(Settings settings) {

        String masterName = settings.get(RedisSentinelConfigurations.SENTINEL_MASTER_NAME)
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException("The sentinel master name is required"));

        Set<HostAndPort> hostAndPorts = settings.get(RedisSentinelConfigurations.SENTINEL_HOSTS)
                .map(Object::toString)
                .map(h -> h.split(","))
                .map(h -> Arrays.stream(h).map(HostAndPort::from).collect(Collectors.toSet()))
                .orElseThrow(() -> new IllegalArgumentException("The sentinel hosts are required"));

        ConnectionPoolConfig connectionPoolConfig = getConnectionPoolConfig(settings);

        var masterJedisClientConfig = getJedisClientConfig(
                RedisSentinelConfigurations.SentinelMasterConfigurationsResolver.INSTANCE,settings);

        var slaveJedisClientConfig = getJedisClientConfig(
                RedisSentinelConfigurations.SentinelMasterConfigurationsResolver.INSTANCE, settings);

        JedisSentineled jedis = new JedisSentineled(masterName,
                masterJedisClientConfig,
                connectionPoolConfig,
                hostAndPorts,
                slaveJedisClientConfig);

        return new DefaultRedisBucketManagerFactory(jedis);
    }

    private JedisClientConfig getJedisClientConfig(RedisConfigurationsResolver resolver, Settings settings) {

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

        settings.get(RedisConfigurations.MAX_TOTAL)
                .map(Object::toString).map(Integer::parseInt)
                .ifPresent(poolConfig::setMaxTotal);

        settings.get(RedisConfigurations.MAX_IDLE)
                .map(Object::toString).map(Integer::parseInt)
                .ifPresent(poolConfig::setMaxIdle);

        settings.get(RedisConfigurations.MIN_IDLE)
                .map(Object::toString).map(Integer::parseInt)
                .ifPresent(poolConfig::setMinIdle);

        settings.get(RedisConfigurations.MAX_WAIT_MILLIS)
                .map(Object::toString).map(Integer::parseInt)
                .ifPresent(poolConfig::setMaxWaitMillis);

        return poolConfig;
    }

}

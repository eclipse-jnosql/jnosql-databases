/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
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
 *   Maximillian Arruda
 */

package org.eclipse.jnosql.databases.redis.communication;

import java.util.function.Supplier;

/**
 * An enumeration to show the available options to connect to the Redis database by cluster configuration.
 * It implements {@link Supplier}, where its it returns the property name that might be
 * overwritten by the system environment using Eclipse Microprofile or Jakarta Config API.
 *
 * @see org.eclipse.jnosql.communication.Settings
 */
public enum RedisClusterConfigurations implements Supplier<String> {
    /**
     * The key property that defines if the redis cluster configuration should be loaded
     */
    CLUSTER("jnosql.redis.cluster"),
    /**
     * The value for the sentinel HOST:PORT (separated by comma) configuration attribute for the jedis client configuration with this configuration instance.
     */
    CLUSTER_HOSTS("jnosql.redis.cluster.hosts"),
    /**
     * The cluster client's name. The default value is 0.
     */
    CLIENT_NAME("jnosql.redis.cluster.client.name"),
    /**
     * The cluster redis timeout, the default value is {@link redis.clients.jedis.Protocol#DEFAULT_TIMEOUT} on milliseconds
     */
    TIMEOUT("jnosql.redis.cluster.timeout"),
    /**
     * The value for the connection timeout in milliseconds configuration attribute for the cluster jedis client configuration
     * created with this configuration instance.
     * The connection timeout on millis on {@link redis.clients.jedis.JedisClientConfig}, the default value is {@link redis.clients.jedis.Protocol#DEFAULT_TIMEOUT}
     */
    CONNECTION_TIMEOUT("jnosql.redis.cluster.connection.timeout"),
    /**
     * The value for the socket timeout in milliseconds configuration attribute for the cluster jedis client configuration with
     * this configuration instance.
     * The socket timeout on millis on {@link redis.clients.jedis.JedisClientConfig}, the default value is {@link redis.clients.jedis.Protocol#DEFAULT_TIMEOUT}
     */
    SOCKET_TIMEOUT("jnosql.redis.cluster.socket.timeout"),
    /**
     * The value for the user configuration attribute for the cluster jedis client configuration with this configuration instance.
     * The user on {@link redis.clients.jedis.JedisClientConfig}
     */
    USER("jnosql.redis.cluster.user"),
    /**
     * The value for the password configuration attribute for the cluster jedis client configuration with this configuration instance.
     * The user on {@link redis.clients.jedis.JedisClientConfig}
     */
    PASSWORD("jnosql.redis.cluster.password"),
    /**
     * The value for the ssl configuration attribute for the cluster jedis client configuration with this configuration instance.
     * The ssl on {@link redis.clients.jedis.JedisClientConfig}, the default value is false.
     */
    SSL("jnosql.redis.cluster.ssl"),
    /**
     * The value for the protocol configuration attribute for the cluster jedis client configuration with this configuration instance.
     * The ssl on {@link redis.clients.jedis.JedisClientConfig}. The default value is false.
     * The default value is not defined.
     */
    REDIS_PROTOCOL("jnosql.redis.cluster.protocol"),
    /**
     * The value for the clientset info disabled configuration attribute for the cluster jedis client configuration with this configuration instance.
     * The clientset info disabled on {@link redis.clients.jedis.JedisClientConfig}
     * The default value is false.
     */
    CLIENTSET_INFO_CONFIG_DISABLED("jnosql.redis.cluster.clientset.info.config.disabled"),
    /**
     * The value for the clientset info configuration libname suffix attribute for the cluster jedis client configuration with this configuration instance.
     * The clientset info libname suffix on {@link redis.clients.jedis.JedisClientConfig}
     * The default value is not defined.
     */
    CLIENTSET_INFO_CONFIG_LIBNAME_SUFFIX("jnosql.redis.cluster.clientset.info.config.libname.suffix"),

    /**
     * The value for the max attempts configuration attribute for the cluster jedis client configuration with this configuration instance.
     * Default is {@link redis.clients.jedis.JedisCluster#DEFAULT_MAX_ATTEMPTS}
     */
    CLUSTER_MAX_ATTEMPTS("jnosql.redis.cluster.max.attempts"),
    /**
     * The value for the max total retries configuration attribute for the cluster jedis client configuration with this configuration instance.
     * Default is {@link RedisClusterConfigurations#SOCKET_TIMEOUT} * {@link RedisClusterConfigurations#CLUSTER_MAX_ATTEMPTS}
     */
    CLUSTER_MAX_TOTAL_RETRIES_DURATION("jnosql.redis.cluster.max.total.retries.duration");

    private final String configuration;

    RedisClusterConfigurations(String configuration) {
        this.configuration = configuration;
    }

    @Override
    public String get() {
        return configuration;
    }

    public static enum ClusterConfigurationsResolver implements
            RedisConfigurationsResolver {

        INSTANCE;

        @Override
        public Supplier<String> connectionTimeoutSupplier() {
            return RedisClusterConfigurations.CONNECTION_TIMEOUT;
        }

        @Override
        public Supplier<String> socketTimeoutSupplier() {
            return RedisClusterConfigurations.SOCKET_TIMEOUT;
        }

        @Override
        public Supplier<String> clientNameSupplier() {
            return RedisClusterConfigurations.CLIENT_NAME;
        }

        @Override
        public Supplier<String> userSupplier() {
            return RedisClusterConfigurations.USER;
        }

        @Override
        public Supplier<String> passwordSupplier() {
            return RedisClusterConfigurations.PASSWORD;
        }

        @Override
        public Supplier<String> timeoutSupplier() {
            return RedisClusterConfigurations.TIMEOUT;
        }

        @Override
        public Supplier<String> sslSupplier() {
            return RedisClusterConfigurations.SSL;
        }

        @Override
        public Supplier<String> redisProtocolSupplier() {
            return RedisClusterConfigurations.REDIS_PROTOCOL;
        }

        @Override
        public Supplier<String> clientsetInfoConfigLibNameSuffixSupplier() {
            return RedisClusterConfigurations.CLIENTSET_INFO_CONFIG_LIBNAME_SUFFIX;
        }

        @Override
        public Supplier<String> clientsetInfoConfigDisabled() {
            return RedisClusterConfigurations.CLIENTSET_INFO_CONFIG_DISABLED;
        }
    }

}
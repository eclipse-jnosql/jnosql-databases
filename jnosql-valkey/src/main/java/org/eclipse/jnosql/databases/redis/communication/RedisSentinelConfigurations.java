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
 * An enumeration to show the available options to connect to the Redis database by Sentinel configuration .
 * It implements {@link Supplier}, where its it returns the property name that might be
 * overwritten by the system environment using Eclipse Microprofile or Jakarta Config API.
 *
 * @see org.eclipse.jnosql.communication.Settings
 */
public enum RedisSentinelConfigurations implements Supplier<String> {
    /**
     * The key property that defines if the redis sentinel configuration should be loaded
     * */
    SENTINEL("jnosql.redis.sentinel"),
    /**
     * The value for the master name configuration attribute for the jedis client configuration with this configuration instance.
     */
    SENTINEL_MASTER_NAME("jnosql.redis.sentinel.master.name"),
    /**
     * The value for the sentinel HOST:PORT (separated by comma) configuration attribute for the jedis client configuration with this configuration instance.
     */
    SENTINEL_HOSTS("jnosql.redis.sentinel.hosts"),
    /**
     * The master client's name, the default value is 0
     */
    MASTER_CLIENT_NAME("jnosql.redis.sentinel.master.client.name"),
    /**
     * The slave client's name, the default value is 0
     */
    SLAVE_CLIENT_NAME("jnosql.redis.sentinel.slave.client.name"),
    /**
     * The master redis timeout, the default value is {@link redis.clients.jedis.Protocol#DEFAULT_TIMEOUT} on milliseconds
     */
    MASTER_TIMEOUT("jnosql.redis.sentinel.master.timeout"),
    /**
     * The slave redis timeout, the default value is {@link redis.clients.jedis.Protocol#DEFAULT_TIMEOUT} on milliseconds
     */
    SLAVE_TIMEOUT("jnosql.redis.sentinel.slave.timeout"),
    /**
     * The value for the connection timeout in milliseconds configuration attribute for the master jedis client configuration
     * created with this configuration instance.
     * The connection timeout on millis on {@link redis.clients.jedis.JedisClientConfig}, the default value is {@link redis.clients.jedis.Protocol#DEFAULT_TIMEOUT}
     */
    MASTER_CONNECTION_TIMEOUT("jnosql.redis.sentinel.master.connection.timeout"),
    /**
     * The value for the connection timeout in milliseconds configuration attribute for the slave jedis client configuration
     * created with this configuration instance.
     * The connection timeout on millis on {@link redis.clients.jedis.JedisClientConfig}, the default value is {@link redis.clients.jedis.Protocol#DEFAULT_TIMEOUT}
     */
    SLAVE_CONNECTION_TIMEOUT("jnosql.redis.sentinel.slave.connection.timeout"),
    /**
     * The value for the socket timeout in milliseconds configuration attribute for the master jedis client configuration with
     * this configuration instance.
     * The socket timeout on millis on {@link redis.clients.jedis.JedisClientConfig}, the default value is {@link redis.clients.jedis.Protocol#DEFAULT_TIMEOUT}
     */
    MASTER_SOCKET_TIMEOUT("jnosql.redis.sentinel.master.socket.timeout"),
    /**
     * The value for the socket timeout in milliseconds configuration attribute for the slave jedis client configuration with
     * this configuration instance.
     * The socket timeout on millis on {@link redis.clients.jedis.JedisClientConfig}, the default value is {@link redis.clients.jedis.Protocol#DEFAULT_TIMEOUT}
     */
    SLAVE_SOCKET_TIMEOUT("jnosql.redis.sentinel.slave.socket.timeout"),
    /**
     * The value for the user configuration attribute for the master jedis client configuration with this configuration instance.
     * The user on {@link redis.clients.jedis.JedisClientConfig}
     */
    MASTER_USER("jnosql.redis.sentinel.master.user"),
    /**
     * The value for the user configuration attribute for the slave jedis client configuration with this configuration instance.
     * The user on {@link redis.clients.jedis.JedisClientConfig}
     */
    SLAVE_USER("jnosql.redis.sentinel.slave.user"),
    /**
     * The value for the password configuration attribute for the master jedis client configuration with this configuration instance.
     * The user on {@link redis.clients.jedis.JedisClientConfig}
     */
    MASTER_PASSWORD("jnosql.redis.sentinel.master.password"),
    /**
     * The value for the password configuration attribute for the slave jedis client configuration with this configuration instance.
     * The user on {@link redis.clients.jedis.JedisClientConfig}
     */
    SLAVE_PASSWORD("jnosql.redis.sentinel.slave.password"),
    /**
     * The value for the ssl configuration attribute for the master jedis client configuration with this configuration instance.
     * The ssl on {@link redis.clients.jedis.JedisClientConfig}. The default value is false.
     */
    MASTER_SSL("jnosql.redis.sentinel.master.ssl"),
    /**
     * The value for the ssl configuration attribute for the slave jedis client configuration with this configuration instance.
     * The ssl on {@link redis.clients.jedis.JedisClientConfig}. The default value is false
     */
    SLAVE_SSL("jnosql.redis.sentinel.slave.ssl"),
    /**
     * The value for the protocol configuration attribute for the master jedis client configuration with this configuration instance.
     * The protocol on {@link redis.clients.jedis.JedisClientConfig}
     */
    MASTER_REDIS_PROTOCOL("jnosql.redis.sentinel.master.protocol"),
    /**
     * The value for the protocol configuration attribute for the slave jedis client configuration with this configuration instance.
     * The protocol on {@link redis.clients.jedis.JedisClientConfig}
     */
    SLAVE_REDIS_PROTOCOL("jnosql.redis.sentinel.slave.protocol"),
    /**
     * The value for the clientset info disabled configuration attribute for the master jedis client configuration with this configuration instance.
     * The clientset info disabled on {@link redis.clients.jedis.JedisClientConfig}
     */
    MASTER_CLIENTSET_INFO_CONFIG_DISABLED("jnosql.redis.sentinel.master.clientset.info.config.disabled"),
    /**
     * The value for the clientset info disabled configuration attribute for the slave jedis client configuration with this configuration instance.
     * The clientset info disabled on {@link redis.clients.jedis.JedisClientConfig}
     */
    SLAVE_CLIENTSET_INFO_CONFIG_DISABLED("jnosql.redis.sentinel.slave.clientset.info.config.disabled"),
    /**
     * The value for the clientset info configuration libname suffix attribute for the master jedis client configuration with this configuration instance.
     * The clientset info libname suffix on {@link redis.clients.jedis.JedisClientConfig}
     */
    MASTER_CLIENTSET_INFO_CONFIG_LIBNAME_SUFFIX("jnosql.redis.sentinel.master.clientset.info.config.libname.suffix"),
    /**
     * The value for the clientset info configuration libname suffix attribute for the slave jedis client configuration with this configuration instance.
     * The clientset info libname suffix on {@link redis.clients.jedis.JedisClientConfig}
     */
    SLAVE_CLIENTSET_INFO_CONFIG_LIBNAME_SUFFIX("jnosql.redis.sentinel.slave.clientset.info.config.libname.suffix");

    private final String configuration;

    RedisSentinelConfigurations(String configuration) {
        this.configuration = configuration;
    }

    @Override
    public String get() {
        return configuration;
    }

    public static enum SentinelMasterConfigurationsResolver implements RedisConfigurationsResolver {

        INSTANCE;

        @Override
        public Supplier<String> connectionTimeoutSupplier() {
            return RedisSentinelConfigurations.MASTER_CONNECTION_TIMEOUT;
        }

        @Override
        public Supplier<String> socketTimeoutSupplier() {
            return RedisSentinelConfigurations.MASTER_SOCKET_TIMEOUT;
        }

        @Override
        public Supplier<String> clientNameSupplier() {
            return RedisSentinelConfigurations.MASTER_CLIENT_NAME;
        }

        @Override
        public Supplier<String> userSupplier() {
            return RedisSentinelConfigurations.MASTER_USER;
        }

        @Override
        public Supplier<String> passwordSupplier() {
            return RedisSentinelConfigurations.MASTER_PASSWORD;
        }

        @Override
        public Supplier<String> timeoutSupplier() {
            return RedisSentinelConfigurations.MASTER_TIMEOUT;
        }

        @Override
        public Supplier<String> sslSupplier() {
            return RedisSentinelConfigurations.MASTER_SSL;
        }

        @Override
        public Supplier<String> redisProtocolSupplier() {
            return RedisSentinelConfigurations.MASTER_REDIS_PROTOCOL;
        }

        @Override
        public Supplier<String> clientsetInfoConfigLibNameSuffixSupplier() {
            return RedisSentinelConfigurations.MASTER_CLIENTSET_INFO_CONFIG_LIBNAME_SUFFIX;
        }

        @Override
        public Supplier<String> clientsetInfoConfigDisabled() {
            return RedisSentinelConfigurations.MASTER_CLIENTSET_INFO_CONFIG_DISABLED;
        }
    }

    public static enum SentinelSlaveConfigurationsResolver implements RedisConfigurationsResolver {

        INSTANCE;

        @Override
        public Supplier<String> connectionTimeoutSupplier() {
            return RedisSentinelConfigurations.SLAVE_CONNECTION_TIMEOUT;
        }

        @Override
        public Supplier<String> socketTimeoutSupplier() {
            return RedisSentinelConfigurations.SLAVE_SOCKET_TIMEOUT;
        }

        @Override
        public Supplier<String> clientNameSupplier() {
            return RedisSentinelConfigurations.SLAVE_CLIENT_NAME;
        }

        @Override
        public Supplier<String> userSupplier() {
            return RedisSentinelConfigurations.SLAVE_USER;
        }

        @Override
        public Supplier<String> passwordSupplier() {
            return RedisSentinelConfigurations.SLAVE_PASSWORD;
        }

        @Override
        public Supplier<String> timeoutSupplier() {
            return RedisSentinelConfigurations.SLAVE_TIMEOUT;
        }

        @Override
        public Supplier<String> sslSupplier() {
            return RedisSentinelConfigurations.SLAVE_SSL;
        }

        @Override
        public Supplier<String> redisProtocolSupplier() {
            return RedisSentinelConfigurations.SLAVE_REDIS_PROTOCOL;
        }

        @Override
        public Supplier<String> clientsetInfoConfigLibNameSuffixSupplier() {
            return RedisSentinelConfigurations.SLAVE_CLIENTSET_INFO_CONFIG_LIBNAME_SUFFIX;
        }

        @Override
        public Supplier<String> clientsetInfoConfigDisabled() {
            return RedisSentinelConfigurations.SLAVE_CLIENTSET_INFO_CONFIG_DISABLED;
        }
    }
}
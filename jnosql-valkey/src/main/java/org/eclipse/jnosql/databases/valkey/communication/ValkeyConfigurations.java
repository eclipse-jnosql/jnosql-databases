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


import java.util.function.Supplier;

/**
 * An enumeration to show the available options to connect to the Redis database.
 * It implements {@link Supplier}, where its it returns the property name that might be
 * overwritten by the system environment using Eclipse Microprofile or Jakarta Config API.
 *
 * @see org.eclipse.jnosql.communication.Settings
 */
public enum ValkeyConfigurations implements Supplier<String> {

    /**
     * The database host
     */
    HOST("jnosql.valkey.host"),
    /**
     * The database port
     */
    PORT("jnosql.valkey.port"),
    /**
     * The redis timeout, the default value is {@link io.valkey.Protocol#DEFAULT_TIMEOUT} on milliseconds
     */
    TIMEOUT("jnosql.valkey.timeout"),
    /**
     * The password's credential
     */
    PASSWORD("jnosql.valkey.password"),
    /**
     * The redis database number
     */
    DATABASE("jnosql.valkey.database"),
    /**
     * The cluster client's name. The default value is 0.
     */
    CLIENT_NAME("jnosql.valkey.client.name"),
    /**
     * The value for the maxTotal configuration attribute for pools created with this configuration instance.
     * The default value is {@link org.apache.commons.pool2.impl.GenericObjectPoolConfig#DEFAULT_MAX_TOTAL}
     */
    MAX_TOTAL("jnosql.valkey.max.total"),
    /**
     * The value for the maxIdle configuration attribute for pools created with this configuration instance.
     * The default value is {@link org.apache.commons.pool2.impl.GenericObjectPoolConfig#DEFAULT_MAX_IDLE}
     */
    MAX_IDLE("jnosql.valkey.max.idle"),
    /**
     * The value for the minIdle configuration attribute for pools created with this configuration instance.
     * The default value is {@link org.apache.commons.pool2.impl.GenericObjectPoolConfig#DEFAULT_MIN_IDLE}
     */
    MIN_IDLE("jnosql.valkey.min.idle"),
    /**
     * The value for the {@code maxWait} configuration attribute for pools created with this configuration instance.
     * The default value is {@link org.apache.commons.pool2.impl.GenericObjectPoolConfig#DEFAULT_MAX_WAIT_MILLIS}
     */
    MAX_WAIT_MILLIS("jnosql.valkey.max.wait.millis"),
    /**
     * The value for the connection timeout in milliseconds configuration attribute for the jedis client configuration
     * created with this configuration instance.
     * The connection timeout on millis on {@link io.valkey.JedisClientConfig}, the default value is {@link io.valkey.Protocol#DEFAULT_TIMEOUT}
     */
    CONNECTION_TIMEOUT("jnosql.valkey.connection.timeout"),
    /**
     * The value for the socket timeout in milliseconds configuration attribute for the jedis client configuration with
     * this configuration instance.
     * The socket timeout on millis on {@link io.valkey.JedisClientConfig}, the default value is {@link io.valkey.Protocol#DEFAULT_TIMEOUT}
     */
    SOCKET_TIMEOUT("jnosql.valkey.socket.timeout"),
    /**
     * The value for the user configuration attribute for the jedis client configuration with this configuration instance.
     * The user on {@link io.valkey.JedisClientConfig}
     */
    USER("jnosql.valkey.user"),
    /**
     * The value for the ssl configuration attribute for the jedis client configuration with this configuration instance.
     * The ssl on {@link io.valkey.JedisClientConfig}. The default value is false.
     */
    SSL("jnosql.valkey.ssl"),
    /**
     * The value for the protocol configuration attribute for the jedis client configuration with this configuration instance.
     * The protocol on {@link io.valkey.JedisClientConfig}.
     * The default value is not defined.
     */
    REDIS_PROTOCOL("jnosql.valkey.protocol"),
    /**
     * The value for the clientset info disabled configuration attribute for the jedis client configuration with this configuration instance.
     * The clientset info disabled on {@link io.valkey.JedisClientConfig}.
     * The default value is false.
     */
    CLIENTSET_INFO_CONFIG_DISABLED("jnosql.valkey.clientset.info.config.disabled"),
    /**
     * The value for the clientset info configuration libname suffix attribute for the jedis client configuration with this configuration instance.
     * The clientset info libname suffix on {@link io.valkey.JedisClientConfig}.
     * The default value is not defined.
     */
    CLIENTSET_INFO_CONFIG_LIBNAME_SUFFIX("jnosql.valkey.clientset.info.config.libname.suffix"),;

    private final String configuration;

    ValkeyConfigurations(String configuration) {
        this.configuration = configuration;
    }

    @Override
    public String get() {
        return configuration;
    }

    public static enum SingleValkeyConfigurationsResolver
            implements ValkeyConfigurationsResolver {

        INSTANCE;

        @Override
        public Supplier<String> connectionTimeoutSupplier() {
            return ValkeyConfigurations.CONNECTION_TIMEOUT;
        }

        @Override
        public Supplier<String> socketTimeoutSupplier() {
            return ValkeyConfigurations.SOCKET_TIMEOUT;
        }

        @Override
        public Supplier<String> clientNameSupplier() {
            return ValkeyConfigurations.CLIENT_NAME;
        }

        @Override
        public Supplier<String> userSupplier() {
            return ValkeyConfigurations.USER;
        }

        @Override
        public Supplier<String> passwordSupplier() {
            return ValkeyConfigurations.PASSWORD;
        }

        @Override
        public Supplier<String> timeoutSupplier() {
            return ValkeyConfigurations.TIMEOUT;
        }

        @Override
        public Supplier<String> sslSupplier() {
            return ValkeyConfigurations.SSL;
        }

        @Override
        public Supplier<String> redisProtocolSupplier() {
            return ValkeyConfigurations.REDIS_PROTOCOL;
        }

        @Override
        public Supplier<String> clientsetInfoConfigLibNameSuffixSupplier() {
            return ValkeyConfigurations.CLIENTSET_INFO_CONFIG_LIBNAME_SUFFIX;
        }

        @Override
        public Supplier<String> clientsetInfoConfigDisabled() {
            return ValkeyConfigurations.CLIENTSET_INFO_CONFIG_DISABLED;
        }
    }
}

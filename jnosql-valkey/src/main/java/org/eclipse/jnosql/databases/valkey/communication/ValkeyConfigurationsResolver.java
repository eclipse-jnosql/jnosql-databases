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

package org.eclipse.jnosql.databases.valkey.communication;

import java.util.function.Supplier;

/**
 * Defines the valkey configurations resolver contract.
 */
public sealed interface ValkeyConfigurationsResolver permits
        ValkeyConfigurations.SingleValkeyConfigurationsResolver,
        ValkeyClusterConfigurations.ClusterConfigurationsResolver,
        ValkeySentinelConfigurations.SentinelMasterConfigurationsResolver,
        ValkeySentinelConfigurations.SentinelSlaveConfigurationsResolver {

/**
 * Returns the connection timeout supplier.
 *
 * @return the result
 */
    Supplier<String> connectionTimeoutSupplier();

/**
 * Returns the socket timeout supplier.
 *
 * @return the result
 */
    Supplier<String> socketTimeoutSupplier();

/**
 * Returns the client name supplier.
 *
 * @return the result
 */
    Supplier<String> clientNameSupplier();

/**
 * Returns the user supplier.
 *
 * @return the result
 */
    Supplier<String> userSupplier();

/**
 * Returns the password supplier.
 *
 * @return the result
 */
    Supplier<String> passwordSupplier();

/**
 * Returns the timeout supplier.
 *
 * @return the result
 */
    Supplier<String> timeoutSupplier();

/**
 * Returns the ssl supplier.
 *
 * @return the result
 */
    Supplier<String> sslSupplier();

/**
 * Returns the redis protocol supplier.
 *
 * @return the result
 */
    Supplier<String> redisProtocolSupplier();

/**
 * Returns the clientset info config lib name suffix supplier.
 *
 * @return the result
 */
    Supplier<String> clientsetInfoConfigLibNameSuffixSupplier();

/**
 * Returns the clientset info config disabled.
 *
 * @return the result
 */
    Supplier<String> clientsetInfoConfigDisabled();
}

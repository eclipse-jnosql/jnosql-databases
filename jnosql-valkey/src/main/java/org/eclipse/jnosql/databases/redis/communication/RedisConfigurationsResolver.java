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

public sealed interface RedisConfigurationsResolver permits
        RedisConfigurations.SingleRedisConfigurationsResolver,
        RedisClusterConfigurations.ClusterConfigurationsResolver,
        RedisSentinelConfigurations.SentinelMasterConfigurationsResolver,
        RedisSentinelConfigurations.SentinelSlaveConfigurationsResolver {

    Supplier<String> connectionTimeoutSupplier();

    Supplier<String> socketTimeoutSupplier();

    Supplier<String> clientNameSupplier();

    Supplier<String> userSupplier();

    Supplier<String> passwordSupplier();

    Supplier<String> timeoutSupplier();

    Supplier<String> sslSupplier();

    Supplier<String> redisProtocolSupplier();

    Supplier<String> clientsetInfoConfigLibNameSuffixSupplier();

    Supplier<String> clientsetInfoConfigDisabled();
}
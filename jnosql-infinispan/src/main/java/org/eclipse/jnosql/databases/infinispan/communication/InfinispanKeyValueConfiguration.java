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
 *   The Infinispan Team
 */

package org.eclipse.jnosql.databases.infinispan.communication;

import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.SettingsBuilder;
import org.eclipse.jnosql.communication.keyvalue.KeyValueConfiguration;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The Infinispan implementation of {@link KeyValueConfiguration} that returns
 * {@link InfinispanBucketManagerFactory}.
 * @see InfinispanConfigurations
 *
 */
public class InfinispanKeyValueConfiguration implements KeyValueConfiguration {


    /**
     * Creates a {@link InfinispanBucketManagerFactory} from configuration map
     * @param configurations the configuration map
     * @return the InfinispanBucketManagerFactory instance
     * @throws NullPointerException when configurations is null
     */
    public InfinispanBucketManagerFactory get(Map<String, String> configurations) {
        requireNonNull(configurations, "configurations is required");
        SettingsBuilder builder = Settings.builder();
        configurations.forEach(builder::put);
        return apply(builder.build());
    }



    @Override
    public InfinispanBucketManagerFactory apply(Settings settings) {
        requireNonNull(settings, "settings is required");

        List<String> servers = settings.prefixSupplier(Arrays.asList(InfinispanConfigurations.HOST, Configurations.HOST))
                .stream().map(Object::toString).toList();

        Optional<String> config = settings.get(InfinispanConfigurations.CONFIG)
                .map(Object::toString);
        if (!servers.isEmpty()) {
            org.infinispan.client.hotrod.configuration.ConfigurationBuilder builder = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
            for(String server : servers) {
                builder.addServer().host(server);
            }
            return  new InfinispanBucketManagerFactory(new RemoteCacheManager(builder.build()));
        } else if (config.isPresent()) {
            try {
                return new InfinispanBucketManagerFactory(new DefaultCacheManager(config.get()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            GlobalConfigurationBuilder builder = new GlobalConfigurationBuilder();
            builder.jmx().enable();
            return new InfinispanBucketManagerFactory(new DefaultCacheManager(builder.build()));
        }
    }
}

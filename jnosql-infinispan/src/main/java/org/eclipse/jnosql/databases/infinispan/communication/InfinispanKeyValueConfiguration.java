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
 *   The Infinispan Team
 */

package org.eclipse.jnosql.databases.infinispan.communication;

import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.SettingsBuilder;
import org.eclipse.jnosql.communication.keyvalue.KeyValueConfiguration;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The Infinispan implementation of {@link KeyValueConfiguration}. It creates an
 * {@link InfinispanBucketManagerFactory} in one of three modes, chosen in this order:
 * <ol>
 *   <li>when {@link InfinispanConfigurations#HOST} is set, a Hot Rod client connects to those servers;</li>
 *   <li>when {@link InfinispanConfigurations#CONFIG} is set, an embedded cache manager starts from that
 *       configuration file;</li>
 *   <li>otherwise, an embedded cache manager starts with default settings.</li>
 * </ol>
 *
 * <p>Example:</p>
 * {@snippet lang="java" :
 * Settings settings = Settings.builder()
 *         .put(InfinispanConfigurations.HOST, "localhost")
 *         .build();
 * InfinispanBucketManagerFactory factory = new InfinispanKeyValueConfiguration().apply(settings);
 * InfinispanBucketManager bucket = factory.apply("users");
 * }
 *
 * @see InfinispanConfigurations
 */
public class InfinispanKeyValueConfiguration implements KeyValueConfiguration {

    /**
     * Creates an {@link InfinispanBucketManagerFactory} from a map of configuration keys, such as the values of
     * {@link InfinispanConfigurations}. It is equivalent to {@link #apply(Settings)} with the same entries.
     *
     * @param configurations the configuration keys and values
     * @return a new factory
     * @throws NullPointerException if {@code configurations} is {@code null}
     */
    public InfinispanBucketManagerFactory get(Map<String, String> configurations) {
        requireNonNull(configurations, "configurations is required");
        SettingsBuilder builder = Settings.builder();
        configurations.forEach(builder::put);
        return apply(builder.build());
    }

    /**
     * Creates an {@link InfinispanBucketManagerFactory} backed by an embedded cache manager built from an
     * Infinispan {@link GlobalConfiguration}.
     *
     * @param config the Infinispan global configuration
     * @return a new factory
     * @throws NullPointerException if {@code config} is {@code null}
     */
    public InfinispanBucketManagerFactory get(GlobalConfiguration config) throws NullPointerException {
        requireNonNull(config, "config is required");

        return new InfinispanBucketManagerFactory(new DefaultCacheManager(config));
    }


    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code settings} is {@code null}
     * @throws RuntimeException     if the configuration file given by {@link InfinispanConfigurations#CONFIG}
     *                              cannot be read
     */
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

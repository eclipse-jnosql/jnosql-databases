/*
 *  Copyright (c) 2023 Eclipse Contribuitor
 * All rights reserved. This program and the accompanying materials
 *  and Apache License v2.0 which accompanies this distribution.
 *  The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *  and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *    You may elect to redistribute this code under either of these licenses.
 */
package org.eclipse.jnosql.databases.redis.mapping;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.databases.redis.communication.RedisBucketManagerFactory;
import org.eclipse.jnosql.databases.redis.communication.RedisConfiguration;
import org.eclipse.jnosql.mapping.core.config.MicroProfileSettings;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
class BucketManagerFactorySupplier implements Supplier<RedisBucketManagerFactory> {

    private static final Logger LOGGER = Logger.getLogger(BucketManagerFactorySupplier.class.getName());

    @Override
    @Produces
    @Typed(RedisBucketManagerFactory.class)
    public RedisBucketManagerFactory get() {
        Settings settings = MicroProfileSettings.INSTANCE;
        RedisConfiguration configuration = new RedisConfiguration();
        return configuration.apply(settings);
    }

    public void close(@Disposes RedisBucketManagerFactory factory) {
        LOGGER.log(Level.FINEST, "Closing RedisBucketManagerFactory resource");
        factory.close();
    }
}

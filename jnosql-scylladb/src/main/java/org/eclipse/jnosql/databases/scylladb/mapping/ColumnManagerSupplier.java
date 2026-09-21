/*
 *  Copyright (c) 2022 Eclipse Contribuitor
 * All rights reserved. This program and the accompanying materials
 *  and Apache License v2.0 which accompanies this distribution.
 *  The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *  and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *    You may elect to redistribute this code under either of these licenses.
 */

package org.eclipse.jnosql.databases.scylladb.mapping;

import jakarta.data.exceptions.MappingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.databases.scylladb.communication.ScyllaDBColumnManager;
import org.eclipse.jnosql.databases.scylladb.communication.ScyllaDBColumnManagerFactory;
import org.eclipse.jnosql.databases.scylladb.communication.ScyllaDBConfiguration;
import org.eclipse.jnosql.mapping.core.config.MicroProfileSettings;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.eclipse.jnosql.mapping.core.config.MappingConfigurations.COLUMN_DATABASE;

@ApplicationScoped
class ColumnManagerSupplier implements Supplier<ScyllaDBColumnManager> {

    private static final Logger LOGGER = Logger.getLogger(ColumnManagerSupplier.class.getName());


    @Override
    @Produces
    @Typed(ScyllaDBColumnManager.class)
    public ScyllaDBColumnManager get() {
        Settings settings = MicroProfileSettings.INSTANCE;
        ScyllaDBConfiguration configuration = new ScyllaDBConfiguration();
        ScyllaDBColumnManagerFactory factory = configuration.apply(settings);
        Optional<String> database = settings.get(COLUMN_DATABASE, String.class);
        String db = database.orElseThrow(() -> new MappingException("Please, inform the database filling up the property "
                + COLUMN_DATABASE.get()));
        ScyllaDBColumnManager manager = factory.apply(db);
        LOGGER.log(Level.FINEST, "Starting  a ScyllaDBColumnManager instance using Eclipse MicroProfile Config," +
                " database name: " + db);
        return manager;
    }

    public void close(@Disposes ScyllaDBColumnManager manager) {
        LOGGER.log(Level.FINEST, "Closing ScyllaDBColumnManager resource, database name: " + manager.name());
        manager.close();
    }

}

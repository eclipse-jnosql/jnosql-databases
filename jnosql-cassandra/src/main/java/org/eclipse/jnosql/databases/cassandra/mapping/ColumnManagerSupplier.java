/*
 *  Copyright (c) 2022 Eclipse Contribuitor
 * All rights reserved. This program and the accompanying materials
 *  and Apache License v2.0 which accompanies this distribution.
 *  The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *  and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *    You may elect to redistribute this code under either of these licenses.
 */

package org.eclipse.jnosql.databases.cassandra.mapping;

import jakarta.data.exceptions.MappingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.databases.cassandra.communication.CassandraColumnManager;
import org.eclipse.jnosql.databases.cassandra.communication.CassandraColumnManagerFactory;
import org.eclipse.jnosql.databases.cassandra.communication.CassandraConfiguration;
import org.eclipse.jnosql.mapping.core.config.MicroProfileSettings;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.eclipse.jnosql.mapping.core.config.MappingConfigurations.COLUMN_DATABASE;

@ApplicationScoped
class ColumnManagerSupplier implements Supplier<CassandraColumnManager> {

    private static final Logger LOGGER = Logger.getLogger(ColumnManagerSupplier.class.getName());


    @Override
    @Produces
    @Typed(CassandraColumnManager.class)
    public CassandraColumnManager get() {
        Settings settings = MicroProfileSettings.INSTANCE;
        CassandraConfiguration configuration = new CassandraConfiguration();
        CassandraColumnManagerFactory factory = configuration.apply(settings);
        Optional<String> database = settings.get(COLUMN_DATABASE, String.class);
        String db = database.orElseThrow(() -> new MappingException("Please, inform the database filling up the property "
                + COLUMN_DATABASE.get()));
        CassandraColumnManager manager = factory.apply(db);
        LOGGER.log(Level.FINEST, "Starting  a CassandraColumnManager instance using Eclipse MicroProfile Config," +
                " database name: " + db);
        return manager;
    }

    public void close(@Disposes CassandraColumnManager manager) {
        LOGGER.log(Level.FINEST, "Closing CassandraColumnManager resource, database name: " + manager.name());
        manager.close();
    }

}

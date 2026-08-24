/*
 *  Copyright (c) 2024 Eclipse Contribuitor
 * All rights reserved. This program and the accompanying materials
 *  and Apache License v2.0 which accompanies this distribution.
 *  The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *  and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *    You may elect to redistribute this code under either of these licenses.
 */

package org.eclipse.jnosql.databases.oracle.mapping;


import jakarta.data.exceptions.MappingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.databases.oracle.communication.OracleDocumentConfiguration;
import org.eclipse.jnosql.databases.oracle.communication.OracleDocumentManagerFactory;
import org.eclipse.jnosql.databases.oracle.communication.OracleNoSQLDocumentManager;
import org.eclipse.jnosql.mapping.core.config.MicroProfileSettings;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.eclipse.jnosql.mapping.core.config.MappingConfigurations.DOCUMENT_DATABASE;

@ApplicationScoped
class DocumentManagerSupplier implements Supplier<OracleNoSQLDocumentManager> {

    private static final Logger LOGGER = Logger.getLogger(DocumentManagerSupplier.class.getName());


    @Override
    @Produces
    @Typed(OracleNoSQLDocumentManager.class)
    @ApplicationScoped
    public OracleNoSQLDocumentManager get() {
        Settings settings = MicroProfileSettings.INSTANCE;
        OracleDocumentConfiguration configuration = new OracleDocumentConfiguration();
        OracleDocumentManagerFactory factory = configuration.apply(settings);
        Optional<String> database = settings.get(DOCUMENT_DATABASE, String.class);
        String db = database.orElseThrow(() -> new MappingException("Please, inform the database filling up the property "
                + DOCUMENT_DATABASE.get()));
        OracleNoSQLDocumentManager manager = factory.apply(db);
        LOGGER.log(Level.FINEST, "Starting  a OracleDocumentManager instance using Eclipse MicroProfile Config," +
                " database name: " + db);
        return manager;
    }

    public void close(@Disposes OracleNoSQLDocumentManager manager) {
        LOGGER.log(Level.FINEST, "Closing OracleDocumentManager resource, database name: " + manager.name());
        manager.close();
    }

}

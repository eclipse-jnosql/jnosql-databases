/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 * and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *
 * Maximillian Arruda
 */

package org.eclipse.jnosql.databases.dynamodb.mapping;

import jakarta.data.exceptions.MappingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.databases.dynamodb.communication.DynamoDBDatabaseManager;
import org.eclipse.jnosql.databases.dynamodb.communication.DynamoDBDocumentConfiguration;
import org.eclipse.jnosql.mapping.core.config.MicroProfileSettings;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.eclipse.jnosql.mapping.core.config.MappingConfigurations.DOCUMENT_DATABASE;

@ApplicationScoped
public class DocumentManagerSupplier implements Supplier<DynamoDBDatabaseManager> {

    private static final Logger LOGGER = Logger.getLogger(DocumentManagerSupplier.class.getName());

    @Override
    @Produces
    @Typed(DynamoDBDatabaseManager.class)
    @ApplicationScoped
    public DynamoDBDatabaseManager get() {
        Settings settings = MicroProfileSettings.INSTANCE;
        var configuration = new DynamoDBDocumentConfiguration();
        var factory = configuration.apply(settings);
        Optional<String> database = settings.get(DOCUMENT_DATABASE, String.class);
        String db = database.orElseThrow(() -> new MappingException("Please, inform the database filling up the property "
                + DOCUMENT_DATABASE.get()));
        DynamoDBDatabaseManager manager = (DynamoDBDatabaseManager) factory.apply(db);
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, """
                    Starting a DynamoDBDocumentManager instance using Eclipse MicroProfile Config,\
                    database name: %s
                    """.formatted(db));
        }
        return manager;
    }

    public void close(@Disposes DynamoDBDatabaseManager manager) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "Closing OracleDocumentManager resource, database name: %s".formatted(manager.name()));
        }
        manager.close();
    }
}

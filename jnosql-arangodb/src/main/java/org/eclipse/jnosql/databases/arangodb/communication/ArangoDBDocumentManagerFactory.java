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
 *   Otavio Santana
 */
package org.eclipse.jnosql.databases.arangodb.communication;


import com.arangodb.ArangoDB;
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;


/**
 * Factory for ArangoDB document managers. Create it with
 * {@link ArangoDBDocumentConfiguration#apply(org.eclipse.jnosql.communication.Settings)}.
 *
 * <p>{@code factory.apply(database)} opens a new ArangoDB connection and returns an
 * {@link ArangoDBDocumentManager} for that database, creating the database when it does not exist and
 * the account can create it. Closing the factory has no effect; close each manager to shut down its
 * connection.</p>
 */
public final class ArangoDBDocumentManagerFactory implements DatabaseManagerFactory {


    private final ArangoDBBuilder arangoDBBuilder;

    ArangoDBDocumentManagerFactory(ArangoDBBuilder arangoDBBuilder) {
        this.arangoDBBuilder = arangoDBBuilder;
    }

    @Override
    public ArangoDBDocumentManager apply(String database) {
        ArangoDB arangoDB = arangoDBBuilder.build();
        ArangoDBUtil.checkDatabase(database, arangoDB);
        return new DefaultArangoDBDocumentManager(database, arangoDB);
    }

    @Override
    public void close() {
        // no-op
        // ArangoDB driver instance will be closed in ArangoDBDocumentManager.close()
    }
}

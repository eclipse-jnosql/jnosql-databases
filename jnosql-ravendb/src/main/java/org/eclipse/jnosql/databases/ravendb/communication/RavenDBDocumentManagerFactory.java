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

package org.eclipse.jnosql.databases.ravendb.communication;

import net.ravendb.client.documents.DocumentStore;
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;

import java.util.Arrays;
import java.util.Objects;

/**
 * The RavenDB implementation to {@link DatabaseManagerFactory}
 * <br/>
 * Closing a {@link RavenDBDocumentManagerFactory} has no effect.
 */
public class RavenDBDocumentManagerFactory implements DatabaseManagerFactory {

    private final String[] hosts;

    RavenDBDocumentManagerFactory(String[] hosts) {
        this.hosts = hosts;
    }

    @Override
    public RavenDBDocumentManager apply(String database) {
        Objects.requireNonNull(database, "database is required");
        DocumentStore documentStore = new DocumentStore(hosts, database);
        return new RavenDBDocumentManager(documentStore, database);
    }

    /**
     * Closing a {@link RavenDBDocumentManagerFactory} has no effect.
     */
    @Override
    public void close() {

    }

    @Override
    public String toString() {
        return "RavenDBDocumentManagerFactory{" + "hosts=" + Arrays.toString(hosts) +
                '}';
    }
}

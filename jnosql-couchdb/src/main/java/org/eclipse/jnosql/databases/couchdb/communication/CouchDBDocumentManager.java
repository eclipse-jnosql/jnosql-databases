/*
 *
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
 *
 */
package org.eclipse.jnosql.databases.couchdb.communication;

import org.eclipse.jnosql.communication.semistructured.DatabaseManager;

/**
 * A CouchDB-specific extension of {@link DatabaseManager} that provides an additional
 * feature to count the number of documents in the database.
 * This interface offers a {@code count()} method to retrieve the total number of documents
 * stored in the CouchDB database. It extends the {@link DatabaseManager} to align with
 * Eclipse JNoSQL's database management abstraction.
 * Example Usage:
 * <pre>{@code
 * @Inject
 * private CouchDBDocumentManager documentManager;
 *
 * long totalDocuments = documentManager.count();
 * }</pre>
 *
 * @see DatabaseManager
 */
public interface CouchDBDocumentManager extends DatabaseManager {

    /**
     * Retrieves the total number of documents in the database.
     * Note: Not all CouchDB implementations support this feature. If the operation is not
     * supported, an {@link UnsupportedOperationException} will be thrown.
     *
     * @return the total number of documents in the database
     * @throws UnsupportedOperationException if the database does not support counting documents
     */
    long count();
}

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
package org.eclipse.jnosql.databases.couchbase.communication;


import com.couchbase.client.java.json.JsonObject;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;

import java.util.stream.Stream;

/**
 * The couchbase implementation of {@link DatabaseManager}
 */
public interface CouchbaseDocumentManager extends DatabaseManager {

    /**
     * Executes the N1QL with params and then result que result
     *
     * @param n1ql   the query
     * @param params the params
     * @return the query result
     * @throws NullPointerException when either n1ql or params are null
     */
    Stream<CommunicationEntity> n1qlQuery(String n1ql, JsonObject params) throws NullPointerException;


    /**
     * Executes the n1qlquery  plain query and then result que result
     *
     * @param n1ql the query
     * @return the query result
     * @throws NullPointerException when either n1ql or params are null
     */
    Stream<CommunicationEntity> n1qlQuery(String n1ql) throws NullPointerException;

}

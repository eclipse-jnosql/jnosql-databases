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
package org.eclipse.jnosql.databases.orientdb.communication;


import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.util.Map;
import java.util.stream.Stream;

/**
 * The orientdb implementation to {@link DatabaseManager}, this implementation
 * does not support TTL.
 * <p>{@link OrientDBDocumentManager#insert(CommunicationEntity, java.time.Duration)}</p>
 * Also this implementation has support SQL query and also live query.
 * <p>{@link OrientDBDocumentManager#sql(String, Object...)}</p>
 */
public interface OrientDBDocumentManager extends DatabaseManager {
    /**
     * Find using query
     *
     * @param query  the query
     * @param params the params
     * @return the query result
     * @throws NullPointerException when either query or params are null
     */
    Stream<CommunicationEntity> sql(String query, Object... params);

    /**
     * Find using query
     *
     * @param query  the query
     * @param params the params
     * @return the query result
     * @throws NullPointerException when either query or params are null
     */
    Stream<CommunicationEntity> sql(String query, Map<String, Object> params);

    /**
     * Execute live query
     *
     * @param query     the query
     * @param callbacks Callbacks for create, update and delete operations
     * @throws NullPointerException when both query and callBack are null
     */
    void live(SelectQuery query, OrientDBLiveCallback<CommunicationEntity> callbacks);

    /**
     * Execute live query
     *
     * @param query     the query
     * @param callbacks Callbacks for create, update and delete operations
     * @throws NullPointerException when both query and callBack are null
     */
    void live(String query, OrientDBLiveCallback<CommunicationEntity> callbacks, Object... params);
}

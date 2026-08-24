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

package org.eclipse.jnosql.databases.cassandra.communication;


import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;

import java.util.List;

/**
 * The Cassandra implementation to {@link DatabaseManagerFactory}
 * <br/>
 * Closing a {@link CassandraColumnManagerFactory} has no effect.
 */
public class CassandraColumnManagerFactory implements DatabaseManagerFactory {

    private final CqlSessionBuilder sessionBuilder;

    CassandraColumnManagerFactory(final CqlSessionBuilder sessionBuilder, List<String> queries) {
        this.sessionBuilder = sessionBuilder;
        load(queries);
    }

    void load(List<String> queries) {
        final CqlSession session = sessionBuilder.build();
        queries.forEach(session::execute);
        session.close();
    }

    @Override
    public CassandraColumnManager apply(String database) {
        return new DefaultCassandraColumnManager(sessionBuilder.build(), database);
    }

    /**
     * Closing a {@link CassandraColumnManagerFactory} has no effect.
     */
    @Override
    public void close() {
    }

    @Override
    public String toString() {
        return "CassandraColumnManagerFactory{" + "cluster=" + sessionBuilder +
                '}';
    }
}

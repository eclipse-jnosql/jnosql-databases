/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
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
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import java.util.Collections;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.interceptor.Interceptor;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.DatabaseType;
import org.mockito.Mockito;

/**
 * Produces the real Cassandra manager over a mocked client so tests can capture the native requests.
 */
@Alternative
@Priority(Interceptor.Priority.APPLICATION)
@ApplicationScoped
public class MockedCassandraProducer {

    public static final CqlSession SESSION = Mockito.mock(CqlSession.class);

    public static void reset() {
        Mockito.reset(SESSION);
        var resultSet = Mockito.mock(ResultSet.class);
        Mockito.when(resultSet.iterator()).thenAnswer(i -> Collections.emptyIterator());
        Mockito.when(SESSION.execute(Mockito.any(SimpleStatement.class))).thenReturn(resultSet);
    }

    @Produces
    @Database(DatabaseType.COLUMN)
    public DatabaseManager columnManager() {
        reset();
        return new DefaultCassandraColumnManager(SESSION, "library");
    }
}

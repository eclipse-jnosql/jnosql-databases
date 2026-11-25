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
package org.eclipse.jnosql.databases.orientdb.communication;

import com.orientechnologies.orient.core.db.ODatabasePool;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.interceptor.Interceptor;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.DatabaseType;
import org.mockito.invocation.Invocation;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Produces the real OrientDB communication manager on top of a mocked connection pool.
 */
@Alternative
@Priority(Interceptor.Priority.APPLICATION + 1)
@ApplicationScoped
public class MockedOrientProducer {

    public static final ODatabasePool POOL = mock(ODatabasePool.class);
    public static final ODatabaseSession SESSION = mock(ODatabaseSession.class);

    @Produces
    public OrientDBDocumentManager manager() {
        return new DefaultOrientDBDocumentManager(POOL, "library");
    }

    @Produces
    @Database(DatabaseType.DOCUMENT)
    public DatabaseManager documentManager() {
        return manager();
    }

    public static void reset() {
        org.mockito.Mockito.reset(POOL, SESSION);
        OResultSet empty = mock(OResultSet.class);
        when(empty.hasNext()).thenReturn(false);
        when(POOL.acquire()).thenReturn(SESSION);
        when(SESSION.command(anyString(), org.mockito.ArgumentMatchers.any(Object[].class))).thenReturn(empty);
    }

    /**
     * @return the SQL commands that reached the session
     */
    public static List<Invocation> commands() {
        return mockingDetails(SESSION).getInvocations().stream()
                .filter(call -> call.getMethod().getName().equals("command")).toList();
    }

    /**
     * @return the number of connections acquired from the pool
     */
    public static long acquisitions() {
        return mockingDetails(POOL).getInvocations().stream()
                .filter(call -> call.getMethod().getName().equals("acquire")).count();
    }
}

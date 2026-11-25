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
package org.eclipse.jnosql.databases.oracle.communication;

import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.json.bind.Jsonb;
import oracle.nosql.driver.NoSQLHandle;
import oracle.nosql.driver.ops.PrepareRequest;
import oracle.nosql.driver.ops.PrepareResult;
import oracle.nosql.driver.ops.PreparedStatement;
import oracle.nosql.driver.ops.QueryRequest;
import oracle.nosql.driver.ops.QueryResult;

import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.DatabaseType;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Produces the real Oracle NoSQL communication manager on top of a mocked {@link NoSQLHandle}.
 */
@Alternative
public class MockedHandleProducer {

    public static final NoSQLHandle HANDLE = mock(NoSQLHandle.class);
    public static final PreparedStatement STATEMENT = mock(PreparedStatement.class);

    @Produces
    public OracleNoSQLDocumentManager manager() {
        return new DefaultOracleNoSQLDocumentManager("library", HANDLE, mock(Jsonb.class));
    }

    @Produces
    @Database(DatabaseType.DOCUMENT)
    public DatabaseManager documentManager() {
        return manager();
    }

    public static void reset() {
        org.mockito.Mockito.reset(HANDLE, STATEMENT);
        when(HANDLE.prepare(any(PrepareRequest.class)))
                .thenReturn(new PrepareResult().setPreparedStatement(STATEMENT));
        when(HANDLE.query(any(QueryRequest.class))).thenAnswer(invocation -> {
            QueryRequest request = invocation.getArgument(0);
            request.setContKey(null);
            return new QueryResult(request).setResults(List.of());
        });
    }
}

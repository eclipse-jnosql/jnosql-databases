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
package org.eclipse.jnosql.databases.neo4j.communication;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import jakarta.interceptor.Interceptor;
import org.mockito.Mockito;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;

import java.util.ArrayList;

/**
 * Produces the real Neo4j manager over a mocked driver session so tests can capture the Cypher requests.
 */
@Alternative
@Priority(Interceptor.Priority.APPLICATION)
@ApplicationScoped
public class MockedNeo4JProducer {

    public static final Session SESSION = Mockito.mock(Session.class);
    public static final Transaction TRANSACTION = Mockito.mock(Transaction.class);

    public static void reset() {
        Mockito.reset(SESSION, TRANSACTION);
        var result = Mockito.mock(Result.class);
        Mockito.when(result.list(Mockito.any())).thenReturn(new ArrayList<>());
        Mockito.when(SESSION.beginTransaction()).thenReturn(TRANSACTION);
        Mockito.when(TRANSACTION.run(Mockito.anyString(), Mockito.any(Value.class))).thenReturn(result);
    }

    @Produces
    @ApplicationScoped
    @Typed(Neo4JDatabaseManager.class)
    public Neo4JDatabaseManager manager() {
        reset();
        return new DefaultNeo4JDatabaseManager(SESSION, "neo4j");
    }
}

/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License 2.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *   and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 *   You may elect to redistribute this code under either of these licenses.
 */
package org.eclipse.jnosql.databases.questdb.communication;

import io.questdb.client.QuestDB;
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;

import java.util.Objects;

/**
 * Creates time-series managers backed by one QuestDB deployment.
 *
 * <p>QuestDB has one logical database per server. The database name remains the
 * JNoSQL manager name while entity names map to QuestDB tables.</p>
 */
public class QuestDBTimeSeriesManagerFactory implements DatabaseManagerFactory {

    private final String url;

    private QuestDB questDB;

    private boolean closed;

    QuestDBTimeSeriesManagerFactory(String url) {
        this.url = url;
    }

    QuestDBTimeSeriesManagerFactory(QuestDB questDB) {
        this.url = null;
        this.questDB = questDB;
    }

    @Override
    public synchronized QuestDBTimeSeriesManager apply(String database) {
        Objects.requireNonNull(database, "database is required");
        if (database.isBlank()) {
            throw new IllegalArgumentException("database is required");
        }
        if (closed) {
            throw new IllegalStateException("QuestDB manager factory is closed");
        }
        if (questDB == null) {
            questDB = QuestDB.connect(url);
        }
        return new QuestDBTimeSeriesManager(questDB, database);
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            if (questDB != null) {
                questDB.close();
            }
        }
    }
}

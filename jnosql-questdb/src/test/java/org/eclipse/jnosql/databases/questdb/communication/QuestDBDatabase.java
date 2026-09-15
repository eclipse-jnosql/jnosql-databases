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

import org.eclipse.jnosql.communication.Settings;
import org.testcontainers.containers.QuestDBContainer;

import java.util.Map;

public enum QuestDBDatabase {

    INSTANCE;

    public static final String DATABASE = "qdb";

    private final QuestDBContainer questDB = new QuestDBContainer("questdb/questdb:10.0.1");

    {
        questDB.start();
    }

    public QuestDBTimeSeriesManager manager() {
        Settings settings = Settings.of(Map.of(
                QuestDBTimeSeriesConfigurations.URL.get(), url()));
        return new QuestDBTimeSeriesConfiguration().apply(settings).apply(DATABASE);
    }

    public String url() {
        return "ws::addr=" + questDB.getHost() + ":" + questDB.getMappedPort(9000) + ";";
    }
}

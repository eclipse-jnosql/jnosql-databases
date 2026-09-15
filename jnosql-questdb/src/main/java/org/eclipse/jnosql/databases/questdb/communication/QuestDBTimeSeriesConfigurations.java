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

import java.util.function.Supplier;

/**
 * Connection settings for QuestDB's native QWP client.
 */
public enum QuestDBTimeSeriesConfigurations implements Supplier<String> {

    /**
     * QuestDB client connection string, for example {@code ws::addr=localhost:9000;}.
     */
    URL("jnosql.questdb.url");

    private final String configuration;

    QuestDBTimeSeriesConfigurations(String configuration) {
        this.configuration = configuration;
    }

    @Override
    public String get() {
        return configuration;
    }
}

/*
 *  Copyright (c) 2025 Contributors to the Eclipse Foundation
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *  The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *  and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *  You may elect to redistribute this code under either of these licenses.
 *  Contributors:
 *  Otavio Santana
 */
package org.eclipse.jnosql.databases.tinkerpop.mapping.configuration;

import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.graph.GraphDatabaseManager;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;
import org.mockito.Mockito;

public class GraphDatabaseConfiguration implements DatabaseConfiguration {
    @Override
    public DatabaseManagerFactory apply(Settings settings) {
        return new DatabaseManagerFactoryGraph();
    }

    static class DatabaseManagerFactoryGraph implements DatabaseManagerFactory{

        @Override
        public void close() {

        }

        @Override
        public DatabaseManager apply(String s) {
            return Mockito.mock(GraphDatabaseManager.class);
        }
    }
}

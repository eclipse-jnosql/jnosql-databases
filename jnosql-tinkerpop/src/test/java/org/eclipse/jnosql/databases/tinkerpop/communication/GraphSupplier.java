/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.databases.tinkerpop.communication;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;

import java.util.function.Supplier;
import java.util.logging.Logger;

public enum GraphSupplier implements Supplier<Graph> {
    INSTANCE;

    private static final Logger LOGGER = Logger.getLogger(GraphSupplier.class.getName());

    private final Graph graph;

    {
        graph = GraphFactory.open("src/test/resources/adb.yaml");
    }

    @Override
    public Graph get() {
        LOGGER.info("Starting Graph database");
        return graph;
    }
}

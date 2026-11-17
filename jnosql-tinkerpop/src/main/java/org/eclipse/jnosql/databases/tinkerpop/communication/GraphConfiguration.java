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
package org.eclipse.jnosql.databases.tinkerpop.communication;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.jnosql.communication.CommunicationException;
import org.eclipse.jnosql.communication.Settings;

import java.util.ServiceLoader;
import java.util.function.Function;

/**
 * The SPI that creates the Apache TinkerPop {@link Graph} for a given {@link Settings}. Implement it for each
 * TinkerPop provider (for example Neo4j-Gremlin, JanusGraph, TinkerGraph) and register it in
 * {@code META-INF/services/org.eclipse.jnosql.databases.tinkerpop.communication.GraphConfiguration}.
 *
 * <p>The provider is selected with the {@code jnosql.graph.tinkerpop.provider} setting, which holds the
 * implementation class; when it is absent, the first implementation found by {@link ServiceLoader} is used.</p>
 *
 * <p>Example:</p>
 * {@snippet lang="java" :
 * GraphConfiguration configuration = GraphConfiguration.getConfiguration();
 * Graph graph = configuration.apply(Settings.builder().build());
 * TinkerpopGraphDatabaseManager manager = TinkerpopGraphDatabaseManager.of(graph);
 * }
 */
public interface GraphConfiguration extends Function<Settings, Graph> {

    /**
     * Returns the first {@link GraphConfiguration} registered in the {@link ServiceLoader}.
     *
     * @param <T> the configuration type
     * @return the {@link GraphConfiguration} instance
     * @throws CommunicationException when no implementation is registered
     */
    @SuppressWarnings("unchecked")
    static <T extends GraphConfiguration> T getConfiguration() {
        return (T) ServiceLoader.load(GraphConfiguration.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .findFirst().orElseThrow(() -> new CommunicationException("It does not find GraphConfiguration"));
    }

    /**
     * Returns the {@link GraphConfiguration} registered in the {@link ServiceLoader} that is an instance of
     * the given type.
     *
     * <p>Example:</p>
     * {@snippet lang="java" :
     * MyGraphConfiguration configuration = GraphConfiguration.getConfiguration(MyGraphConfiguration.class);
     * }
     *
     * @param <T>  the configuration type
     * @param type the particular provider
     * @return the {@link GraphConfiguration} instance
     * @throws CommunicationException when no registered implementation matches the type
     */
    @SuppressWarnings("unchecked")
    static <T extends GraphConfiguration> T getConfiguration(Class<T> type) {
        return (T) ServiceLoader.load(GraphConfiguration.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .filter(type::isInstance)
                .findFirst().orElseThrow(() -> new CommunicationException("It does not find GraphConfiguration"));
    }
}

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
package org.eclipse.jnosql.databases.tinkerpop.mapping.configuration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.jnosql.databases.tinkerpop.communication.GraphConfiguration;
import org.eclipse.jnosql.mapping.core.config.MicroProfileSettings;
import org.eclipse.jnosql.mapping.reflection.Reflections;

import java.util.function.Supplier;
import java.util.logging.Logger;

import static org.eclipse.jnosql.databases.tinkerpop.communication.DatabaseConfigurationAdapter.TINKERPOP_PROVIDER;


@ApplicationScoped
class GraphSupplier implements Supplier<Graph> {

    public static final Logger LOGGER = Logger.getLogger(GraphSupplier.class.getName());

    @SuppressWarnings("unchecked")
    @Override
    @Produces
    @ApplicationScoped
    public Graph get(){
        var settings = MicroProfileSettings.INSTANCE;
        LOGGER.fine("Loading the Graph configuration");
        var configuration = settings.get(TINKERPOP_PROVIDER, Class.class)
                .filter(GraphConfiguration.class::isAssignableFrom)
                .map(c -> (GraphConfiguration) Reflections.newInstance(c)).orElseGet(GraphConfiguration::getConfiguration);
        LOGGER.fine("The Graph configuration loaded successfully with: " + configuration.getClass());
        return configuration.apply(settings);
    }

    public void close(@Disposes Graph graph) throws Exception {
        LOGGER.fine("Closing the Graph");
        graph.close();
    }
}

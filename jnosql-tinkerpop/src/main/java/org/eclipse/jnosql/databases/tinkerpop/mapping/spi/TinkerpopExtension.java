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
package org.eclipse.jnosql.databases.tinkerpop.mapping.spi;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessProducer;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.jnosql.databases.tinkerpop.mapping.TinkerPopRepository;
import org.eclipse.jnosql.databases.tinkerpop.mapping.query.TinkerpopRepositoryBean;
import org.eclipse.jnosql.mapping.DatabaseMetadata;
import org.eclipse.jnosql.mapping.Databases;
import org.eclipse.jnosql.mapping.metadata.ClassScanner;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static org.eclipse.jnosql.mapping.DatabaseType.GRAPH;

/**
 * Extension to start up the GraphTemplate, Repository
 * from the {@link org.eclipse.jnosql.mapping.Database} qualifier
 */
public class TinkerpopExtension implements Extension {

    private static final Logger LOGGER = Logger.getLogger(TinkerpopExtension.class.getName());

    private final Set<DatabaseMetadata> databases = new HashSet<>();

    <T, X extends Graph> void observes(@Observes final ProcessProducer<T, X> pp) {
        Databases.addDatabase(pp, GRAPH, databases);
    }

    void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery afterBeanDiscovery) {

        ClassScanner scanner = ClassScanner.load();
        Set<Class<?>> crudTypes = scanner.repositories(TinkerPopRepository.class);



        LOGGER.info("Processing repositories as a Graph implementation: " + crudTypes);
        databases.forEach(type -> {
            if (!type.getProvider().isBlank()) {
                final TemplateBean bean = new TemplateBean(type.getProvider());
                afterBeanDiscovery.addBean(bean);
            }
        });

        crudTypes.forEach(type -> afterBeanDiscovery.addBean(new TinkerpopRepositoryBean<>(type)));

    }
}

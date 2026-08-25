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
package org.eclipse.jnosql.databases.cassandra.mapping;


import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import org.eclipse.jnosql.mapping.metadata.ClassScanner;

import java.util.Set;
import java.util.logging.Logger;

/**
 * CDI extension for Cassandra integration.
 */
public class CassandraExtension implements Extension {

    private static final Logger LOGGER = Logger.getLogger(CassandraExtension.class.getName());

    /**
     * Observes the AfterBeanDiscovery event to add Cassandra repository beans.
     *
     * @param afterBeanDiscovery the AfterBeanDiscovery event
     */
    void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery afterBeanDiscovery) {
        ClassScanner scanner = ClassScanner.load();
        Set<Class<?>> crudTypes = scanner.repositories(CassandraRepository.class);

        LOGGER.info("Starting the onAfterBeanDiscovery with elements number: " + crudTypes.size());

        crudTypes.forEach(type -> afterBeanDiscovery.addBean(new CassandraRepositoryBean(type)));

        LOGGER.info("Finished the onAfterBeanDiscovery");
    }
}

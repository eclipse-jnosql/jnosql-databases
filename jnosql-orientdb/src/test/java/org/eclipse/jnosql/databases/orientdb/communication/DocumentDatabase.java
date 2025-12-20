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
 *   Otavio Santana,
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.orientdb.communication;

import com.orientechnologies.orient.core.db.ODatabaseType;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.mapping.core.config.MappingConfigurations;
import org.testcontainers.containers.GenericContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public enum DocumentDatabase implements Supplier<OrientDBDocumentManagerFactory> {

    INSTANCE;

    private final GenericContainer<?> orientdb =
            new GenericContainer<>("orientdb:latest")
                    .withEnv(Map.of(
                            "ORIENTDB_ROOT_PASSWORD", "rootpwd"
                    ))
                    .withExposedPorts(2424, 2480);

    {
        orientdb.start();
    }

    private Settings getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put(OrientDBDocumentConfigurations.HOST.get(), "remote:" + hostAndPort());
        settings.put(OrientDBDocumentConfigurations.USER.get(), "root");
        settings.put(OrientDBDocumentConfigurations.PASSWORD.get(), "rootpwd");
        settings.put(OrientDBDocumentConfigurations.STORAGE_TYPE.get(), ODatabaseType.PLOCAL.toString());
        return Settings.of(settings);
    }

    private String hostAndPort() {
        return "%s:%d".formatted(orientdb.getHost(), orientdb.getFirstMappedPort());
    }

    @Override
    public OrientDBDocumentManagerFactory get() {
        return new OrientDBDocumentConfiguration().apply(getSettings());
    }

    public OrientDBDocumentManager get(String database) {
        Settings settings = getSettings();
        OrientDBDocumentManagerFactory factory = new OrientDBDocumentConfiguration().apply(getSettings());
        OrientDBDocumentManager manager = factory.apply(database);
        System.setProperty(MappingConfigurations.DOCUMENT_DATABASE.get(), database);
        System.setProperty(OrientDBDocumentConfigurations.HOST.get(),
                settings.get(OrientDBDocumentConfigurations.HOST.get(),String.class).orElseThrow());
        System.setProperty(OrientDBDocumentConfigurations.USER.get(),
                settings.get(OrientDBDocumentConfigurations.USER.get(),String.class).orElseThrow());
        System.setProperty(OrientDBDocumentConfigurations.PASSWORD.get(),
                settings.get(OrientDBDocumentConfigurations.PASSWORD.get(),String.class).orElseThrow());
        System.setProperty(OrientDBDocumentConfigurations.STORAGE_TYPE.get(),
                settings.get(OrientDBDocumentConfigurations.STORAGE_TYPE.get(),String.class).orElseThrow());
        return manager;
    }
}

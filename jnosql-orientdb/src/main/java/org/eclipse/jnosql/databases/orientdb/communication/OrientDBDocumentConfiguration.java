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
package org.eclipse.jnosql.databases.orientdb.communication;


import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * The orientDB implementation of {@link DatabaseConfiguration}  that returns
 * {@link OrientDBDocumentManagerFactory}.
 *
 * @see OrientDBDocumentConfigurations
 */
public class OrientDBDocumentConfiguration implements DatabaseConfiguration {

    private String host;

    private String user;

    private String password;

    private String storageType;


/**
 * Sets the host.
 *
 * @param host the host
 */
    public void setHost(String host) {
        this.host = host;
    }

/**
 * Sets the user.
 *
 * @param user the user
 */
    public void setUser(String user) {
        this.user = user;
    }

/**
 * Sets the password.
 *
 * @param password the password
 */
    public void setPassword(String password) {
        this.password = password;
    }

/**
 * Sets the storage type.
 *
 * @param storageType the storage type
 */
    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    @Override
    public OrientDBDocumentManagerFactory apply(Settings settings) throws NullPointerException {
        Objects.requireNonNull(settings, "settings is required");
        return getOrientDBDocumentManagerFactory(settings);
    }

    private OrientDBDocumentManagerFactory getOrientDBDocumentManagerFactory(Settings settings) {
        requireNonNull(settings, "settings is required");

        String host = Optional.ofNullable(getHost(settings)).orElse(this.host);
        String user = Optional.ofNullable(getUser(settings)).orElse(this.user);
        String password = Optional.ofNullable(getPassword(settings)).orElse(this.password);
        String storageType = Optional.ofNullable(getStorageType(settings)).orElse(this.storageType);
        return new OrientDBDocumentManagerFactory(host, user, password, storageType);
    }

    private String getHost(Settings settings) {
        return find(settings, OrientDBDocumentConfigurations.HOST,
                Configurations.HOST);
    }

    private String getUser(Settings settings) {
        return find(settings, OrientDBDocumentConfigurations.USER,
                Configurations.USER);
    }

    private String getPassword(Settings settings) {
        return find(settings, OrientDBDocumentConfigurations.PASSWORD,
                Configurations.PASSWORD);
    }

    private String getStorageType(Settings settings) {
        return find(settings, OrientDBDocumentConfigurations.STORAGE_TYPE);
    }

    @SafeVarargs
    private String find(Settings settings, Supplier<String>... keys) {
        return settings.get(Stream.of(keys)
                        .map(Supplier::get).collect(toList()))
                .map(Object::toString)
                .orElse(null);
    }
}

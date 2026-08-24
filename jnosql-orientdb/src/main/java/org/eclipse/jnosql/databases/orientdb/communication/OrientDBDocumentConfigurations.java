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

import java.util.function.Supplier;

/**
 * An enumeration to show the available options to connect to the OrientDB database.
 * It implements {@link Supplier}, where its it returns the property name that might be
 * overwritten by the system environment using Eclipse Microprofile or Jakarta Config API.
 *
 * @see org.eclipse.jnosql.communication.Settings
 */
public enum OrientDBDocumentConfigurations implements Supplier<String> {

    /**
     * The database host
     */
    HOST("jnosql.orientdb.host"),
    /**
     * The user's credential.
     */
    USER("jnosql.orientdb.user"),
    /**
     * The password's credential
     */
    PASSWORD("jnosql.orientdb.password"),
    /**
     * The storage type {@link com.orientechnologies.orient.core.db.ODatabaseType}
     */
    STORAGE_TYPE("jnosql.orientdb.storage.type");

    private final String configuration;

    OrientDBDocumentConfigurations(String configuration) {
        this.configuration = configuration;
    }

    @Override
    public String get() {
        return configuration;
    }
}

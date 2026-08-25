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
package org.eclipse.jnosql.databases.couchbase.communication;

import com.couchbase.client.java.Bucket;

import java.util.function.Supplier;

/**
 * An enumeration to show the available options to connect to the Couchbase database.
 * It implements {@link Supplier}, where its it returns the property name that might be
 * overwritten by the system environment using Eclipse Microprofile or Jakarta Config API.
 *
 * @see org.eclipse.jnosql.communication.Settings
 */
public enum CouchbaseConfigurations implements Supplier<String> {

    /**
     * The host at the database.
     */
    HOST("jnosql.couchbase.host"),

    /**
     * The user's credential.
     */
    USER("jnosql.couchbase.user"),

    /**
     * The password's credential
     */
    PASSWORD("jnosql.couchbase.password"),
    /**
     * The scope to use at couchbase otherwise, it will use the default.
     */
    SCOPE("jnosql.couchbase.scope"),
    /**
     * couchbase collection split by a comma.
     * At the start-up of a {@link CouchbaseConfiguration}, there is this option to check if
     * these collections exist; if not, it will create using the default settings.
     */
    COLLECTIONS("jnosql.couchbase.collections"),
    /**
     * A default couchbase collection.
     * When it is not defined the default value comes from {@link Bucket#defaultCollection()}
     */
    COLLECTION("jnosql.couchbase.collection"),
    /**
     * A couchbase collection index.
     * At the start-up of a {@link CouchbaseConfiguration}, it will read this property to check if the index does exist,
     * if not it will create combined by scope and the database.
     */
    INDEX("jnosql.couchbase.index");

    private final String configuration;

    CouchbaseConfigurations(String configuration) {
        this.configuration = configuration;
    }

    @Override
    public String get() {
        return configuration;
    }
}

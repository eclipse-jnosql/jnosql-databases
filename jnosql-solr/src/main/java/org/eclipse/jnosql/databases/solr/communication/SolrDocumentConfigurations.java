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

package org.eclipse.jnosql.databases.solr.communication;

import java.util.function.Supplier;

/**
 * An enumeration to show the available options to connect to the Solr database.
 * It implements {@link Supplier}, where its it returns the property name that might be
 * overwritten by the system environment using Eclipse Microprofile or Jakarta Config API.
 *
 * @see org.eclipse.jnosql.communication.Settings
 */
public enum SolrDocumentConfigurations implements Supplier<String> {
    /**
     * Database's host. E.g.: jnosql.solr.host=<a href="http://localhost:8983/solr/">http://localhost:8983/solr</a>
     */
    HOST("jnosql.solr.host"),
    /**
     * The user's credential.
     */
    USER("jnosql.solr.user"),
    /**
     * The password's credential
     */
    PASSWORD("jnosql.solr.password"),
    /**
     * Define if each operation Apache Solr will commit automatically, true by default.
     */
    AUTOMATIC_COMMIT("jnosql.solr.automatic.commit");

    private final String configuration;

    SolrDocumentConfigurations(String configuration) {
        this.configuration = configuration;
    }

    @Override
    public String get() {
        return configuration;
    }
}

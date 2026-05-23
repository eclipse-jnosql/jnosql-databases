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

package org.eclipse.jnosql.databases.solr.communication;

import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;

import java.util.Objects;
/**
 * The solr implementation to {@link DatabaseManagerFactory}
 * <br/>
 * Closing a {@link SolrDocumentManagerFactory} has no effect.
 */
public class SolrDocumentManagerFactory implements DatabaseManagerFactory {

    private final HttpJdkSolrClient solrClient;

    private final boolean automaticCommit;

    SolrDocumentManagerFactory(
            HttpJdkSolrClient solrClient,
            boolean automaticCommit) {

        this.solrClient = Objects.requireNonNull(
                solrClient,
                "solrClient is required");

        this.automaticCommit = automaticCommit;
    }


    @Override
    public SolrDocumentManager apply(String database) {

        Objects.requireNonNull(
                database,
                "database is required");

        return new DefaultSolrDocumentManager(solrClient, database, automaticCommit);
    }

    /**
     * Closing a {@link SolrDocumentManagerFactory}
     * has no effect.
     */
    @Override
    public void close() {

    }
}
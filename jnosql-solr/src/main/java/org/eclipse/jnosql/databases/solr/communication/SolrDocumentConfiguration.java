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
import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;

import java.util.Arrays;

import static java.util.Objects.requireNonNull;
import static org.eclipse.jnosql.databases.solr.communication.SolrDocumentConfigurations.AUTOMATIC_COMMIT;


/**
 * The Apache Solr implementation to {@link DatabaseConfiguration}
 * that returns  {@link SolrDocumentManagerFactory}
 * @see SolrDocumentConfigurations
 */
public class SolrDocumentConfiguration implements DatabaseConfiguration {


    private static final String DEFAULT_HOST = "http://localhost:8983/solr/";

    /**
     * Creates a {@link SolrDocumentManagerFactory}
     * from a {@link HttpJdkSolrClient}.
     *
     * @param solrClient the Solr client
     * @return a SolrDocumentManagerFactory instance
     * @throws NullPointerException when solrClient is null
     */
    public SolrDocumentManagerFactory get(HttpJdkSolrClient solrClient) {

        requireNonNull(solrClient, "solrClient is required");

        return new SolrDocumentManagerFactory(solrClient, true);
    }

    @Override
    public SolrDocumentManagerFactory apply(Settings settings) {

        requireNonNull(settings, "settings is required");

        String host = settings.getSupplier(
                        Arrays.asList(
                                SolrDocumentConfigurations.HOST,
                                Configurations.HOST))
                .map(Object::toString)
                .orElse(DEFAULT_HOST);

        boolean automaticCommit = settings.getOrDefault(AUTOMATIC_COMMIT, true);

        var solrClient = new HttpJdkSolrClient.Builder(host).build();
        return new SolrDocumentManagerFactory(solrClient, automaticCommit);
    }

}

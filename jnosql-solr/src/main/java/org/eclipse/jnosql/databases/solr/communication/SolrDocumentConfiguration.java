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


import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;

import java.util.Arrays;

import static java.util.Objects.requireNonNull;
import static org.eclipse.jnosql.databases.solr.communication.SolrDocumentConfigurations.AUTOMATIC_COMMIT;


/**
 * The Apache Solr implementation of {@link DatabaseConfiguration}. It returns a
 * {@link SolrDocumentManagerFactory} connected to the Solr URL in {@link SolrDocumentConfigurations#HOST}, or to
 * {@code http://localhost:8983/solr/} when none is set.
 *
 * <p>Set {@link SolrDocumentConfigurations#AUTOMATIC_COMMIT} to {@code false} to stop each write operation from
 * committing; it is {@code true} by default.</p>
 *
 * <p>Example:</p>
 * {@snippet lang="java" :
 * Settings settings = Settings.builder()
 *         .put(SolrDocumentConfigurations.HOST, "http://localhost:8983/solr")
 *         .build();
 * SolrDocumentManagerFactory factory = new SolrDocumentConfiguration().apply(settings);
 * SolrDocumentManager manager = factory.apply("books");
 * }
 *
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

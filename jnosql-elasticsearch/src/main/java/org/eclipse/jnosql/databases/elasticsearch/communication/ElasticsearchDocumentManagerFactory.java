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
package org.eclipse.jnosql.databases.elasticsearch.communication;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.OpenRequest;
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * The elasticsearch implementation to {@link DatabaseManagerFactory} that returns:
 * {@link ElasticsearchDocumentManager}
 * If the database does not exist, it tries to read a json mapping from the database name.
 * Eg: {@link ElasticsearchDocumentManagerFactory#apply(String)}} with database, if does not exist it tries to
 * read a "/database.json" file. The file must have the mapping to elasticsearch.
 */
public class ElasticsearchDocumentManagerFactory implements DatabaseManagerFactory {

    private static final Logger LOGGER = Logger.getLogger(ElasticsearchDocumentManagerFactory.class.getName());

    private final ElasticsearchClient elasticsearchClient;

    ElasticsearchDocumentManagerFactory(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }


    @Override
    public ElasticsearchDocumentManager apply(String database) throws UnsupportedOperationException, NullPointerException {
        Objects.requireNonNull(database, "database is required");

        initDatabase(database);
        return new DefaultElasticsearchDocumentManager(elasticsearchClient, database);
    }

    private void initDatabase(String database) {
        boolean exists = isExists(database);
        if (!exists) {
            createIndex(database);
        }
    }

    private void createIndex(String database) {
        InputStream stream = ElasticsearchDocumentManagerFactory.class.getResourceAsStream('/' + database + ".json");
        if (Objects.nonNull(stream)) {
            try {
                CreateIndexRequest request = CreateIndexRequest.of(
                        b -> b.index(database).withJson(stream)
                );
                elasticsearchClient.indices().create(request);
            } catch (Exception ex) {
                throw new ElasticsearchException("Error when create a new mapping", ex);
            }
        } else {
            try {
                CreateIndexRequest request = CreateIndexRequest.of(
                        b -> b.index(database)
                );
                elasticsearchClient.indices().create(request);
            } catch (Exception ex) {
                throw new ElasticsearchException("Error when create a new mapping", ex);
            }
        }
    }

    private boolean isExists(String database) {
        try {
            elasticsearchClient.indices().open(OpenRequest.of(b -> b.index(database)));
            return true;
        } catch (IOException e) {
            throw new ElasticsearchException("And error on admin access to verify if the database exists", e);
        } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
            return false;
        }
    }

    @Override
    public void close() {
        try {
            elasticsearchClient._transport().close();
        } catch (IOException e) {
            throw new ElasticsearchException("An error when close the elasticsearchClient client", e);
        }
    }
}

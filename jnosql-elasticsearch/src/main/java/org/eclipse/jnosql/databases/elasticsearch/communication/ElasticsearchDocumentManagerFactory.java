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
import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Elasticsearch implementation of {@link DatabaseManagerFactory} that returns
 * {@link ElasticsearchDocumentManager}.
 * <p>
 * If the index does not exist, this factory tries to load a JSON mapping file
 * from the classpath using the database name.
 * </p>
 * <p>
 * For example, calling {@link ElasticsearchDocumentManagerFactory#apply(String)}
 * with {@code "users"} will try to load {@code /users.json}. If the file exists,
 * it is used as the index creation request body. Otherwise, the index is created
 * with Elasticsearch defaults.
 * </p>
 */
public class ElasticsearchDocumentManagerFactory implements DatabaseManagerFactory {

    private static final Logger LOGGER = Logger.getLogger(ElasticsearchDocumentManagerFactory.class.getName());

    private static final String MAPPING_EXTENSION = ".json";

    private final ElasticsearchClient elasticsearchClient;

    ElasticsearchDocumentManagerFactory(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = Objects.requireNonNull(elasticsearchClient, "elasticsearchClient is required");
    }

    @Override
    public ElasticsearchDocumentManager apply(String database) throws UnsupportedOperationException, NullPointerException {
        Objects.requireNonNull(database, "database is required");

        LOGGER.log(Level.FINE, "Initializing Elasticsearch document manager for index: {0}", database);

        initDatabase(database);

        LOGGER.log(Level.FINE, "Elasticsearch document manager initialized for index: {0}", database);

        return new DefaultElasticsearchDocumentManager(elasticsearchClient, database);
    }

    private void initDatabase(String database) {
        if (indexExists(database)) {
            LOGGER.log(Level.FINE, "Elasticsearch index already exists: {0}", database);
            return;
        }

        LOGGER.log(Level.INFO, "Elasticsearch index does not exist and will be created: {0}", database);
        createIndex(database);
    }

    private void createIndex(String database) {
        String mappingResource = "/" + database + MAPPING_EXTENSION;

        try (InputStream stream = ElasticsearchDocumentManagerFactory.class.getResourceAsStream(mappingResource)) {
            CreateIndexRequest request = createIndexRequest(database, mappingResource, stream);

            elasticsearchClient.indices().create(request);

            LOGGER.log(Level.INFO, "Elasticsearch index created successfully: {0}", database);
        } catch (IOException exception) {
            throw new ElasticsearchException("Error while reading Elasticsearch mapping resource: " + mappingResource, exception);
        } catch (Exception exception) {
            throw new ElasticsearchException("Error while creating Elasticsearch index: " + database, exception);
        }
    }

    private CreateIndexRequest createIndexRequest(String database, String mappingResource, InputStream stream) {
        if (Objects.nonNull(stream)) {
            LOGGER.log(Level.INFO, "Creating Elasticsearch index {0} using mapping resource: {1}",
                    new Object[]{database, mappingResource});

            return CreateIndexRequest.of(builder -> builder
                    .index(database)
                    .withJson(stream));
        }

        LOGGER.log(Level.INFO, "No mapping resource found for Elasticsearch index {0}. Creating index with default settings.",
                database);

        return CreateIndexRequest.of(builder -> builder.index(database));
    }

    private boolean indexExists(String database) {
        try {
            elasticsearchClient.indices().get(GetIndexRequest.of(builder -> builder.index(database)));

            LOGGER.log(Level.FINE, "Elasticsearch index exists: {0}", database);
            return true;

        } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException exception) {
            if (exception.status() == 404) {
                LOGGER.log(Level.FINE, "Elasticsearch index does not exist: {0}", database);
                return false;
            }

            throw new ElasticsearchException(
                    "Elasticsearch rejected the index existence check for index: " + database,
                    exception
            );

        } catch (co.elastic.clients.transport.TransportException exception) {
            throw new ElasticsearchException(
                    "Elasticsearch transport error while checking whether index exists: " + database +
                            ". This is not an index-not-found case. Check Elasticsearch client/server version compatibility " +
                            "and default HTTP headers, especially Content-Type and Accept.",
                    exception
            );

        } catch (IOException exception) {
            throw new ElasticsearchException(
                    "Error while checking whether Elasticsearch index exists: " + database,
                    exception
            );
        }
    }

    @Override
    public void close() {
        try {
            LOGGER.log(Level.FINE, "Closing Elasticsearch transport");
            elasticsearchClient._transport().close();
            LOGGER.log(Level.FINE, "Elasticsearch transport closed successfully");
        } catch (IOException exception) {
            throw new ElasticsearchException("Error while closing Elasticsearch transport", exception);
        }
    }
}

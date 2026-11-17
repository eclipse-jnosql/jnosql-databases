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

package org.eclipse.jnosql.databases.mongodb.communication;

import com.mongodb.client.MongoClient;
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;

import java.util.Objects;

/**
 * The MongoDB implementation of {@link DatabaseManagerFactory}. It wraps one {@link MongoClient}, which
 * is shared by every manager it creates.
 *
 * <p>{@code factory.apply(database)} returns a {@link MongoDBDocumentManager} for that MongoDB database.
 * Closing the factory closes the {@link MongoClient}, so close it after all managers are no longer used.</p>
 *
 * <p>Example:</p>
 * {@snippet lang="java" :
 * try (MongoDBDocumentManagerFactory factory = new MongoDBDocumentConfiguration().get(mongoClient)) {
 *     MongoDBDocumentManager manager = factory.apply("library");
 * }
 * }
 *
 * @see MongoDBDocumentConfiguration
 */
public class MongoDBDocumentManagerFactory implements DatabaseManagerFactory {

    private final MongoClient mongoClient;

    MongoDBDocumentManagerFactory(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    @Override
    public MongoDBDocumentManager apply(String database) {
        Objects.requireNonNull(database, "database is required");
        return new MongoDBDocumentManager(mongoClient.getDatabase(database), database);
    }


    @Override
    public void close() {
        mongoClient.close();
    }

    @Override
    public String toString() {
       return "MongoDBDocumentManagerFactory{" + "mongoClient=" + mongoClient +
                '}';
    }
}

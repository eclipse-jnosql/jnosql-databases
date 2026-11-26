/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
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

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.interceptor.Interceptor;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.DatabaseType;

import java.util.Spliterators;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Produces the real MongoDB communication manager on top of a mocked {@link MongoDatabase}.
 */
@Alternative
@Priority(Interceptor.Priority.APPLICATION)
public class MockedMongoProducer {

    @SuppressWarnings("unchecked")
    public static final MongoCollection<Document> COLLECTION = mock(MongoCollection.class);
    public static final MongoDatabase DATABASE = mock(MongoDatabase.class);

    @Produces
    public MongoDBDocumentManager manager() {
        return new MongoDBDocumentManager(DATABASE, "library");
    }

    @Produces
    @Database(DatabaseType.DOCUMENT)
    public DatabaseManager documentManager() {
        return manager();
    }

    @SuppressWarnings("unchecked")
    public static void reset() {
        org.mockito.Mockito.reset(COLLECTION, DATABASE);
        FindIterable<Document> found = mock(FindIterable.class);
        when(found.spliterator()).thenReturn(Spliterators.emptySpliterator());
        when(DATABASE.getCollection(any(String.class))).thenReturn(COLLECTION);
        when(DATABASE.getCollection(any(String.class), any(Class.class))).thenReturn(mock(MongoCollection.class));
        when(COLLECTION.find(any(Bson.class))).thenReturn(found);
        when(COLLECTION.countDocuments(any(Bson.class))).thenReturn(0L);
        when(COLLECTION.deleteMany(any(Bson.class))).thenReturn(DeleteResult.acknowledged(0));
        when(COLLECTION.updateMany(any(Bson.class), any(Bson.class))).thenReturn(UpdateResult.acknowledged(0, 0L, null));
    }
}

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
package org.eclipse.jnosql.databases.couchdb.communication;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.eclipse.jnosql.communication.CommunicationException;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

final class CouchDBHttpClient {

    private final CloseableHttpClient client;

    private final String database;

    private final HttpExecute httpExecute;

    CouchDBHttpClient(CouchDBHttpConfiguration configuration, CloseableHttpClient client, String database) {
        this.client = client;
        this.database = database;
        this.httpExecute = new HttpExecute(configuration, client);
    }

    void createDatabase() {
        List<String> databases = httpExecute.getDatabases();
        if (!databases.contains(database)) {
            httpExecute.createDatabase(database);
        }
    }

    public CommunicationEntity insert(CommunicationEntity entity) {
        return this.httpExecute.insert(database, entity);
    }

    public CommunicationEntity update(CommunicationEntity entity) {
        return this.httpExecute.update(database, entity);
    }

    public Stream<CommunicationEntity> select(SelectQuery query) {
        return this.httpExecute.select(database, query);
    }

    public void delete(DeleteQuery query) {
        this.httpExecute.delete(database, query);
    }

    public long count() {
        return httpExecute.count(database);
    }


    public void close() {
        try {
            this.client.close();
        } catch (IOException e) {
            throw new CommunicationException("An error when try to close the http client", e);
        }
    }

}

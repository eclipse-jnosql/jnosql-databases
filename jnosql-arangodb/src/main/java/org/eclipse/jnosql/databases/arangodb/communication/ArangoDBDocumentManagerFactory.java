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
package org.eclipse.jnosql.databases.arangodb.communication;


import com.arangodb.ArangoDB;
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;


/**
 * Factory for ArangoDB document managers.
 */
public final class ArangoDBDocumentManagerFactory implements DatabaseManagerFactory {


    private final ArangoDBBuilder arangoDBBuilder;

    ArangoDBDocumentManagerFactory(ArangoDBBuilder arangoDBBuilder) {
        this.arangoDBBuilder = arangoDBBuilder;
    }

    @Override
    public ArangoDBDocumentManager apply(String database) {
        ArangoDB arangoDB = arangoDBBuilder.build();
        ArangoDBUtil.checkDatabase(database, arangoDB);
        return new DefaultArangoDBDocumentManager(database, arangoDB);
    }

    @Override
    public void close() {
        // no-op
        // ArangoDB driver instance will be closed in ArangoDBDocumentManager.close()
    }
}

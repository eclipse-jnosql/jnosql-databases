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
package org.eclipse.jnosql.databases.oracle.mapping;


import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.interceptor.Interceptor;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.databases.oracle.communication.OracleNoSQLDocumentManager;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.DatabaseType;
import org.mockito.Mockito;

import java.util.function.Supplier;

@Alternative
@Priority(Interceptor.Priority.APPLICATION)
@Database(DatabaseType.DOCUMENT)
public class MockProducer implements Supplier<OracleNoSQLDocumentManager> {

    @Produces
    @Override
    public OracleNoSQLDocumentManager get() {
        OracleNoSQLDocumentManager manager = Mockito.mock(OracleNoSQLDocumentManager.class);
        CommunicationEntity entity = CommunicationEntity.of("Person");
        entity.add(Element.of("name", "Ada"));
        Mockito.when(manager.insert(Mockito.any(CommunicationEntity.class))).thenReturn(entity);
        return manager;
    }

}

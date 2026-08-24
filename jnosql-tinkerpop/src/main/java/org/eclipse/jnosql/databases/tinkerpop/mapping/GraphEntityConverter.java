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
package org.eclipse.jnosql.databases.tinkerpop.mapping;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.jnosql.databases.tinkerpop.communication.CommunicationEntityConverter;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;

enum GraphEntityConverter {

    INSTANCE;

    public <T> T toEntity(EntityConverter converter, Vertex vertex) {
        var entity = CommunicationEntityConverter.INSTANCE.apply(vertex);
        return converter.toEntity(entity);
    }

}

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

package org.eclipse.jnosql.databases.cassandra.communication;

import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.delete.Delete;
import com.datastax.oss.driver.api.querybuilder.delete.DeleteSelection;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;

final class DeleteQueryConverter {
    private DeleteQueryConverter() {
    }

    static Delete delete(DeleteQuery query, String keyspace) {
        final String columnFamily = query.name();
        DeleteSelection deleteSelection = QueryBuilder.deleteFrom(keyspace, columnFamily);

        for (String column : query.columns()) {
            deleteSelection = deleteSelection.column(QueryUtils.getName(column));
        }

        return deleteSelection.where(Relations.createClause(query.condition().orElse(null)));
    }
}

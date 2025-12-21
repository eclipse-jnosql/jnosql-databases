/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.oracle.communication;

import oracle.nosql.driver.values.FieldValue;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;

import java.util.ArrayList;
import java.util.List;

final class DeleteBuilder extends AbstractQueryBuilder {

    private static final int ORIGIN = 0;
    private final DeleteQuery documentQuery;

    private final String table;

    DeleteBuilder(DeleteQuery documentQuery, String table) {
        super(table);
        this.documentQuery = documentQuery;
        this.table = table;
    }

    @Override
    public OracleQuery get() {
        StringBuilder query = new StringBuilder();
        List<FieldValue> params =new ArrayList<>();
        List<String> ids = new ArrayList<>();

        query.append("DELETE from ").append(table);
        entityCondition(query, documentQuery.name());
        this.documentQuery.condition().ifPresent(c -> {
            query.append(" AND ");
            condition(c, query, params, ids,false);
        });
        return new OracleQuery(query.toString(), params, ids);
    }



}
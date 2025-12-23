/*
 *  Copyright (c) 2025 Contributors to the Eclipse Foundation
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
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.oracle.communication;

import oracle.nosql.driver.values.FieldValue;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.util.ArrayList;
import java.util.List;

final class SelectCountBuilder extends AbstractQueryBuilder {

    public static final String COUNT = "count";
    private final SelectQuery documentQuery;

    private final String table;

    SelectCountBuilder(SelectQuery documentQuery, String table) {
        super(table);
        this.documentQuery = documentQuery;
        this.table = table;
    }

    @Override
    public OracleQuery get() {
        List<String> ids = new ArrayList<>();
        List<FieldValue> params = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        query.append("select ");
        query.append("count(*) as ").append(COUNT).append(' ');
        query.append("from ").append(table);
        entityCondition(query, documentQuery.name());
        this.documentQuery.condition().ifPresent(c -> {
            query.append(" AND ");
            condition(c, query, params, ids, true);
        });
        return new OracleQuery(query.toString(), params, ids);
    }
}

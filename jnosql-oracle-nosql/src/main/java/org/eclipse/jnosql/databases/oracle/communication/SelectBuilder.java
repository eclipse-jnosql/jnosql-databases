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

import jakarta.data.Direction;
import oracle.nosql.driver.values.FieldValue;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class SelectBuilder extends AbstractQueryBuilder {

    private final SelectQuery documentQuery;

    private final String table;

    SelectBuilder(SelectQuery documentQuery, String table) {
        super(table);
        this.documentQuery = documentQuery;
        this.table = table;
    }

    @Override
    public OracleQuery get() {
        StringBuilder query = new StringBuilder();
        List<FieldValue> params =new ArrayList<>();
        List<String> ids = new ArrayList<>();

        query.append("select ");
        query.append(select()).append(' ');
        query.append("from ").append(table);
        entityCondition(query, documentQuery.name());
        this.documentQuery.condition().ifPresent(c -> {
            query.append(" AND ");
            condition(c, query, params, ids, false);
        });


        if (!this.documentQuery.sorts().isEmpty()) {
            query.append(" ORDER BY ");

            String order = this.documentQuery.sorts().stream()
                    .map(s -> identifierOf(s.property()) + " " + (s.isAscending() ? Direction.ASC : Direction.DESC))
                    .collect(Collectors.joining(", "));
            query.append(order);
        }

        if (this.documentQuery.limit() > ORIGIN) {
            query.append(" LIMIT ").append(this.documentQuery.limit());
        }

        if (this.documentQuery.skip() > ORIGIN) {
            query.append(" OFFSET ").append(this.documentQuery.skip());
        }
        return new OracleQuery(query.toString(), params, ids);
    }

    private String select() {
        List<String> documents = documentQuery.columns();
        if (documents.isEmpty()) {
            return "*";
        } else {
            return "id, entity," + documents.stream()
                    .map(this::identifierOf).collect(Collectors.joining(", "));
        }
    }

}

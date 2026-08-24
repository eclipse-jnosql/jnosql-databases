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
import com.datastax.oss.driver.api.querybuilder.relation.Relation;
import com.datastax.oss.driver.api.querybuilder.term.Term;
import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;

final class Relations {

    private Relations() {
    }

    static List<Relation> createClause(CriteriaCondition columnCondition) {
        if (Objects.isNull(columnCondition)) {
            return Collections.emptyList();
        }

        List<Relation> relations = new ArrayList<>();
        load(columnCondition, relations);
        return relations;
    }

    private static void load(CriteriaCondition columnCondition, List<Relation> relations) {

        Element column = columnCondition.element();
        Condition condition = columnCondition.condition();

        switch (condition) {
            case EQUALS:
                relations.add(Relation.column(QueryUtils.getName(column)).isEqualTo(getTerm(column)));
                return;
            case GREATER_THAN:
                relations.add(Relation.column(QueryUtils.getName(column)).isGreaterThan(getTerm(column)));
                return;
            case GREATER_EQUALS_THAN:
                relations.add(Relation.column(QueryUtils.getName(column)).isGreaterThanOrEqualTo(getTerm(column)));
                return;
            case LESSER_THAN:
                relations.add(Relation.column(QueryUtils.getName(column)).isLessThan(getTerm(column)));
                return;
            case LESSER_EQUALS_THAN:
                relations.add(Relation.column(QueryUtils.getName(column)).isLessThanOrEqualTo(getTerm(column)));
                return;
            case IN:
                relations.add(Relation.column(QueryUtils.getName(column)).in(getIinValue(column.value())));
                return;
            case LIKE:
                relations.add(Relation.column(QueryUtils.getName(column)).like(getTerm(column)));
                return;
            case AND:
                column.get(new TypeReference<List<CriteriaCondition>>() {}).forEach(cc -> load(cc, relations));
                return;
            case OR:
            default:
                throw new UnsupportedOperationException("The columnCondition " + condition +
                        " is not supported in cassandra column driver");
        }
    }

    private static Term getTerm(Element column) {
        return literal(ValueUtil.convert(column.value()));
    }

    private static Term[] getIinValue(Value value) {
        return ValueUtil.convertToList(value).stream().map(QueryBuilder::literal).toArray(Term[]::new);
    }
}

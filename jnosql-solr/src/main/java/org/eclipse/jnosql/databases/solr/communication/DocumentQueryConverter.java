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

package org.eclipse.jnosql.databases.solr.communication;


import org.apache.solr.client.solrj.util.ClientUtils;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.util.List;
import java.util.stream.Collectors;

final class DocumentQueryConverter {

    private static final String SELECT_ALL_QUERY = "_entity:";

    private DocumentQueryConverter() {
    }


    static String convert(SelectQuery query) {
        String rootCondition = entityCondition(query.name());
        return rootCondition + query.condition()
                .map(DocumentQueryConverter::convert)
                .map(s -> " AND " + s).orElse("");
    }

    static String convert(DeleteQuery query) {
        String rootCondition = entityCondition(query.name());
        return rootCondition + query.condition()
                .map(DocumentQueryConverter::convert)
                .map(s -> " AND " + s).orElse("");
    }

    private static String convert(CriteriaCondition condition) {
        Element document = condition.element();
        Object value = ValueUtil.convert(document.value());

        return switch (condition.condition()) {
            case EQUALS -> document.name() + ':' + escape(value);
            case LIKE -> document.name() + ':' + escapeLike(value);
            case GREATER_EQUALS_THAN, GREATER_THAN -> document.name() + ":[" + escape(value) + " TO *]";
            case LESSER_EQUALS_THAN, LESSER_THAN -> document.name() + ":[* TO " + escape(value) + "]";
            case IN -> {
                final String inConditions = ValueUtil.convertToList(document.value())
                        .stream()
                        .map(DocumentQueryConverter::escape).collect(Collectors.joining(" OR "));
                yield document.name() + ":(" + inConditions + ')';
            }
            case NOT -> " NOT " + convert(document.get(CriteriaCondition.class));
            case AND -> getDocumentConditions(condition).stream()
                    .map(DocumentQueryConverter::convert)
                    .collect(Collectors.joining(" AND "));
            case OR -> getDocumentConditions(condition).stream()
                    .map(DocumentQueryConverter::convert)
                    .collect(Collectors.joining(" OR "));
            default -> throw new UnsupportedOperationException("The condition " + condition.condition()
                    + " is not supported from mongoDB diana driver");
        };
    }

    private static List<CriteriaCondition> getDocumentConditions(CriteriaCondition condition) {
        return condition.element().value().get(new TypeReference<>() {
        });
    }

    static String escape(Object value) {
        return ClientUtils.escapeQueryChars(String.valueOf(value));
    }

    static String entityCondition(String entity) {
        return SolrUtils.ENTITY + ':' + escape(entity);
    }

    private static String escapeLike(Object value) {
        return escape(value)
                .replace("\\*", "*")
                .replace("\\?", "?");
    }

}

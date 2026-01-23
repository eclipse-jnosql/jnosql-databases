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
 *   Michele Rastelli
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.arangodb.communication;

import jakarta.data.Direction;
import jakarta.data.Sort;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.driver.StringMatch;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.communication.semistructured.UpdateQuery;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class QueryAQLConverter {

    private static final String FILTER = " FILTER ";
    private static final String LIMIT = " LIMIT ";
    private static final String IN = " IN ";
    private static final String SORT = " SORT ";
    private static final String REMOVE = " REMOVE ";
    private static final String RETURN = " RETURN ";
    private static final String SEPARATOR = " ";
    private static final String START_EXPRESSION = "(";
    private static final String END_EXPRESSION = ")";
    private static final String AND = " AND ";
    private static final String OR = " OR ";
    private static final String EQUALS = " == ";
    private static final String GREATER_EQUALS_THAN = " >= ";
    private static final String GREATER_THAN = " > ";
    private static final String LESSER_THAN = " < ";
    private static final String LESSER_EQUALS_THAN = " <= ";
    private static final String LIKE = " LIKE ";
    private static final String NOT = " NOT ";
    private static final char PARAM_APPENDER = '@';

    private QueryAQLConverter() {
    }

    public static AQLQueryResult delete(DeleteQuery query) throws NullPointerException {

        return convert(query.name(),
                query.condition().orElse(null),
                Collections.emptyList(),
                0L,
                0L,
                QueryAQLConverter::onDelete);
    }

    public static AQLQueryResult select(SelectQuery query) throws NullPointerException {

        return convert(query.name(),
                query.condition().orElse(null),
                query.sorts(),
                query.skip(),
                query.limit(),
                QueryAQLConverter::onSelect);

    }

    public static AQLQueryResult count(SelectQuery query) throws NullPointerException {

        AQLQueryResult q = convert(query.name(),
                query.condition().orElse(null),
                Collections.emptyList(),
                0L,
                0L,
                QueryAQLConverter::onSelect);

        StringBuilder aql = new StringBuilder();

        aql.append("RETURN LENGTH(")
                .append(q.query())
                .append(")");
        return new AQLQueryResult(aql.toString(), q.values());
    }

    public static AQLQueryResult update(UpdateQuery query) throws NullPointerException {
        return convert(query.name(),
                query.condition().orElse(null),
                Collections.emptyList(),
                0L,
                0L,
                onUpdate(query));

    }

    private static ConclusionFunction onUpdate(UpdateQuery query) {
        return (alias, aql, params, documentCollection) -> {
            StringBuilder updatePart = new StringBuilder();
            updatePart.append(" UPDATE ").append(alias)
                    .append(" WITH { ");
            if (query.set() == null || query.set().isEmpty()) {
                throw new IllegalArgumentException("At least one element is required to update");
            }
            String separator = "";
            for (Element element : query.set()) {
                String nameParam = getNameParam(element.name(), params);
                updatePart.append(separator)
                        .append(element.name())
                        .append(": ")
                        .append(PARAM_APPENDER)
                        .append(nameParam);
                params.put(nameParam, ValueUtil.convert(element.value(), ArangoDBValueWriteDecorator.ARANGO_DB_VALUE_WRITER));
                separator = ", ";
            }
            updatePart.append(" } IN ")
                    .append(documentCollection);
            aql.append(updatePart)
                    .append(RETURN).append(" NEW");
        };
    }

    private static void onDelete(char alias, StringBuilder aql, Map<String, Object> params, String documentCollection) {
        aql.append(REMOVE)
                .append(alias)
                .append(IN)
                .append(documentCollection);
    }

    private static void onSelect(char alias, StringBuilder aql, Map<String, Object> params, String documentCollection) {
        aql.append(RETURN)
                .append(alias);
    }

    interface ConclusionFunction {
        void apply(char alias, StringBuilder aql, Map<String, Object> params, String documentCollection);
    }

    private static AQLQueryResult convert(String documentCollection,
                                          CriteriaCondition documentCondition,
                                          List<Sort<?>> sorts,
                                          long firstResult,
                                          long maxResult,
                                          ConclusionFunction conclusion) {
        StringBuilder aql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        char entity = Character.toLowerCase(documentCollection.charAt(0));
        aql.append("FOR ").append(entity).append(IN).append(documentCollection);

        Optional.ofNullable(documentCondition).ifPresent(condition -> {
            aql.append(FILTER);
            definesCondition(condition, aql, params, entity, 0);
        });
        if (!sorts.isEmpty()) {
            sort(sorts, aql, entity);
        }

        if (firstResult > 0 && maxResult > 0) {
            aql.append(LIMIT).append(firstResult)
                    .append(", ").append(maxResult);
        } else if (maxResult > 0) {
            aql.append(LIMIT).append(maxResult);
        } else if (firstResult > 0) {
            aql.append(LIMIT).append(firstResult).append(", null");
        }
        conclusion.apply(entity, aql, params, documentCollection);
        return new AQLQueryResult(aql.toString(), params);
    }

    private static void sort(List<Sort<?>> sorts, StringBuilder aql, char entity) {
        aql.append(SORT);
        String separator = SEPARATOR;
        for (Sort<?> sort : sorts) {
            aql.append(separator)
                    .append(entity).append('.')
                    .append(sort.property())
                    .append(SEPARATOR).append(sort.isAscending() ? Direction.ASC : Direction.DESC);
            separator = " , ";
        }
    }

    private static void definesCondition(CriteriaCondition condition,
                                         StringBuilder aql,
                                         Map<String, Object> params,
                                         char entity, int counter) {

        Element document = condition.element();
        int localCounter = counter;
        switch (condition.condition()) {
            case IN:
                appendCondition(aql, params, entity, document, IN);
                return;
            case EQUALS:
                appendCondition(aql, params, entity, document, EQUALS);
                return;
            case GREATER_EQUALS_THAN:
                appendCondition(aql, params, entity, document, GREATER_EQUALS_THAN);
                return;
            case GREATER_THAN:
                appendCondition(aql, params, entity, document, GREATER_THAN);
                return;
            case LESSER_THAN:
                appendCondition(aql, params, entity, document, LESSER_THAN);
                return;
            case LESSER_EQUALS_THAN:
                appendCondition(aql, params, entity, document, LESSER_EQUALS_THAN);
                return;
            case LIKE:
                appendCondition(aql, params, entity, document, LIKE);
                return;
            case CONTAINS:
                appendCondition(aql, params, entity, Element.of(document.name(), StringMatch.CONTAINS.format(document.get(String.class))), LIKE);
                return;
            case STARTS_WITH:
                appendCondition(aql, params, entity, Element.of(document.name(), StringMatch.STARTS_WITH.format(document.get(String.class))), LIKE);
                return;
            case ENDS_WITH:
                appendCondition(aql, params, entity, Element.of(document.name(), StringMatch.ENDS_WITH.format(document.get(String.class))), LIKE);
                return;
            case AND:

                for (CriteriaCondition dc : document.get(new TypeReference<List<CriteriaCondition>>() {
                })) {

                    if (isFirstCondition(aql, localCounter)) {
                        aql.append(AND);
                    }
                    definesCondition(dc, aql, params, entity, ++localCounter);
                }
                return;
            case OR:

                for (CriteriaCondition dc : document.get(new TypeReference<List<CriteriaCondition>>() {
                })) {
                    if (isFirstCondition(aql, localCounter)) {
                        aql.append(OR);
                    }
                    definesCondition(dc, aql, params, entity, ++localCounter);
                }
                return;
            case NOT:
                CriteriaCondition documentCondition = document.get(CriteriaCondition.class);
                aql.append(NOT).append(START_EXPRESSION);
                definesCondition(documentCondition, aql, params, entity, ++localCounter);
                aql.append(END_EXPRESSION);
                return;
            case BETWEEN:
                List<Object> betweenList = ValueUtil.convertToList(document.value(), ArangoDBValueWriteDecorator.ARANGO_DB_VALUE_WRITER);
                appendCondition(aql, params, entity, Element.of(document.name(), betweenList.get(0)), GREATER_EQUALS_THAN);
                aql.append(AND);
                appendCondition(aql, params, entity, Element.of(document.name(), betweenList.get(1)), LESSER_EQUALS_THAN);
                return;
            default:
                throw new IllegalArgumentException("The condition does not support in AQL: " + condition.condition());
        }
    }

    private static boolean isFirstCondition(StringBuilder aql, int count) {
        return count > 0 && !FILTER.equals(aql.substring(aql.length() - 8));
    }

    private static void appendCondition(StringBuilder aql, Map<String, Object> params,
                                        char entity, Element document, String condition) {
        String nameParam = getNameParam(document.name(), params);
        aql.append(SEPARATOR).append(entity).append('.').append(document.name())
                .append(condition).append(PARAM_APPENDER).append(nameParam);
        if (IN.equals(condition)) {
            params.put(nameParam, ValueUtil.convertToList(document.value(), ArangoDBValueWriteDecorator.ARANGO_DB_VALUE_WRITER));
        } else {
            params.put(nameParam, ValueUtil.convert(document.value(), ArangoDBValueWriteDecorator.ARANGO_DB_VALUE_WRITER));
        }
    }

    private static String getNameParam(String name, Map<String, Object> params) {
        String parameter = getNameParam(name);

        String paramName = parameter;
        int counter = 1;
        while (params.containsKey(paramName)) {
            paramName = parameter + '_' + counter;
        }

        return paramName;
    }

    private static String getNameParam(String name) {
        if ('_' == name.charAt(0)) {
            return name.substring(1);
        }
        return name;
    }


}

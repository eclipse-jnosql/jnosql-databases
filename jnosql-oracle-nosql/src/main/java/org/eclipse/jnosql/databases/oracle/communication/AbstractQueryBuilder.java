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
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.eclipse.jnosql.databases.oracle.communication.TableCreationConfiguration.ID_FIELD;
import static org.eclipse.jnosql.databases.oracle.communication.TableCreationConfiguration.JSON_FIELD;

abstract class AbstractQueryBuilder implements Supplier<OracleQuery> {

    static final int ORIGIN = 0;
    private final String table;
    private final String entityName;

    AbstractQueryBuilder(String table, String entityName) {
        this.table = table;
        this.entityName = entityName;
    }

    protected void condition(CriteriaCondition condition, StringBuilder query, List<FieldValue> params, List<String> ids, boolean forCount) {
        condition(condition, query, params, ids, forCount, true);
    }

    protected void condition(CriteriaCondition condition, StringBuilder query, List<FieldValue> params,
                             List<String> ids, boolean forCount, boolean allowIdFastPath) {
        var document = condition.element();
        switch (condition.condition()) {
            case EQUALS:
                if (!forCount && allowIdFastPath && document.name().equals(DefaultOracleNoSQLDocumentManager.ID)) {
                    ids.add(document.get(String.class));
                } else {
                    predicate(query, " = ", document, params);
                }
                return;
            case IN:
                if (!forCount && allowIdFastPath && document.name().equals(DefaultOracleNoSQLDocumentManager.ID)) {
                    ids.addAll(document.get(new TypeReference<List<String>>() {
                    }));
                } else {
                    predicate(query, " IN ", document, params);
                }
                return;
            case LESSER_THAN:
                predicate(query, " < ", document, params);
                return;
            case GREATER_THAN:
                predicate(query, " > ", document, params);
                return;
            case LESSER_EQUALS_THAN:
                predicate(query, " <= ", document, params);
                return;
            case GREATER_EQUALS_THAN:
                predicate(query, " >= ", document, params);
                return;
            case LIKE:
                predicateLike(query, document);
                return;
            case CONTAINS:
                predicateContains(query, document);
                return;
            case STARTS_WITH:
                predicateStartsWith(query, document);
                return;
            case ENDS_WITH:
                predicateEndsWith(query, document);
                return;
            case NOT:
                query.append(" NOT ");
                condition(document.get(CriteriaCondition.class), query, params, ids, forCount, allowIdFastPath);
                return;
            case OR:
                query.append("(");
                appendCondition(query, params, document.get(new TypeReference<>() {
                }), " OR ", ids, forCount, allowIdFastPath);
                query.append(")");
                return;
            case AND:
                query.append("(");
                appendCondition(query, params, document.get(new TypeReference<>() {
                }), " AND ", ids, forCount, allowIdFastPath);
                query.append(")");
                return;
            case BETWEEN:
                predicateBetween(query, params, document);
                return;
            default:
                throw new UnsupportedOperationException("There is not support condition for " + condition.condition());
        }
    }

    protected boolean hasOnlyIdConditions(CriteriaCondition condition) {
        var document = condition.element();
        switch (condition.condition()) {
            case EQUALS:
            case IN:
                return document.name().equals(DefaultOracleNoSQLDocumentManager.ID);
            case OR:
            case AND:
                var conditions = document.get(new TypeReference<List<CriteriaCondition>>() {
                });
                return !conditions.isEmpty() && conditions.stream().allMatch(this::hasOnlyIdConditions);
            default:
                return false;
        }
    }

    protected void predicateBetween(StringBuilder query,List<FieldValue> params, Element document) {
        String name = identifierOf(document.name());

        List<Object> values = new ArrayList<>();
        ((Iterable<?>) document.get()).forEach(values::add);

        query.append(name).append(" BETWEEN ? AND ? ");
        FieldValue fieldValue = FieldValueConverter.of(values.get(ORIGIN));
        FieldValue fieldValue2 = FieldValueConverter.of(values.get(1));
        params.add(fieldValue);
        params.add(fieldValue2);
    }

    protected void appendCondition(StringBuilder query, List<FieldValue> params,
                                 List<CriteriaCondition> conditions,
                                 String condition, List<String> ids, boolean forCount) {
        appendCondition(query, params, conditions, condition, ids, forCount, true);
    }

    protected void appendCondition(StringBuilder query, List<FieldValue> params,
                                 List<CriteriaCondition> conditions,
                                 String condition, List<String> ids, boolean forCount, boolean allowIdFastPath) {
        int index = ORIGIN;
        for (CriteriaCondition documentCondition : conditions) {
            StringBuilder appendQuery = new StringBuilder();
            condition(documentCondition, appendQuery, params, ids, forCount, allowIdFastPath);
            if(index == ORIGIN && !appendQuery.isEmpty()){
                query.append(appendQuery);
            } else if(!appendQuery.isEmpty()) {
                if(!query.substring(query.length()-condition.length()).equals(condition)){
                    query.append(condition);
                }
                query.append(appendQuery);
            }
            index++;
        }
    }

    protected void predicate(StringBuilder query,
                           String condition,
                           Element document,
                           List<FieldValue> params) {
        String name = identifierOf(document.name());
        Object value = sqlValueOf(document);
        FieldValue fieldValue = FieldValueConverter.of(value);
        if(fieldValue.isArray()){
            query.append(name).append(condition).append(" ?[] ");
        } else {
            query.append(name).append(condition).append(" ? ");
        }
        params.add(fieldValue);
    }

    protected void predicateLike(StringBuilder query,
                             Element document) {
        String name = identifierOf(document.name());
        Object value = OracleNoSqlLikeConverter.INSTANCE.convert(document.get());
        query.append("regex_like(").append(name).append(", \"").append(value).append("\")");
    }

    protected void predicateStartsWith(StringBuilder query,
                                 Element document) {
        String name = identifierOf(document.name());
        var value = document.get() == null ? "" : document.get(String.class);
        query.append("regex_like(").append(name).append(", \"").append(OracleNoSqlLikeConverter.INSTANCE.startsWith(value)).append(
                "\")");
    }

    protected void predicateEndsWith(StringBuilder query,
                                       Element document) {
        String name = identifierOf(document.name());
        var value = document.get() == null ? "" : document.get(String.class);
        query.append("regex_like(").append(name).append(", \"").append(OracleNoSqlLikeConverter.INSTANCE.endsWith(value)).append(
                "\")");
    }

    protected void predicateContains(StringBuilder query,
                                     Element document) {
        String name = identifierOf(document.name());
        var value = document.get() == null ? "" : document.get(String.class);
        query.append("regex_like(").append(name).append(", \"").append(OracleNoSqlLikeConverter.INSTANCE.contains(value)).append(
                "\")");
    }

    protected String identifierOf(String name) {
        if (DefaultOracleNoSQLDocumentManager.ID.equals(name)) {
            return ' ' + table + "." + ID_FIELD + ' ';
        }
        return ' ' + table + "." + JSON_FIELD + "." + name + ' ';
    }

    private Object sqlValueOf(Element document) {
        if (!DefaultOracleNoSQLDocumentManager.ID.equals(document.name())) {
            return document.get();
        }

        Object value = document.get();
        if (value instanceof Iterable<?> iterable) {
            List<String> ids = new ArrayList<>();
            iterable.forEach(id -> ids.add(generateId(String.valueOf(id))));
            return ids;
        }
        return generateId(String.valueOf(value));
    }

    private String generateId(String id) {
        if (id.contains(":")) {
            return id;
        }
        return entityName + ":" + id;
    }

    protected void entityCondition(StringBuilder query, String tableName) {
        query.append(" WHERE ").append(table).append(".entity= '").append(tableName).append("'");
    }
}

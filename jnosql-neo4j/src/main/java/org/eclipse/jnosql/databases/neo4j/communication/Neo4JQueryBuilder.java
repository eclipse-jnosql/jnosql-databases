/*
 *
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
 *   Otavio Santana
 *
 */
package org.eclipse.jnosql.databases.neo4j.communication;


import org.eclipse.jnosql.communication.CommunicationException;
import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.communication.semistructured.UpdateQuery;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

enum Neo4JQueryBuilder {

    INSTANCE;

    private static final String INTERNAL_ID = "_id";

    String buildQuery(DeleteQuery query, Map<String, Object> parameters) {
        StringBuilder cypher = buildCypher(query.name(), query.condition(), parameters);

        List<String> columns = query.columns();
        if (!columns.isEmpty()) {
            cypher.append(" SET ");
            cypher.append(columns.stream()
                    .map(this::translateField)
                    .map(col -> col + " = NULL")
                    .collect(Collectors.joining(", ")));
        } else {
            cypher.append(" DELETE e");
        }

        return cypher.toString();
    }

    String buildQuery(SelectQuery query, Map<String, Object> parameters) {
        StringBuilder cypher = buildCypher(query.name(), query.condition(), parameters);

        if (!query.sorts().isEmpty()) {
            cypher.append(" ORDER BY ");
            cypher.append(query.sorts().stream()
                    .map(sort -> "e." + sort.property() + (sort.isAscending() ? " ASC" : " DESC"))
                    .collect(Collectors.joining(", "))); // Fix double "e."
        }
        if (query.skip() > 0) {
            cypher.append(" SKIP ").append(query.skip());
        }

        if (query.limit() > 0) {
            cypher.append(" LIMIT ").append(query.limit());
        }

        cypher.append(" RETURN ");
        List<String> columns = query.columns();
        if (columns.isEmpty()) {
            cypher.append("e");
        } else {
            cypher.append(columns.stream()
                    .map(this::translateField)
                    .collect(Collectors.joining(", ")));
        }

        return cypher.toString();
    }

    String buildCountQuery(SelectQuery query, Map<String, Object> parameters) {
        StringBuilder cypher = buildCypher(query.name(), query.condition(), parameters);
        cypher.append(" RETURN COUNT(e) as count");
        return cypher.toString();
    }

    String buildQuery(UpdateQuery query, Map<String, Object> parameters) {
        StringBuilder cypher = buildCypher(query.name(), query.condition(), parameters);

        List<Element> elements = query.set();
        if (elements.isEmpty()) {
            throw new CommunicationException("At least one element is required to update");
        }

        cypher.append(" SET ");
        cypher.append(elements.stream()
                .map(element -> {
                    String field = translateField(element.name());
                    String paramName = element.name().replace(".", "_");
                    parameters.put(paramName, element.get());
                    return field + " = $" + paramName;
                })
                .collect(Collectors.joining(", ")));

        cypher.append(" RETURN e");

        return cypher.toString();
    }

    private StringBuilder buildCypher(String entityName,
                                      Optional<CriteriaCondition> condition,
                                      Map<String, Object> parameters) {
        StringBuilder cypher = new StringBuilder("MATCH (e:");
        cypher.append(entityName).append(")");
        condition.ifPresent(c -> {
            cypher.append(" WHERE ");
            createWhereClause(cypher, c, parameters);
        });
        return cypher;
    }

    private void createWhereClause(StringBuilder cypher, CriteriaCondition condition, Map<String, Object> parameters) {
        Element element = condition.element();
        String fieldName = element.name();
        String queryField = translateField(fieldName);

        switch (condition.condition()) {
            case EQUALS:
            case GREATER_THAN:
            case GREATER_EQUALS_THAN:
            case LESSER_THAN:
            case LESSER_EQUALS_THAN:
            case LIKE:
            case STARTS_WITH:
            case ENDS_WITH:
            case CONTAINS:
            case IN:
                String paramName = INTERNAL_ID.equals(fieldName) ? "id" : fieldName; // Ensure valid parameter name
                parameters.put(paramName, value(element.get(), condition.condition()));
                cypher.append(queryField).append(" ")
                        .append(getConditionOperator(condition.condition()))
                        .append(" $").append(paramName);
                break;
            case NOT:
                cypher.append("NOT (");
                createWhereClause(cypher, element.get(CriteriaCondition.class), parameters);
                cypher.append(")");
                break;
            case AND:
            case OR:
                cypher.append("(");
                List<CriteriaCondition> conditions = element.get(new TypeReference<>() {});
                for (int index = 0; index < conditions.size(); index++) {
                    if (index > 0) {
                        cypher.append(" ").append(getConditionOperator(condition.condition())).append(" ");
                    }
                    createWhereClause(cypher, conditions.get(index), parameters);
                }
                cypher.append(")");
                break;
            default:
                throw new CommunicationException("Unsupported condition: " + condition.condition());
        }
    }

    private Object value(Object value, Condition condition) {
        if (Condition.LIKE.equals(condition)) {
            return LikeToCypherRegex.INSTANCE.toCypherRegex(value.toString());
        }
        return value;
    }
    private String translateField(String field) {
        if (INTERNAL_ID.equals(field)) {
            return "elementId(e)";
        }
        if (field.startsWith("e.")) {
            return field;
        }
        return "e." + field;
    }


    private String getConditionOperator(Condition condition) {
        return switch (condition) {
            case EQUALS -> "=";
            case GREATER_THAN -> ">";
            case GREATER_EQUALS_THAN -> ">=";
            case LESSER_THAN -> "<";
            case LESSER_EQUALS_THAN -> "<=";
            case LIKE -> "=~";
            case IN -> "IN";
            case AND -> "AND";
            case OR -> "OR";
            case STARTS_WITH -> "STARTS WITH";
            case ENDS_WITH -> "ENDS WITH";
            case CONTAINS -> "CONTAINS";
            default -> throw new CommunicationException("Unsupported operator: " + condition);
        };
    }
}

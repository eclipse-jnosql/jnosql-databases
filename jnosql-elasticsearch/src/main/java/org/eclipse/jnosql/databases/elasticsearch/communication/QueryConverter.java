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
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.elasticsearch.communication;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import co.elastic.clients.json.JsonData;
import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.driver.StringMatch;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.eclipse.jnosql.communication.Condition.IN;

final class QueryConverter {

    private static final Set<Condition> NOT_APPENDABLE = EnumSet.of(IN, Condition.AND, Condition.OR);

    private QueryConverter() {
    }

    static QueryConverterResult select(ElasticsearchClient client, String database, SelectQuery query) {

        var indexMappingRecord = getIndexMappingRecord(client, database, query);

        Query.Builder nameCondition = Optional.of(query.name())
                .map(collection -> {
                    if (supportTermQuery(indexMappingRecord, EntityConverter.ENTITY)) {
                        return new Query.Builder().term(q -> q
                                .field(EntityConverter.ENTITY).value(collection));
                    }
                    return new Query.Builder().match(q -> q
                            .field(EntityConverter.ENTITY).query(collection));

                })
                .map(Query.Builder.class::cast)
                .orElse(null);

        Query.Builder queryConditions = query.condition()
                .map(c -> getCondition(indexMappingRecord, c))
                .orElse(null);


        Query.Builder builder = Stream.of(nameCondition, queryConditions)
                .filter(Objects::nonNull)
                .reduce((c1, c2) -> (Query.Builder) new Query.Builder().bool(BoolQuery.of(b -> b
                        .must(c1.build(), c2.build()))))
                .orElse(null);

        return new QueryConverterResult(builder);

    }

    public static boolean supportTermQuery(IndexMappingRecord indexMappingRecord, String attribute) {
        return supportTermQuery(indexMappingRecord.mappings().properties(), attribute);
    }

    public static boolean supportTermQuery(Map<String, Property> properties, String attribute) {
        String attributeName = attribute;
        if (attributeName.contains(".")) {
            attributeName = attribute.substring(0, attribute.indexOf("."));
            Property property = properties.get(attributeName);
            if (Objects.nonNull(property) && property.isObject()) {
                return supportTermQuery(property.object().properties(),
                        attribute.substring(attribute.indexOf(".") + 1));
            }
            return false;
        }
        Property property = properties.get(attributeName);
        return Objects.nonNull(property) && property.isKeyword();
    }

    private static IndexMappingRecord getIndexMappingRecord(ElasticsearchClient client, String database, SelectQuery query) {
        try {
            return client.indices().getMapping(q -> q.index(database))
                    .get(database);
        } catch (IOException e) {
            throw new IllegalStateException("cannot retrieve the index's mapping: %s".formatted(e.getMessage()), e);
        }
    }


    private static Query.Builder getCondition(IndexMappingRecord indexMappingRecord, CriteriaCondition condition) {
        Element document = condition.element();
        String fieldName = document.name();
        JsonData value = JsonData.of(document.value().get());
        switch (condition.condition()) {
            case EQUALS:
                if (supportTermQuery(indexMappingRecord, fieldName)) {
                    return (Query.Builder) new Query.Builder()
                            .term(TermQuery.of(tq -> tq
                                    .field(fieldName)
                                    .value(v -> v
                                            .anyValue(value))));
                }
                return (Query.Builder) new Query.Builder()
                        .match(MatchQuery.of(tq -> tq
                                .field(fieldName)
                                .query(v -> v
                                        .anyValue(value))));
            case LESSER_THAN:
                return (Query.Builder) new Query.Builder()
                        .range(RangeQuery.of(rq -> rq
                                .untyped(u -> u
                                        .field(fieldName)
                                        .lt(value))));
            case LESSER_EQUALS_THAN:
                return (Query.Builder) new Query.Builder()
                        .range(RangeQuery.of(rq -> rq
                                .untyped(u -> u
                                        .field(fieldName)
                                        .lte(value))));
            case GREATER_THAN:
                return (Query.Builder) new Query.Builder()
                        .range(RangeQuery.of(rq -> rq
                                .untyped(u -> u
                                        .field(fieldName)
                                        .gt(value))));
            case GREATER_EQUALS_THAN:
                return (Query.Builder) new Query.Builder()
                        .range(RangeQuery.of(rq -> rq
                                .untyped(u -> u
                                        .field(fieldName)
                                        .gte(value))));
            case LIKE:
                return (Query.Builder) new Query.Builder()
                        .queryString(QueryStringQuery.of(rq -> rq
                                .query(document.value().get(String.class))
                                .allowLeadingWildcard(true)
                                .fields(fieldName)));
            case CONTAINS:
                return (Query.Builder) new Query.Builder()
                        .queryString(QueryStringQuery.of(rq -> rq
                                .query(StringMatch.CONTAINS.format(document.value().get(String.class)))
                                .allowLeadingWildcard(true)
                                .fields(fieldName)));
            case STARTS_WITH:
                return (Query.Builder) new Query.Builder()
                        .queryString(QueryStringQuery.of(rq -> rq
                                .query(StringMatch.STARTS_WITH.format(document.value().get(String.class)))
                                .allowLeadingWildcard(true)
                                .fields(fieldName)));
            case ENDS_WITH:
                return (Query.Builder) new Query.Builder()
                        .queryString(QueryStringQuery.of(rq -> rq
                                .query(StringMatch.ENDS_WITH.format(document.value().get(String.class)))
                                .allowLeadingWildcard(true)
                                .fields(fieldName)));
            case IN:
                return (Query.Builder) ValueUtil.convertToList(document.value())
                        .stream()
                        .map(val -> {
                            if (supportTermQuery(indexMappingRecord, fieldName)) {
                                return new Query.Builder()
                                        .term(TermQuery.of(tq -> tq
                                                .field(fieldName)
                                                .value(v -> v.anyValue(JsonData.of(val)))));
                            }
                            return new Query.Builder()
                                    .match(MatchQuery.of(tq -> tq
                                            .field(fieldName)
                                            .query(v -> v.anyValue(JsonData.of(val)))));
                        })
                        .reduce((d1, d2) -> new Query.Builder()
                                .bool(BoolQuery.of(bq -> bq
                                        .should(List.of(d1.build(), d2.build())))))
                        .orElseThrow(() -> new IllegalStateException("An and condition cannot be empty"));
            case AND:
                return document.get(new TypeReference<List<CriteriaCondition>>() {
                        })
                        .stream()
                        .map(d -> getCondition(indexMappingRecord, d))
                        .filter(Objects::nonNull)
                        .reduce((d1, d2) -> (Query.Builder) new Query.Builder()
                                .bool(BoolQuery.of(bq -> bq
                                        .must(List.of(d1.build(), d2.build()))))
                        ).orElseThrow(() -> new IllegalStateException("An and condition cannot be empty"));

            case OR:
                return document.get(new TypeReference<List<CriteriaCondition>>() {
                        })
                        .stream()
                        .map(d -> getCondition(indexMappingRecord, d))
                        .filter(Objects::nonNull)
                        .reduce((d1, d2) -> (Query.Builder) new Query.Builder()
                                .bool(BoolQuery.of(bq -> bq
                                        .should(List.of(d1.build(), d2.build())))))
                        .orElseThrow(() -> new IllegalStateException("An and condition cannot be empty"));
            case NOT:
                CriteriaCondition dc = document.get(CriteriaCondition.class);
                Query.Builder queryBuilder = Optional.ofNullable(getCondition(indexMappingRecord, dc))
                        .orElseThrow(() -> new IllegalStateException("An and condition cannot be empty"));
                return (Query.Builder) new Query.Builder()
                        .bool(BoolQuery.of(bq -> bq
                                .mustNot(queryBuilder.build())));
            default:
                throw new IllegalStateException("This condition is not supported at elasticsearch: " + condition.condition());
        }
    }

    private static boolean isIdField(Element document) {
        return EntityConverter.ID_FIELD.equals(document.name());
    }


}

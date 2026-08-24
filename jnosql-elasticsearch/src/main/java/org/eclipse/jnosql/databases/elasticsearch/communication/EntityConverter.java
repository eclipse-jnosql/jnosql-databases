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
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.elasticsearch.communication;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

final class EntityConverter {

    static final String ID_FIELD = "_id";

    static final String ENTITY = "@entity";

    private EntityConverter() {
    }

    static Map<String, Object> getMap(CommunicationEntity entity) {
        Map<String, Object> jsonObject = new HashMap<>();

        entity.elements().stream()
                .filter(d -> !d.name().equals(ID_FIELD))
                .forEach(feedJSON(jsonObject));
        jsonObject.put(ENTITY, entity.name());
        return jsonObject;
    }

    static Stream<CommunicationEntity> query(SelectQuery query, ElasticsearchClient client, String index) {
        QueryConverterResult select = QueryConverter.select(client, index, query);

        try {
            Stream<CommunicationEntity> statementQueryStream = Stream.empty();
            if (select.hasStatement()) {
                statementQueryStream = executeStatement(query, client, index, select);
            }
            return statementQueryStream.distinct();
        } catch (IOException e) {
            throw new ElasticsearchException("An error to execute a query on elasticsearch", e);
        }
    }

    private static Stream<CommunicationEntity> executeStatement(SelectQuery query, ElasticsearchClient client, String index,
                                                           QueryConverterResult select) throws IOException {
        SearchRequest.Builder searchRequest = buildSearchRequestBuilder(query, select);
        searchRequest.index(index);

        SearchResponse<Map> searchResponse = client.search(searchRequest.build(), Map.class);

        return getDocumentEntityStream(client, searchResponse);
    }


    private static Consumer<Element> feedJSON(Map<String, Object> jsonObject) {
        return d -> {
            Object value = ValueUtil.convert(d.value());
            if (value instanceof Element) {
                Element subDocument = Element.class.cast(value);
                jsonObject.put(d.name(), singletonMap(subDocument.name(), subDocument.get()));
            } else if (isSudDocument(value)) {
                Map<String, Object> subDocument = getMap(value);
                jsonObject.put(d.name(), subDocument);
            } else if (isSudDocumentList(value)) {
                jsonObject.put(d.name(), StreamSupport.stream(Iterable.class.cast(value).spliterator(), false)
                        .map(EntityConverter::getMap).collect(toList()));
            } else {
                jsonObject.put(d.name(), value);
            }
        };
    }

    private static Map<String, Object> getMap(Object value) {
        Map<String, Object> subDocument = new HashMap<>();
        StreamSupport.stream(Iterable.class.cast(value).spliterator(),
                false).forEach(feedJSON(subDocument));
        return subDocument;
    }

    private static boolean isSudDocument(Object value) {
        return value instanceof Iterable && StreamSupport.stream(Iterable.class.cast(value).spliterator(), false).
                allMatch(Element.class::isInstance);
    }

    private static boolean isSudDocumentList(Object value) {
        return value instanceof Iterable && StreamSupport.stream(Iterable.class.cast(value).spliterator(), false).
                allMatch(d -> d instanceof Iterable && isSudDocument(d));
    }

    static Stream<CommunicationEntity> getDocumentEntityStream(ElasticsearchClient client, SearchResponse<Map> responses) {
        return responses.hits().hits().stream()
                .map(hits -> ElasticsearchEntry.of(hits.id(), hits.source()))
                .filter(ElasticsearchEntry::isNotEmpty)
                .map(ElasticsearchEntry::toEntity);
    }

    private static SearchRequest.Builder buildSearchRequestBuilder(SelectQuery query, QueryConverterResult select) {
        SearchRequest.Builder searchBuilder = new SearchRequest.Builder();

        if (select.hasQuery()) {
            searchBuilder.query(select.statement().build());
        }

        feedBuilder(query, searchBuilder);
        return searchBuilder;
    }

    private static void feedBuilder(SelectQuery query, SearchRequest.Builder searchSource) {
        query.sorts().forEach(d -> {
            if (d.isAscending()) {
                searchSource.sort(s -> s.field(f -> f.field(d.property()).order(SortOrder.Asc)));
            } else {
                searchSource.sort(s -> s.field(f -> f.field(d.property()).order(SortOrder.Desc)));
            }
        });

        int from = (int) query.skip();
        int size = (int) query.limit();
        if (from > 0) {
            searchSource.from(from);
        }
        if (size > 0) {
            searchSource.size(size);
        }
    }


}

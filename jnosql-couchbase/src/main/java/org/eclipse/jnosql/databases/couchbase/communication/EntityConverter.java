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
package org.eclipse.jnosql.databases.couchbase.communication;


import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.json.JsonObject;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.StreamSupport.stream;

final class EntityConverter {

    static final String ID_FIELD = "_id";

    static final String COLLECTION_FIELD = "_collection";
    static final String SPLIT_KEY = ":";
    static final char SPLIT_KEY_CHAR = ':';

    private EntityConverter() {
    }


    static Stream<CommunicationEntity> convert(List<JsonObject> result, String database) {
        return
                result.stream()
                        .map(JsonObject::toMap)
                        .filter(Objects::nonNull)
                        .map(map -> {
                            if (map.size() == 1) {
                                Map.Entry<String, Object> entry = map.entrySet().stream().findFirst().get();
                                if (entry.getValue() instanceof Map) {
                                    List<Element> documents = toDocuments((Map<String, Object>) entry.getValue());
                                    return CommunicationEntity.of(entry.getKey(), documents);
                                }
                            }
                            List<Element> documents = toDocuments(map);
                            Optional<Element> entityDocument = documents.stream().filter(d -> COLLECTION_FIELD.equals(d.name())).findFirst();
                            String collection = entityDocument.map(d -> d.get(String.class)).orElse(database);
                            return CommunicationEntity.of(collection, documents);
                        });
    }

    private static List<Element> toDocuments(Map<String, Object> map) {
        List<Element> documents = new ArrayList<>();
        for (String key : map.keySet()) {
            Object value = map.get(key);
            if (Map.class.isInstance(value)) {
                documents.add(Element.of(key, toDocuments(Map.class.cast(value))));
            } else if (isADocumentIterable(value)) {
                List<List<Element>> subDocuments = new ArrayList<>();
                stream(Iterable.class.cast(value).spliterator(), false)
                        .map(m -> toDocuments(Map.class.cast(m)))
                        .forEach(e -> subDocuments.add((List<Element>) e));
                documents.add(Element.of(key, subDocuments));
            } else {
                documents.add(Element.of(key, value));
            }
        }
        return documents;
    }

    private static boolean isADocumentIterable(Object value) {
        return Iterable.class.isInstance(value) &&
                stream(Iterable.class.cast(value).spliterator(), false)
                        .allMatch(Map.class::isInstance);
    }

    static String getPrefix(String collection, String id) {
        String[] ids = id.split(SPLIT_KEY);
        if (ids.length == 2 && collection.equals(ids[0])) {
            return id;
        }
        return collection + SPLIT_KEY_CHAR + id;
    }


    static JsonObject convert(CommunicationEntity entity) {
        requireNonNull(entity, "entity is required");

        JsonObject jsonObject = JsonObject.create();
        entity.elements()
                .forEach(toJsonObject(jsonObject));
        return jsonObject;
    }

    private static Consumer<Element> toJsonObject(JsonObject jsonObject) {
        return d -> {
            Object value = ValueUtil.convert(d.value());
            if (Element.class.isInstance(value)) {
                convertDocument(jsonObject, d, value);
            } else if (Iterable.class.isInstance(value)) {
                convertIterable(jsonObject, d, value);
            } else {
                jsonObject.put(d.name(), value);
            }
        };
    }


    private static void convertDocument(JsonObject jsonObject, Element d, Object value) {
        Element document = Element.class.cast(value);
        jsonObject.put(d.name(), Collections.singletonMap(document.name(), document.get()));
    }

    private static void convertIterable(JsonObject jsonObject, Element document, Object value) {
        JsonObject map = JsonObject.create();
        JsonArray array = JsonArray.create();
        Iterable.class.cast(value).forEach(element -> {
            if (Element.class.isInstance(element)) {
                Element subdocument = Element.class.cast(element);
                map.put(subdocument.name(), subdocument.get());
            } else if (isSudDocument(element)) {
                JsonObject subJson = JsonObject.create();

                stream(Iterable.class.cast(element).spliterator(), false)
                        .forEach(getSubdocument(subJson));
                array.add(subJson);
            } else {
                array.add(element);
            }
        });
        if (array.isEmpty()) {
            jsonObject.put(document.name(), map);
        } else {
            jsonObject.put(document.name(), array);
        }
    }

    private static Consumer getSubdocument(JsonObject subJson) {
        return e -> toJsonObject(subJson).accept((Element) e);
    }

    private static boolean isSudDocument(Object value) {
        return value instanceof Iterable && stream(Iterable.class.cast(value).spliterator(), false).
                allMatch(Element.class::isInstance);
    }

}
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
 */
package org.eclipse.jnosql.databases.arangodb.communication;


import com.arangodb.ArangoDB;
import com.arangodb.entity.CollectionEntity;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

/**
 * The utilitarian class to ArangoDB
 */
public final class ArangoDBUtil {

    public static final String KEY = "_key";
    public static final String ID = "_id";
    public static final String REV = "_rev";
    private static final Logger LOGGER = Logger.getLogger(ArangoDBUtil.class.getName());

    private ArangoDBUtil() {
    }


    static void checkDatabase(String database, ArangoDB arangoDB) {
        Objects.requireNonNull(database, "database is required");
        try {
            Collection<String> databases = arangoDB.getDatabases();
            if (!databases.contains(database)) {
                arangoDB.createDatabase(database);
            }
        } catch (ArangoDBException e) {
            LOGGER.log(Level.WARNING, "Failed to create database: " + database, e);
        }
    }

    public static void checkCollection(String bucketName, ArangoDB arangoDB, String namespace) {
        checkDatabase(bucketName, arangoDB);
        List<String> collections = arangoDB.db(bucketName)
                .getCollections().stream()
                .map(CollectionEntity::getName)
                .toList();
        if (!collections.contains(namespace)) {
            arangoDB.db(bucketName).createCollection(namespace);
        }
    }

    static CommunicationEntity toEntity(JsonObject jsonObject) {
        List<Element> documents = toDocuments(jsonObject);

        String id = jsonObject.getString(ID);
        documents.add(Element.of(KEY, jsonObject.getString(KEY)));
        documents.add(Element.of(ID, id));
        documents.add(Element.of(REV, jsonObject.getString(REV)));
        String collection = id.split("/")[0];
        return CommunicationEntity.of(collection, documents);
    }

    static JsonObject toJsonObject(CommunicationEntity entity) {
        return toJsonObject(entity.elements());
    }

    private static List<Element> toDocuments(JsonObject object) {
        return object.entrySet().stream()
                .map(it -> Element.of(it.getKey(), toDocuments(it.getValue())))
                .collect(toList());
    }

    private static List<?> toDocuments(JsonArray array) {
        return array.stream()
                .map(ArangoDBUtil::toDocuments)
                .toList();
    }

    private static Object toDocuments(JsonValue value) {
        return switch (value.getValueType()) {
            case OBJECT -> toDocuments(value.asJsonObject());
            case ARRAY -> toDocuments(value.asJsonArray());
            case STRING -> ((JsonString) value).getString();
            case NUMBER -> ((JsonNumber) value).numberValue();
            case TRUE -> true;
            case FALSE -> false;
            case NULL -> null;
        };
    }

    private static JsonObject toJsonObject(Iterable<Element> elements) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        for (Element document : elements) {
            if (KEY.equals(document.name()) && Objects.isNull(document.get())) {
                continue;
            }
            Object value = ValueUtil.convert(document.value());
            builder.add(document.name(), toJsonValue(value));
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static JsonValue toJsonValue(Object value) {
        if (value instanceof Element document) {
            return toJsonObject(Collections.singletonList(document));
        } else if (value instanceof Iterable<?> iterable) {
            if (isSubDocument(iterable)) {
                return toJsonObject((Iterable<Element>) iterable);
            } else {
                JsonArrayBuilder builder = Json.createArrayBuilder();
                for (Object it : iterable) {
                    builder.add(toJsonValue(it));
                }
                return builder.build();
            }
        } else if (value instanceof Map<?, ?> map) {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                builder.add((String) e.getKey(), toJsonValue(e.getValue()));
            }
            return builder.build();
        } else if (Objects.isNull(value)) {
            return JsonValue.NULL;
        } else if (value instanceof Number number) {
            return Json.createValue(number);
        } else if (value instanceof String string) {
            return Json.createValue(string);
        } else if (Boolean.TRUE.equals(value)) {
            return JsonValue.TRUE;
        } else if (Boolean.FALSE.equals(value)) {
            return JsonValue.FALSE;
        } else {
            throw new IllegalArgumentException("Unsupported type: " + value.getClass());
        }
    }

    private static boolean isSubDocument(Iterable<?> iterable) {
        return stream(iterable.spliterator(), false).allMatch(Element.class::isInstance);
    }

}

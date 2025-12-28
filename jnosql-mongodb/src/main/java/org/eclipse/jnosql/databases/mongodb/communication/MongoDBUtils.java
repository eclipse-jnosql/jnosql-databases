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
package org.eclipse.jnosql.databases.mongodb.communication;

import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import static java.util.stream.StreamSupport.stream;

final class MongoDBUtils {
    static final String ID_FIELD = "_id";


    private MongoDBUtils() {
    }

    static Document getDocument(CommunicationEntity entity) {
        Document document = new Document();
        entity.elements().forEach(d -> document.append(d.name(), convert(d.value())));
        return document;
    }

    static Bson updateDocument(CommunicationEntity entity) {
        return updateDocument(entity::elements);
    }

    static Bson updateDocument(Supplier<List<Element>> elementsSupplier) {
        List<Bson> fields = new ArrayList<>();
        elementsSupplier.get().forEach(d -> fields.add(Updates.set(d.name(), convert(d.value()))));
        return Updates.combine(fields);
    }

    private static Object convert(Value value) {
        Object val = ValueUtil.convert(value, MongoDBValueWriteDecorator.MONGO_DB_VALUE_WRITER);
        if (val instanceof Element subDocument) {
            Object converted = convert(subDocument.value());
            return new Document(subDocument.name(), converted);
        }
        if (isSudDocument(val)) {
            return getMap(val);
        }
        if (isSudDocumentList(val)) {
            return StreamSupport.stream(Iterable.class.cast(val).spliterator(), false)
                    .map(MongoDBUtils::getMap).toList();
        }
        return val;
    }


    public static List<Element> of(Map<String, ?> values) {
        Predicate<String> isNotNull = s -> values.get(s) != null;
        Function<String, Element> documentMap = key -> {
            Object value = values.get(key);
            return getDocument(key, value);
        };
        return values.keySet().stream().filter(isNotNull).map(documentMap).toList();
    }

    private static Element getDocument(String key, Object value) {
        if (value instanceof Document) {
            return Element.of(key, of(Document.class.cast(value)));
        } else if (isDocumentIterable(value)) {
            List<List<Element>> documents = new ArrayList<>();
            for (Object object : Iterable.class.cast(value)) {
                Map<?, ?> map = Map.class.cast(object);
                documents.add(map.entrySet().stream().map(e -> getDocument(e.getKey().toString(), e.getValue())).toList());
            }
            return Element.of(key, documents);
        }

        return Element.of(key, Value.of(convertValue(value)));
    }

    private static Object convertValue(Object value) {
        if (value instanceof Binary) {
            return Binary.class.cast(value).getData();
        }
        return value;
    }

    private static boolean isDocumentIterable(Object value) {
        return value instanceof Iterable &&
                stream(Iterable.class.cast(value).spliterator(), false)
                        .allMatch(Document.class::isInstance);
    }

    private static Object getMap(Object val) {
        Iterable<?> iterable = Iterable.class.cast(val);
        Map<Object, Object> map = new HashMap<>();
        for (Object item : iterable) {
            var document = cast(item);
            map.put(document.name(), convert(document.value()));
        }
        return map;
    }

    private static Element cast(Object document) {
        return Element.class.cast(document);
    }

    private static boolean isSudDocument(Object value) {
        return value instanceof Iterable && StreamSupport.stream(Iterable.class.cast(value).spliterator(), false).
                allMatch(Element.class::isInstance);
    }

    private static boolean isSudDocumentList(Object value) {
        return value instanceof Iterable && StreamSupport.stream(Iterable.class.cast(value).spliterator(), false).
                allMatch(d -> d instanceof Iterable && isSudDocument(d));
    }
}

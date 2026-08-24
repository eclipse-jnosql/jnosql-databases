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
package org.eclipse.jnosql.databases.ravendb.communication;

import net.ravendb.client.Constants;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

final class EntityConverter {

    static final String ID_FIELD = "_id";


    private EntityConverter() {
    }

    public static String getId(Map<?, ?> entity) {
        if (entity != null) {
            Map<String, Object> metadata = (Map<String, Object>) entity.remove(Constants.Documents.Metadata.KEY);
            return (String) metadata.get(Constants.Documents.Metadata.ID);
        }
        return "";
    }

    static CommunicationEntity getEntity(Map map) {

        Map<String, Object> entity = new HashMap<>(map);
        Map<String, Object> metadata = (Map<String, Object>) entity.remove(Constants.Documents.Metadata.KEY);
        String id = metadata.get(Constants.Documents.Metadata.ID).toString();
        String collection = metadata.get(Constants.Documents.Metadata.COLLECTION).toString();
        return new RavenDBEntry(id, collection, entity).toEntity();
    }

    static Map<String, Object> getMap(CommunicationEntity entity) {

        Map<String, Object> entityMap = new HashMap<>();

        entity.elements().stream()
                .filter(d -> !ID_FIELD.equals(d.name()))
                .forEach(feedJSON(entityMap));
        return entityMap;
    }


    private static Consumer<Element> feedJSON(Map<String, Object> map) {
        return d -> {
            Object value = ValueUtil.convert(d.value());
            if (value instanceof Element) {
                Element subDocument = Element.class.cast(value);
                map.put(d.name(), singletonMap(subDocument.name(), subDocument.get()));
            } else if (isSudDocument(value)) {
                Map<String, Object> subDocument = getMap(value);
                map.put(d.name(), subDocument);
            } else if (isSudDocumentList(value)) {
                map.put(d.name(), StreamSupport.stream(Iterable.class.cast(value).spliterator(), false)
                        .map(EntityConverter::getMap).collect(toList()));
            } else {
                map.put(d.name(), value);
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


}

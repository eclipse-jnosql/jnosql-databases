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


import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

final class RavenDBEntry {

    private final String id;

    private final Map<String, Object> map;

    private final String collection;

    private static final Function<Map.Entry<?, ?>, Element> ENTRY_DOCUMENT = entry ->
            Element.of(entry.getKey().toString(), entry.getValue());


    RavenDBEntry(String id, String collection, Map<String, Object> map) {
        this.id = id;
        this.collection = collection;
        this.map = map;
    }

    boolean isEmpty() {
        return isNull(id) || isNull(collection) || isNull(map);
    }

    boolean isNotEmpty() {
        return !isEmpty();
    }

    CommunicationEntity toEntity() {
        Element id = Element.of(EntityConverter.ID_FIELD, this.id);
        List<Element> documents = map.keySet().stream()
                .map(k -> toDocument(k, map))
                .collect(Collectors.toList());
        CommunicationEntity entity = CommunicationEntity.of(collection, documents);
        entity.remove(EntityConverter.ID_FIELD);
        entity.add(id);
        return entity;
    }

    private Element toDocument(String key, Map<String, Object> properties) {
        Object value = properties.get(key);
        if (Map.class.isInstance(value)) {
            Map map = Map.class.cast(value);
            return Element.of(key, map.keySet()
                    .stream().map(k -> toDocument(k.toString(), map))
                    .collect(Collectors.toList()));
        }
        if (isADocumentIterable(value)) {
            List<List<Element>> documents = new ArrayList<>();
            for (Object object : Iterable.class.cast(value)) {
                Map<?, ?> map = Map.class.cast(object);
                documents.add(map.entrySet().stream().map(ENTRY_DOCUMENT).collect(toList()));
            }
            return Element.of(key, documents);

        }
        return Element.of(key, value);
    }

    private boolean isADocumentIterable(Object value) {
        return Iterable.class.isInstance(value) &&
                stream(Iterable.class.cast(value).spliterator(), false)
                        .allMatch(Map.class::isInstance);
    }


}

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
package org.eclipse.jnosql.databases.orientdb.communication;


import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

final class OrientDBConverter {

    static final String RID_FIELD = "@rid";
    static final String VERSION_FIELD = "@version";

    static final String ID_FIELD = "_id";

    private OrientDBConverter() {
    }

    static List<CommunicationEntity> convert(OResultSet resultSet) {
        List<CommunicationEntity> entities = new ArrayList<>();

        while (resultSet.hasNext()) {
            entities.add(convert(resultSet.next()));
        }
        return entities;
    }

    static CommunicationEntity convert(OResult row) {
        OElement element = row.toElement();
        String name = element.getSchemaType()
                .map(OClass::getName)
                .orElseThrow(() -> new IllegalArgumentException("SchemaType is required in a row"));

        CommunicationEntity entity = CommunicationEntity.of(name);
        for (String propertyName : element.getPropertyNames()) {
            Object property = element.getProperty(propertyName);
            entity.add(propertyName, convert(property));

        }
        ORID identity = element.getIdentity();
        int clusterId = identity.getClusterId();
        long clusterPosition = identity.getClusterPosition();
        entity.add(VERSION_FIELD, element.getVersion());
        entity.add(RID_FIELD, "#" + clusterId + ":" + clusterPosition);
        entity.add(ID_FIELD, "#" + clusterId + ":" + clusterPosition);
        return entity;
    }


    static CommunicationEntity convert(ODocument document) {
        if (document == null) {
            return null;
        }
        CommunicationEntity entity = CommunicationEntity.of(document.getClassName());
        Stream.of(document.fieldNames())
                .map(f -> Element.of(f, convert((Object) document.field(f))))
                .forEach(entity::add);
        entity.add(Element.of(RID_FIELD, document.field(RID_FIELD).toString()));
        entity.add(Element.of(VERSION_FIELD, document.getVersion()));
        entity.add(Element.of(ID_FIELD, document.field(RID_FIELD).toString()));
        return entity;
    }


    private static Object convert(Object object) {
        if (Map.class.isInstance(object)) {
            Map map = Map.class.cast(object);
            return map.keySet().stream()
                    .map(k -> Element.of(k.toString(), map.get(k)))
                    .collect(toList());
        } else if (List.class.isInstance(object)) {
            return List.class.cast(object).stream()
                    .map(OrientDBConverter::convert).collect(toList());
        }
        return object;
    }


    public static Map<String, Object> toMap(CommunicationEntity entity) {
        Map<String, Object> entityValues = new HashMap<>();
        for (Element document : entity.elements()) {
            toDocument(entityValues, document);
        }

        return entityValues;
    }

    private static void toDocument(Map<String, Object> entityValues, Element document) {
        Object value = ValueUtil.convert(document.value());
        if (Element.class.isInstance(value)) {
            Element subDocument = Element.class.cast(value);
            entityValues.put(document.name(), singletonMap(subDocument.name(), subDocument.get()));
        } else if (isDocumentIterable(value)) {
            entityValues.put(document.name(), getMap(value));
        } else if (isSudDocumentList(value)) {
            entityValues.put(document.name(), StreamSupport.stream(Iterable.class.cast(value).spliterator(), false)
                    .map(OrientDBConverter::getMap).collect(toList()));
        } else {
            entityValues.put(document.name(), value);
        }
    }

    private static Map<String, Object> getMap(Object valueAsObject) {
        Map<String, Object> map = new java.util.HashMap<>();
        stream(Iterable.class.cast(valueAsObject).spliterator(), false)
                .forEach(d -> toDocument(map, Element.class.cast(d)));
        return map;
    }

    private static boolean isDocumentIterable(Object value) {
        return Iterable.class.isInstance(value) &&
                stream(Iterable.class.cast(value).spliterator(), false)
                        .allMatch(Element.class::isInstance);
    }

    private static boolean isSudDocumentList(Object value) {
        return value instanceof Iterable && StreamSupport.stream(Iterable.class.cast(value).spliterator(), false).
                allMatch(d -> d instanceof Iterable && isDocumentIterable(d));
    }
}

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
package org.eclipse.jnosql.databases.solr.communication;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.Elements;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

final class SolrUtils {

    static final String ENTITY = "_entity";

    private SolrUtils() {
    }

    static SolrInputDocument getDocument(CommunicationEntity entity) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(ENTITY, entity.name());
        entity.elements().forEach(d -> document.addField(d.name(), convert(d.value())));
        return document;
    }

    private static Object convert(Value value) {
        Object val = ValueUtil.convert(value);
        if (val instanceof Element || isSudDocument(val) || isSudDocumentList(val)) {
            throw new SolrException("Apache Solr does not support to embedded field");
        }
        return val;
    }


    public static List<CommunicationEntity> of(SolrDocumentList values) {

        return values.stream()
                .map(SolrDocument::getFieldValueMap)
                .map(SolrUtils::solrToMap)
                .map(Elements::of)
                .map(documents -> {
                    final String entity = documents.stream()
                            .filter(d -> ENTITY.equals(d.name()))
                            .findFirst()
                            .map(d -> d.get(String.class))
                            .orElseThrow(() -> new SolrException("The field _entity is required"));
                    return CommunicationEntity.of(entity, documents);
                }).collect(Collectors.toList());
    }

    private static Map<String, Object> solrToMap(Map<String, Object> map) {
        return map.keySet().stream().collect(Collectors.toMap(k -> k, map::get));
    }

    private static boolean isSudDocument(Object value) {
        return value instanceof Iterable && StreamSupport.stream(Iterable.class.cast(value).spliterator(), false).
                allMatch(Elements.class::isInstance);
    }

    private static boolean isSudDocumentList(Object value) {
        return value instanceof Iterable && StreamSupport.stream(Iterable.class.cast(value).spliterator(), false).
                allMatch(d -> d instanceof Iterable && isSudDocument(d));
    }
}

/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 * and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *
 * Maximillian Arruda
 */

package org.eclipse.jnosql.databases.dynamodb.communication;

import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.StreamSupport;

import static java.util.Collections.singletonMap;

class DynamoDBConverter {

    static final String ENTITY = "@entity";
    static final String ID = "id";

    private DynamoDBConverter() {
    }

    private static Object convertValue(Object value) {
        if (value instanceof AttributeValue attributeValue) {
            return switch (attributeValue.type()) {
                case S -> attributeValue.s();
                case N -> Double.valueOf(attributeValue.n());
                case B -> attributeValue.b().asByteArray();
                case SS -> attributeValue.ss();
                case NS -> attributeValue.ns().stream().map(Double::valueOf).toList();
                case BS -> attributeValue.bs().stream().map(SdkBytes::asByteArray).toList();
                case L -> attributeValue.l().stream().map(DynamoDBConverter::convertValue).toList();
                case M ->
                        attributeValue.m().entrySet().stream().map(e -> Element.of(e.getKey(), convertValue(e.getValue()))).toList();
                case NUL -> null;
                case BOOL -> attributeValue.bool();
                default -> null; // map type
            };
        }
        return value;
    }

    static Map<String, Object> getMap(UnaryOperator<String> entityNameResolver, CommunicationEntity entity) {
        var nameResolver = Optional.ofNullable(entityNameResolver).orElse(UnaryOperator.identity());
        Map<String, Object> jsonObject = new HashMap<>();
        entity.elements().forEach(feedJSON(jsonObject));
        jsonObject.put(entityAttributeName(nameResolver), entity.name());
        return jsonObject;
    }

    public static String entityAttributeName(UnaryOperator<String> nameResolver) {
        return Optional.ofNullable(nameResolver.apply(ENTITY)).orElse(ENTITY);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Consumer<Element> feedJSON(Map<String, Object> jsonObject) {
        return d -> {
            Object value = ValueUtil.convert(d.value());
            if (value instanceof Element subElement) {
                jsonObject.put(d.name(), singletonMap(subElement.name(), subElement.get()));
            } else if (isSudDocument(value)) {
                Map<String, Object> subDocument = getMap(value);
                jsonObject.put(d.name(), subDocument);
            } else if (isSudDocumentList(value)) {
                jsonObject.put(d.name(), StreamSupport.stream(((Iterable) value).spliterator(), false)
                        .map(DynamoDBConverter::getMap).toList());
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

    public static Map<String, AttributeValue> toItem(UnaryOperator<String> entityNameResolver, CommunicationEntity entity) {
        UnaryOperator<String> resolver = Optional.ofNullable(entityNameResolver).orElse(UnaryOperator.identity());
        Map<String, Object> documentAttributes = getMap(resolver, entity);
        return toItem(documentAttributes);
    }


    private static Map<String, AttributeValue> toItem(Map<String, Object> documentAttributes) {
        HashMap<String, AttributeValue> result = new HashMap<>();
        documentAttributes.forEach((attribute, value) -> result.put(attribute, toAttributeValue(value)));
        return result;
    }

    public static AttributeValue toAttributeValue(Object value) {
        if (value == null)
            return AttributeValue.builder().nul(true).build();
        if (value instanceof String str)
            return AttributeValue.builder().s(str).build();
        if (value instanceof Number number)
            return AttributeValue.builder().n(String.valueOf(number)).build();
        if (value instanceof Boolean bool)
            return AttributeValue.builder().bool(bool).build();
        if (value instanceof List<?> list)
            return AttributeValue.builder().l(list.stream().filter(Objects::nonNull)
                    .map(DynamoDBConverter::toAttributeValue).toList()).build();
        if (value instanceof Map<?, ?> mapValue) {
            HashMap<String, AttributeValue> values = new HashMap<>();
            mapValue.forEach((k, v) -> values.put(String.valueOf(k), toAttributeValue(v)));
            return AttributeValue.builder().m(values).build();
        }
        if (value instanceof byte[] data) {
            return AttributeValue.builder().b(SdkBytes.fromByteArray(data)).build();
        }
        if (value instanceof ByteBuffer byteBuffer) {
            return AttributeValue.builder().b(SdkBytes.fromByteBuffer(byteBuffer)).build();
        }
        if (value instanceof InputStream input) {
            return AttributeValue.builder().b(SdkBytes.fromInputStream(input)).build();
        }
        if (value instanceof Element element) {
            return toAttributeValue(getMap(element));
        }
        return AttributeValue.builder().s(String.valueOf(value)).build();
    }

    public static Map<String, AttributeValueUpdate> toItemUpdate(UnaryOperator<String> entityNameResolver, CommunicationEntity entity) {
        UnaryOperator<String> resolver = Optional.ofNullable(entityNameResolver).orElse(UnaryOperator.identity());
        Map<String, Object> documentAttributes = getMap(resolver, entity);
        return toItemUpdate(documentAttributes);
    }

    private static Map<String, AttributeValueUpdate> toItemUpdate(Map<String, Object> documentAttributes) {
        return documentAttributes
                .entrySet()
                .stream()
                .map(entry -> Map.of(entry.getKey(), toAttributeValueUpdate(entry.getValue())))
                .reduce(new HashMap<>(), (a, b) -> {
                    a.putAll(b);
                    return a;
                });
    }

    public static AttributeValueUpdate toAttributeValueUpdate(Object value) {
        return AttributeValueUpdate.builder().value(toAttributeValue(value)).action(AttributeAction.PUT).build();
    }


    public static CommunicationEntity toCommunicationEntity(UnaryOperator<String> entityNameResolver, Map<String, AttributeValue> item) {
        if (item == null) {
            return null;
        }
        if (item.isEmpty()) {
            return null;
        }
        UnaryOperator<String> resolver = Optional.ofNullable(entityNameResolver).orElse(UnaryOperator.identity());
        String entityAttribute = resolver.apply(ENTITY);
        var entityName = item.containsKey(entityAttribute) ? item.get(entityAttribute).s() : entityAttribute;
        var elements = item.entrySet()
                .stream()
                .filter(entry -> !Objects.equals(entityAttribute, entry.getKey()))
                .map(entry -> Element.of(entry.getKey(), convertValue(entry.getValue())))
                .toList();
        return CommunicationEntity.of(entityName, elements);
    }
}

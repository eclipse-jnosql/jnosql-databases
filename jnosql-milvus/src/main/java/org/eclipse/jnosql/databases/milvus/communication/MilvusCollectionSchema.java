/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.milvus.communication;

import io.milvus.v2.common.DataType;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.response.DescribeCollectionResp;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.mapping.vector.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the Milvus schema of a collection from a {@link CommunicationEntity}
 * and checks that later entities are compatible with it.
 *
 * @param idType      the type of the primary key {@code _id}
 * @param vectorField the name of the vector field
 * @param dimension   the vector dimension, zero for sparse vectors
 * @param fields      the scalar fields by name
 * @param kind        the vector representation of the vector field
 */
record MilvusCollectionSchema(DataType idType, String vectorField, int dimension, Map<String, DataType> fields,
                              MilvusVectors.Kind kind) {

    MilvusCollectionSchema(DataType idType, String vectorField, int dimension, Map<String, DataType> fields) {
        this(idType, vectorField, dimension, fields, MilvusVectors.Kind.DENSE);
    }

    static MilvusCollectionSchema of(DescribeCollectionResp description) {
        DataType idType = null;
        String vectorField = null;
        int dimension = 0;
        MilvusVectors.Kind kind = MilvusVectors.Kind.DENSE;
        Map<String, DataType> fields = new HashMap<>();
        for (CreateCollectionReq.FieldSchema field : description.getCollectionSchema().getFieldSchemaList()) {
            if (Boolean.TRUE.equals(field.getIsPrimaryKey())) {
                idType = field.getDataType();
            } else if (field.getDataType() == DataType.FloatVector) {
                vectorField = field.getName();
                dimension = field.getDimension();
                kind = MilvusVectors.Kind.DENSE;
            } else if (field.getDataType() == DataType.SparseFloatVector) {
                vectorField = field.getName();
                dimension = 0;
                kind = MilvusVectors.Kind.SPARSE;
            } else {
                fields.put(field.getName(), field.getDataType());
            }
        }
        List<CreateCollectionReq.StructFieldSchema> structs = description.getCollectionSchema().getStructFields();
        if (structs != null) {
            for (CreateCollectionReq.StructFieldSchema struct : structs) {
                for (CreateCollectionReq.FieldSchema nested : struct.getFields()) {
                    if (nested.getDataType() == DataType.FloatVector) {
                        vectorField = struct.getName();
                        dimension = nested.getDimension();
                        kind = MilvusVectors.Kind.MULTI;
                    }
                }
            }
        }
        if (idType == null || vectorField == null) {
            throw new IllegalStateException("The collection " + description.getCollectionName()
                    + " was not created by JNoSQL: it needs a primary key and a vector field");
        }
        return new MilvusCollectionSchema(idType, vectorField, dimension, fields, kind);
    }

    static CreateCollectionReq.CollectionSchema create(CommunicationEntity entity, Element vectorElement,
                                                       int dimension, int maxLength, io.milvus.v2.client.MilvusClientV2 client) {
        CreateCollectionReq.CollectionSchema schema = client.createSchema();
        schema.setEnableDynamicField(true);
        Object id = MilvusRowConverter.ensureId(entity);
        AddFieldReq.AddFieldReqBuilder<?> idField = AddFieldReq.builder()
                .fieldName(MilvusRowConverter.ID_FIELD)
                .isPrimaryKey(true)
                .autoID(false);
        if (isIntegral(id)) {
            idField.dataType(DataType.Int64);
        } else {
            idField.dataType(DataType.VarChar).maxLength(maxLength);
        }
        schema.addField(idField.build());
        addVectorField(schema, vectorElement, dimension);
        for (Element element : entity.elements()) {
            String name = element.name();
            if (element == vectorElement || MilvusRowConverter.ID_FIELD.equals(name)) {
                continue;
            }
            Object value = MilvusValueWriteDecorator.convert(element.value());
            if (value == null) {
                continue;
            }
            DataType type = typeOf(name, value);
            AddFieldReq.AddFieldReqBuilder<?> field = AddFieldReq.builder()
                    .fieldName(name).dataType(type).isNullable(true);
            if (type == DataType.VarChar) {
                field.maxLength(maxLength);
            }
            schema.addField(field.build());
        }
        return schema;
    }

    private static void addVectorField(CreateCollectionReq.CollectionSchema schema, Element vectorElement,
                                       int dimension) {
        Vector vector = (Vector) MilvusValueWriteDecorator.convert(vectorElement.value());
        switch (MilvusVectors.kind(vector)) {
            case DENSE -> schema.addField(AddFieldReq.builder().fieldName(vectorElement.name())
                    .dataType(DataType.FloatVector).dimension(dimension).build());
            case SPARSE -> schema.addField(AddFieldReq.builder().fieldName(vectorElement.name())
                    .dataType(DataType.SparseFloatVector).build());
            default -> schema.addField(AddFieldReq.builder().fieldName(vectorElement.name())
                    .dataType(DataType.Array).elementType(DataType.Struct)
                    .maxCapacity(MilvusVectors.maxCapacity())
                    .addStructField(AddFieldReq.builder().fieldName(MilvusVectors.MULTI_FIELD)
                            .dataType(DataType.FloatVector).dimension(dimension).build())
                    .build());
        }
    }

    void validate(String collection, CommunicationEntity entity, Element vectorElement, int size) {
        Vector vector = (Vector) MilvusValueWriteDecorator.convert(vectorElement.value());
        if (MilvusVectors.kind(vector) != kind) {
            throw new IllegalArgumentException("The collection " + collection + " stores " + kind
                    + " vectors but the entity uses " + MilvusVectors.kind(vector));
        }
        if (!vectorField.equals(vectorElement.name())) {
            throw new IllegalArgumentException("The collection " + collection + " has the vector field "
                    + vectorField + " but the entity uses " + vectorElement.name());
        }
        if (kind != MilvusVectors.Kind.SPARSE && dimension != size) {
            throw new IllegalArgumentException("The vector has " + size + " dimensions but the collection "
                    + collection + " expects " + dimension);
        }
        Object id = MilvusValueWriteDecorator.convert(entity.find(MilvusRowConverter.ID_FIELD)
                .orElseThrow().value());
        if (isIntegral(id) != (idType == DataType.Int64)) {
            throw new IllegalArgumentException("The id " + id + " is not compatible with the collection "
                    + collection + " that uses ids of type " + idType);
        }
        for (Element element : entity.elements()) {
            DataType expected = fields.get(element.name());
            Object value = MilvusValueWriteDecorator.convert(element.value());
            if (expected == null || value == null || element == vectorElement) {
                continue;
            }
            DataType actual = typeOf(element.name(), value);
            boolean compatible = actual == expected
                    || (expected == DataType.Double && actual == DataType.Int64);
            if (!compatible) {
                throw new IllegalArgumentException("The field " + element.name() + " of the collection "
                        + collection + " is " + expected + " but the value is " + actual);
            }
        }
    }

    static boolean isIntegral(Object value) {
        return value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long;
    }

    private static DataType typeOf(String name, Object value) {
        if (value instanceof CharSequence || value instanceof Character || value instanceof UUID
                || value instanceof Enum<?>) {
            return DataType.VarChar;
        }
        if (isIntegral(value)) {
            return DataType.Int64;
        }
        if (value instanceof Float || value instanceof Double) {
            return DataType.Double;
        }
        if (value instanceof Boolean) {
            return DataType.Bool;
        }
        if (value instanceof Iterable<?> || value instanceof Map<?, ?> || value instanceof Element
                || (value.getClass().isArray() && !(value instanceof byte[]))) {
            return DataType.JSON;
        }
        throw new UnsupportedOperationException("Milvus cannot store the field " + name
                + " of type " + value.getClass().getName());
    }
}

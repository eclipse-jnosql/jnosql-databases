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

package org.eclipse.jnosql.databases.cassandra.communication;


import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.data.UdtValue;
import com.datastax.oss.driver.api.core.metadata.schema.ClusteringOrder;
import com.datastax.oss.driver.api.core.metadata.schema.ColumnMetadata;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import com.datastax.oss.driver.api.core.metadata.schema.TableMetadata;
import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.SetType;
import com.datastax.oss.driver.api.core.type.UserDefinedType;
import com.datastax.oss.driver.api.core.type.codec.CodecNotFoundException;
import com.datastax.oss.driver.api.core.type.codec.TypeCodec;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.insert.Insert;
import com.datastax.oss.driver.api.querybuilder.insert.InsertInto;
import com.datastax.oss.driver.api.querybuilder.insert.RegularInsert;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.datastax.oss.driver.api.querybuilder.term.Term;
import com.datastax.oss.driver.api.querybuilder.update.Assignment;
import com.datastax.oss.driver.api.querybuilder.update.Update;
import com.datastax.oss.driver.api.querybuilder.update.UpdateStart;
import com.datastax.oss.protocol.internal.ProtocolConstants;
import jakarta.data.Sort;
import org.eclipse.jnosql.communication.CommunicationException;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.communication.semistructured.UpdateQuery;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

final class QueryUtils {


    private QueryUtils() {
    }


    static Insert insert(CommunicationEntity entity, String keyspace, CqlSession session, Duration duration) {

        Map<String, Term> values = new HashMap<>();
        InsertInto insert = QueryBuilder.insertInto(keyspace, entity.name());
        entity.elements()
                .forEach(c -> {
                    if (UDT.class.isInstance(c)) {
                        insertUDT(UDT.class.cast(c), keyspace, entity.name(), session, values::put);
                    } else {
                        insertSingleField(c, values::put);
                    }
                });

        RegularInsert regularInsert = insert.values(values);
        if (duration != null) {
            return regularInsert.usingTtl((int) duration.getSeconds());
        }
        return regularInsert;
    }

    static Update update(UpdateQuery updateQuery, String keyspace, CqlSession session) {
        return update(updateQuery, keyspace, session, null);
    }

    static Update update(UpdateQuery updateQuery, String keyspace, CqlSession session, Duration duration) {

        List<Assignment> assignments = new LinkedList<>();

        final BiConsumer<String, Term> valueConsumer = (name, value) ->
                assignments.add(Assignment.setColumn("\"%s\"".formatted(name), value));

        UpdateStart updateStart = QueryBuilder.update(keyspace, updateQuery.name());

        if (duration != null) {
            updateStart = updateStart.usingTtl((int) duration.getSeconds());
        }

        updateQuery.set()
                .forEach(c -> {
                    if (UDT.class.isInstance(c)) {
                        insertUDT(UDT.class.cast(c), keyspace, updateQuery.name(), session, valueConsumer);
                    } else {
                        insertSingleField(c, valueConsumer);
                    }
                });

        return updateStart.set(assignments)
                .where(Relations.createClause(updateQuery.condition().orElse(null)));
    }

    public static Select select(SelectQuery query, String keyspace) {
        String columnFamily = query.name();
        final List<String> columns = query.columns();

        Select select = null;
        if (columns.isEmpty()) {
            select = QueryBuilder.selectFrom(keyspace, columnFamily).all();
        } else {
            select = QueryBuilder.selectFrom(keyspace, columnFamily).columns(columns);
        }

        select = select.where(Relations.createClause(query.condition().orElse(null)));
        final Map<String, ClusteringOrder> sort = query.sorts().stream()
                .collect(Collectors.toMap(Sort::property, mapSort()));
        select = select.orderBy(sort);
        return select;
    }

    private static Function<Sort, ClusteringOrder> mapSort() {
        return s -> s.isAscending() ? ClusteringOrder.ASC : ClusteringOrder.DESC;
    }

    private static void insertUDT(UDT udt, String keyspace, String columnFamily, CqlSession session,
                                  BiConsumer<String, Term> values) {

        final Optional<KeyspaceMetadata> keyspaceMetadata = session.getMetadata().getKeyspace(keyspace);
        UserDefinedType userType = keyspaceMetadata
                .flatMap(ks -> ks.getUserDefinedType(udt.userType()))
                .orElseThrow(() -> new IllegalArgumentException("Missing UDT definition"));

        final TableMetadata tableMetadata = keyspaceMetadata
                .flatMap(k -> k.getTable(columnFamily))
                .orElseThrow(() -> new IllegalArgumentException("Missing Table definition"));

        final ColumnMetadata columnMetadata = tableMetadata.getColumn(getName(udt))
                .orElseThrow(() -> new IllegalArgumentException("Missing the column definition"));

        final DataType type = columnMetadata.getType();
        Iterable elements = Iterable.class.cast(udt.get());
        Object udtValue = getUdtValue(userType, elements, type);
        values.accept(getName(udt), QueryBuilder.literal(udtValue));
    }

    private static Object getUdtValue(UserDefinedType userType, Iterable elements, DataType type) {

        Collection<Object> udtValues = getCollectionUdt(type);

        UdtValue udtValue = userType.newValue();
        final List<String> udtNames = userType.getFieldNames().stream().map(CqlIdentifier::asInternal)
                .toList();
        for (Object object : elements) {
            if (Element.class.isInstance(object)) {
                Element column = Element.class.cast(object);
                Object convert = ValueUtil.convert(column.value());

                final int index = udtNames.indexOf(column.name());
                if (index < 0) {
                    throw new CommunicationException("This field has not been found: " + column.name() +
                            " the fields available are " + udtNames + " in the UDT type " + userType.getName()
                            .asCql(true) + " at the keyspace " + userType.getKeyspace());
                }
                DataType fieldType = userType.getFieldTypes().get(index);
                TypeCodec<Object> objectTypeCodec = CodecRegistry.DEFAULT.codecFor(fieldType);
                if (fieldType instanceof SetType) {
                    udtValue.set(getName(column), new HashSet<Object>((Collection<?>) convert), objectTypeCodec);
                } else {
                    udtValue.set(getName(column), convert, objectTypeCodec);
                }

            } else if (Iterable.class.isInstance(object)) {
                udtValues.add(getUdtValue(userType, Iterable.class.cast(Iterable.class.cast(object)), type));
            }
        }
        if (udtValues.isEmpty()) {
            return udtValue;
        }
        return udtValues;

    }

    private static Collection<Object> getCollectionUdt(DataType type) {
        if (ProtocolConstants.DataType.SET == type.getProtocolCode()) {
            return new HashSet<>();
        } else {
            return new ArrayList<>();
        }
    }

    private static void insertSingleField(Element column, BiConsumer<String, Term> values) {
        Object value = column.get();
        if (value == null) {
            values.accept(getName(column), QueryBuilder.literal(null));
            return;
        }
        try {
            CodecRegistry.DEFAULT.codecFor(value);
            values.accept(getName(column), QueryBuilder.literal(value));
        } catch (CodecNotFoundException exp) {
            values.accept(getName(column), QueryBuilder.literal(ValueUtil.convert(column.value())));
        }
    }


    public static String count(String columnFamily, String keyspace) {
        return String.format("select count(*) from %s.%s", keyspace, columnFamily);
    }

    static String getName(Element column) {
        return getName(column.name());
    }

    static String getName(String name) {
        if (name.charAt(0) == '_') {
            return "\"" + name + "\"";
        }
        return name;
    }

    private static Object[] getIinValue(Value value) {
        return ValueUtil.convertToList(value).toArray();
    }


}

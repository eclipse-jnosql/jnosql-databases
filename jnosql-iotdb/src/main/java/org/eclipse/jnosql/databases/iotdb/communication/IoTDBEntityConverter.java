/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License 2.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *   and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 *   You may elect to redistribute this code under either of these licenses.
 */
package org.eclipse.jnosql.databases.iotdb.communication;

import org.apache.tsfile.enums.ColumnCategory;
import org.apache.tsfile.enums.TSDataType;
import org.apache.tsfile.read.common.Field;
import org.apache.tsfile.read.common.RowRecord;
import org.apache.tsfile.utils.Binary;
import org.apache.tsfile.write.record.Tablet;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

final class IoTDBEntityConverter {

    static final String ID_FIELD = "_id";
    static final String TIME_COLUMN = "time";
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private IoTDBEntityConverter() {
    }

    static IoTDBRow toRow(CommunicationEntity entity) {
        identifier(entity.name());
        Element id = entity.find(ID_FIELD)
                .orElseThrow(() -> new IllegalArgumentException(
                        "IoTDB entities require a temporal '" + ID_FIELD + "' identifier"));
        long timestamp = toEpochMillis(id.get());
        List<IoTDBColumn> columns = entity.elements().stream()
                .filter(element -> !ID_FIELD.equals(element.name()))
                .filter(element -> element.get() != null)
                .map(IoTDBEntityConverter::column)
                .toList();
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("IoTDB entities require at least one non-null column");
        }
        return new IoTDBRow(entity.name(), timestamp, columns);
    }

    static Tablet tablet(List<IoTDBRow> rows) {
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("IoTDB tablet requires at least one row");
        }
        IoTDBRow first = rows.getFirst();
        List<String> names = first.columns().stream().map(IoTDBColumn::name).toList();
        List<TSDataType> types = first.columns().stream().map(IoTDBColumn::type).toList();
        List<ColumnCategory> categories = first.columns().stream()
                .map(column -> ColumnCategory.FIELD).toList();
        Tablet tablet = new Tablet(first.table(), names, types, categories, rows.size());
        for (IoTDBRow row : rows) {
            int index = tablet.getRowSize();
            tablet.addTimestamp(index, row.timestamp());
            row.columns().forEach(column -> tablet.addValue(column.name(), index, column.value()));
        }
        return tablet;
    }

    static CommunicationEntity toEntity(String table, List<String> names, List<TSDataType> types,
                                        RowRecord row) {
        CommunicationEntity entity = CommunicationEntity.of(table);
        List<Field> fields = row.getFields();
        for (int index = 0; index < names.size(); index++) {
            Field field = fields.get(index);
            if (field == null || field.getDataType() == null) {
                continue;
            }
            String name = names.get(index);
            Object value = resultValue(field, types.get(index));
            if (TIME_COLUMN.equalsIgnoreCase(name)) {
                entity.add(ID_FIELD, value instanceof Instant instant
                        ? instant : Instant.ofEpochMilli(((Number) value).longValue()));
            } else {
                entity.add(name, value);
            }
        }
        if (entity.find(ID_FIELD).isEmpty()) {
            throw new IllegalArgumentException("IoTDB result does not contain the TIME column");
        }
        return entity;
    }

    static long toEpochMillis(Object value) {
        Object converted = unwrap(value);
        try {
            return switch (converted) {
                case Instant instant -> instant.toEpochMilli();
                case LocalDateTime dateTime -> dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
                case OffsetDateTime dateTime -> dateTime.toInstant().toEpochMilli();
                case ZonedDateTime dateTime -> dateTime.toInstant().toEpochMilli();
                case null -> throw new IllegalArgumentException("IoTDB identifier cannot be null");
                default -> throw new IllegalArgumentException(
                        "IoTDB identifier must be Instant, LocalDateTime, OffsetDateTime, or ZonedDateTime");
            };
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("IoTDB timestamp is outside the supported millisecond range", exception);
        }
    }

    static String identifier(String value) {
        if (value == null || value.isBlank() || !IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Invalid IoTDB identifier '" + value + "': use letters, digits, and underscores only");
        }
        return "\"" + value + "\"";
    }

    static Object unwrap(Object value) {
        return value instanceof Value jnosqlValue ? jnosqlValue.get() : value;
    }

    private static IoTDBColumn column(Element element) {
        identifier(element.name());
        if (TIME_COLUMN.equalsIgnoreCase(element.name())) {
            throw new IllegalArgumentException(
                    "IoTDB column name '" + TIME_COLUMN + "' is reserved for the temporal identifier");
        }
        Object raw = element.get();
        return new IoTDBColumn(element.name(), normalize(raw), type(element.name(), raw));
    }

    private static Object normalize(Object value) {
        return switch (value) {
            case Byte number -> number.intValue();
            case Short number -> number.intValue();
            case BigInteger number when number.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) >= 0
                    && number.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0 -> number.longValue();
            case Character character -> character.toString();
            case CharSequence text -> text.toString();
            case UUID uuid -> uuid.toString();
            case Enum<?> enumeration -> enumeration.name();
            case Instant instant -> instant.toEpochMilli();
            case LocalDateTime dateTime -> dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
            case OffsetDateTime dateTime -> dateTime.toInstant().toEpochMilli();
            case ZonedDateTime dateTime -> dateTime.toInstant().toEpochMilli();
            default -> value;
        };
    }

    private static TSDataType type(String name, Object value) {
        return switch (value) {
            case Boolean ignored -> TSDataType.BOOLEAN;
            case Byte ignored -> TSDataType.INT32;
            case Short ignored -> TSDataType.INT32;
            case Integer ignored -> TSDataType.INT32;
            case Long ignored -> TSDataType.INT64;
            case BigInteger number when number.bitLength() < 63 -> TSDataType.INT64;
            case Float ignored -> TSDataType.FLOAT;
            case Double ignored -> TSDataType.DOUBLE;
            case Character ignored -> TSDataType.STRING;
            case CharSequence ignored -> TSDataType.STRING;
            case UUID ignored -> TSDataType.STRING;
            case Enum<?> ignored -> TSDataType.STRING;
            case Instant ignored -> TSDataType.TIMESTAMP;
            case LocalDateTime ignored -> TSDataType.TIMESTAMP;
            case OffsetDateTime ignored -> TSDataType.TIMESTAMP;
            case ZonedDateTime ignored -> TSDataType.TIMESTAMP;
            case LocalDate ignored -> TSDataType.DATE;
            default -> throw new IllegalArgumentException(
                    "IoTDB column '" + name + "' has an unsupported type: " + value.getClass().getName());
        };
    }

    private static Object resultValue(Field field, TSDataType type) {
        Object value = field.getObjectValue(type);
        if (value instanceof Binary binary) {
            return binary.toString();
        }
        if (type == TSDataType.TIMESTAMP && value instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        return value;
    }

    record IoTDBRow(String table, long timestamp, List<IoTDBColumn> columns) {

        IoTDBRow {
            columns = List.copyOf(columns);
        }

        String schemaKey() {
            List<String> values = new ArrayList<>();
            values.add(table);
            columns.forEach(column -> values.add(column.name() + ":" + column.type()));
            return String.join("|", values);
        }
    }

    record IoTDBColumn(String name, Object value, TSDataType type) {
    }
}

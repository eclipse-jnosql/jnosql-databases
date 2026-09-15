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
package org.eclipse.jnosql.databases.questdb.communication;

import io.questdb.client.Sender;
import io.questdb.client.cutlass.qwp.client.QwpColumnBatch;
import io.questdb.client.cutlass.qwp.protocol.QwpConstants;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

final class QuestDBEntityConverter {

    static final String ID_FIELD = "_id";

    static final String TIMESTAMP_COLUMN = "timestamp";

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private static final int MAX_IDENTIFIER_LENGTH =
            Math.min(QwpConstants.MAX_TABLE_NAME_LENGTH, QwpConstants.MAX_COLUMN_NAME_LENGTH);

    private QuestDBEntityConverter() {
    }

    static QuestDBRow toRow(CommunicationEntity entity) {
        identifier(entity.name());
        Element identifier = entity.find(ID_FIELD)
                .orElseThrow(() -> new IllegalArgumentException(
                        "QuestDB entities require a temporal '" + ID_FIELD + "' identifier"));
        Instant timestamp = normalize(toInstant(identifier.get()));
        List<QuestDBColumn> columns = entity.elements().stream()
                .filter(element -> !ID_FIELD.equals(element.name()))
                .filter(element -> unwrap(element.value()) != null)
                .map(QuestDBEntityConverter::column)
                .toList();
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("QuestDB entities require at least one column");
        }
        return new QuestDBRow(entity.name(), timestamp, columns);
    }

    static QuestDBRow toUpdateRow(CommunicationEntity entity) {
        entity.elements().stream()
                .filter(element -> !ID_FIELD.equals(element.name()))
                .filter(element -> unwrap(element.value()) == null)
                .findAny()
                .ifPresent(element -> {
                    throw new UnsupportedOperationException(
                            "QuestDB native binds cannot update column '" + element.name() + "' to null");
                });
        return toRow(entity);
    }

    static void append(Sender sender, QuestDBRow row) {
        sender.table(row.table());
        row.columns().forEach(column -> column.type().append(sender, column.name(), column.value()));
        sender.at(toEpochMicros(row.timestamp()), ChronoUnit.MICROS);
    }

    static CommunicationEntity toEntity(String table, QwpColumnBatch batch, int row) {
        CommunicationEntity entity = CommunicationEntity.of(table);
        for (int index = 0; index < batch.getColumnCount(); index++) {
            String name = batch.getColumnName(index);
            Object value = resultValue(batch, index, row);
            if (TIMESTAMP_COLUMN.equalsIgnoreCase(name)) {
                if (!(value instanceof Instant timestamp)) {
                    throw new IllegalArgumentException("QuestDB result does not contain a designated timestamp");
                }
                entity.add(ID_FIELD, timestamp);
            } else if (value != null) {
                entity.add(name, value);
            }
        }
        if (entity.find(ID_FIELD).isEmpty()) {
            throw new IllegalArgumentException("QuestDB result does not contain a designated timestamp");
        }
        return entity;
    }

    static Instant toInstant(Object value) {
        Object converted = unwrap(value);
        return switch (converted) {
            case Instant instant -> instant;
            case LocalDateTime localDateTime -> localDateTime.toInstant(ZoneOffset.UTC);
            case OffsetDateTime offsetDateTime -> offsetDateTime.toInstant();
            case ZonedDateTime zonedDateTime -> zonedDateTime.toInstant();
            case null -> throw new IllegalArgumentException("QuestDB identifier cannot be null");
            default -> throw new IllegalArgumentException(
                    "QuestDB identifier must be Instant, LocalDateTime, OffsetDateTime, or ZonedDateTime");
        };
    }

    static Object queryValue(String name, Object value) {
        Object converted = unwrap(value);
        if (converted == null) {
            throw new IllegalArgumentException("QuestDB predicates cannot compare null values");
        }
        return ID_FIELD.equals(name) ? normalize(toInstant(converted)) : converted;
    }

    static String identifier(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("QuestDB identifier cannot be blank");
        }
        if (value.length() > MAX_IDENTIFIER_LENGTH || !IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Invalid QuestDB identifier '" + value + "': use letters, digits, and underscores only");
        }
        return "\"" + value + "\"";
    }

    private static QuestDBColumn column(Element element) {
        identifier(element.name());
        if (TIMESTAMP_COLUMN.equalsIgnoreCase(element.name())) {
            throw new IllegalArgumentException(
                    "QuestDB column name '" + TIMESTAMP_COLUMN + "' is reserved for the temporal identifier");
        }
        Object value = unwrap(element.value());
        return new QuestDBColumn(element.name(), value, QuestDBType.of(element.name(), value));
    }

    private static Object unwrap(Object value) {
        return value instanceof Value jnosqlValue ? ValueUtil.convert(jnosqlValue) : value;
    }

    private static Instant normalize(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MICROS);
    }

    static long toEpochMicros(Instant instant) {
        try {
            return Math.addExact(Math.multiplyExact(instant.getEpochSecond(), 1_000_000L), instant.getNano() / 1_000L);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("QuestDB timestamp is outside the supported microsecond range", exception);
        }
    }

    record QuestDBRow(String table, Instant timestamp, List<QuestDBColumn> columns) {
    }

    record QuestDBColumn(String name, Object value, QuestDBType type) {
    }

    private static Object resultValue(QwpColumnBatch batch, int column, int row) {
        if (batch.isNull(column, row)) {
            return null;
        }
        return switch (batch.getColumnWireType(column)) {
            case QwpConstants.TYPE_BOOLEAN -> batch.getBoolValue(column, row);
            case QwpConstants.TYPE_BYTE -> batch.getByteValue(column, row);
            case QwpConstants.TYPE_SHORT -> batch.getShortValue(column, row);
            case QwpConstants.TYPE_CHAR -> batch.getCharValue(column, row);
            case QwpConstants.TYPE_INT -> batch.getIntValue(column, row);
            case QwpConstants.TYPE_LONG -> batch.getLongValue(column, row);
            case QwpConstants.TYPE_FLOAT -> batch.getFloatValue(column, row);
            case QwpConstants.TYPE_DOUBLE -> batch.getDoubleValue(column, row);
            case QwpConstants.TYPE_DATE -> Instant.ofEpochMilli(batch.getLongValue(column, row));
            case QwpConstants.TYPE_TIMESTAMP ->
                    instantFromEpoch(batch.getLongValue(column, row), 1_000_000L);
            case QwpConstants.TYPE_TIMESTAMP_NANOS ->
                    instantFromEpoch(batch.getLongValue(column, row), 1_000_000_000L);
            case QwpConstants.TYPE_SYMBOL -> batch.getSymbol(column, row);
            case QwpConstants.TYPE_VARCHAR -> batch.getString(column, row);
            case QwpConstants.TYPE_UUID -> new UUID(
                    batch.getUuidHi(column, row), batch.getUuidLo(column, row));
            case QwpConstants.TYPE_BINARY -> batch.getBinary(column, row);
            default -> throw new UnsupportedOperationException(
                    "QuestDB result column '" + batch.getColumnName(column)
                            + "' has unsupported QWP type "
                            + QwpConstants.getTypeName(batch.getColumnWireType(column)));
        };
    }

    private static Instant instantFromEpoch(long value, long unitsPerSecond) {
        return Instant.ofEpochSecond(Math.floorDiv(value, unitsPerSecond),
                Math.floorMod(value, unitsPerSecond) * (1_000_000_000L / unitsPerSecond));
    }

    private enum QuestDBType {
        BOOLEAN {
            @Override
            void append(Sender sender, String name, Object value) {
                sender.boolColumn(name, (Boolean) value);
            }
        },
        BYTE {
            @Override
            void append(Sender sender, String name, Object value) {
                sender.byteColumn(name, (Byte) value);
            }
        },
        SHORT {
            @Override
            void append(Sender sender, String name, Object value) {
                sender.shortColumn(name, (Short) value);
            }
        },
        INTEGER {
            @Override
            void append(Sender sender, String name, Object value) {
                sender.intColumn(name, (Integer) value);
            }
        },
        LONG {
            @Override
            void append(Sender sender, String name, Object value) {
                sender.longColumn(name, ((Number) value).longValue());
            }
        },
        FLOAT {
            @Override
            void append(Sender sender, String name, Object value) {
                sender.floatColumn(name, (Float) value);
            }
        },
        DOUBLE {
            @Override
            void append(Sender sender, String name, Object value) {
                sender.doubleColumn(name, (Double) value);
            }
        },
        CHARACTER {
            @Override
            void append(Sender sender, String name, Object value) {
                sender.charColumn(name, (Character) value);
            }
        },
        VARCHAR {
            @Override
            void append(Sender sender, String name, Object value) {
                sender.stringColumn(name, value.toString());
            }
        },
        TIMESTAMP {
            @Override
            void append(Sender sender, String name, Object value) {
                sender.timestampColumn(name, toInstant(value));
            }
        },
        UUID_TYPE {
            @Override
            void append(Sender sender, String name, Object value) {
                UUID uuid = (UUID) value;
                sender.uuidColumn(name, uuid.getLeastSignificantBits(), uuid.getMostSignificantBits());
            }
        };

        abstract void append(Sender sender, String name, Object value);

        static QuestDBType of(String name, Object value) {
            return switch (value) {
                case Boolean ignored -> BOOLEAN;
                case Byte ignored -> BYTE;
                case Short ignored -> SHORT;
                case Integer ignored -> INTEGER;
                case Long ignored -> LONG;
case BigInteger bigInteger
                        when bigInteger.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) >= 0
                                && bigInteger.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0 -> LONG;
                case Float ignored -> FLOAT;
                case Double ignored -> DOUBLE;
                case Character ignored -> CHARACTER;
                case CharSequence ignored -> VARCHAR;
                case Instant ignored -> TIMESTAMP;
                case LocalDateTime ignored -> TIMESTAMP;
                case OffsetDateTime ignored -> TIMESTAMP;
                case ZonedDateTime ignored -> TIMESTAMP;
                case UUID ignored -> UUID_TYPE;
                default -> throw new IllegalArgumentException(
                        "QuestDB column '" + name + "' has an unsupported type: " + value.getClass().getName());
            };
        }
    }
}

/*
 *  Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.oracle.communication;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import oracle.nosql.driver.NoSQLHandle;
import oracle.nosql.driver.TimeToLive;
import oracle.nosql.driver.ops.DeleteRequest;
import oracle.nosql.driver.ops.GetRequest;
import oracle.nosql.driver.ops.GetResult;
import oracle.nosql.driver.ops.PutRequest;
import oracle.nosql.driver.values.JsonOptions;
import oracle.nosql.driver.values.MapValue;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.driver.ValueJSON;
import org.eclipse.jnosql.communication.keyvalue.BucketManager;
import org.eclipse.jnosql.communication.keyvalue.KeyValueEntity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.jnosql.databases.oracle.communication.TableCreationConfiguration.ID_FIELD;
import static org.eclipse.jnosql.databases.oracle.communication.TableCreationConfiguration.JSON_FIELD;


final class OracleNoSQLBucketManager implements BucketManager {
    private static final JsonOptions OPTIONS = new JsonOptions();
    private final String bucketName;
    private final NoSQLHandle serviceHandle;

    private final Jsonb jsonB;

    OracleNoSQLBucketManager(String bucketName, NoSQLHandle serviceHandle, Jsonb jsonB) {
        this.bucketName = bucketName;
        this.serviceHandle = serviceHandle;
        this.jsonB = jsonB;
    }

    @Override
    public String name() {
        return bucketName;
    }

    @Override
    public <K, V> void put(K key, V value) {
        Objects.requireNonNull(key, "key is required");
        Objects.requireNonNull(value, "value is required");
        putImplementation(key, value, TimeToLive.DO_NOT_EXPIRE);
    }

    @Override
    public void put(KeyValueEntity keyValueEntity) {
        Objects.requireNonNull(keyValueEntity, "keyValueEntity is required");
        putImplementation(keyValueEntity.key(), keyValueEntity.value(), TimeToLive.DO_NOT_EXPIRE);
    }

    @Override
    public void put(KeyValueEntity entity, Duration duration) {
        Objects.requireNonNull(entity, "keyValueEntity is required");
        Objects.requireNonNull(duration, "duration is required");
        putImplementation(entity.key(), entity.value(), TimeToLive.ofHours(duration.toHours()));
    }

    @Override
    public void put(Iterable<KeyValueEntity> iterable) {
        Objects.requireNonNull(iterable, "iterable is required");
        iterable.forEach(this::put);
    }

    @Override
    public void put(Iterable<KeyValueEntity> entities, Duration duration) {
        Objects.requireNonNull(entities, "entities is required");
        entities.forEach(e -> put(e, duration));
    }

    @Override
    public <K> Optional<Value> get(K key) {
        Objects.requireNonNull(key, "key is required");
        GetRequest getRequest = new GetRequest();
        getRequest.setKey(new MapValue().put(ID_FIELD, key.toString()));
        getRequest.setTableName(name());
        GetResult getResult = serviceHandle.get(getRequest);
        if (getResult != null && getResult.getValue() != null) {
            String json = getResult.getValue().toJson(OPTIONS);
            InputStream stream = new ByteArrayInputStream(json.getBytes(UTF_8));
            JsonReader jsonReader = Json.createReader(stream);
            JsonObject readObject = jsonReader.readObject();
            JsonValue content = readObject.get(JSON_FIELD);
            return Optional.of(ValueJSON.of(content.toString()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public <K> Iterable<Value> get(Iterable<K> keys) {
        Objects.requireNonNull(keys, "keys is required");
        return StreamSupport.stream(keys.spliterator(), false)
                .map(this::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    @Override
    public <K> void delete(K key) {
        Objects.requireNonNull(key, "key is required");
        DeleteRequest deleteRequest = new DeleteRequest();
        deleteRequest.setKey(new MapValue().put(ID_FIELD, key.toString()));
        deleteRequest.setTableName(name());
        serviceHandle.delete(deleteRequest);
    }

    @Override
    public <K> void delete(Iterable<K> keys) {
        Objects.requireNonNull(keys, "keys is required");
        keys.forEach(this::delete);
    }

    @Override
    public void close() {
        this.serviceHandle.close();
    }

    private <K, V> void putImplementation(K key, V value, TimeToLive ttl) {
        MapValue mapValue = new MapValue().put("id", key.toString());
        MapValue contentVal = mapValue.putFromJson("content", jsonB.toJson(value),
                OPTIONS);
        PutRequest putRequest = new PutRequest()
                .setValue(contentVal)
                .setTTL(ttl)
                .setTableName(name());
        serviceHandle.put(putRequest);
    }
}

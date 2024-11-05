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
 *   Michele Rastelli
 */
package org.eclipse.jnosql.databases.arangodb.communication;

import com.arangodb.serde.ArangoSerde;
import jakarta.json.bind.Jsonb;
import org.eclipse.jnosql.communication.driver.JsonbSupplier;

import java.nio.charset.StandardCharsets;

/**
 * ArangoDB user-data serde that serializes and deserializes user data using JSONB.
 * This supports natively JsonP types, i.e. {@link jakarta.json.JsonValue} and its children.
 */
public class JsonbSerde implements ArangoSerde {

    private final Jsonb jsonb;

    public JsonbSerde() {
        this(JsonbSupplier.getInstance().get());
    }

    /**
     * Alternative constructor to provide {@link Jsonb} instance to use,
     * i.e. using custom configuration, see {@link jakarta.json.bind.JsonbBuilder#create(jakarta.json.bind.JsonbConfig)}
     * @param jsonb Jsonb
     */
    public JsonbSerde(Jsonb jsonb) {
        this.jsonb = jsonb;
    }

    @Override
    public byte[] serialize(Object value) {
        return jsonb.toJson(value).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public <T> T deserialize(byte[] content, Class<T> type) {
        return jsonb.fromJson(new String(content, StandardCharsets.UTF_8), type);
    }

}
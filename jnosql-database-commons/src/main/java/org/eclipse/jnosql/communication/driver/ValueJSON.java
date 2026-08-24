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
package org.eclipse.jnosql.communication.driver;

import jakarta.json.bind.Jsonb;
import org.eclipse.jnosql.communication.TypeSupplier;
import org.eclipse.jnosql.communication.Value;

import java.util.Objects;


/**
 * A {@link Value} implementation that storage all the information as a {@link String} with JSON format.
 */
public class ValueJSON implements Value {

    private static final Jsonb JSONB = JsonbSupplier.getInstance().get();

    private final String json;

    ValueJSON(String json) {
        this.json = json;
    }


    @Override
    public Object get() {
        return json;
    }

    @Override
    public <T> T get(Class<T> clazz) throws NullPointerException, UnsupportedOperationException {
        Objects.requireNonNull(clazz, "clazz is required");
        return JSONB.fromJson(json, clazz);
    }

    @Override
    public <T> T get(TypeSupplier<T> typeSupplier) throws NullPointerException, UnsupportedOperationException {
        Objects.requireNonNull(typeSupplier, "typeSupplier is required");
        return JSONB.fromJson(json, typeSupplier.get());
    }

    @Override
    public boolean isInstanceOf(Class<?> typeClass) {
        Objects.requireNonNull(typeClass, "typeClass is requried");
        return typeClass.isInstance(json);
    }

    @Override
    public boolean isNull() {
        return false;
    }

    /**
     * Returns a new instance of {@link Value} keeping the value as JSON
     *
     * @param json the value
     * @return the new Value instance
     * @throws NullPointerException when json is null
     */
    public static Value of(String json) throws NullPointerException {
        Objects.requireNonNull(json, "json is required");
        return new ValueJSON(json);
    }

    /**
     * Returns a new instance of {@link Value} converting to JSON first
     *
     * @param json the value
     * @return the new Value instance
     * @throws NullPointerException when json is null
     */
    public static Value of(Object json) throws NullPointerException {
        Objects.requireNonNull(json, "json is required");
        return new ValueJSON(JSONB.toJson(json));
    }
}

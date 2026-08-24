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
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.config.PropertyVisibilityStrategy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

enum DefaultJsonbSupplier implements JsonbSupplier {

    INSTANCE;

    private final Jsonb json;

    DefaultJsonbSupplier() {
        JsonbConfig config = new JsonbConfig()
                .withPropertyVisibilityStrategy(new PrivateVisibilityStrategy());
        this.json = JsonbBuilder.newBuilder()
                .withConfig(config)
                .build();
    }

    @Override
    public Jsonb get() {
        return json;
    }


    static class PrivateVisibilityStrategy implements PropertyVisibilityStrategy {

        @Override
        public boolean isVisible(Field field) {
            return true;
        }

        @Override
        public boolean isVisible(Method method) {
            return true;
        }

    }
}

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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DefaultJsonBSupplierTest {

    @Test
    public void shouldReturnDefaultInstance() {
        assertNotNull(JsonbSupplier.getInstance());
    }

    @Test
    public void shouldProvideJSON() {
        JsonbSupplier supplier = JsonbSupplier.getInstance();
        assertNotNull(supplier);
        assertNotNull(supplier.get());
    }

    @Test
    public void shouldReadFromField() {
        Jsonb jsonb = JsonbSupplier.getInstance().get();
        User user = new User("Ada", 32);
        String json = jsonb.toJson(user);
        assertNotNull(json);
        assertEquals(user, jsonb.fromJson(json, User.class));
    }
}

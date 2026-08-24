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

import java.util.function.Supplier;

/**
 * Defines a supplier to {@link Jsonb} already configured and ready to use in the drivers whose need a JSON processor.
 */
public interface JsonbSupplier extends Supplier<Jsonb> {

    /**
     * It returns a {@link JsonbSupplier} from {@link java.util.ServiceLoader} otherwise,
     * it will return the default JsonbSupplier that reads from the field instead of the method.
     *
     * @return {@link JsonbSupplier} instance
     */
    static JsonbSupplier getInstance() {
        return JsonbSupplierServiceLoader.getInstance();
    }
}

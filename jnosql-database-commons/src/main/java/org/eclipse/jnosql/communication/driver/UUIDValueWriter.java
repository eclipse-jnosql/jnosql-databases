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
package org.eclipse.jnosql.communication.driver;

import org.eclipse.jnosql.communication.ValueWriter;

import java.util.Objects;
import java.util.UUID;


/**
 * A {@link ValueWriter} implementation responsible for converting a {@link UUID}
 * into its {@link String} representation.
 * <p>
 * This custom writer allows Eclipse JNoSQL to seamlessly serialize UUID fields
 * into standard strings before persisting them to the underlying NoSQL database.
 * </p>
 * @see ValueWriter
 * @see UUID
 */
public class UUIDValueWriter implements ValueWriter<UUID, String> {

    /**
     * Creates a UUID value writer.
     */
    public UUIDValueWriter() {
    }

    @Override
    public boolean test(Class<?> type) {
        return UUID.class.equals(type);
    }


    @Override
    public String write(UUID uuid) {
        if(Objects.nonNull(uuid)) {
            return uuid.toString();
        }
        return null;
    }

}

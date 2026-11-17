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
 * A {@link ValueWriter} that converts a {@link UUID} into its canonical {@link String} form
 * (the lower-case, hyphenated representation returned by {@link UUID#toString()}).
 *
 * <p>It lets Eclipse JNoSQL persist UUID ids and attributes as strings in databases without a native UUID
 * type. Reading the value back into a {@link UUID} is the job of the matching converter in the mapping layer,
 * not of this writer.</p>
 *
 * {@snippet lang="java" :
 * UUIDValueWriter writer = new UUIDValueWriter();
 * UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
 * String text = writer.write(id); // "123e4567-e89b-12d3-a456-426614174000"
 * }
 *
 * @see ValueWriter
 * @see UUID
 */
public class UUIDValueWriter implements ValueWriter<UUID, String> {

    /**
     * Creates a UUID value writer.
     */
    public UUIDValueWriter() {
    }

    /**
     * Tells whether this writer handles the given type.
     *
     * @param type the type to check
     * @return {@code true} only when {@code type} is exactly {@link UUID}
     */
    @Override
    public boolean test(Class<?> type) {
        return UUID.class.equals(type);
    }


    /**
     * Converts the UUID to its string form.
     *
     * @param uuid the UUID to write
     * @return the canonical string, or {@code null} when {@code uuid} is {@code null}
     */
    @Override
    public String write(UUID uuid) {
        if(Objects.nonNull(uuid)) {
            return uuid.toString();
        }
        return null;
    }

}

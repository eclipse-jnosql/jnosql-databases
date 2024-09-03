/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
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
 *   Otavio Santana
 */
package org.eclipse.jnosql.databases.mongodb.communication;

import org.eclipse.jnosql.communication.ValueWriter;

import java.util.Objects;
import java.util.UUID;

public class UUIDValueWriter implements ValueWriter<UUID, String> {

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

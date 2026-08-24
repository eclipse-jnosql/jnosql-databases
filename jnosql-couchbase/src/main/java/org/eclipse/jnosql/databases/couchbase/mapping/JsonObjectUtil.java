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
package org.eclipse.jnosql.databases.couchbase.mapping;


import com.couchbase.client.java.json.JsonObject;
import jakarta.data.repository.Param;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Stream;

final class JsonObjectUtil {

    private JsonObjectUtil() {
    }

    static JsonObject getParams(Object[] args, Method method) {

        JsonObject jsonObject = JsonObject.create();
        Annotation[][] annotations = method.getParameterAnnotations();

        for (int index = 0; index < annotations.length; index++) {

            final Object arg = args[index];

            Optional<Param> param = Stream.of(annotations[index])
                    .filter(Param.class::isInstance)
                    .map(Param.class::cast)
                    .findFirst();
            param.ifPresent(p -> jsonObject.put(p.value(), arg));

        }

        return jsonObject;
    }
}

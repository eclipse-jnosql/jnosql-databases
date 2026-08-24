/*
 *  Copyright (c) 2025 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.mapping.driver;

import jakarta.data.repository.Param;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Utility class for extracting method parameters annotated with {@link Param} across all repository interfaces.
 *
 * This utility supports repositories using custom query annotations such as {@code @AQL}, {@code @Cypher},
 * and others, where parameters are annotated with {@link Param} to enable named parameter binding.
 *
 * Example Usage:
 * <pre>{@code
 * public interface PersonRepository extends DatabaseBRepository<Person, String> {
 *
 *     @DatabaseQuery("FOR p IN Person FILTER p.name == @name RETURN p")
 *     List<Person> findByName(@Param("name") String name);
 * }
 *
 * Method method = PersonRepository.class.getMethod("findByName", String.class);
 * Object[] args = {"John Doe"};
 * Map<String, Object> params = ParamUtil.INSTANCE.getParams(args, method);
 * System.out.println(params); // {name=John Doe}
 * }</pre>
 *
 * <p>The returned map is then used for binding values to query parameters dynamically at runtime.</p>
 */
public enum ParamUtil {

    INSTANCE;

    /**
     * Extracts parameters annotated with {@link Param} from repository methods and returns them as a key-value map.
     *
     * <p>This method is designed to work with various repository query types.
     *
     * @param args   the arguments passed to the repository method invocation
     * @param method the repository method whose parameters should be extracted
     * @return a map of parameter names (from {@code @Param}) and their corresponding values
     * @throws IllegalArgumentException if {@code args} or {@code method} is {@code null}
     */
    public Map<String, Object> getParams(Object[] args, Method method) {

        Map<String, Object> params = new HashMap<>();
        Annotation[][] annotations = method.getParameterAnnotations();

        for (int index = 0; index < annotations.length; index++) {

            final Object arg = args[index];

            Optional<Param> param = Stream.of(annotations[index])
                    .filter(Param.class::isInstance)
                    .map(Param.class::cast)
                    .findFirst();
            param.ifPresent(p -> params.put(p.value(), arg));

        }

        return params;
    }
}

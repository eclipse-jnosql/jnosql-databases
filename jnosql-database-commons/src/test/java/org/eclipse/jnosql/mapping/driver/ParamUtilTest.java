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
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ParamUtilTest {

    interface TestRepository {
        void findByName(@Param("name") String name);

        void findByAge(@Param("age") int age);

        void findByNameAndAge(@Param("name") String name, @Param("age") int age);

        void findWithoutParams(String input);
    }

    @Test
    void shouldExtractSingleNamedParameter() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("findByName", String.class);
        Object[] args = {"John Doe"};

        Map<String, Object> params = ParamUtil.INSTANCE.getParams(args, method);

        assertThat(params)
                .hasSize(1)
                .containsEntry("name", "John Doe");
    }

    @Test
    void shouldExtractIntegerParameter() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("findByAge", int.class);
        Object[] args = {30};

        Map<String, Object> params = ParamUtil.INSTANCE.getParams(args, method);

        assertThat(params)
                .hasSize(1)
                .containsEntry("age", 30);
    }

    @Test
    void shouldExtractMultipleParameters() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("findByNameAndAge", String.class, int.class);
        Object[] args = {"Jane Doe", 25};

        Map<String, Object> params = ParamUtil.INSTANCE.getParams(args, method);

        assertThat(params)
                .hasSize(2)
                .containsEntry("name", "Jane Doe")
                .containsEntry("age", 25);
    }

    @Test
    void shouldReturnEmptyMapWhenNoParamAnnotationExists() throws NoSuchMethodException {
        Method method = TestRepository.class.getMethod("findWithoutParams", String.class);
        Object[] args = {"test"};

        Map<String, Object> params = ParamUtil.INSTANCE.getParams(args, method);

        assertThat(params).isEmpty();
    }

}
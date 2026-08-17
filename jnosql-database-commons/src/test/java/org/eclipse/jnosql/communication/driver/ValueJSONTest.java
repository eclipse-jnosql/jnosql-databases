/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.communication.driver;

import org.assertj.core.api.Assertions;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.Value;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


public class ValueJSONTest {

    @Test
    public void shouldReturnErrorWhenElementIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> ValueJSON.of(null));
    }

    @Test
    public void shouldConvertType() {
        AtomicInteger number = new AtomicInteger(5_000);
        Value value = ValueJSON.of(number);
        assertThat(value.get(Integer.class)).isEqualTo(Integer.valueOf(5_000));
        assertThat(value.get(String.class)).isEqualTo("5000");
    }

    @Test
    public void shouldConvertMapIgnoringKeyValue() {
        Map<Integer, List<String>> map = Collections.singletonMap(10, Arrays.asList("1", "2", "3"));
        Value value = ValueJSON.of(map);
        Map<String, List<String>> result = value.get(new TypeReference<>() {
        });
        List<String> valueResult = result.get("10");
        Assertions.assertThat(result.keySet()).contains("10");
        Assertions.assertThat(valueResult).contains("1", "2", "3");
    }

}
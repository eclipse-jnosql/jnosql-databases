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

package org.eclipse.jnosql.databases.redis.communication;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


public class RedisUtilsTest {

    @Test
    public void shouldReturnNameSpace() {
        assertThat(RedisUtils.createKeyWithNameSpace("key", "namespace")).isEqualTo("namespace:key");
    }

    @Test
    public void shouldThrowWithNullKey() {
        assertThatExceptionOfType(IrregularKeyValue.class).isThrownBy(() -> RedisUtils.createKeyWithNameSpace(null, ""));
    }
}

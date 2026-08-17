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
 *   Lucas Furlaneto
 */
package org.eclipse.jnosql.databases.orientdb.communication;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


public class OrientDBLiveCallbackBuilderTest {

    @Test
    public void shouldBuildWithAtLeastOneCallback() {
        OrientDBLiveCallback build = OrientDBLiveCallbackBuilder.builder().onCreate(d -> {
        }).build();

        assertThat(build).isNotNull();
    }

    @Test
    public void shouldThrowException() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> OrientDBLiveCallbackBuilder.builder().build());
    }
}

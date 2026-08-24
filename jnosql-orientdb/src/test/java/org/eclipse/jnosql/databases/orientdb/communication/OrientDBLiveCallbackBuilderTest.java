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
 *   Lucas Furlaneto
 */
package org.eclipse.jnosql.databases.orientdb.communication;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OrientDBLiveCallbackBuilderTest {

    @Test
    public void shouldBuildWithAtLeastOneCallback() {
        OrientDBLiveCallback build = OrientDBLiveCallbackBuilder.builder().onCreate(d -> {
        }).build();

        assertNotNull(build);
    }

    @Test
    public void shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> OrientDBLiveCallbackBuilder.builder().build());
    }
}

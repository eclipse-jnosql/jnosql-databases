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

package org.eclipse.jnosql.databases.weaviate.communication;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class WeaviateDistanceTest {

    @Test
    @DisplayName("Should parse names case-insensitively")
    void shouldParseName() {
        assertThat(WeaviateDistance.parse(" cosine ")).isEqualTo(WeaviateDistance.COSINE);
        assertThat(WeaviateDistance.parse("manhattan")).isEqualTo(WeaviateDistance.MANHATTAN);
    }

    @Test
    @DisplayName("Should parse the native alias")
    void shouldParseAlias() {
        assertThat(WeaviateDistance.parse("l2-squared")).isEqualTo(WeaviateDistance.EUCLIDEAN);
    }

    @Test
    @DisplayName("Should round-trip through the native distance")
    void shouldRoundTrip() {
        for (WeaviateDistance distance : WeaviateDistance.values()) {
            assertThat(WeaviateDistance.from(distance.toDistance())).isEqualTo(distance);
        }
    }

    @Test
    @DisplayName("Should reject unknown and null values")
    void shouldRejectInvalid() {
        assertThatIllegalArgumentException().isThrownBy(() -> WeaviateDistance.parse("unknown"));
        assertThatNullPointerException().isThrownBy(() -> WeaviateDistance.parse(null));
    }
}

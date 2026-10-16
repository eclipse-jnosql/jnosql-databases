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
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class WeaviateThresholdTest {

    @Test
    @DisplayName("Should translate a cosine similarity into a distance")
    void shouldTranslateCosine() {
        assertThat(WeaviateThreshold.maxDistance(WeaviateDistance.COSINE, 0.8f)).isCloseTo(0.2f, org.assertj.core.data.Offset.offset(1e-6f));
    }

    @Test
    @DisplayName("Should negate the dot product")
    void shouldTranslateDot() {
        assertThat(WeaviateThreshold.maxDistance(WeaviateDistance.DOT, 2f)).isEqualTo(-2f);
    }

    @Test
    @DisplayName("Should square the Euclidean distance")
    void shouldTranslateEuclidean() {
        assertThat(WeaviateThreshold.maxDistance(WeaviateDistance.EUCLIDEAN, 3f)).isEqualTo(9f);
    }

    @Test
    @DisplayName("Should keep Manhattan and Hamming distances")
    void shouldKeepDistance() {
        assertThat(WeaviateThreshold.maxDistance(WeaviateDistance.MANHATTAN, 3f)).isEqualTo(3f);
        assertThat(WeaviateThreshold.maxDistance(WeaviateDistance.HAMMING, 1f)).isEqualTo(1f);
    }

    @Test
    @DisplayName("Should reject negative distances and unknown metrics")
    void shouldReject() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> WeaviateThreshold.maxDistance(WeaviateDistance.EUCLIDEAN, -1f));
        assertThatIllegalStateException().isThrownBy(() -> WeaviateThreshold.maxDistance(null, 1f));
    }
}

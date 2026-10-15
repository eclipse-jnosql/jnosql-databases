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
package org.eclipse.jnosql.databases.qdrant.communication;

import io.qdrant.client.grpc.Collections.Distance;

import java.util.Locale;
import java.util.Objects;

/**
 * The distance metrics Qdrant supports for a collection. It is a collection-level setting,
 * defined when the collection is created, and is not part of the search API.
 */
public enum QdrantDistance {

    COSINE(Distance.Cosine),
    EUCLID(Distance.Euclid),
    DOT(Distance.Dot),
    MANHATTAN(Distance.Manhattan);

    private final Distance distance;

    QdrantDistance(Distance distance) {
        this.distance = distance;
    }

    /**
     * @return the Qdrant client representation of this metric
     */
    Distance toDistance() {
        return distance;
    }

    /**
     * Parses a configuration value, ignoring case and surrounding whitespace.
     * The Qdrant name for the metric ({@code Cosine}, {@code Euclid}, {@code Dot}, {@code Manhattan}) is accepted.
     *
     * @param value the configuration value
     * @return the matching metric
     * @throws NullPointerException     when the value is null
     * @throws IllegalArgumentException when the value is not a supported metric
     */
    public static QdrantDistance parse(String value) {
        Objects.requireNonNull(value, "value is required");
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (QdrantDistance candidate : values()) {
            if (candidate.name().equals(normalized)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unsupported Qdrant distance: '" + value
                + "'. Supported values: COSINE, EUCLID, DOT, MANHATTAN");
    }
}

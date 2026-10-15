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
package org.eclipse.jnosql.databases.milvus.communication;

import io.milvus.v2.common.ConsistencyLevel;
import org.eclipse.jnosql.communication.Settings;

import java.util.Locale;

/**
 * Collection-level settings applied when a Milvus collection is created and when it is read.
 *
 * @param metric       the vector index metric
 * @param vectorSize   the vector dimension, if configured
 * @param consistency  the consistency level
 * @param maxLength    the maximum length of the text fields
 */
record MilvusCollectionSettings(MilvusMetric metric, Integer vectorSize, ConsistencyLevel consistency,
                                int maxLength) {

    static final int DEFAULT_MAX_LENGTH = 65_535;

    static final MilvusCollectionSettings DEFAULT = new MilvusCollectionSettings(MilvusMetric.COSINE, null,
            ConsistencyLevel.STRONG, DEFAULT_MAX_LENGTH);

    static MilvusCollectionSettings from(Settings settings) {
        MilvusMetric metric = settings.get(MilvusConfigurations.METRIC, String.class)
                .map(MilvusMetric::parse)
                .orElse(MilvusMetric.COSINE);
        Integer vectorSize = settings.get(MilvusConfigurations.VECTOR_SIZE, Integer.class).orElse(null);
        if (vectorSize != null && vectorSize < 1) {
            throw new IllegalArgumentException(MilvusConfigurations.VECTOR_SIZE.get() + " must be positive");
        }
        ConsistencyLevel consistency = settings.get(MilvusConfigurations.CONSISTENCY, String.class)
                .map(MilvusCollectionSettings::consistency)
                .orElse(ConsistencyLevel.STRONG);
        int maxLength = settings.get(MilvusConfigurations.MAX_LENGTH, Integer.class).orElse(DEFAULT_MAX_LENGTH);
        if (maxLength < 1 || maxLength > DEFAULT_MAX_LENGTH) {
            throw new IllegalArgumentException(MilvusConfigurations.MAX_LENGTH.get()
                    + " must be between 1 and " + DEFAULT_MAX_LENGTH);
        }
        return new MilvusCollectionSettings(metric, vectorSize, consistency, maxLength);
    }

    private static ConsistencyLevel consistency(String value) {
        try {
            return ConsistencyLevel.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported Milvus consistency level: " + value, exception);
        }
    }
}

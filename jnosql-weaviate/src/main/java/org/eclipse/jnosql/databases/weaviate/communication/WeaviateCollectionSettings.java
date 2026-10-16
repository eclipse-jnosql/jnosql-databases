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

import org.eclipse.jnosql.communication.Settings;

/**
 * Collection-level settings applied when a Weaviate collection is created.
 *
 * @param distance the distance of the vector index
 */
record WeaviateCollectionSettings(WeaviateDistance distance) {

    static final WeaviateCollectionSettings DEFAULT = new WeaviateCollectionSettings(WeaviateDistance.COSINE);

    static WeaviateCollectionSettings from(Settings settings) {
        WeaviateDistance distance = settings.get(WeaviateConfigurations.DISTANCE, String.class)
                .map(WeaviateDistance::parse)
                .orElse(WeaviateDistance.COSINE);
        return new WeaviateCollectionSettings(distance);
    }
}

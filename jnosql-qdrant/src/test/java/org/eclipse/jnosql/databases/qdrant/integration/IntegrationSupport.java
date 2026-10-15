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
package org.eclipse.jnosql.databases.qdrant.integration;

import org.eclipse.jnosql.databases.qdrant.communication.QdrantConfigurations;
import org.eclipse.jnosql.databases.qdrant.communication.QdrantDistance;
import org.eclipse.jnosql.databases.qdrant.communication.VectorDatabase;
import org.eclipse.jnosql.mapping.core.config.MappingConfigurations;
import org.eclipse.jnosql.mapping.vector.DenseVector;

import java.util.List;

/**
 * Shared bootstrap and deterministic data for the Qdrant integration tests.
 */
final class IntegrationSupport {

    static final List<Article> ARTICLES = List.of(
            new Article("a", "Jakarta EE", "java", 2025, DenseVector.of(1f, 0f, 0f)),
            new Article("b", "Records", "java", 2024, DenseVector.of(0.9f, 0.1f, 0f)),
            new Article("c", "Indexes", "database", 2025, DenseVector.of(0f, 1f, 0f)));

    static final List<Movie> MOVIES = List.of(
            new Movie("a", "Heat", "action", 2025, DenseVector.of(1f, 0f, 0f)),
            new Movie("b", "Speed", "action", 2024, DenseVector.of(0.8f, 0.2f, 0f)),
            new Movie("c", "Amelie", "drama", 2025, DenseVector.of(0f, 1f, 0f)));

    private IntegrationSupport() {
    }

    static List<String> ids(List<?> entities) {
        return entities.stream().map(entity -> switch (entity) {
            case Article article -> article.id();
            case Movie movie -> movie.id();
            default -> throw new IllegalArgumentException("unexpected entity " + entity);
        }).toList();
    }

    static void configure() {
        VectorDatabase.INSTANCE.settings().toMap().forEach((key, value) -> System.setProperty(key, value.toString()));
        System.setProperty(QdrantConfigurations.DISTANCE.get(), QdrantDistance.DOT.name());
        System.setProperty(MappingConfigurations.VECTOR_DATABASE.get(), "library");
    }
}

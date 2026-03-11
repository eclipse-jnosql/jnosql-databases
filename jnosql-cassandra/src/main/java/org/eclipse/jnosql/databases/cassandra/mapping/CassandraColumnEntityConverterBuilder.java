/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
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
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.cassandra.mapping;

import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.semistructured.ProjectorConverter;

import java.util.Objects;

/**
 * Step builder for creating {@link CassandraColumnEntityConverter} instances outside a CDI container.
 */
public sealed interface CassandraColumnEntityConverterBuilder permits CassandraColumnEntityConverterBuilder.EntitiesStep,
        CassandraColumnEntityConverterBuilder.ConvertersStep,
        CassandraColumnEntityConverterBuilder.ProjectorStep,
        CassandraColumnEntityConverterBuilder.TerminalStep {

    /**
     * Returns a new builder starting from {@link EntitiesStep}.
     *
     * @return the first step of the builder
     */
    static EntitiesStep builder() {
        return new EntitiesStep();
    }

    /**
     * First step of the builder.
     */
    record EntitiesStep() implements CassandraColumnEntityConverterBuilder {
        /**
         * Provides the {@link EntitiesMetadata}.
         *
         * @param entities the entities metadata
         * @return a {@link ConvertersStep}
         * @throws NullPointerException if {@code entities} is null
         */
        public ConvertersStep withEntities(EntitiesMetadata entities) {
            Objects.requireNonNull(entities, "entities is required");
            return new ConvertersStep(entities);
        }
    }

    /**
     * Second step of the builder.
     */
    record ConvertersStep(EntitiesMetadata entities) implements CassandraColumnEntityConverterBuilder {
        /**
         * Provides the {@link Converters}.
         *
         * @param converters the converters collection
         * @return a {@link ProjectorStep}
         * @throws NullPointerException if {@code converters} is null
         */
        public ProjectorStep withConverters(Converters converters) {
            Objects.requireNonNull(converters, "converters is required");
            return new ProjectorStep(entities, converters);
        }
    }

    /**
     * Third step of the builder.
     */
    record ProjectorStep(EntitiesMetadata entities, Converters converters) implements CassandraColumnEntityConverterBuilder {
        /**
         * Provides the {@link ProjectorConverter}.
         *
         * @param projectorConverter the projector converter
         * @return a {@link TerminalStep}
         * @throws NullPointerException if {@code projectorConverter} is null
         */
        public TerminalStep withProjectorConverter(ProjectorConverter projectorConverter) {
            Objects.requireNonNull(projectorConverter, "projectorConverter is required");
            return new TerminalStep(entities, converters, projectorConverter);
        }
    }

    /**
     * Final step of the builder.
     */
    record TerminalStep(EntitiesMetadata entities, Converters converters, ProjectorConverter projectorConverter)
            implements CassandraColumnEntityConverterBuilder {
        /**
         * Builds a {@link CassandraColumnEntityConverter} instance.
         *
         * @return a new {@link CassandraColumnEntityConverter} instance
         */
        public CassandraColumnEntityConverter build() {
            return new CassandraColumnEntityConverter(entities, converters, projectorConverter);
        }
    }
}

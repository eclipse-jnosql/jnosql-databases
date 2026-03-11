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
package org.eclipse.jnosql.databases.solr.mapping;

import org.eclipse.jnosql.databases.solr.communication.SolrDocumentManager;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.semistructured.EventPersistManager;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Step builder for creating {@link SolrTemplate} instances outside a CDI container.
 */
public sealed interface SolrTemplateBuilder permits SolrTemplateBuilder.ConvertersStep,
        SolrTemplateBuilder.EntitiesStep,
        SolrTemplateBuilder.ManagerStep,
        SolrTemplateBuilder.EntityConverterStep,
        SolrTemplateBuilder.EventPersistManagerStep,
        SolrTemplateBuilder.TerminalStep {

    static ConvertersStep builder() {
        return new ConvertersStep();
    }

    record ConvertersStep() implements SolrTemplateBuilder {
        public EntitiesStep withConverters(Converters converters) {
            Objects.requireNonNull(converters, "converters is required");
            return new EntitiesStep(converters);
        }
    }

    record EntitiesStep(Converters converters) implements SolrTemplateBuilder {
        public ManagerStep withEntities(EntitiesMetadata entities) {
            Objects.requireNonNull(entities, "entities is required");
            return new ManagerStep(converters, entities);
        }
    }

    record ManagerStep(Converters converters, EntitiesMetadata entities) implements SolrTemplateBuilder {
        public EntityConverterStep withManager(SolrDocumentManager manager) {
            Objects.requireNonNull(manager, "manager is required");
            return new EntityConverterStep(converters, entities, () -> manager);
        }

        public EntityConverterStep withManager(Supplier<SolrDocumentManager> manager) {
            Objects.requireNonNull(manager, "manager is required");
            return new EntityConverterStep(converters, entities, manager);
        }
    }

    record EntityConverterStep(Converters converters, EntitiesMetadata entities,
                               Supplier<SolrDocumentManager> manager) implements SolrTemplateBuilder {
        public EventPersistManagerStep withEntityConverter(EntityConverter entityConverter) {
            Objects.requireNonNull(entityConverter, "entityConverter is required");
            return new EventPersistManagerStep(converters, entities, manager, entityConverter);
        }
    }

    record EventPersistManagerStep(Converters converters, EntitiesMetadata entities,
                                   Supplier<SolrDocumentManager> manager,
                                   EntityConverter entityConverter) implements SolrTemplateBuilder {
        public TerminalStep withEventPersistManager(EventPersistManager eventPersistManager) {
            Objects.requireNonNull(eventPersistManager, "eventPersistManager is required");
            return new TerminalStep(converters, entities, manager, entityConverter, eventPersistManager);
        }
    }

    record TerminalStep(Converters converters, EntitiesMetadata entities,
                        Supplier<SolrDocumentManager> manager,
                        EntityConverter entityConverter,
                        EventPersistManager eventPersistManager) implements SolrTemplateBuilder {
        public SolrTemplate build() {
            return new DefaultSolrTemplate(manager, entityConverter, eventPersistManager, entities, converters);
        }
    }
}

/*
 *  Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.solr.integration;


import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.solr.communication.DocumentDatabase;
import org.eclipse.jnosql.databases.solr.communication.SolrDocumentConfigurations;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.core.config.MappingConfigurations;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;
import org.eclipse.jnosql.mapping.document.spi.DocumentExtension;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.Optional;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, DocumentTemplate.class})
@AddPackages(Magazine.class)
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@AddExtensions({ReflectionEntityMetadataExtension.class,
        DocumentExtension.class})
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
@DisplayName("Document Template integration with Solr")
class TemplateIntegrationTest {

    @Inject
    private DocumentTemplate template;

    static {
        DocumentDatabase.INSTANCE.get();
        System.setProperty(SolrDocumentConfigurations.HOST.get(), DocumentDatabase.INSTANCE.host());
        System.setProperty(MappingConfigurations.DOCUMENT_DATABASE.get(), "database");
    }


    @Nested
    @DisplayName("When inserting entities with the document template")
    class WhenInsertingWithDocumentTemplate {

        @Test
        @DisplayName("Should insert a magazine and find it by id")
        public void shouldInsert() {
            Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
            template.insert(magazine);
            Optional<Magazine> optional = template.find(Magazine.class, magazine.id());
            assertThat(optional).isNotNull().isNotEmpty()
                    .get().isEqualTo(magazine);
        }
    }

    @Nested
    @DisplayName("When updating entities with the document template")
    class WhenUpdatingWithDocumentTemplate {

        @Test
        @DisplayName("Should update an inserted magazine and read the updated values")
        public void shouldUpdate() {
            Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
            assertThat(template.insert(magazine))
                    .isNotNull()
                    .isEqualTo(magazine);

            Magazine updated = new Magazine(magazine.id(), magazine.title() + " updated", 2);

            assertThat(template.update(updated))
                    .isNotNull()
                    .isNotEqualTo(magazine);

            assertThat(template.find(Magazine.class, magazine.id()))
                    .isNotNull().get().isEqualTo(updated);
        }
    }

    @Nested
    @DisplayName("When finding entities with the document template")
    class WhenFindingWithDocumentTemplate {

        @Test
        @DisplayName("Should find an inserted magazine by id")
        public void shouldFindById() {
            Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
            assertThat(template.insert(magazine))
                    .isNotNull()
                    .isEqualTo(magazine);

            assertThat(template.find(Magazine.class, magazine.id()))
                    .isNotNull().get().isEqualTo(magazine);
        }
    }

    @Nested
    @DisplayName("When deleting entities with the document template")
    class WhenDeletingWithDocumentTemplate {

        @Test
        @DisplayName("Should delete an inserted magazine by id")
        public void shouldDelete() {
            Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
            assertThat(template.insert(magazine))
                    .isNotNull()
                    .isEqualTo(magazine);

            template.delete(Magazine.class, magazine.id());
            assertThat(template.find(Magazine.class, magazine.id()))
                    .isNotNull().isEmpty();
        }
    }


}

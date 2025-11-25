/*
 *  Copyright (c) 2025 Contributors to the Eclipse Foundation
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
 *   Otavio Santana
 */
package org.eclipse.jnosql.databases.arangodb.integration;


import jakarta.inject.Inject;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.databases.arangodb.communication.ArangoDBConfigurations;
import org.eclipse.jnosql.databases.arangodb.communication.DocumentDatabase;
import org.eclipse.jnosql.databases.arangodb.mapping.ArangoDBExtension;
import org.eclipse.jnosql.databases.arangodb.mapping.ArangoDBTemplate;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.core.config.MappingConfigurations;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, ArangoDBTemplate.class})
@AddPackages(Magazine.class)
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, ArangoDBExtension.class})
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class RepositoryIntegrationTest {

    static {
        System.setProperty(ArangoDBConfigurations.HOST.get() + ".1", DocumentDatabase.INSTANCE.host());
        System.setProperty(MappingConfigurations.DOCUMENT_DATABASE.get(), "library");
    }

    @Inject
    private MagazineRepository repository;

    @BeforeEach
    void beforeEach() {
        repository.deleteAll();
    }

    @Test
    void shouldSave() {
        Magazine magazine = new Magazine(null, "Effective Java", 1);
        assertThat(repository.save(magazine))
                .isNotNull();

    }

    @Test
    void shouldFindAll() {
        for (int index = 0; index < 5; index++) {
            Magazine magazine = repository.save(new Magazine(null, "Effective Java", index));
            assertThat(magazine).isNotNull();
        }
        var result = repository.findAllByCypher();
        SoftAssertions.assertSoftly(soft -> {
            assertThat(result).isNotNull();
            assertThat(result).hasSize(5);
        });
    }

    @Test
    void shouldFindByName() {
        for (int index = 0; index < 5; index++) {
            Magazine magazine = repository.save(new Magazine(null, "Effective Java", index));
            assertThat(magazine).isNotNull();
        }
        var result = repository.findByTitle("Effective Java");
        SoftAssertions.assertSoftly(soft -> {
            assertThat(result).isNotNull();
            assertThat(result).hasSize(5);
        });
    }

}

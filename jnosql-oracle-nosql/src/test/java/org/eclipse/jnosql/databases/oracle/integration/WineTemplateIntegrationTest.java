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
package org.eclipse.jnosql.databases.oracle.integration;


import jakarta.inject.Inject;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.databases.oracle.communication.Database;
import org.eclipse.jnosql.databases.oracle.communication.OracleNoSQLConfigurations;
import org.eclipse.jnosql.databases.oracle.mapping.OracleNoSQLTemplate;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, DocumentTemplate.class})
@AddPackages(Wine.class)
@AddPackages(OracleNoSQLTemplate.class)
@AddExtensions({ReflectionEntityMetadataExtension.class,
        DocumentExtension.class})
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class WineTemplateIntegrationTest {

    @Inject
    private OracleNoSQLTemplate template;

    static {
        System.setProperty(OracleNoSQLConfigurations.HOST.get(), Database.INSTANCE.host());
        System.setProperty(MappingConfigurations.DOCUMENT_DATABASE.get(), "library");
    }

    @Test
    void shouldInsert() {

        Wine wine = Wine.builder()
                .id(UUID.randomUUID().toString())
                .data(Map.of("name", "beer"))
                .comments(List.of("comment1", "comment2"))
                .crew(List.of(new Crew("Otavio")))
                .build();

        this.template.insert(wine);

        Optional<Wine> result = this.template.select(Wine.class).where("id").eq(wine.id()).singleResult();

        SoftAssertions.assertSoftly(soft ->{
            soft.assertThat(result).isPresent();
            Wine updateWine = result.orElseThrow();
            soft.assertThat(updateWine.id()).isEqualTo(wine.id());
            soft.assertThat(updateWine.data()).isEqualTo(wine.data());
            soft.assertThat(updateWine.comments()).isEqualTo(wine.comments());
            soft.assertThat(updateWine.crew()).isEqualTo(wine.crew());
        });

    }

    


}

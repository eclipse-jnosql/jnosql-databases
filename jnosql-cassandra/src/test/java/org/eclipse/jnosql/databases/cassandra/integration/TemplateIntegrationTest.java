/*
 *  Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.cassandra.integration;


import jakarta.inject.Inject;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.communication.driver.ConfigurationReader;
import org.eclipse.jnosql.databases.cassandra.communication.CassandraConfigurations;
import org.eclipse.jnosql.databases.cassandra.communication.ColumnDatabase;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.column.ColumnTemplate;
import org.eclipse.jnosql.mapping.column.spi.ColumnExtension;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.core.config.MappingConfigurations;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, ColumnTemplate.class})
@AddPackages(Magazine.class)
@AddExtensions({ReflectionEntityMetadataExtension.class,
        ColumnExtension.class})
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class TemplateIntegrationTest {

    @Inject
    private ColumnTemplate template;

    static {
        Map<String, Object> configuration = new HashMap<>(ConfigurationReader.from("mapping.properties"));
        for (Map.Entry<String, Object> entry : configuration.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue().toString());
        }
        System.setProperty(MappingConfigurations.COLUMN_DATABASE.get(), "library");
        System.setProperty(CassandraConfigurations.HOST.get()+".1", ColumnDatabase.INSTANCE.host());
        System.setProperty(CassandraConfigurations.PORT.get(), Integer.toString(ColumnDatabase.INSTANCE.port()));
    }

    @Test
    void shouldInsert() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        template.insert(magazine);
        Optional<Magazine> optional = template.find(Magazine.class, magazine.id());
        assertThat(optional).isNotNull().isNotEmpty()
                .get().isEqualTo(magazine);
    }

    @Test
    void shouldUpdate() {
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

    @Test
    void shouldFindById() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        assertThat(template.insert(magazine))
                .isNotNull()
                .isEqualTo(magazine);

        assertThat(template.find(Magazine.class, magazine.id()))
                .isNotNull().get().isEqualTo(magazine);
    }

    @Test
    void shouldDelete() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        assertThat(template.insert(magazine))
                .isNotNull()
                .isEqualTo(magazine);

        template.delete(Magazine.class, magazine.id());
        assertThat(template.find(Magazine.class, magazine.id()))
                .isNotNull().isEmpty();
    }

    @Test
    void shouldUpdateNullValues(){
        var book = new Magazine(randomUUID().toString(), "Effective Java", 1);
        template.insert(book);
        template.update(new Magazine(book.id(), null, 2));
        Optional<Magazine> optional = template.select(Magazine.class).where("id")
                .eq(book.id()).singleResult();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(optional).isPresent();
            softly.assertThat(optional).get().extracting(Magazine::title).isNull();
            softly.assertThat(optional).get().extracting(Magazine::edition).isEqualTo(2);
        });
    }


}

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
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.mongodb.integration;

import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.mongodb.communication.MongoDBDocumentConfigurations;
import org.eclipse.jnosql.databases.mongodb.mapping.MongoDBTemplate;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.DatabaseType;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.core.config.MappingConfigurations;
import org.eclipse.jnosql.mapping.core.spi.EntityMetadataExtension;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;
import org.eclipse.jnosql.mapping.document.spi.DocumentExtension;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.eclipse.jnosql.databases.mongodb.communication.DocumentDatabase.INSTANCE;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, DocumentTemplate.class, MongoDBTemplate.class})
@AddPackages(Magazine.class)
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@AddExtensions({EntityMetadataExtension.class,
        DocumentExtension.class})
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class RepositoryIntegrationTest {

    static {
        INSTANCE.get("library");
        System.setProperty(MongoDBDocumentConfigurations.HOST.get() + ".1", INSTANCE.host());
        System.setProperty(MappingConfigurations.DOCUMENT_DATABASE.get(), "library");
    }

    @Inject
    @Database(DatabaseType.DOCUMENT)
    MagazineRepository repository;


    @Inject
    @Database(DatabaseType.DOCUMENT)
    BookStore bookStore;

    @Inject
    @Database(DatabaseType.DOCUMENT)
    AsciiCharacters characters;

    @Test
    void shouldSave() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        assertThat(repository.save(magazine))
                .isNotNull()
                .isEqualTo(magazine);

        Magazine updated = new Magazine(magazine.id(), magazine.title() + " updated", 2);

        assertThat(repository.save(updated))
                .isNotNull()
                .isNotEqualTo(magazine);

        assertThat(repository.findById(magazine.id()))
                .isNotNull().get().isEqualTo(updated);

        assertSoftly(softly -> {

            BookOrder order = new BookOrder(
                    randomUUID().toString(),
                    List.of(
                            new BookOrderItem(new Magazine(randomUUID().toString(), "Effective Java", 3), 1)
                            , new BookOrderItem(new Magazine(randomUUID().toString(), "Java Persistence Layer", 1), 10)
                            , new BookOrderItem(new Magazine(randomUUID().toString(), "Jakarta EE Cookbook", 1), 5)
                    )
            );

            bookStore.save(order);

            softly.assertThat(bookStore.findById(order.id()))
                    .as("cannot find the order persisted previously")
                    .isPresent()
                    .get()
                    .as("the loaded the persisted BookOrder doesn't matches with the BookOrder origin")
                    .satisfies(persistedOrder -> {
                        softly.assertThat(persistedOrder.id())
                                .as("the loaded the persisted BookOrder id is not equals to the BookOrder origin id")
                                .isEqualTo(order.id());
                        softly.assertThat(persistedOrder.items())
                                .as("the loaded the persisted BookOrder items is not equals to the BookOrder origin items")
                                .containsAll(order.items());
                    });


        });

    }

    @Test
    void shouldDelete() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        assertThat(repository.save(magazine))
                .isNotNull()
                .isEqualTo(magazine);

        repository.delete(magazine);
        assertThat(repository.findById(magazine.id()))
                .isNotNull().isEmpty();
    }

    @Test
    void shouldDeleteById() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        assertThat(repository.save(magazine))
                .isNotNull()
                .isEqualTo(magazine);

        repository.deleteById(magazine.id());
        assertThat(repository.findById(magazine.id()))
                .isNotNull().isEmpty();
    }

    @Test
    void shouldDeleteAll() {
        for (int index = 0; index < 20; index++) {
            Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
            assertThat(repository.save(magazine))
                    .isNotNull()
                    .isEqualTo(magazine);
        }

        repository.deleteAll();
        bookStore.deleteAll();
        assertThat(repository.findAll()).as("the repository is not empty").isEmpty();
        assertThat(bookStore.findAll()).as("the bookStore is not empty").isEmpty();
    }

    @Test
    public void testQueryWithNot() {
        // Given
        characters.populate();

        assertThatCode(() -> characters.getABCDFO())
                .as("Should not throw any exception because it should be supported by this implementation")
                .doesNotThrowAnyException();

        var abcdfo = characters.getABCDFO();

        assertSoftly(softly -> softly.assertThat(abcdfo)
                .as("should return a non null reference")
                .isNotNull()
                .as("Should return the characters 'A', 'B', 'C', 'D', 'F', and 'O'")
                .contains('A', 'B', 'C', 'D', 'F', 'O'));
    }
}

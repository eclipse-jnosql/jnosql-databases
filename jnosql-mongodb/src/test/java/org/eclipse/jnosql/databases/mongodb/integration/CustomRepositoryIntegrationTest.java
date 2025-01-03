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
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.mongodb.integration;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.inject.Inject;
import org.bson.BsonDocument;
import org.bson.Document;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.util.List;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;
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
class CustomRepositoryIntegrationTest {

    public static final String DATABASE_NAME = "library";

    static {
        INSTANCE.get(DATABASE_NAME);
        System.setProperty(MongoDBDocumentConfigurations.HOST.get() + ".1", INSTANCE.host());
        System.setProperty(MappingConfigurations.DOCUMENT_DATABASE.get(), DATABASE_NAME);
    }

    @Inject
    @Database(DatabaseType.DOCUMENT)
    MagazineCustomRepository magazineCustomRepository;


    @BeforeEach
    void cleanUp() {
        try (MongoClient mongoClient = INSTANCE.mongoClient()) {
            MongoCollection<Document> collection = mongoClient.getDatabase(DATABASE_NAME)
                    .getCollection(Magazine.class.getSimpleName());
            collection.deleteMany(new BsonDocument());
            await().atMost(Duration.ofSeconds(2))
                    .until(() -> collection.find().limit(1).first() == null);
        }
    }

    @Test
    void shouldSave() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        assertThat(magazineCustomRepository.save(magazine))
                .isNotNull()
                .isEqualTo(magazine);

        assertThat(magazineCustomRepository.getById(magazine.id()))
                .as("should return the persisted book")
                .hasValue(magazine);

        Magazine updated = new Magazine(magazine.id(), magazine.title() + " updated", 2);

        assertThat(magazineCustomRepository.save(updated))
                .isNotNull()
                .isNotEqualTo(magazine);

        assertThat(magazineCustomRepository.getById(magazine.id()))
                .as("should return the updated book")
                .hasValue(updated);
    }

    @Test
    void shouldSaveAllAndFindByIdIn() {

        List<Magazine> magazines = List.of(
                new Magazine(randomUUID().toString(), "Java Persistence Layer", 1)
                , new Magazine(randomUUID().toString(), "Effective Java", 3)
                , new Magazine(randomUUID().toString(), "Jakarta EE Cookbook", 1)
                , new Magazine(randomUUID().toString(), "Mastering The Java Virtual Machine", 1)
        );

        assertThat(magazineCustomRepository.saveAll(magazines))
                .isNotNull()
                .containsAll(magazines);

        assertThat(magazineCustomRepository.findByIdIn(magazines.stream().map(Magazine::id).toList()))
                .as("should return the persisted books")
                .containsAll(magazines);

    }

    @Test
    void shouldSaveAllAndFindBy() {

        Magazine javaPersistenceLayer = new Magazine(randomUUID().toString(), "Java Persistence Layer", 1);
        Magazine effectiveJava = new Magazine(randomUUID().toString(), "Effective Java", 3);
        Magazine jakartaEeCookbook = new Magazine(randomUUID().toString(), "Jakarta EE Cookbook", 1);
        Magazine masteringTheJavaVirtualMachine = new Magazine(randomUUID().toString(), "Mastering The Java Virtual Machine", 1);

        List<Magazine> magazines = List.of(
                javaPersistenceLayer
                , effectiveJava
                , jakartaEeCookbook
                , masteringTheJavaVirtualMachine
        );

        assertThat(magazineCustomRepository.saveAll(magazines))
                .isNotNull()
                .containsAll(magazines);

        PageRequest pageRequest = PageRequest.ofSize(2);
        Order<Magazine> orderByTitleAsc = Order.by(Sort.asc("title"));

        Page<Magazine> page1 = magazineCustomRepository.listAll(pageRequest, orderByTitleAsc);
        Page<Magazine> page2 = magazineCustomRepository.listAll(page1.nextPageRequest(), orderByTitleAsc);
        Page<Magazine> page3 = magazineCustomRepository.listAll(page2.nextPageRequest(), orderByTitleAsc);

        assertSoftly(softly -> {

            softly.assertThat(page1)
                    .as("should return the first page")
                    .hasSize(2)
                    .containsSequence(effectiveJava, jakartaEeCookbook);

            softly.assertThat(page2)
                    .as("should return the first page")
                    .hasSize(2)
                    .containsSequence(javaPersistenceLayer, masteringTheJavaVirtualMachine);

            softly.assertThat(page3)
                    .as("should return the third and last page with no items")
                    .hasSize(0);
        });


    }


    @Test
    void shouldDelete() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        assertThat(magazineCustomRepository.save(magazine))
                .isNotNull()
                .isEqualTo(magazine);

        assertThat(magazineCustomRepository.getById(magazine.id()))
                .isNotNull()
                .hasValue(magazine);

        magazineCustomRepository.delete(magazine);

        assertThat(magazineCustomRepository.getById(magazine.id()))
                .isNotNull()
                .isEmpty();
    }

    @Test
    void shouldDeleteAll() {

        List<Magazine> magazines = List.of(
                new Magazine(randomUUID().toString(), "Java Persistence Layer", 1)
                , new Magazine(randomUUID().toString(), "Effective Java", 3)
                , new Magazine(randomUUID().toString(), "Jakarta EE Cookbook", 1)
                , new Magazine(randomUUID().toString(), "Mastering The Java Virtual Machine", 1)
        );

        assertThat(magazineCustomRepository.saveAll(magazines))
                .isNotNull()
                .containsAll(magazines);

        await().atMost(Duration.ofSeconds(2))
                .until(() -> magazineCustomRepository.listAll().toList().size() >= magazines.size());

        assertThat(magazineCustomRepository.listAll())
                .isNotNull()
                .containsAll(magazines);

        magazineCustomRepository.deleteAll();

        await().atMost(Duration.ofSeconds(2))
                .until(() -> magazineCustomRepository.listAll().toList().isEmpty());


        assertThat(magazineCustomRepository.listAll())
                .isNotNull()
                .isEmpty();

    }

    @Test
    void shouldRemoveAll() {

        List<Magazine> magazines = List.of(
                new Magazine(randomUUID().toString(), "Java Persistence Layer", 1)
                , new Magazine(randomUUID().toString(), "Effective Java", 3)
                , new Magazine(randomUUID().toString(), "Jakarta EE Cookbook", 1)
                , new Magazine(randomUUID().toString(), "Mastering The Java Virtual Machine", 1)
        );

        assertThat(magazineCustomRepository.saveAll(magazines))
                .isNotNull()
                .containsAll(magazines);

        await().atMost(Duration.ofSeconds(2))
                .until(() -> magazineCustomRepository.listAll().toList().size() >= magazines.size());

        assertThat(magazineCustomRepository.listAll())
                .isNotNull()
                .containsAll(magazines);

        magazineCustomRepository.removeAll(magazines);

        await().atMost(Duration.ofSeconds(2))
                .until(() -> magazineCustomRepository.listAll()
                        .filter(magazines::contains)
                        .toList().isEmpty());

        assertThat(magazineCustomRepository.findByIdIn(magazines.stream().map(Magazine::id).toList()))
                .isNotNull()
                .isEmpty();
    }

}

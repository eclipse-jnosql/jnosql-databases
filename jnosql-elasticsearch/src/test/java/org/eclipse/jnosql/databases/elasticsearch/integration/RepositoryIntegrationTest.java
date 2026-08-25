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
package org.eclipse.jnosql.databases.elasticsearch.integration;


import jakarta.inject.Inject;
import org.awaitility.Awaitility;
import org.eclipse.jnosql.databases.elasticsearch.communication.DocumentDatabase;
import org.eclipse.jnosql.databases.elasticsearch.communication.ElasticsearchConfigurations;
import org.eclipse.jnosql.databases.elasticsearch.mapping.ElasticsearchTemplate;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnableAutoWeld
@AddPackages(value = {Database.class, Converters.class, EntityConverter.class, DocumentTemplate.class})
@AddPackages(Magazine.class)
@AddPackages(ElasticsearchTemplate.class)
@AddExtensions({ReflectionEntityMetadataExtension.class,
        DocumentExtension.class})
@AddPackages(Reflections.class)
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class RepositoryIntegrationTest {

    public static final String INDEX = "library";

    static {
        DocumentDatabase instance = DocumentDatabase.INSTANCE;
        instance.get("library");
        System.setProperty(ElasticsearchConfigurations.HOST.get() + ".1", instance.host());
        System.setProperty(MappingConfigurations.DOCUMENT_DATABASE.get(), INDEX);
        Awaitility.setDefaultPollDelay(100, MILLISECONDS);
        Awaitility.setDefaultTimeout(60L, SECONDS);
    }


    @Inject
    private Library library;

    @BeforeEach
    @AfterEach
    public void clearDatabase() {
        DocumentDatabase.clearDatabase(INDEX);
    }

    @Test
    public void shouldInsert() {
        Author joshuaBloch = new Author("Joshua Bloch");
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1, joshuaBloch);
        library.save(magazine);

        AtomicReference<Magazine> reference = new AtomicReference<>();
        await().until(() -> {
            Optional<Magazine> optional = library.findById(magazine.id());
            optional.ifPresent(reference::set);
            return optional.isPresent();
        });
        assertThat(reference.get()).isNotNull().isEqualTo(magazine);
    }

    @Test
    public void shouldUpdate() {
        Author joshuaBloch = new Author("Joshua Bloch");
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1, joshuaBloch);
        assertThat(library.save(magazine))
                .isNotNull()
                .isEqualTo(magazine);

        Magazine updated = magazine.updateEdition(2);

        assertThat(library.save(updated))
                .isNotNull()
                .isNotEqualTo(magazine);

        AtomicReference<Magazine> reference = new AtomicReference<>();
        await().until(() -> {
            Optional<Magazine> optional = library.findById(magazine.id());
            optional.ifPresent(reference::set);
            return optional.isPresent();
        });
        assertThat(reference.get()).isNotNull().isEqualTo(updated);

    }

    @Test
    public void shouldFindById() {
        Author joshuaBloch = new Author("Joshua Bloch");
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1, joshuaBloch);

        assertThat(library.save(magazine))
                .isNotNull()
                .isEqualTo(magazine);

        AtomicReference<Magazine> reference = new AtomicReference<>();
        await().until(() -> {
            Optional<Magazine> optional = library.findById(magazine.id());
            optional.ifPresent(reference::set);
            return optional.isPresent();
        });

        assertThat(reference.get()).isNotNull().isEqualTo(magazine);
    }

    @Test
    public void shouldDelete() {
        Author joshuaBloch = new Author("Joshua Bloch");
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1, joshuaBloch);
        assertThat(library.save(magazine))
                .isNotNull()
                .isEqualTo(magazine);


        library.deleteById(magazine.id());

        assertThat(library.findById(magazine.id()))
                .isNotNull().isEmpty();
    }


    @Test
    public void shouldFindByAuthorName() {
        Author joshuaBloch = new Author("Joshua Bloch");
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1, joshuaBloch);

        List<Magazine> expectedMagazines = List.of(magazine, magazine.newEdition(), magazine.newEdition());
        library.saveAll(expectedMagazines);

        await().until(() ->
                !library.findByAuthorName(magazine.author().name()).toList().isEmpty());

        var books = library.findByAuthorName(magazine.author().name()).toList();
        assertThat(books)
                .hasSize(3);

        assertThat(books)
                .containsAll(expectedMagazines);
    }

    @Test
    public void shouldFindByTitleLike() {
        Author joshuaBloch = new Author("Joshua Bloch");

        Magazine effectiveJava1stEdition = new Magazine(randomUUID().toString(), "Effective Java", 1, joshuaBloch);
        Magazine effectiveJava2ndEdition = effectiveJava1stEdition.newEdition();
        Magazine effectiveJava3rdEdition = effectiveJava2ndEdition.newEdition();

        Author elderMoraes = new Author("Elder Moraes");
        Magazine jakartaEECookMagazine = new Magazine(randomUUID().toString(), "Jakarta EE CookBook", 1, elderMoraes);

        List<Magazine> allMagazines = List.of(jakartaEECookMagazine, effectiveJava1stEdition, effectiveJava2ndEdition, effectiveJava3rdEdition);

        List<Magazine> effectiveMagazines = List.of(effectiveJava1stEdition, effectiveJava2ndEdition, effectiveJava3rdEdition);

        library.saveAll(allMagazines);

        AtomicReference<List<Magazine>> booksWithEffective = new AtomicReference<>();
        await().until(() -> {
            var books = library.findByTitleLike("Effective").toList();
            booksWithEffective.set(books);
            return !books.isEmpty();
        });

        AtomicReference<List<Magazine>> booksWithJa = new AtomicReference<>();
        await().until(() -> {
            var books = library.findByTitleLike("Ja*").toList();
            booksWithJa.set(books);
            return !books.isEmpty();
        });

        assertSoftly(softly -> assertThat(booksWithEffective.get())
                .as("returned book list with 'Effective' is not equals to the expected items ")
                .containsAll(effectiveMagazines));


        assertSoftly(softly -> assertThat(booksWithJa.get())
                .as("returned book list with 'Ja*' is not equals to the expected items ")
                .containsAll(allMagazines));
    }

    @Test
    public void shouldFindByEditionBetween() {
        Author joshuaBloch = new Author("Joshua Bloch");
        var magazine1 = new Magazine(randomUUID().toString(), "Effective Java", 1, joshuaBloch);
        var magazine2 = magazine1.newEdition();
        var magazine3 = magazine2.newEdition();
        var magazine4 = magazine3.newEdition();
        var magazine5 = magazine4.newEdition();
        var magazine6 = magazine5.newEdition();
        var magazine7 = magazine6.newEdition();

        List<Magazine> allMagazines = List.of(
                magazine1,
                magazine2,
                magazine3,
                magazine4,
                magazine5,
                magazine6,
                magazine7);

        library.saveAll(allMagazines);

        List<Magazine> magazinesGreaterThanEquals = allMagazines
                .stream()
                .dropWhile(m -> magazine3.edition() > m.edition())
                .toList();

        await().until(() ->
                !library.findByEditionGreaterThanEqual(magazine3.edition()).toList().isEmpty());

        var magazines = library.findByEditionGreaterThanEqual(magazine3.edition()).toList();
        assertThat(magazines)
                .containsAll(magazinesGreaterThanEquals);
    }

}

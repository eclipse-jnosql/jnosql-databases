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
package org.eclipse.jnosql.databases.couchbase.integration;


import com.couchbase.client.core.env.TimeoutConfig;
import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.couchbase.communication.CouchbaseUtil;
import org.eclipse.jnosql.databases.couchbase.communication.Database;
import org.eclipse.jnosql.databases.couchbase.mapping.CouchbaseExtension;
import org.eclipse.jnosql.databases.couchbase.mapping.CouchbaseTemplate;
import org.eclipse.jnosql.mapping.core.Converters;
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

import java.util.Optional;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, DocumentTemplate.class})
@AddPackages(Magazine.class)
@AddPackages(CouchbaseTemplate.class)
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@AddExtensions({ReflectionEntityMetadataExtension.class,
        DocumentExtension.class,
        CouchbaseExtension.class})
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class CouchbaseDocumentRepositoryIntegrationTest {

    @Inject
    private Library library;

    static {
        CouchbaseUtil.systemPropertySetup();
    }

    @BeforeEach
    @AfterEach
    public void cleanUp() {
        library.deleteAll();
    }

    @Test
    public void shouldInsert() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        library.save(magazine);
        await().atLeast(TimeoutConfig.DEFAULT_KV_DURABLE_TIMEOUT);
        Optional<Magazine> optional = library.findById(magazine.id());
        assertThat(optional).isNotNull().isNotEmpty()
                .get().isEqualTo(magazine);
    }

    @Test
    public void shouldUpdate() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        assertThat(library.save(magazine))
                .isNotNull()
                .isEqualTo(magazine);

        await().atLeast(TimeoutConfig.DEFAULT_KV_DURABLE_TIMEOUT);

        Magazine updated = new Magazine(magazine.id(), magazine.title() + " updated", 2);

        assertThat(library.save(updated))
                .isNotNull()
                .isNotEqualTo(magazine);

        await().atLeast(TimeoutConfig.DEFAULT_KV_DURABLE_TIMEOUT);

        assertThat(library.findById(magazine.id()))
                .isNotNull().get().isEqualTo(updated);

    }

    @Test
    public void shouldFindById() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        assertThat(library.save(magazine))
                .isNotNull()
                .isEqualTo(magazine);
        await().atLeast(TimeoutConfig.DEFAULT_KV_DURABLE_TIMEOUT);
        assertThat(library.findById(magazine.id()))
                .isNotNull().get().isEqualTo(magazine);
    }

    @Test
    public void shouldFindByTitleLike() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        assertThat(library.save(magazine))
                .isNotNull()
                .isEqualTo(magazine);

        await().atLeast(TimeoutConfig.DEFAULT_KV_DURABLE_TIMEOUT);

        var data = library.findByTitleLike(magazine.title()).toList();

        assertSoftly(softly -> {
            softly.assertThat(data).as("query result is a non-null instance").isNotNull();
            softly.assertThat(data.size()).as("query result size is correct").isEqualTo(1);
            softly.assertThat(data.getFirst()).as("returned data is correct").isEqualTo(magazine);
        });
    }

    @Test
    public void shouldFindByEditionLessThan() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        assertThat(library.save(magazine))
                .isNotNull()
                .isEqualTo(magazine);

        await().atLeast(TimeoutConfig.DEFAULT_KV_DURABLE_TIMEOUT);

        var booksFoundByEditionLessThan = library.findByEditionLessThan(Integer.MAX_VALUE).toList();

        assertSoftly(softly -> {
            softly.assertThat(booksFoundByEditionLessThan).as("query result from findByEditionLessThan() is a non-null instance").isNotNull();
            softly.assertThat(booksFoundByEditionLessThan.size()).as("query result size from findByEditionLessThan() is correct").isEqualTo(1);
            softly.assertThat(booksFoundByEditionLessThan.getFirst()).as("returned books from findByEditionLessThan() are correct").isEqualTo(magazine);
        });

        var booksFoundByEditionLessThanZero = library.findByEditionLessThan(0).toList();

        assertSoftly(softly -> {
            softly.assertThat(booksFoundByEditionLessThanZero).as("query result from findByEditionLessThan(0) is a non-null instance").isNotNull();
            softly.assertThat(booksFoundByEditionLessThanZero.size()).as("query result size from findByEditionLessThan(0) is correct").isEqualTo(0);
        });

        var booksFoundByEditionGreaterThan = library.findByEditionGreaterThan(0).toList();

        assertSoftly(softly -> {
            softly.assertThat(booksFoundByEditionGreaterThan).as("query result from findByEditionGreaterThan() is a non-null instance").isNotNull();
            softly.assertThat(booksFoundByEditionGreaterThan.size()).as("query result size from findByEditionGreaterThan() is correct").isEqualTo(1);
            softly.assertThat(booksFoundByEditionGreaterThan.getFirst()).as("returned books from findByEditionGreaterThan() are correct").isEqualTo(magazine);
        });


        var booksFoundByEditionGreaterThanMaxValue = library.findByEditionGreaterThan(Integer.MAX_VALUE).toList();

        assertSoftly(softly -> {
            softly.assertThat(booksFoundByEditionGreaterThanMaxValue).as("query result from findByEditionGreaterThan(Integer.MAX_VALUE) is a non-null instance").isNotNull();
            softly.assertThat(booksFoundByEditionGreaterThanMaxValue.size()).as("query result size from findByEditionGreaterThan(Integer.MAX_VALUE) is correct").isEqualTo(0);
        });
    }

    @Test
    public void shouldFindByEditionGreaterThan() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        assertThat(library.save(magazine))
                .isNotNull()
                .isEqualTo(magazine);

        await().atLeast(TimeoutConfig.DEFAULT_KV_DURABLE_TIMEOUT);

        var booksFoundByEditionGreaterThanZero = library.findByEditionGreaterThan(0).toList();

        assertSoftly(softly -> {
            softly.assertThat(booksFoundByEditionGreaterThanZero).as("query result from findByEditionGreaterThan() is a non-null instance").isNotNull();
            softly.assertThat(booksFoundByEditionGreaterThanZero.size()).as("query result size from findByEditionGreaterThan() is correct").isEqualTo(1);
            softly.assertThat(booksFoundByEditionGreaterThanZero.getFirst()).as("returned books from findByEditionGreaterThan() are correct").isEqualTo(magazine);
        });


        var booksFoundByEditionGreaterThanMaxValue = library.findByEditionGreaterThan(Integer.MAX_VALUE).toList();

        assertSoftly(softly -> {
            softly.assertThat(booksFoundByEditionGreaterThanMaxValue).as("query result from findByEditionGreaterThan(Integer.MAX_VALUE) is a non-null instance").isNotNull();
            softly.assertThat(booksFoundByEditionGreaterThanMaxValue.size()).as("query result size from findByEditionGreaterThan(Integer.MAX_VALUE) is correct").isEqualTo(0);
        });
    }

    @Test
    public void shouldDeleteById() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        assertThat(library.save(magazine))
                .isNotNull()
                .isEqualTo(magazine);

        await().atLeast(TimeoutConfig.DEFAULT_KV_DURABLE_TIMEOUT);

        library.deleteById(magazine.id());

        await().atLeast(TimeoutConfig.DEFAULT_KV_DURABLE_TIMEOUT);

        assertThat(library.findById(magazine.id()))
                .isNotNull().isEmpty();
    }

}

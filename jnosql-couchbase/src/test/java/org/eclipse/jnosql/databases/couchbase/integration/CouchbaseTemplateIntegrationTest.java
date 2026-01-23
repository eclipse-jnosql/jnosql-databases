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
import com.couchbase.client.java.json.JsonObject;
import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.couchbase.communication.CouchbaseUtil;
import org.eclipse.jnosql.databases.couchbase.communication.Database;
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

import java.util.Map;
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
@AddExtensions({ReflectionEntityMetadataExtension.class,
        DocumentExtension.class})
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class CouchbaseTemplateIntegrationTest {

    @Inject
    private CouchbaseTemplate template;

    static {
        CouchbaseUtil.systemPropertySetup();
    }

    @BeforeEach
    @AfterEach
    public void cleanUp(){
        template.deleteAll(Magazine.class);
    }

    @Test
    public void shouldInsert() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        template.insert(magazine);
        Optional<Magazine> optional = template.find(Magazine.class, magazine.id());
        assertThat(optional).isNotNull().isNotEmpty()
                .get().isEqualTo(magazine);
    }

    @Test
    public void shouldUpdate() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        assertThat(template.insert(magazine))
                .isNotNull()
                .isEqualTo(magazine);

        Magazine updated = new Magazine(magazine.id(), magazine.title() + " updated", 2);

        assertThat(template.update(updated))
                .isNotNull()
                .isNotEqualTo(magazine);

        await().atLeast(TimeoutConfig.DEFAULT_KV_DURABLE_TIMEOUT);

        assertThat(template.find(Magazine.class, magazine.id()))
                .isNotNull().get().isEqualTo(updated);

    }

    @Test
    public void shouldFindById() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        assertThat(template.insert(magazine))
                .isNotNull()
                .isEqualTo(magazine);

        assertThat(template.find(Magazine.class, magazine.id()))
                .isNotNull().get().isEqualTo(magazine);
    }

    @Test
    public void shouldFindByN1qlWithParams() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        assertThat(template.insert(magazine))
                .isNotNull()
                .isEqualTo(magazine);

        var data = template.n1qlQuery("select * from " + CouchbaseUtil.BUCKET_NAME + "._default." + Magazine.class.getSimpleName() + " where title = $title",
                JsonObject.from(Map.of("title", magazine.title()))).toList();

        assertSoftly(softly -> {
            softly.assertThat(data).as("query result is a non-null instance").isNotNull();
            softly.assertThat(data.size()).as("query result size is correct").isEqualTo(1);
            softly.assertThat(data.get(0)).as("returned data is correct").isEqualTo(magazine);
        });
    }

    @Test
    public void shouldFindByN1qlWithoutParams() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        assertThat(template.insert(magazine))
                .isNotNull()
                .isEqualTo(magazine);

        var data = template.n1qlQuery("select * from " + CouchbaseUtil.BUCKET_NAME + "._default." + Magazine.class.getSimpleName()).toList();
        assertSoftly(softly -> {
            softly.assertThat(data).as("query result is a non-null instance").isNotNull();
            softly.assertThat(data.size()).as("query result size is correct").isEqualTo(1);
            softly.assertThat(data.get(0)).as("returned data is correct").isEqualTo(magazine);
        });
    }

    @Test
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

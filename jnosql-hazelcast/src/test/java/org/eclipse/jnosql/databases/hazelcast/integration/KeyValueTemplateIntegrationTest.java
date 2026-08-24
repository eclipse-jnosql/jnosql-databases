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
package org.eclipse.jnosql.databases.hazelcast.integration;


import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.hazelcast.mapping.HazelcastTemplate;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.core.config.MappingConfigurations;
import org.eclipse.jnosql.mapping.keyvalue.KeyValueEntityConverter;
import org.eclipse.jnosql.mapping.keyvalue.KeyValueTemplate;
import org.eclipse.jnosql.mapping.keyvalue.spi.KeyValueExtension;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

@EnableAutoWeld
@AddPackages(value = {Converters.class, KeyValueEntityConverter.class})
@AddPackages(Magazine.class)
@AddPackages(Reflections.class)
@AddPackages(HazelcastTemplate.class)
@AddExtensions({ReflectionEntityMetadataExtension.class,
        KeyValueExtension.class})
class KeyValueTemplateIntegrationTest {

    @Inject
    private KeyValueTemplate template;

    static {
        System.setProperty(MappingConfigurations.KEY_VALUE_DATABASE.get(), "library");
    }

    @Test
    public void shouldPutValue() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        template.put(magazine);
        Optional<Magazine> effective = template.get(magazine.id(), Magazine.class);
        assertThat(effective)
                .isNotNull()
                .isPresent()
                .get().isEqualTo(magazine);
    }

    @Test
    public void shouldGet() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        template.put(magazine);
        Optional<Magazine> effective = template.get(magazine.id(), Magazine.class);
        assertThat(effective)
                .isNotNull()
                .isPresent()
                .get().isEqualTo(magazine);
    }

    @Test
    public void shouldDelete() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        template.put(magazine);
        Optional<Magazine> effective = template.get(magazine.id(), Magazine.class);
        assertThat(effective)
                .isNotNull()
                .isPresent()
                .get().isEqualTo(magazine);
        template.delete(Magazine.class, magazine.id());

        assertThat(template.get(magazine.id(), Magazine.class))
                .isEmpty();
    }
}

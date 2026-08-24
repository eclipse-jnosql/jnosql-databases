/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.hazelcast.mapping;

import jakarta.inject.Inject;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.keyvalue.AbstractKeyValueTemplate;
import org.eclipse.jnosql.mapping.keyvalue.spi.KeyValueExtension;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static com.hazelcast.query.Predicates.equal;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

@EnableAutoWeld
@AddPackages(value = {Converters.class, AbstractKeyValueTemplate.class, HazelcastTemplate.class})
@AddPackages(MockProducer.class)
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class,
        KeyValueExtension.class})
public class DefaultHazelcastTemplateTest {

    @Inject
    private HazelcastTemplate template;

    @Test
    public void shouldRunQuery() {
        Collection<Human> people = template.sql("active");
        assertThat(people).isNotNull();
        assertThat(people.stream().allMatch(Human.class::isInstance)).isTrue();
    }

    @Test
    public void shouldRunQuery2() {
        Collection<Human> people = template.sql("age = :age", singletonMap("age", 10));
        assertThat(people).isNotNull();
        assertThat(people.stream().allMatch(Human.class::isInstance)).isTrue();
    }

    @Test
    public void shouldRunQuery3() {
        Collection<Human> people = template.sql(equal("name",  "Poliana"));
        assertThat(people).isNotNull();
        assertThat(people.stream().allMatch(Human.class::isInstance)).isTrue();
    }

}
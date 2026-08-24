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

package org.eclipse.jnosql.databases.redis.communication;

import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class RedisMapStringTest {

    private BucketManagerFactory entityManagerFactory;

    private static final String MAMMALS = "mammals";
    private static final String FISHES = "fishes";
    private static final String AMPHIBIANS = "amphibians";
    private static final String BUCKET_NAME = "vertebrates_string";

    private Map<String, String> vertebrates;

    @BeforeEach
    public void init() {
        entityManagerFactory = KeyValueDatabase.INSTANCE.get();
        vertebrates = entityManagerFactory.getMap(BUCKET_NAME, String.class, String.class);
    }

    @Test
    public void shouldPutAndGetMap() {
        assertThat(vertebrates.put(MAMMALS, MAMMALS)).isNotNull();
        String species = vertebrates.get(MAMMALS);
        assertThat(species).isNotNull();
        assertThat(vertebrates.get(MAMMALS)).isEqualTo(MAMMALS);
        assertThat(vertebrates.size()).isEqualTo(1);
    }

    @Test
    public void shouldVerifyExist() {
        vertebrates.put(MAMMALS, MAMMALS);
        assertThat(vertebrates.containsKey(MAMMALS)).isTrue();
        assertThat(vertebrates.containsKey(FISHES)).isFalse();

        assertThat(vertebrates.containsValue(MAMMALS)).isTrue();
        assertThat(vertebrates.containsValue(FISHES)).isFalse();
    }

    @Test
    public void shouldShowKeyAndValues() {
        vertebrates.put(MAMMALS, MAMMALS);
        vertebrates.put(FISHES, FISHES);
        vertebrates.put(AMPHIBIANS, AMPHIBIANS);

        Set<String> keys = vertebrates.keySet();
        Collection<String> collectionSpecies = vertebrates.values();

        assertThat(keys.size()).isEqualTo(3);
        assertThat(collectionSpecies.size()).isEqualTo(3);
        assertThat(vertebrates.remove(MAMMALS)).isNotNull();
        assertThat(vertebrates.remove(MAMMALS)).isNull();
        assertThat(vertebrates.get(MAMMALS)).isNull();
        assertThat(vertebrates.size()).isEqualTo(2);
    }

    @Test
    public void shouldRemove() {
        vertebrates.put(MAMMALS, MAMMALS);
        vertebrates.put(FISHES, FISHES);
        vertebrates.put(AMPHIBIANS, AMPHIBIANS);

        vertebrates.remove(FISHES);
        assertThat(vertebrates.size()).isEqualTo(2);
        assertThat(vertebrates).isNotIn(FISHES);
    }

    @Test
    public void shouldClear() {
        vertebrates.put(MAMMALS, MAMMALS);
        vertebrates.put(FISHES, FISHES);

        vertebrates.clear();
        assertThat(vertebrates.isEmpty()).isTrue();
    }

    @AfterEach
    public void dispose() {
        vertebrates.clear();
    }

}

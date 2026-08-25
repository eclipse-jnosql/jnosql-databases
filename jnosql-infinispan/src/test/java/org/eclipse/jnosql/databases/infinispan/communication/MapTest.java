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
 *   The Infinispan Team
 */

package org.eclipse.jnosql.databases.infinispan.communication;

import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;
import org.eclipse.jnosql.databases.infinispan.communication.model.Species;
import org.eclipse.jnosql.databases.infinispan.communication.util.KeyValueEntityManagerFactoryUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


public class MapTest {

    private BucketManagerFactory entityManagerFactory;

    private Species mammals = new Species("lion", "cow", "dog");
    private Species fishes = new Species("redfish", "glassfish");
    private Species amphibians = new Species("crododile", "frog");

    @BeforeEach
    public void init() {
        entityManagerFactory = KeyValueEntityManagerFactoryUtils.get();
    }

    @Test
    public void shouldPutAndGetMap() {
        Map<String, Species> vertebrates = entityManagerFactory.getMap("vertebrates", String.class, Species.class);
        assertThat(vertebrates.isEmpty()).isTrue();

        vertebrates.put("mammals", mammals);
        Species species = vertebrates.get("mammals");
        assertThat(species).isNotNull();
        assertThat(mammals.getAnimals().getFirst()).isEqualTo(species.getAnimals().getFirst());
        assertThat(vertebrates.size()).isEqualTo(1);
    }

    @Test
    public void shouldVerifyExist() {

        Map<String, Species> vertebrates = entityManagerFactory.getMap("vertebrates", String.class, Species.class);
        vertebrates.put("mammals", mammals);
        assertThat(vertebrates.containsKey("mammals")).isTrue();
        assertThat(vertebrates.containsKey("redfish")).isFalse();

        assertThat(vertebrates.containsValue(mammals)).isTrue();
        assertThat(vertebrates.containsValue(fishes)).isFalse();
    }

    @Test
    public void shouldShowKeyAndValues() {
        Map<String, Species> vertebratesMap = new HashMap<>();
        vertebratesMap.put("mammals", mammals);
        vertebratesMap.put("fishes", fishes);
        vertebratesMap.put("amphibians", amphibians);
        Map<String, Species> vertebrates = entityManagerFactory.getMap("vertebrates", String.class, Species.class);
        vertebrates.putAll(vertebratesMap);

        Set<String> keys = vertebrates.keySet();
        Collection<Species> collectionSpecies = vertebrates.values();

        assertThat(keys.size()).isEqualTo(3);
        assertThat(collectionSpecies.size()).isEqualTo(3);
        assertThat(vertebrates.remove("mammals")).isNotNull();
        assertThat(vertebrates.remove("mammals")).isNull();
        assertThat(vertebrates.get("mammals")).isNull();
        assertThat(vertebrates.size()).isEqualTo(2);
    }

    @AfterEach
    public void dispose() {
        Map<String, Species> vertebrates = entityManagerFactory.getMap("vertebrates", String.class, Species.class);
        vertebrates.clear();
    }

}

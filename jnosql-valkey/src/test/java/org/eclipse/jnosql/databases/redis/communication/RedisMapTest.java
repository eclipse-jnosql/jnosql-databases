/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.jnosql.databases.redis.communication;

import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class RedisMapTest {

    private BucketManagerFactory entityManagerFactory;

    private Species mammals = new Species("lion", "cow", "dog");
    private Species fishes = new Species("redfish", "glassfish");
    private Species amphibians = new Species("crododile", "frog");

    private Map<String, Species> vertebrates;

    @BeforeEach
    public void init() {
        entityManagerFactory = KeyValueDatabase.INSTANCE.get();
        vertebrates = entityManagerFactory.getMap("vertebrates", String.class, Species.class);
    }

    @Test
    public void shouldPutAndGetMap() {
        assertNotNull(vertebrates.put("mammals", mammals));
        Species species = vertebrates.get("mammals");
        assertNotNull(species);
        assertEquals(species.getAnimals().get(0), mammals.getAnimals().get(0));
        assertEquals(1, vertebrates.size());
    }

    @Test
    public void shouldPutAll() {
        Map toPutAll = new HashMap<String, Species>();
        toPutAll.put("mammals", mammals);
        toPutAll.put("fishes", fishes);

        vertebrates.putAll(toPutAll);
        assertEquals(2, vertebrates.size());
    }

    @Test
    public void shouldVerifyExist() {
        vertebrates.put("mammals", mammals);
        assertTrue(vertebrates.containsKey("mammals"));
        assertFalse(vertebrates.containsKey("redfish"));

        assertTrue(vertebrates.containsValue(mammals));
        assertFalse(vertebrates.containsValue(fishes));
    }

    @Test
    public void shouldShowKeyAndValues() {
        vertebrates.put("mammals", mammals);
        vertebrates.put("fishes", fishes);
        vertebrates.put("amphibians", amphibians);

        Set<String> keys = vertebrates.keySet();
        Collection<Species> collectionSpecies = vertebrates.values();

        assertEquals(3, keys.size());
        assertEquals(3, collectionSpecies.size());
        assertNotNull(vertebrates.remove("mammals"));
        assertNull(vertebrates.remove("mammals"));
        assertNull(vertebrates.get("mammals"));
        assertEquals(2, vertebrates.size());
    }

    @Test
    public void shouldRemove() {
        vertebrates.put("mammals", mammals);
        vertebrates.put("fishes", fishes);
        vertebrates.put("amphibians", amphibians);

        vertebrates.remove("fishes");
        assertEquals(2, vertebrates.size());
        assertThat(vertebrates).isNotIn(fishes);
    }

    @Test
    public void shouldClear() {
        vertebrates.put("mammals", mammals);
        vertebrates.put("fishes", fishes);

        vertebrates.clear();
        assertTrue(vertebrates.isEmpty());
    }

    @AfterEach
    public void dispose() {
        vertebrates.clear();
    }

}

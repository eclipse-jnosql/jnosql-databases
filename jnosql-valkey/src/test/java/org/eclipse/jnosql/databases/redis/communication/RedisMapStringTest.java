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
        assertNotNull(vertebrates.put(MAMMALS, MAMMALS));
        String species = vertebrates.get(MAMMALS);
        assertNotNull(species);
        assertEquals(MAMMALS, vertebrates.get(MAMMALS));
        assertEquals(1, vertebrates.size());
    }

    @Test
    public void shouldVerifyExist() {
        vertebrates.put(MAMMALS, MAMMALS);
        assertTrue(vertebrates.containsKey(MAMMALS));
        assertFalse(vertebrates.containsKey(FISHES));

        assertTrue(vertebrates.containsValue(MAMMALS));
        assertFalse(vertebrates.containsValue(FISHES));
    }

    @Test
    public void shouldShowKeyAndValues() {
        vertebrates.put(MAMMALS, MAMMALS);
        vertebrates.put(FISHES, FISHES);
        vertebrates.put(AMPHIBIANS, AMPHIBIANS);

        Set<String> keys = vertebrates.keySet();
        Collection<String> collectionSpecies = vertebrates.values();

        assertEquals(3, keys.size());
        assertEquals(3, collectionSpecies.size());
        assertNotNull(vertebrates.remove(MAMMALS));
        assertNull(vertebrates.remove(MAMMALS));
        assertNull(vertebrates.get(MAMMALS));
        assertEquals(2, vertebrates.size());
    }

    @Test
    public void shouldRemove() {
        vertebrates.put(MAMMALS, MAMMALS);
        vertebrates.put(FISHES, FISHES);
        vertebrates.put(AMPHIBIANS, AMPHIBIANS);

        vertebrates.remove(FISHES);
        assertEquals(2, vertebrates.size());
        assertThat(vertebrates).isNotIn(FISHES);
    }

    @Test
    public void shouldClear() {
        vertebrates.put(MAMMALS, MAMMALS);
        vertebrates.put(FISHES, FISHES);

        vertebrates.clear();
        assertTrue(vertebrates.isEmpty());
    }

    @AfterEach
    public void dispose() {
        vertebrates.clear();
    }

}

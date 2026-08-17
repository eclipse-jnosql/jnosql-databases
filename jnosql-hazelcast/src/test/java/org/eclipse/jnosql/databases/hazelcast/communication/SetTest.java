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

package org.eclipse.jnosql.databases.hazelcast.communication;


import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;
import org.eclipse.jnosql.databases.hazelcast.communication.model.User;
import org.eclipse.jnosql.databases.hazelcast.communication.util.KeyValueEntityManagerFactoryUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


public class SetTest {


    private BucketManagerFactory keyValueEntityManagerFactory;
    private User userOtavioJava = new User("otaviojava");
    private User felipe = new User("ffrancesquini");
    private Set<User> users;

    @BeforeEach
    public void init() {
        keyValueEntityManagerFactory =  KeyValueEntityManagerFactoryUtils.get();
        users = keyValueEntityManagerFactory.getSet("social-media", User.class);
    }

    @Test
    public void shouldAddUsers() {
        assertThat(users.isEmpty()).isTrue();
        users.add(userOtavioJava);
        assertThat(users.size()).isEqualTo(1);

        users.remove(userOtavioJava);
        assertThat(users.isEmpty()).isTrue();
    }


    @SuppressWarnings("unused")
    @Test
    public void shouldIterate() {

        users.add(userOtavioJava);
        users.add(userOtavioJava);
        users.add(felipe);
        users.add(userOtavioJava);
        users.add(felipe);
        int count = 0;
        for (User user: users) {
            count++;
        }
        assertThat(count).isEqualTo(2);
        users.remove(userOtavioJava);
        users.remove(felipe);
        count = 0;
        for (User user: users) {
            count++;
        }
        assertThat(count).isEqualTo(0);
    }

    @AfterEach
    public void dispose() {
        users.clear();
    }
}

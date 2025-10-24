/*
 *   Copyright (c) 2023 Contributors to the Eclipse Foundation
 *    All rights reserved. This program and the accompanying materials
 *    are made available under the terms of the Eclipse Public License v1.0
 *    and Apache License v2.0 which accompanies this distribution.
 *    The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *    and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *    You may elect to redistribute this code under either of these licenses.
 *
 *    Contributors:
 *
 *    Otavio Santana
 */
package org.eclipse.jnosql.databases.tinkerpop.mapping.entities;

import java.util.List;

public class HumanBuilder {
    private String id;
    private String name;
    private int age;
    private List<String> phones;
    private String ignore;

    public HumanBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public HumanBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public HumanBuilder withAge() {
        this.age = 10;
        return this;
    }

    public HumanBuilder withAge(int age) {
        this.age = age;
        return this;
    }


    public HumanBuilder withPhones(List<String> phones) {
        this.phones = phones;
        return this;
    }

    public HumanBuilder withIgnore() {
        this.ignore = "Just Ignore";
        return this;
    }

    public Human build() {
        return new Human(id, name, age, phones, ignore);
    }
}

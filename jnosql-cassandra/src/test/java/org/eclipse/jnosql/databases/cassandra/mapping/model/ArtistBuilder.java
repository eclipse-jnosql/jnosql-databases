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
package org.eclipse.jnosql.databases.cassandra.mapping.model;

import java.util.List;

public class ArtistBuilder {

    private long id;

    private String name;

    private int age;

    private List<String> phones;
    
    private String ignore;

    public ArtistBuilder withId(long id) {
        this.id = id;
        return this;
    }

    public ArtistBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ArtistBuilder withAge() {
        this.age = 10;
        return this;
    }

    public ArtistBuilder withAge(int age) {
        this.age = age;
        return this;
    }


    public ArtistBuilder withPhones(List<String> phones) {
        this.phones = phones;
        return this;
    }

    public ArtistBuilder withIgnore() {
        this.ignore = "Just Ignore";
        return this;
    }

    public Artist build() {
        return new Artist(id, name, age, phones, ignore);
    }
}
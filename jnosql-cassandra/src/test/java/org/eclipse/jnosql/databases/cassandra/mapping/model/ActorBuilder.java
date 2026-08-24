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
import java.util.Map;

public class ActorBuilder {
    private long id;
    private String name;
    private int age;
    private List<String> phones;
    private String ignore;
    private Map<String, String> movieCharacter;
    private Map<String, Integer> movierRating;

    ActorBuilder() {
    }

    public ActorBuilder withId() {
        this.id = 12;
        return this;
    }

    public ActorBuilder withName() {
        this.name = "Otavio";
        return this;
    }

    public ActorBuilder withAge() {
        this.age = 10;
        return this;
    }

    public ActorBuilder withPhones(List<String> phones) {
        this.phones = phones;
        return this;
    }

    public ActorBuilder withIgnore(String ignore) {
        this.ignore = ignore;
        return this;
    }

    public ActorBuilder withMovieCharacter(Map<String, String> movieCharacter) {
        this.movieCharacter = movieCharacter;
        return this;
    }

    public ActorBuilder withMovierRating(Map<String, Integer> movierRating) {
        this.movierRating = movierRating;
        return this;
    }

    public Actor build() {
        return new Actor(id, name, age, phones, ignore, movieCharacter, movierRating);
    }
}
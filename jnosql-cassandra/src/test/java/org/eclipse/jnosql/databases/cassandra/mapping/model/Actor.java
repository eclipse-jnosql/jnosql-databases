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


import jakarta.nosql.Column;
import jakarta.nosql.Entity;

import java.util.List;
import java.util.Map;

@Entity
public class Actor extends Artist {

    @Column
    private Map<String, String> movieCharacter;

    @Column
    private Map<String, Integer> movieRating;

    Actor(long id, String name, int age, List<String> phones, String ignore, Map<String, String> movieCharacter, Map<String, Integer> movieRating) {
        super(id, name, age, phones, ignore);
        this.movieCharacter = movieCharacter;
        this.movieRating = movieRating;
    }

    Actor() {
    }

    public Map<String, String> getMovieCharacter() {
        return movieCharacter;
    }

    public Map<String, Integer> getMovieRating() {
        return movieRating;
    }

    public static ActorBuilder actorBuilder() {
        return new ActorBuilder();
    }


}

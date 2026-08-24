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

package org.eclipse.jnosql.databases.infinispan.communication.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Species implements Serializable {

    private static final long serialVersionUID = -1493508757572337719L;

    private final List<String> animals;

    public Species(String... animals) {
        this.animals = Arrays.asList(animals);
    }

    public List<String> getAnimals() {
        return animals;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Species species = (Species) o;
        return Objects.equals(animals, species.animals);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(animals);
    }

    @Override
    public String toString() {
        return "Species{" + "animals=" + animals +
                '}';
    }
}

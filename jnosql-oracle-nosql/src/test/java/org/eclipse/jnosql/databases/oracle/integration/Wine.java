/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.oracle.integration;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
public class Wine {

    @Id
    private String id;

    @Column
    private List<String> comments;

    @Column
    public List<Crew> crew;

    @Column
    public Map<String, Object> data;


    public String id() {
        return id;
    }

    public List<String> comments() {
        return comments;
    }

    public List<Crew> crew() {
        return crew;
    }

    public Map<String, Object> data() {
        return data;
    }

    Wine() {
    }

    Wine(String id, List<String> comments, List<Crew> crew, Map<String, Object> data) {
        this.id = id;
        this.comments = comments;
        this.crew = crew;
        this.data = data;
    }

    @Override
    public String toString() {
        return "Beer{" +
                "id='" + id + '\'' +
                ", comments=" + comments +
                ", crew=" + crew +
                ", data=" + data +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Wine wine = (Wine) o;
        return Objects.equals(id, wine.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public static WineBuilder builder() {
        return new WineBuilder();
    }
}

/*
 *  Copyright (c) 2025 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.mongodb.integration;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.util.Map;
import java.util.Objects;

@Entity
public class MongoDBComputer {
    @Id
    private String name;

    @Column
    private Map<String, MongoDBProgram> programs;

    private MongoDBComputer(String name, Map<String, MongoDBProgram> programs) {
        this.name = name;
        this.programs = programs;
    }

    @Deprecated
    MongoDBComputer() {
    }

    public String getName() {
        return name;
    }

    public Map<String, MongoDBProgram> getPrograms() {
        return programs;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MongoDBComputer mongoDBComputer = (MongoDBComputer) o;
        return Objects.equals(name, mongoDBComputer.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return "Computer{" +
                "name='" + name + '\'' +
                ", programs=" + programs +
                '}';
    }


    public static MongoDBComputer of(String name, Map<String, MongoDBProgram> programs) {
        return new MongoDBComputer(name, programs);
    }
}

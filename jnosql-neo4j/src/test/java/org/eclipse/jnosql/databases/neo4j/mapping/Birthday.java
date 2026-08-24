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
package org.eclipse.jnosql.databases.neo4j.mapping;


import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.util.Objects;

@Entity
public class Birthday {

    @Id
    private String name;

    @Column
    private Integer age;


    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }

    public Birthday(String name, Integer age) {
        this.name = name;
        this.age = age;
    }

    public Birthday() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Birthday birthday = (Birthday) o;
        return Objects.equals(name, birthday.name) &&
                Objects.equals(age, birthday.age);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }

    @Override
    public String toString() {
        return "Person{" + "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}

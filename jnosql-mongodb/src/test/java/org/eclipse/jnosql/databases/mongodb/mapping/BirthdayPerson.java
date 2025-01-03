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
package org.eclipse.jnosql.databases.mongodb.mapping;


import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.util.Objects;

@Entity
public class BirthdayPerson {

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

    public BirthdayPerson(String name, Integer age) {
        this.name = name;
        this.age = age;
    }

    public BirthdayPerson() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BirthdayPerson birthdayPerson = (BirthdayPerson) o;
        return Objects.equals(name, birthdayPerson.name) &&
                Objects.equals(age, birthdayPerson.age);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }

    @Override
    public String toString() {
        String sb = "Person{" + "name='" + name + '\'' +
                ", age=" + age +
                '}';
        return sb;
    }
}

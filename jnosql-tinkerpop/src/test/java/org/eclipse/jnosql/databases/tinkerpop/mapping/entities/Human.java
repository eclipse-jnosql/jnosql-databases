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


import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;
import jakarta.nosql.MappedSuperclass;

import java.util.List;
import java.util.Objects;

@Entity
@MappedSuperclass
public class Human {

    @Id
    private String id;

    @Column
    private String name;

    @Column
    private int age;

    @Column
    private List<String> phones;

    private String ignore;


    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public List<String> getPhones() {
        return phones;
    }

    public String getIgnore() {
        return ignore;
    }

    public boolean isAdult() {
        return age > 21;
    }

    Human() {
    }

    Human(String id, String name, int age, List<String> phones, String ignore) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.phones = phones;
        this.ignore = ignore;
    }

    @Override
    public String toString() {
        return  "Human{" + "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", phones=" + phones +
                ", ignore='" + ignore + '\'' +
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
        Human human = (Human) o;
        return Objects.equals(id, human.id) &&
                age == human.age &&
                Objects.equals(name, human.name) &&
                Objects.equals(phones, human.phones);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, age, phones, ignore);
    }

    public static HumanBuilder builder() {
        return new HumanBuilder();
    }

    public void setName(String name){
        this.name = name;
    }
}

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
package org.eclipse.jnosql.databases.tinkerpop.mapping.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.util.Objects;

@Entity
public class Magazine {

    @Id("~id")
    private String id;

    @Column
    private String name;

    @Column
    private Integer age;


    Magazine() {
    }

    Magazine(String id, String name, Integer age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Magazine magazine)) {
            return false;
        }
        return Objects.equals(id, magazine.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                '}';
    }

    public static MagazineBuilder builder() {
        return new MagazineBuilder();
    }

    public static class MagazineBuilder {
        private String name;
        private Integer age;
        private String id;

        private MagazineBuilder() {
        }

        public MagazineBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public MagazineBuilder withAge(Integer age) {
            this.age = age;
            return this;
        }

        public MagazineBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public Magazine build() {
            return new Magazine(id, name, age);
        }
    }
}

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

package org.eclipse.jnosql.databases.redis.communication;


import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

import java.util.Objects;

public record LineBank(String name, Integer age) {


    @JsonbCreator
    public LineBank(@JsonbProperty("name") String name, @JsonbProperty("age") Integer age) {
        this.name = name;
        this.age = age;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LineBank lineBank)) {
            return false;
        }
        return Objects.equals(name, lineBank.name) &&
                Objects.equals(age, lineBank.age);
    }

    @Override
    public String toString() {
        return "LineBank{" + "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
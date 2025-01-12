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

package org.eclipse.jnosql.databases.infinispan.communication.model;


import java.io.Serializable;
import java.util.Objects;

public class LineBank implements Serializable {


    private final Human human;

    public Human getPerson() {
        return human;
    }

    public LineBank(String name, Integer age) {
        this.human = new Human(name, age);

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LineBank lineBank = (LineBank) o;
        return Objects.equals(human, lineBank.human);
    }

    @Override
    public int hashCode() {
        return Objects.hash(human);
    }

    @Override
    public String toString() {
        return "LineBank{" + "person=" + human +
                '}';
    }
}

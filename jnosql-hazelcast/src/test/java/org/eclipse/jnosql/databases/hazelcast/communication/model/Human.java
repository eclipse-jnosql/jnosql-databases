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

package org.eclipse.jnosql.databases.hazelcast.communication.model;

import java.io.Serializable;


public record Human(String name, Integer age) implements Serializable {

    private static final long serialVersionUID = 5089852596376703955L;


    @Override
    public String toString() {
        return "Person{" + "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
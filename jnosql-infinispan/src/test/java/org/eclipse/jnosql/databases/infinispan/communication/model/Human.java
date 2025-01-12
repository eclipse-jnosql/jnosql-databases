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


public record Human(String name, Integer age) implements Serializable {

    private static final long serialVersionUID = 5089852596376703955L;


    @Override
    public String toString() {
        return "Person{" + "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}

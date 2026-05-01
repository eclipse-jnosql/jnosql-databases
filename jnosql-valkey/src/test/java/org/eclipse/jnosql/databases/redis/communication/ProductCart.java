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

import java.io.Serializable;
import java.math.BigDecimal;

public record ProductCart(String name, BigDecimal price) implements Serializable {

    private static final long serialVersionUID = 4087960613230439836L;


    @JsonbCreator
    public ProductCart(@JsonbProperty("name") String name, @JsonbProperty("price") BigDecimal price) {
        this.name = name;
        this.price = price;
    }

    @Override
    public String toString() {
        return "ProductCart{" + "name='" + name + '\'' +
                ", price=" + price +
                '}';
    }
}
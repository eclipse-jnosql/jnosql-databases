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
package org.eclipse.jnosql.databases.couchbase.communication;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public class ProductCart implements Serializable {

    private static final long serialVersionUID = 4087960613230439836L;

    private String name;

    private BigDecimal price;


    public ProductCart(String name, BigDecimal price) {
        this.name = name;
        this.price = price;
    }

    public ProductCart() {
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }


    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProductCart that = (ProductCart) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(price, that.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, price);
    }

    @Override
    public String toString() {
        return "ProductCart{" + "name='" + name + '\'' +
                ", price=" + price +
                '}';
    }
}
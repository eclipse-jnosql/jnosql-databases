/*
 *  Copyright (c) 2019 Otávio Santana and others
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

package org.eclipse.jnosql.communication.mongodb.document.type;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public class Money {

    private final Currency currency;

    private final BigDecimal value;

    private Money(Currency currency, BigDecimal value) {
        this.currency = currency;
        this.value = value;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Money money = (Money) o;
        return Objects.equals(currency, money.currency) &&
                Objects.equals(value, money.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currency, value);
    }

    @Override
    public String toString() {
        return currency.getCurrencyCode() + ' ' + value;
    }

    public static Money of(Currency currency, BigDecimal value) {
        return new Money(currency, value);
    }

    public static Money parse(String text) {
        String[] texts = text.split(" ");
        return new Money(Currency.getInstance(texts[0]),
                BigDecimal.valueOf(Double.parseDouble(texts[1])));
    }
}

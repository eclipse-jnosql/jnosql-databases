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
package org.eclipse.jnosql.databases.cassandra.communication;

import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultUDTTest {


    @Test
    public void shouldCreateUDT() {
        List<Element> columns = new ArrayList<>();
        columns.add(Element.of("firstname", "Ada"));
        columns.add(Element.of("lastname", "Lovelace"));
        UDT udt = UDT.builder("fullname").withName("name")
                .addUDT(columns).build();

        assertThat(udt.userType()).isEqualTo("fullname");
        assertThat(udt.name()).isEqualTo("name");
    }

    @Test
    public void shouldReturnGetType() {
        List<Element> columns = new ArrayList<>();
        columns.add(Element.of("firstname", "Ada"));
        columns.add(Element.of("lastname", "Lovelace"));
        UDT udt = UDT.builder("fullname").withName("name")
                .addUDT(columns).build();

        List<Element> udtColumn = udt.get(new TypeReference<>() {
        });

        assertThat(columns).contains(Element.of("firstname", "Ada"),
                Element.of("lastname", "Lovelace"));
    }

}
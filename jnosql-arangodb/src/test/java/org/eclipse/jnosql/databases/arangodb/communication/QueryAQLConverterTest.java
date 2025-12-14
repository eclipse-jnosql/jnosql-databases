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
 *   Michele Rastelli
 */
package org.eclipse.jnosql.databases.arangodb.communication;

import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class QueryAQLConverterTest {

    @Test
    public void shouldRunEqualsQuery() {
        SelectQuery query = select().from("collection")
                .where("name").eq("value").build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertEquals("value", values.get("name"));
        assertEquals("FOR c IN collection FILTER  c.name == @name RETURN c", aql);

    }

    @Test
    public void shouldRunEqualsQueryAnd() {
        SelectQuery query = select().from("collection")
                .where("name").eq("value")
                .and("age").lte(10)
                .build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertEquals("value", values.get("name"));
        assertEquals("FOR c IN collection FILTER  c.name == @name AND  c.age <= @age RETURN c", aql);

    }

    @Test
    public void shouldRunEqualsQueryOr() {
        SelectQuery query = select().from("collection")
                .where("name").eq("value")
                .or("age").lte(10)
                .build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertEquals("value", values.get("name"));
        assertEquals("FOR c IN collection FILTER  c.name == @name OR  c.age <= @age RETURN c", aql);

    }

    @Test
    public void shouldRunEqualsQuerySort() {
        SelectQuery query = select().from("collection")
                .where("name").eq("value")
                .orderBy("name").asc().build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertEquals("value", values.get("name"));
        assertEquals("FOR c IN collection FILTER  c.name == @name SORT  c.name ASC RETURN c", aql);
    }

    @Test
    public void shouldRunEqualsQuerySort2() {
        SelectQuery query = select().from("collection")
                .where("name").eq("value")
                .orderBy("name").asc()
                .orderBy("age").desc().build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertEquals("value", values.get("name"));
        assertEquals("FOR c IN collection FILTER  c.name == @name SORT  c.name ASC , c.age DESC RETURN c", aql);
    }


    @Test
    public void shouldRunEqualsQueryLimit() {
        SelectQuery query = select().from("collection")
                .where("name").eq("value")
                .limit(5).build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertEquals("value", values.get("name"));
        assertEquals("FOR c IN collection FILTER  c.name == @name LIMIT 5 RETURN c", aql);

    }

    @Test
    public void shouldRunEqualsQueryLimit2() {
        SelectQuery query = select().from("collection")
                .where("name").eq("value")
                .skip(1).limit(5).build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertEquals("value", values.get("name"));
        assertEquals("FOR c IN collection FILTER  c.name == @name LIMIT 1, 5 RETURN c", aql);
    }

    @Test
    public void shouldRunEqualsQueryNot() {
        SelectQuery query = select().from("collection")
                .where("name").not().eq("value").build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertEquals("value", values.get("name"));
        assertEquals("FOR c IN collection FILTER  NOT ( c.name == @name) RETURN c", aql);

    }


    @Test
    public void shouldNegate() {
        SelectQuery query = select().from("collection")
                .where("city").not().eq("Assis")
                .and("name").eq("Otavio")
                .or("name").not().eq("Lucas").build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertEquals(3, values.size());
        assertEquals("Assis", values.get("city"));
        assertEquals("Otavio", values.get("name"));
        assertEquals("Lucas", values.get("name_1"));
        assertEquals("FOR c IN collection FILTER  NOT ( c.city == @city) AND  c.name == @name OR  NOT ( c.name == @name_1) RETURN c", aql);

    }

    @Test
    public void shouldRunEqualsCountQuery() {
        SelectQuery query = select().from("collection")
                .where("name").eq("value").build();

        AQLQueryResult convert = QueryAQLConverter.count(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertEquals("value", values.get("name"));
        assertEquals("RETURN LENGTH(FOR c IN collection FILTER  c.name == @name RETURN c)", aql);

    }


}

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
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.arangodb.communication;

import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.communication.semistructured.UpdateQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

public class QueryAQLConverterTest {

    @Test
    public void shouldRunEqualsQuery() {
        SelectQuery query = select().from("collection")
                .where("name").eq("value").build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertThat(values.get("name")).isEqualTo("value");
        assertThat(aql).isEqualTo("FOR c IN collection FILTER  c.name == @name RETURN c");

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
        assertThat(values.get("name")).isEqualTo("value");
        assertThat(aql).isEqualTo("FOR c IN collection FILTER  c.name == @name AND  c.age <= @age RETURN c");

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
        assertThat(values.get("name")).isEqualTo("value");
        assertThat(aql).isEqualTo("FOR c IN collection FILTER  c.name == @name OR  c.age <= @age RETURN c");

    }

    @Test
    public void shouldRunEqualsQuerySort() {
        SelectQuery query = select().from("collection")
                .where("name").eq("value")
                .orderBy("name").asc().build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertThat(values.get("name")).isEqualTo("value");
        assertThat(aql).isEqualTo("FOR c IN collection FILTER  c.name == @name SORT  c.name ASC RETURN c");
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
        assertThat(values.get("name")).isEqualTo("value");
        assertThat(aql).isEqualTo("FOR c IN collection FILTER  c.name == @name SORT  c.name ASC , c.age DESC RETURN c");
    }


    @Test
    public void shouldRunEqualsQueryLimit() {
        SelectQuery query = select().from("collection")
                .where("name").eq("value")
                .limit(5).build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertThat(values.get("name")).isEqualTo("value");
        assertThat(aql).isEqualTo("FOR c IN collection FILTER  c.name == @name LIMIT 5 RETURN c");

    }

    @Test
    public void shouldRunEqualsQueryLimit2() {
        SelectQuery query = select().from("collection")
                .where("name").eq("value")
                .skip(1).limit(5).build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertThat(values.get("name")).isEqualTo("value");
        assertThat(aql).isEqualTo("FOR c IN collection FILTER  c.name == @name LIMIT 1, 5 RETURN c");
    }

    @Test
    public void shouldRunEqualsQueryNot() {
        SelectQuery query = select().from("collection")
                .where("name").not().eq("value").build();

        AQLQueryResult convert = QueryAQLConverter.select(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertThat(values.get("name")).isEqualTo("value");
        assertThat(aql).isEqualTo("FOR c IN collection FILTER  NOT ( c.name == @name) RETURN c");

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
        assertThat(values.size()).isEqualTo(3);
        assertThat(values.get("city")).isEqualTo("Assis");
        assertThat(values.get("name")).isEqualTo("Otavio");
        assertThat(values.get("name_1")).isEqualTo("Lucas");
        assertThat(aql).isEqualTo("FOR c IN collection FILTER  NOT ( c.city == @city) AND  c.name == @name OR  NOT ( c.name == @name_1) RETURN c");

    }

    @Test
    public void shouldRunEqualsCountQuery() {
        SelectQuery query = select().from("collection")
                .where("name").eq("value").build();

        AQLQueryResult convert = QueryAQLConverter.count(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();
        assertThat(values.get("name")).isEqualTo("value");
        assertThat(aql).isEqualTo("RETURN LENGTH(FOR c IN collection FILTER  c.name == @name RETURN c)");

    }

    @Test
    public void shouldRunUpdateQuery() {

        UpdateQuery query = mock(UpdateQuery.class);
        when(query.name()).thenReturn("collection");
        when(query.sets()).then( i->
                List.of(
                        Element.of("name", "John"),
                        Element.of("surname", "Doe")
                )
        );
        when(query.where()).thenReturn(Optional.of(
                        CriteriaCondition.eq(Element.of("id", 1))));


        AQLQueryResult convert = QueryAQLConverter.update(query);
        String aql = convert.query();
        Map<String, Object> values = convert.values();

        assertSoftly(softly->{
            softly.assertThat(values.get("id")).isEqualTo(1);
            softly.assertThat(values.get("name")).isEqualTo("John");
            softly.assertThat(values.get("surname")).isEqualTo("Doe");
            softly.assertThat(aql.replace("  "," "))
                    .isEqualTo(
                            "FOR c IN collection FILTER c.id == @id " +
                                    "UPDATE c WITH { name: @name, surname: @surname } IN collection " +
                                    "RETURN NEW");
        });

    }


}

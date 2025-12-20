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
 *   Lucas Furlaneto
 */
package org.eclipse.jnosql.databases.orientdb.communication;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;

public class QueryOSQLConverterTest {

    @Test
    public void shouldRunEqualsQuery() {
        var query = select().from("collection")
                .where("name").eq("value").build();

        QueryOSQLConverter.Query selectQuery = QueryOSQLConverter.select(query);
        QueryOSQLConverter.Query selectCountQuery = QueryOSQLConverter.selectCount(query);

        assertSoftly(softly -> {

            softly.assertThat(selectQuery)
                    .as("Converted OrientDB SQL query is different than expected")
                    .extracting(QueryOSQLConverter.Query::query, QueryOSQLConverter.Query::params)
                    .containsExactly("SELECT FROM collection WHERE name = ?", List.of("value"));

            softly.assertThat(selectCountQuery)
                    .as("Converted OrientDB SQL count query is different than expected")
                    .extracting(QueryOSQLConverter.Query::query, QueryOSQLConverter.Query::params)
                    .containsExactly("SELECT COUNT(*) FROM collection WHERE name = ?", List.of("value"));

        });
    }

    @Test
    public void shouldRunEqualsQueryAnd() {
        var query = select().from("collection")
                .where("name").eq("value")
                .and("age").lte(10)
                .build();

        QueryOSQLConverter.Query selectQuery = QueryOSQLConverter.select(query);
        QueryOSQLConverter.Query selectCountQuery = QueryOSQLConverter.selectCount(query);

        assertSoftly(softly -> {

            softly.assertThat(selectQuery)
                    .as("Converted OrientDB SQL query is different than expected")
                    .extracting(QueryOSQLConverter.Query::query, QueryOSQLConverter.Query::params)
                    .containsExactly("SELECT FROM collection WHERE name = ? AND age <= ?", List.of("value", 10));

            softly.assertThat(selectCountQuery)
                    .as("Converted OrientDB SQL count query is different than expected")
                    .extracting(QueryOSQLConverter.Query::query, QueryOSQLConverter.Query::params)
                    .containsExactly("SELECT COUNT(*) FROM collection WHERE name = ? AND age <= ?", List.of("value", 10));

        });
    }

    @Test
    public void shouldRunEqualsQueryOr() {
        var query = select().from("collection")
                .where("name").eq("value")
                .or("age").lte(10)
                .build();

        QueryOSQLConverter.Query selectQuery = QueryOSQLConverter.select(query);
        QueryOSQLConverter.Query selectCountQuery = QueryOSQLConverter.selectCount(query);

        assertSoftly(softly -> {

            softly.assertThat(selectQuery)
                    .as("Converted OrientDB SQL query is different than expected")
                    .extracting(QueryOSQLConverter.Query::query, QueryOSQLConverter.Query::params)
                    .containsExactly("SELECT FROM collection WHERE name = ? OR age <= ?", List.of("value", 10));

            softly.assertThat(selectCountQuery)
                    .as("Converted OrientDB SQL count query is different than expected")
                    .extracting(QueryOSQLConverter.Query::query, QueryOSQLConverter.Query::params)
                    .containsExactly("SELECT COUNT(*) FROM collection WHERE name = ? OR age <= ?", List.of("value", 10));

        });
    }

    @Test
    public void shouldRunEqualsQueryNot() {
        var query = select().from("collection")
                .where("name").not().eq("value").build();

        QueryOSQLConverter.Query selectQuery = QueryOSQLConverter.select(query);
        QueryOSQLConverter.Query selectCountQuery = QueryOSQLConverter.selectCount(query);

        assertSoftly(softly -> {

            softly.assertThat(selectQuery)
                    .as("Converted OrientDB SQL query is different than expected")
                    .extracting(QueryOSQLConverter.Query::query, QueryOSQLConverter.Query::params)
                    .containsExactly("SELECT FROM collection WHERE NOT (name = ?)", List.of("value"));

            softly.assertThat(selectCountQuery)
                    .as("Converted OrientDB SQL count query is different than expected")
                    .extracting(QueryOSQLConverter.Query::query, QueryOSQLConverter.Query::params)
                    .containsExactly("SELECT COUNT(*) FROM collection WHERE NOT (name = ?)", List.of("value"));

        });

    }

    @Test
    public void shouldNegate() {
        var query = select().from("collection")
                .where("city").not().eq("Assis")
                .and("name").eq("Otavio")
                .or("name").not().eq("Lucas").build();

        QueryOSQLConverter.Query selectQuery = QueryOSQLConverter.select(query);
        QueryOSQLConverter.Query selectCountQuery = QueryOSQLConverter.selectCount(query);

        assertSoftly(softly -> {

            softly.assertThat(selectQuery)
                    .as("Converted OrientDB SQL query is different than expected")
                    .extracting(QueryOSQLConverter.Query::query, QueryOSQLConverter.Query::params)
                    .containsExactly("SELECT FROM collection WHERE NOT (city = ?) AND name = ? OR NOT (name = ?)",
                            List.of("Assis", "Otavio", "Lucas"));

            softly.assertThat(selectCountQuery)
                    .as("Converted OrientDB SQL count query is different than expected")
                    .extracting(QueryOSQLConverter.Query::query, QueryOSQLConverter.Query::params)
                    .containsExactly("SELECT COUNT(*) FROM collection WHERE NOT (city = ?) AND name = ? OR NOT (name = ?)",
                            List.of("Assis", "Otavio", "Lucas"));

        });
    }

    @Test
    public void shouldPaginateWithStart() {
        var query = select().from("collection")
                .skip(10)
                .build();

        QueryOSQLConverter.Query selectQuery = QueryOSQLConverter.select(query);
        QueryOSQLConverter.Query selectCountQuery = QueryOSQLConverter.selectCount(query);

        assertSoftly(softly -> {

            softly.assertThat(selectQuery)
                    .as("Converted OrientDB SQL query is different than expected")
                    .extracting(QueryOSQLConverter.Query::query)
                    .isEqualTo("SELECT FROM collection SKIP 10");

            softly.assertThat(selectCountQuery)
                    .as("Converted OrientDB SQL count query is different than expected")
                    .extracting(QueryOSQLConverter.Query::query)
                    .isEqualTo("SELECT COUNT(*) FROM collection");

        });
    }

    @Test
    public void shouldPaginateWithLimit() {
        var query = select().from("collection")
                .limit(100)
                .build();

        QueryOSQLConverter.Query selectQuery = QueryOSQLConverter.select(query);
        QueryOSQLConverter.Query selectCountQuery = QueryOSQLConverter.selectCount(query);

        assertSoftly(softly -> {

            softly.assertThat(selectQuery)
                    .as("Converted OrientDB SQL query is different than expected")
                    .extracting(QueryOSQLConverter.Query::query)
                    .isEqualTo("SELECT FROM collection LIMIT 100");

            softly.assertThat(selectCountQuery)
                    .as("Converted OrientDB SQL count query is different than expected")
                    .extracting(QueryOSQLConverter.Query::query)
                    .isEqualTo("SELECT COUNT(*) FROM collection");

        });
    }

    @Test
    public void shouldPaginateWithStartAndLimit() {
        var query = select().from("collection")
                .skip(10)
                .limit(100)
                .build();

        QueryOSQLConverter.Query selectQuery = QueryOSQLConverter.select(query);
        QueryOSQLConverter.Query selectCountQuery = QueryOSQLConverter.selectCount(query);

        assertSoftly(softly -> {

            softly.assertThat(selectQuery)
                    .as("Converted OrientDB SQL query is different than expected")
                    .extracting(QueryOSQLConverter.Query::query)
                    .isEqualTo("SELECT FROM collection SKIP 10 LIMIT 100");

            softly.assertThat(selectCountQuery)
                    .as("Converted OrientDB SQL count query is different than expected")
                    .extracting(QueryOSQLConverter.Query::query)
                    .isEqualTo("SELECT COUNT(*) FROM collection");

        });
    }

    @Test
    public void shouldSortAsc() {
        var query = select().from("collection")
                .orderBy("name").asc()
                .build();

        QueryOSQLConverter.Query selectQuery = QueryOSQLConverter.select(query);
        QueryOSQLConverter.Query selectCountQuery = QueryOSQLConverter.selectCount(query);

        assertSoftly(softly -> {

            softly.assertThat(selectQuery)
                    .as("Converted OrientDB SQL query is different than expected")
                    .extracting(QueryOSQLConverter.Query::query)
                    .isEqualTo("SELECT FROM collection ORDER BY name ASC");

            softly.assertThat(selectCountQuery)
                    .as("Converted OrientDB SQL count query is different than expected")
                    .extracting(QueryOSQLConverter.Query::query)
                    .isEqualTo("SELECT COUNT(*) FROM collection");

        });
    }

    @Test
    public void shouldSortDesc() {
        var query = select().from("collection")
                .orderBy("name").desc()
                .build();

        QueryOSQLConverter.Query selectQuery = QueryOSQLConverter.select(query);
        QueryOSQLConverter.Query selectCountQuery = QueryOSQLConverter.selectCount(query);

        assertSoftly(softly -> {

            softly.assertThat(selectQuery)
                    .as("Converted OrientDB SQL query is different than expected")
                    .extracting(QueryOSQLConverter.Query::query)
                    .isEqualTo("SELECT FROM collection ORDER BY name DESC");

            softly.assertThat(selectCountQuery)
                    .as("Converted OrientDB SQL count query is different than expected")
                    .extracting(QueryOSQLConverter.Query::query)
                    .isEqualTo("SELECT COUNT(*) FROM collection");

        });

    }

    @Test
    public void shouldMultipleSort() {
        var query = select().from("collection")
                .orderBy("name").asc()
                .orderBy("age").desc()
                .build();

        QueryOSQLConverter.Query selectQuery = QueryOSQLConverter.select(query);
        QueryOSQLConverter.Query selectCountQuery = QueryOSQLConverter.selectCount(query);

        assertSoftly(softly -> {

            softly.assertThat(selectQuery)
                    .as("Converted OrientDB SQL query is different than expected")
                    .extracting(QueryOSQLConverter.Query::query)
                    .isEqualTo("SELECT FROM collection ORDER BY name ASC, age DESC");

            softly.assertThat(selectCountQuery)
                    .as("Converted OrientDB SQL count query is different than expected")
                    .extracting(QueryOSQLConverter.Query::query)
                    .isEqualTo("SELECT COUNT(*) FROM collection");

        });

    }
}

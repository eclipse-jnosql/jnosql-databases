/*
 *
 *  Copyright (c) 2025 Contributors to the Eclipse Foundation
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
 *
 */
package org.eclipse.jnosql.databases.neo4j.communication;

import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Neo4JQueryBuilderTest {

    @Test
    void shouldBuildDeleteQueryForNode() {
        DeleteQuery query = mock(DeleteQuery.class);
        when(query.name()).thenReturn("Person");
        when(query.condition()).thenReturn(java.util.Optional.empty());
        when(query.columns()).thenReturn(List.of());

        Map<String, Object> parameters = new HashMap<>();
        String cypher = Neo4JQueryBuilder.INSTANCE.buildQuery(query, parameters);

        assertThat(cypher).isEqualTo("MATCH (e:Person) DELETE e");
    }

    @Test
    void shouldBuildDeleteQueryForSpecificColumns() {
        DeleteQuery query = mock(DeleteQuery.class);
        when(query.name()).thenReturn("Person");
        when(query.condition()).thenReturn(java.util.Optional.empty());
        when(query.columns()).thenReturn(List.of("name", "age"));

        Map<String, Object> parameters = new HashMap<>();
        String cypher = Neo4JQueryBuilder.INSTANCE.buildQuery(query, parameters);

        assertThat(cypher).isEqualTo("MATCH (e:Person) SET e.name = NULL, e.age = NULL");
    }

    @Test
    void shouldBuildSelectQueryWithCondition() {
        SelectQuery query = mock(SelectQuery.class);
        when(query.name()).thenReturn("Person");

        CriteriaCondition condition = mock(CriteriaCondition.class);
        Element element = mock(Element.class);
        when(condition.element()).thenReturn(element);
        when(element.name()).thenReturn("age");
        when(element.get()).thenReturn(30);
        when(condition.condition()).thenReturn(org.eclipse.jnosql.communication.Condition.EQUALS);
        when(query.condition()).thenReturn(java.util.Optional.of(condition));
        when(query.columns()).thenReturn(List.of("name", "age"));

        Map<String, Object> parameters = new HashMap<>();
        String cypher = Neo4JQueryBuilder.INSTANCE.buildQuery(query, parameters);

        assertThat(cypher).isEqualTo("MATCH (e:Person) WHERE e.age = $age RETURN e.name, e.age");
        assertThat(parameters).containsEntry("age", 30);
    }

    @Test
    void shouldBuildSelectQueryWithoutCondition() {
        SelectQuery query = mock(SelectQuery.class);
        when(query.name()).thenReturn("Person");
        when(query.condition()).thenReturn(java.util.Optional.empty());
        when(query.columns()).thenReturn(List.of("name", "age"));

        Map<String, Object> parameters = new HashMap<>();
        String cypher = Neo4JQueryBuilder.INSTANCE.buildQuery(query, parameters);

        assertThat(cypher).isEqualTo("MATCH (e:Person) RETURN e.name, e.age");
    }

    @Test
    void shouldBuildCountQueryWithCondition() {
        SelectQuery query = mock(SelectQuery.class);
        when(query.name()).thenReturn("Person");

        CriteriaCondition condition = mock(CriteriaCondition.class);
        Element element = mock(Element.class);
        when(condition.element()).thenReturn(element);
        when(element.name()).thenReturn("age");
        when(element.get()).thenReturn(30);
        when(condition.condition()).thenReturn(org.eclipse.jnosql.communication.Condition.EQUALS);
        when(query.condition()).thenReturn(java.util.Optional.of(condition));
        when(query.columns()).thenReturn(List.of());

        Map<String, Object> parameters = new HashMap<>();
        String cypher = Neo4JQueryBuilder.INSTANCE.buildQuery(query, parameters);

        assertThat(cypher).isEqualTo("MATCH (e:Person) WHERE e.age = $age RETURN COUNT(e)");
        assertThat(parameters).containsEntry("age", 30);
    }

    @Test
    void shouldBuildCountQueryWithoutCondition() {
        SelectQuery query = mock(SelectQuery.class);
        when(query.name()).thenReturn("Person");
        when(query.condition()).thenReturn(java.util.Optional.empty());
        when(query.columns()).thenReturn(List.of());

        Map<String, Object> parameters = new HashMap<>();
        String cypher = Neo4JQueryBuilder.INSTANCE.buildQuery(query, parameters);

        assertThat(cypher).isEqualTo("MATCH (e:Person) RETURN COUNT(e)");
    }

    @Test
    void shouldTranslateIdToElementId() {
        SelectQuery query = mock(SelectQuery.class);
        when(query.name()).thenReturn("Person");

        CriteriaCondition condition = mock(CriteriaCondition.class);
        Element element = mock(Element.class);
        when(condition.element()).thenReturn(element);
        when(element.name()).thenReturn("_id");
        when(element.get()).thenReturn("12345");
        when(condition.condition()).thenReturn(org.eclipse.jnosql.communication.Condition.EQUALS);
        when(query.condition()).thenReturn(java.util.Optional.of(condition));
        when(query.columns()).thenReturn(List.of("name", "_id"));

        Map<String, Object> parameters = new HashMap<>();
        String cypher = Neo4JQueryBuilder.INSTANCE.buildQuery(query, parameters);

        assertThat(cypher).isEqualTo("MATCH (e:Person) WHERE elementId(e) = $id RETURN e.name, elementId(e)");
        assertThat(parameters).containsEntry("id", "12345");
    }

}
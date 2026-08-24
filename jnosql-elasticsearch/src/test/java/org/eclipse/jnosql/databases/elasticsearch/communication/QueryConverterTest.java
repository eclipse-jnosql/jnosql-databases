/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 * and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *
 * Maximillian Arruda
 *
 */

package org.eclipse.jnosql.databases.elasticsearch.communication;

import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import co.elastic.clients.util.ObjectBuilder;
import jakarta.nosql.Column;
import jakarta.nosql.Id;
import org.awaitility.Awaitility;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.mapping.core.config.MappingConfigurations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class QueryConverterTest {

    private static final String INDEX = QueryConverterTest.class.getSimpleName().toLowerCase();

    static {
        System.setProperty(ElasticsearchConfigurations.HOST.get() + ".1", DocumentDatabase.INSTANCE.host());
        System.setProperty(MappingConfigurations.DOCUMENT_DATABASE.get(), INDEX);
        Awaitility.setDefaultPollDelay(100, MILLISECONDS);
        Awaitility.setDefaultTimeout(2L, SECONDS);
    }

    private DocumentDatabase.ElasticsearchClientAutoClosable elasticsearch;

    @BeforeEach
    void setUp() {
        this.elasticsearch = DocumentDatabase.INSTANCE.newElasticsearchClient();
        clearDatabase();
    }

    @AfterEach
    void clearDatabase() {
        DocumentDatabase.clearDatabase(INDEX);
    }


    public static record Book(
            @Id
            String id,
            @Column
            String name,
            @Column
            Integer edition) {
    }

    final Book effectiveJava = new Book(
            "6eaeafcd-3e6a-4ef6-9f5f-a1b33677850d",
            "Effective Java",
            1);

    @Test
    void testSupportTermQuery() throws Exception {

        Map<String, Object> map = new HashMap<>();
        map.put("name", effectiveJava.name());
        map.put("edition", effectiveJava.edition);
        map.put("doc2", Map.of("data1", "teste", "data2", 333L));
        map.put("@entity", Book.class.getSimpleName());

        insertData(map);

        IndexMappingRecord indexMappingRecordWithoutKeywordAttributes = elasticsearch.client().indices().getMapping(b -> b.index(INDEX)).get(INDEX);

        assertSoftly(softly -> {

            softly.assertThat(indexMappingRecordWithoutKeywordAttributes)
                    .as("indexMappingRecordWithoutKeywordAttributes wasn't provided")
                    .isNotNull();

            map.keySet().forEach(key -> softly.assertThat(QueryConverter.supportTermQuery(indexMappingRecordWithoutKeywordAttributes, key))
                    .as("%s attribute should not support TermQuery".formatted(key))
                    .isFalse());

            Set.of("doc2.data1", "doc2.data2").forEach(key -> softly.assertThat(QueryConverter.supportTermQuery(indexMappingRecordWithoutKeywordAttributes, key))
                    .as("%s attribute should not support TermQuery".formatted(key))
                    .isFalse());

        });

        clearDatabase();
        recreateIndexWithKeywordFields(m ->
                m.index(INDEX)
                        .properties("@entity", f -> f.keyword(KeywordProperty.of(k -> k)))
                        .properties("doc2", f -> f.object(o -> o.properties("data1", p -> p.keyword(KeywordProperty.of(k -> k)))))
        );

        insertData(map);

        IndexMappingRecord indexMappingRecordWithKeywordAttributes = elasticsearch.client().indices().getMapping(b -> b.index(INDEX)).get(INDEX);

        assertSoftly(softly -> {

            softly.assertThat(indexMappingRecordWithKeywordAttributes)
                    .as("indexMappingRecordWithKeywordAttributes wasn't provided")
                    .isNotNull();

            map.keySet().stream()
                    .filter(anObject -> !"@entity".equals(anObject))
                    .forEach(key -> softly.assertThat(QueryConverter.supportTermQuery(indexMappingRecordWithKeywordAttributes, key))
                            .as("%s attribute should not support TermQuery".formatted(key))
                            .isFalse());


            softly.assertThat(QueryConverter.supportTermQuery(indexMappingRecordWithKeywordAttributes, "@entity"))
                    .as("%s attribute should support TermQuery".formatted("@entity"))
                    .isTrue();


            softly.assertThat(QueryConverter.supportTermQuery(indexMappingRecordWithKeywordAttributes, "doc2.data1"))
                    .as("%s attribute should not support TermQuery".formatted("doc2.data1"))
                    .isTrue();

            softly.assertThat(QueryConverter.supportTermQuery(indexMappingRecordWithKeywordAttributes, "doc2.data2"))
                    .as("%s attribute should not support TermQuery".formatted("doc2.data2"))
                    .isFalse();

        });

    }

    @Test
    void testSelect() {

        Map<String, Object> map = new HashMap<>();
        map.put("_id", effectiveJava.id());
        map.put("name", effectiveJava.name());
        map.put("edition", effectiveJava.edition());
        map.put("doc2", Map.of("data1", "teste", "data2", 333L));
        map.put("@entity", Book.class.getSimpleName());

        insertData(map);

        SelectQuery query = SelectQuery.builder()
                .select()
                .from("Book")
                .where(CriteriaCondition.eq("_id", effectiveJava.id())).build();

        QueryConverterResult selectWithoutKeywordFields = QueryConverter.select(elasticsearch.client(), INDEX, query);

        assertSoftly(softly -> {
            softly.assertThat(selectWithoutKeywordFields)
                    .as("QueryConverter.select() should returns a non-null object")
                    .isNotNull();

            Query esQuery = selectWithoutKeywordFields.statement().build();
            softly.assertThat(esQuery._kind())
                    .as("QueryConverterResult's statement should be a %s query".formatted(Query.Kind.Bool))
                    .isEqualTo(Query.Kind.Bool);

            softly.assertThat(esQuery.bool().must())
                    .as("QueryConverterResult's statement should be a %s query with 2 conditions".formatted(Query.Kind.Bool))
                    .hasSize(2);

            softly.assertThat(esQuery.bool().must().get(0)._kind())
                    .as("first condition of QueryConverterResult's statement should be a %s query".formatted(Query.Kind.Match))
                    .isEqualTo(Query.Kind.Match);

            softly.assertThat(esQuery.bool().must().get(1)._kind())
                    .as("second condition of QueryConverterResult's statement should be a %s query".formatted(Query.Kind.Match))
                    .isEqualTo(Query.Kind.Match);

        });

        System.out.println(selectWithoutKeywordFields);

        clearDatabase();
        recreateIndexWithKeywordFields(m ->
                m.index(INDEX)
                        .properties("@entity", f -> f.keyword(KeywordProperty.of(k -> k)))
                        .properties("name", f -> f.keyword(KeywordProperty.of(k -> k)))
                        .properties("edition", f -> f.keyword(KeywordProperty.of(k -> k)))
                        .properties("doc2", f -> f.object(o -> o.properties("data1", p -> p.keyword(KeywordProperty.of(k -> k)))))
        );

        insertData(map);

        var query2 = SelectQuery.builder()
                .select()
                .from("Book")
                .where(CriteriaCondition.eq("_id", effectiveJava.id())
                        .and(CriteriaCondition.eq("doc2.data1", "teste")
                                .and(CriteriaCondition.eq("doc2.data2", 222L)))).build();

        QueryConverterResult selectWithKeywordFields = QueryConverter.select(elasticsearch.client(), INDEX, query2);

        assertSoftly(softly -> {
            softly.assertThat(selectWithKeywordFields)
                    .as("QueryConverter.select() should returns a non-null object")
                    .isNotNull();

            Query builtQuery = selectWithKeywordFields.statement().build();

            softly.assertThat(builtQuery._kind())
                    .as("QueryConverterResult's statement should be a %s query".formatted(Query.Kind.Bool))
                    .isEqualTo(Query.Kind.Bool);

            softly.assertThat(builtQuery.bool().must())
                    .as("QueryConverterResult's statement should be a %s query".formatted(Query.Kind.Bool))
                    .hasSize(2);

            Query firstCondition = builtQuery.bool().must().get(0);
            softly.assertThat(firstCondition._kind())
                    .as("QueryConverterResult's statement's first condition should be a %s query".formatted(Query.Kind.Term))
                    .isEqualTo(Query.Kind.Term);

            Query secondCondition = builtQuery.bool().must().get(1);
            softly.assertThat(secondCondition._kind())
                    .as("QueryConverterResult's statement's second condition should be a %s query".formatted(Query.Kind.Bool))
                    .isEqualTo(Query.Kind.Bool);

            softly.assertThat(secondCondition.bool().must())
                    .as("QueryConverterResult's statement should be a %s query".formatted(Query.Kind.Term))
                    .hasSize(2);

            Query firstSubConditionOfSecondCondition = secondCondition.bool().must().get(0);
            softly.assertThat(firstSubConditionOfSecondCondition._kind())
                    .as("first sub-condition of QueryConverterResult's statement's second condition should be a %s query".formatted(Query.Kind.Match))
                    .isEqualTo(Query.Kind.Match);

            Query secondSubConditionOfSecondCondition = secondCondition.bool().must().get(1);
            softly.assertThat(secondSubConditionOfSecondCondition._kind())
                    .as("second sub-condition of QueryConverterResult's statement's second condition should be a %s query".formatted(Query.Kind.Bool))
                    .isEqualTo(Query.Kind.Bool);

            softly.assertThat(secondSubConditionOfSecondCondition.bool().must())
                    .as("second sub-condition of QueryConverterResult's statement's second condition should has wrong size")
                    .hasSize(2);


            Query firstSubSubConditionOfSecondCondition = secondSubConditionOfSecondCondition.bool().must().get(0);
            softly.assertThat(firstSubSubConditionOfSecondCondition._kind())
                    .as("first sub-condition of the second sub-condition of QueryConverterResult's statement's second condition should be a %s query".formatted(Query.Kind.Term))
                    .isEqualTo(Query.Kind.Term);

            Query secondSubSubConditionOfSecondCondition = secondSubConditionOfSecondCondition.bool().must().get(1);
            softly.assertThat(secondSubSubConditionOfSecondCondition._kind())
                    .as("second sub-condition of the second sub-condition of QueryConverterResult's statement's second condition should be a %s query".formatted(Query.Kind.Match))
                    .isEqualTo(Query.Kind.Match);

        });

    }

    private void recreateIndexWithKeywordFields(Function<PutMappingRequest.Builder, ObjectBuilder<PutMappingRequest>> fn) {
        DocumentDatabase.createDatabase(INDEX);
        DocumentDatabase.updateMapping(INDEX, fn);
    }

    private void insertData(Map<String, Object> map) {
        DocumentDatabase.insertData(INDEX, map);
    }
}
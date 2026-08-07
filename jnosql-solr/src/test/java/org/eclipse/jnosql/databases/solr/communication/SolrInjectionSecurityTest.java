/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.jnosql.databases.solr.communication;

import org.apache.solr.client.solrj.util.ClientUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.semistructured.DeleteQuery.delete;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;

@DisplayName("Solr injection security")
class SolrInjectionSecurityTest {

    @Nested
    @DisplayName("When converting generated Solr queries")
    class WhenConvertingGeneratedSolrQueries {

        @Test
        @DisplayName("Should escape select condition values that contain Lucene operators")
        void shouldEscapeSelectQueryConditionValue() {
            String payload = "[* TO *] OR _entity:Secret";
            var query = select().from("Public").where("name").eq(payload).build();

            String solr = DocumentQueryConverter.convert(query);

            assertThat(solr).isEqualTo("_entity:Public AND name:" + ClientUtils.escapeQueryChars(payload));
            assertThat(solr).doesNotContain("name:" + payload);
        }

        @Test
        @DisplayName("Should escape delete condition values that could broaden deleteByQuery")
        void shouldEscapeDeleteQueryConditionValue() {
            String payload = "nope OR *:*";
            var query = delete().from("Comment").where("_id").eq(payload).build();

            String solr = DocumentQueryConverter.convert(query);

            assertThat(solr).isEqualTo("_entity:Comment AND _id:" + ClientUtils.escapeQueryChars(payload));
            assertThat(solr).doesNotContain("_id:" + payload);
        }

        @Test
        @DisplayName("Should escape every value in an IN condition")
        void shouldEscapeInConditionValues() {
            var query = select().from("Person")
                    .where("name").in(asList("Ada", "[* TO *]"))
                    .build();

            String solr = DocumentQueryConverter.convert(query);

            assertThat(solr).isEqualTo("_entity:Person AND name:(Ada OR "
                    + ClientUtils.escapeQueryChars("[* TO *]") + ')');
        }

        @Test
        @DisplayName("Should preserve LIKE wildcards while escaping injected operators")
        void shouldPreserveLikeWildcardsAndEscapeOperators() {
            String payload = "Lu* OR _entity:Secret";
            var query = select().from("Public").where("name").like(payload).build();

            String solr = DocumentQueryConverter.convert(query);

            assertThat(solr).isEqualTo("_entity:Public AND name:Lu*\\ OR\\ _entity\\:Secret");
            assertThat(solr).doesNotContain(" OR _entity");
        }
    }

    @Nested
    @DisplayName("When binding native Solr query parameters")
    class WhenBindingNativeSolrQueryParameters {

        @Test
        @DisplayName("Should escape native query parameter values")
        void shouldEscapeNativeQueryParams() {
            String payload = "[* TO *] OR _entity:Secret";

            String solr = DefaultSolrDocumentManager.bindNativeQuery("name:@name AND _entity:person",
                    Map.of("name", payload));

            assertThat(solr).isEqualTo("name:" + ClientUtils.escapeQueryChars(payload) + " AND _entity:person");
        }

        @Test
        @DisplayName("Should replace longer parameter names before shorter prefixes")
        void shouldReplaceLongerNativeQueryParamsFirst() {
            String solr = DefaultSolrDocumentManager.bindNativeQuery("id:@id OR id2:@id2",
                    Map.of("id", "1", "id2", "2 OR *:*"));

            assertThat(solr).isEqualTo("id:1 OR id2:" + ClientUtils.escapeQueryChars("2 OR *:*"));
        }
    }
}

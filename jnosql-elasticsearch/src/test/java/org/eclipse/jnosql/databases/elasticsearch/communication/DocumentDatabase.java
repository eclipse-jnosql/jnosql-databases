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
package org.eclipse.jnosql.databases.elasticsearch.communication;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.elasticsearch.indices.PutMappingResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.ObjectBuilder;
import org.eclipse.jnosql.communication.Settings;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

public enum DocumentDatabase implements Supplier<ElasticsearchDocumentManagerFactory> {

    INSTANCE;

    private final GenericContainer<?> container =
            new GenericContainer("docker.io/elastic/elasticsearch:9.4.3")
                    .withReuse(true)
                    .withExposedPorts(9200, 9300)
                    .withEnv("discovery.type", "single-node")
                    .withEnv("ES_JAVA_OPTS", "-Xms1g -Xmx1g")
                    .withEnv("xpack.security.enabled", "false")
                    .waitingFor(Wait.forHttp("/")
                            .forPort(9200)
                            .forStatusCode(200));

    {
        container.start();
    }

    public static void clearDatabase(String index) {
        try (var elasticsearch = INSTANCE.newElasticsearchClient()) {
            var response = elasticsearch.client().indices().delete(DeleteIndexRequest.of(d ->
                    d.index(index)));
            assertSoftly(softly -> softly.assertThat(response.acknowledged())
                    .isTrue());
        } catch (Exception e) {
            if( !(e instanceof ElasticsearchException)) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void createDatabase(String index) {
        try (var elasticsearch = INSTANCE.newElasticsearchClient()) {
            CreateIndexResponse response = elasticsearch.client().indices().create(
                    CreateIndexRequest.of(b -> b.index(index)));
            assertSoftly(softly -> softly.assertThat(response.acknowledged())
                    .isTrue());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateMapping(String index, Function<PutMappingRequest.Builder, ObjectBuilder<PutMappingRequest>> fn) {
        try (var elasticsearch = INSTANCE.newElasticsearchClient()) {
            PutMappingResponse response = elasticsearch.client().indices().putMapping(fn);
            assertSoftly(softly -> softly.assertThat(response.acknowledged())
                    .isTrue());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void insertData(String index, Map<String, Object> map) {
        try (var elasticsearch = INSTANCE.newElasticsearchClient()) {
            var _id = Optional.ofNullable(map.remove("_id"));

            var indexRequest = _id
                    .map(String.class::cast)
                    .map(id -> IndexRequest.of(b ->
                            b.index(index)
                                    .id(id).document(JsonData.of(map))))
                    .orElseGet(() -> IndexRequest.of(b ->
                            b.index(index).document(JsonData.of(map))));

            IndexResponse response = elasticsearch.client().index(indexRequest);

            assertSoftly(softly -> softly.assertThat(response.result().jsonValue())
                    .isEqualTo("created"));
            _id.ifPresent(id -> map.put("_id", id));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    @Override
    public ElasticsearchDocumentManagerFactory get() {
        ElasticsearchDocumentConfiguration configuration = new ElasticsearchDocumentConfiguration();
        Settings settings1 = getSettings();
        return configuration.apply(settings1);
    }

    @NotNull
    public Settings getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put(ElasticsearchConfigurations.HOST.get() + ".1", host());
        return Settings.of(settings);
    }

    public ElasticsearchClientAutoClosable newElasticsearchClient() {
        ElasticsearchDocumentConfiguration configuration = new ElasticsearchDocumentConfiguration();
        return new ElasticsearchClientAutoClosable(configuration.buildElasticsearchClient(getSettings()));
    }

    public static record ElasticsearchClientAutoClosable(
            ElasticsearchClient client) implements AutoCloseable {

        @Override
        public void close() throws Exception {
            this.client._transport().close();
        }
    }

    public ElasticsearchDocumentManager get(String database) {
        ElasticsearchDocumentManagerFactory managerFactory = get();
        return managerFactory.apply(database);
    }

    public String host() {
        return container.getHost() + ':' + container.getFirstMappedPort();
    }

}

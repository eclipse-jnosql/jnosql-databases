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
package org.eclipse.jnosql.databases.couchdb.communication;


import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpMessage;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.eclipse.jnosql.communication.driver.JsonbSupplier;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Elements;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;

class HttpExecute {


    private static final Jsonb JSONB = JsonbSupplier.getInstance().get();
    private static final ContentType APPLICATION_JSON = ContentType.APPLICATION_JSON;

    private static final Type LIST_STRING =
            new ArrayList<String>() {}.getClass().getGenericSuperclass();

    private static final Type JSON =
            new HashMap<String, Object>() {}.getClass().getGenericSuperclass();

    private final CouchDBHttpConfiguration configuration;
    private final CloseableHttpClient client;
    private final MangoQueryConverter converter;

    HttpExecute(CouchDBHttpConfiguration configuration, CloseableHttpClient client) {
        this.configuration = configuration;
        this.client = client;
        this.converter = new MangoQueryConverter();
    }

    public List<String> getDatabases() {
        HttpGet request = new HttpGet(configuration.getUrl() + CouchDBConstant.ALL_DBS);
        return execute(request, LIST_STRING, HttpStatus.SC_OK);
    }

    public void createDatabase(String database) {
        HttpPut request = new HttpPut(configuration.getUrl() + database);
        Map<String, Object> json = execute(request, JSON, HttpStatus.SC_CREATED);

        if (!"true".equals(json.getOrDefault("ok", "false").toString())) {
            throw new CouchDBHttpClientException("There is an error to create database: " + database);
        }
    }

    public CommunicationEntity insert(String database, CommunicationEntity entity) {
        Map<String, Object> map = new HashMap<>(entity.toMap());
        String id = map.getOrDefault(CouchDBConstant.ID, "").toString();
        map.put(CouchDBConstant.ENTITY, entity.name());

        try {
            ClassicHttpRequest request;

            if (id.isEmpty()) {
                request = new HttpPost(configuration.getUrl() + database + "/");
            } else {
                String encodedId = URLEncoder.encode(id, UTF_8);
                request = new HttpPut(configuration.getUrl() + database + "/" + encodedId);
            }

            setHeader(request);
            request.setEntity(new StringEntity(JSONB.toJson(map), APPLICATION_JSON));

            Map<String, Object> json = execute(request, JSON, HttpStatus.SC_CREATED);

            entity.add(CouchDBConstant.ID, json.get(CouchDBConstant.ID_RESPONSE));
            entity.add(CouchDBConstant.REV, json.get(CouchDBConstant.REV_RESPONSE));
            return entity;

        } catch (Exception ex) {
            throw ex instanceof CouchDBHttpClientException
                    ? (CouchDBHttpClientException) ex
                    : new CouchDBHttpClientException("There is an error when inserting an entity", ex);
        }
    }

    public CommunicationEntity update(String database, CommunicationEntity entity) {
        String id = getId(entity);
        Map<String, Object> json = findById(database, id);
        entity.add(CouchDBConstant.REV, json.get(CouchDBConstant.REV));
        return insert(database, entity);
    }

    public Stream<CommunicationEntity> select(String database, SelectQuery query) {
        return executeQuery(database, query).stream().map(this::toEntity);
    }

    public void delete(String database, org.eclipse.jnosql.communication.semistructured.DeleteQuery query) {
        CouchDBDocumentQuery documentQuery =
                CouchDBDocumentQuery.of(new CouchdbDeleteQuery(query));

        List<Map<String, Object>> entities = executeQuery(database, documentQuery);
        while (!entities.isEmpty()) {
            entities.stream().map(DeleteElement::new).forEach(e -> delete(database, e));
            entities = executeQuery(database, documentQuery);
        }
    }

    public long count(String database) {
        HttpGet request = new HttpGet(configuration.getUrl() + database + CouchDBConstant.COUNT);
        Map<String, Object> json = execute(request, JSON, HttpStatus.SC_OK);
        return Long.parseLong(json.get(CouchDBConstant.TOTAL_ROWS_RESPONSE).toString());
    }

    private void delete(String database, DeleteElement id) {
        HttpDelete request = new HttpDelete(configuration.getUrl() + database + "/" + id.getId());
        request.addHeader(CouchDBConstant.REV_HEADER, id.getRev());
        execute(request, null, HttpStatus.SC_OK, true);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> executeQuery(String database, SelectQuery query) {
        HttpPost request = new HttpPost(configuration.getUrl() + database + CouchDBConstant.FIND);
        setHeader(request);
        JsonObject mangoQuery = converter.apply(query);
        request.setEntity(new StringEntity(mangoQuery.toString(), APPLICATION_JSON));

        Map<String, Object> json = execute(request, JSON, HttpStatus.SC_OK);

        if (query instanceof CouchDBDocumentQuery) {
            ((CouchDBDocumentQuery) query).setBookmark(json);
        }

        return (List<Map<String, Object>>)
                json.getOrDefault(CouchDBConstant.DOCS_RESPONSE, emptyList());
    }

    private CommunicationEntity toEntity(Map<String, Object> jsonEntity) {
        CommunicationEntity entity =
                CommunicationEntity.of(jsonEntity.get(CouchDBConstant.ENTITY).toString());
        entity.addAll(Elements.of(jsonEntity));
        entity.remove(CouchDBConstant.ENTITY);
        return entity;
    }

    private Map<String, Object> findById(String database, String id) {
        HttpGet request = new HttpGet(configuration.getUrl() + database + "/" + id);
        return execute(request, JSON, HttpStatus.SC_OK);
    }

    private String getId(CommunicationEntity entity) {
        return entity.find(CouchDBConstant.ID)
                .orElseThrow(() -> new CouchDBHttpClientException(
                        "To update the entity the id field is required"))
                .get(String.class);
    }

    private <T> T execute(ClassicHttpRequest request, Type type, int expectedStatus) {
        return execute(request, type, expectedStatus, false);
    }

    private <T> T execute(ClassicHttpRequest request,
                          Type type,
                          int expectedStatus,
                          boolean ignoreStatus) {

        configuration.strategy().apply(request);

        try (CloseableHttpResponse response = client.execute(request)) {

            if (!ignoreStatus && response.getCode() != expectedStatus) {
                String body = readBody(response.getEntity());
                throw new CouchDBHttpClientException(
                        "HTTP " + response.getCode() + " error: " + body);
            }

            if (type == null) {
                return null;
            }

            return JSONB.fromJson(readBody(response.getEntity()), type);

        } catch (Exception ex) {
            throw ex instanceof CouchDBHttpClientException
                    ? (CouchDBHttpClientException) ex
                    : new CouchDBHttpClientException("An error accessing the database", ex);
        }
    }

    private String readBody(HttpEntity entity) throws Exception {
        if (entity == null) {
            return "{}";
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        entity.writeTo(out);
        return out.toString(UTF_8);
    }

    private void setHeader(HttpMessage request) {
        request.setHeader("Accept", APPLICATION_JSON.getMimeType());
        request.setHeader("Content-Type", APPLICATION_JSON.getMimeType());
    }

}
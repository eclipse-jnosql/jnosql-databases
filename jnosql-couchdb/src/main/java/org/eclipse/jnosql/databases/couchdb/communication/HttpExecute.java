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
import org.apache.commons.codec.net.URLCodec;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.jnosql.communication.driver.JsonbSupplier;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Elements;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

class HttpExecute {


    private static final Jsonb JSONB = JsonbSupplier.getInstance().get();

    private static final URLCodec CODEC = new URLCodec();

    private static final Type LIST_STRING = new ArrayList<String>() {
    }.getClass().getGenericSuperclass();

    private static final Type JSON = new HashMap<String, Object>() {
    }.getClass().getGenericSuperclass();


    private final CouchDBHttpConfiguration configuration;

    private final CloseableHttpClient client;

    private final MangoQueryConverter converter;

    HttpExecute(CouchDBHttpConfiguration configuration, CloseableHttpClient client) {
        this.configuration = configuration;
        this.client = client;
        this.converter = new MangoQueryConverter();
    }

    public List<String> getDatabases() {
        HttpGet httpget = new HttpGet(configuration.getUrl().concat(CouchDBConstant.ALL_DBS));
        return execute(httpget, LIST_STRING, HttpStatus.SC_OK);
    }

    public void createDatabase(String database) {
        HttpPut httpPut = new HttpPut(configuration.getUrl().concat(database));
        Map<String, Object> json = execute(httpPut, JSON, HttpStatus.SC_CREATED);
        if (!json.getOrDefault("ok", "false").toString().equals("true")) {
            throw new CouchDBHttpClientException("There is an error to create database: " + database);
        }
    }

    public CommunicationEntity insert(String database, CommunicationEntity entity) {
        Map<String, Object> map = new HashMap<>(entity.toMap());
        String id = map.getOrDefault(CouchDBConstant.ID, "").toString();
        map.put(CouchDBConstant.ENTITY, entity.name());
        try {
            HttpEntityEnclosingRequestBase request;
            if (id.isEmpty()) {
                request = new HttpPost(configuration.getUrl().concat(database).concat("/"));
            } else {
                id = CODEC.encode(id);
                request = new HttpPut(configuration.getUrl().concat(database).concat("/").concat(id));
            }

            setHeader(request);
            StringEntity jsonEntity = new StringEntity(JSONB.toJson(map), APPLICATION_JSON);
            request.setEntity(jsonEntity);
            Map<String, Object> json = execute(request, JSON, HttpStatus.SC_CREATED);
            entity.add(CouchDBConstant.ID, json.get(CouchDBConstant.ID_RESPONSE));
            entity.add(CouchDBConstant.REV, json.get(CouchDBConstant.REV_RESPONSE));
            return entity;
        } catch (CouchDBHttpClientException ex) {
            throw ex;
        } catch (Exception exp) {
            throw new CouchDBHttpClientException("There is an error when try to insert an entity at database", exp);
        }
    }

    public CommunicationEntity update(String database, CommunicationEntity entity) {
        String id = getId(entity);
        Map<String, Object> json = findById(database, id);
        entity.add(CouchDBConstant.REV, json.get(CouchDBConstant.REV));
        return insert(database, entity);
    }

    public Stream<CommunicationEntity> select(String database, SelectQuery query) {
        List<Map<String, Object>> entities = executeQuery(database, query);
        return entities.stream().map(this::toEntity);
    }

    public void delete(String database, org.eclipse.jnosql.communication.semistructured.DeleteQuery query) {
        CouchDBDocumentQuery documentQuery = CouchDBDocumentQuery.of(new CouchdbDeleteQuery(query));
        List<Map<String, Object>> entities = executeQuery(database, documentQuery);
        while (!entities.isEmpty()) {
            entities.stream().map(DeleteElement::new).forEach(id -> this.delete(database, id));
            entities = executeQuery(database, documentQuery);
        }
    }

    public long count(String database) {
        HttpGet request = new HttpGet(configuration.getUrl().concat(database).concat(CouchDBConstant.COUNT));
        Map<String, Object> json = execute(request, JSON, HttpStatus.SC_OK);
        String total = json.get(CouchDBConstant.TOTAL_ROWS_RESPONSE).toString();
        return Long.parseLong(total);
    }


    private void delete(String database, DeleteElement id) {
        HttpDelete request = new HttpDelete(configuration.getUrl().concat(database).concat("/").concat(id.getId()));
        request.addHeader(CouchDBConstant.REV_HEADER, id.getRev());
        execute(request, null, HttpStatus.SC_OK, true);
    }


    private List<Map<String, Object>> executeQuery(String database, SelectQuery query) {
        HttpPost request = new HttpPost(configuration.getUrl().concat(database).concat(CouchDBConstant.FIND));
        setHeader(request);
        JsonObject mangoQuery = converter.apply(query);
        request.setEntity(new StringEntity(mangoQuery.toString(), APPLICATION_JSON));
        Map<String, Object> json = execute(request, JSON, HttpStatus.SC_OK);
        if (query instanceof CouchDBDocumentQuery) {
            CouchDBDocumentQuery.class.cast(query).setBookmark(json);
        }
        return (List<Map<String, Object>>) json.getOrDefault(CouchDBConstant.DOCS_RESPONSE, emptyList());
    }


    private CommunicationEntity toEntity(Map<String, Object> jsonEntity) {
        CommunicationEntity entity = CommunicationEntity.of(jsonEntity.get(CouchDBConstant.ENTITY).toString());
        entity.addAll(Elements.of(jsonEntity));
        entity.remove(CouchDBConstant.ENTITY);
        return entity;
    }

    private Map<String, Object> findById(String database, String id) {
        HttpGet request = new HttpGet(configuration.getUrl().concat(database).concat("/").concat(id));
        return execute(request, JSON, HttpStatus.SC_OK);
    }

    private String getId(CommunicationEntity entity) {
        return entity.find(CouchDBConstant.ID)
                .orElseThrow(() -> new CouchDBHttpClientException(
                        String.format("To update the entity %s the id field is required", entity)))
                .get(String.class);
    }

    private <T> T execute(HttpUriRequest request, Type type, int expectedStatus) {
        return execute(request, type, expectedStatus, false);
    }

    private <T> T execute(HttpUriRequest request, Type type, int expectedStatus, boolean ignoreStatus) {

        var strategy = configuration.strategy();
        strategy.apply(request);
        try (CloseableHttpResponse result = client.execute(request)) {
            if (!ignoreStatus && result.getStatusLine().getStatusCode() != expectedStatus) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                result.getEntity().writeTo(stream);
                String response = stream.toString(UTF_8);
                throw new CouchDBHttpClientException("There is an error when load the database status: " +
                        result.getStatusLine().getStatusCode()
                        + " error: " + response);
            }
            if (Objects.isNull(type)) {
                return null;
            }
            HttpEntity entity = result.getEntity();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            entity.writeTo(stream);
            return JSONB.fromJson(new String(stream.toByteArray(), UTF_8), type);
        } catch (CouchDBHttpClientException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CouchDBHttpClientException("An error to access the database", ex);
        }
    }

    private void setHeader(HttpEntityEnclosingRequestBase request) {
        request.setHeader("Accept", APPLICATION_JSON.getMimeType());
        request.setHeader("Content-type", APPLICATION_JSON.getMimeType());
    }

}

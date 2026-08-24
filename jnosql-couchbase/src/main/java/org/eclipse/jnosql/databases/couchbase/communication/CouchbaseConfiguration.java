/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License 2.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *   and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.databases.couchbase.communication;


import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

/**
 * The configuration base to all configuration implementation on couchbase
 */
public abstract class CouchbaseConfiguration {

    protected String host;

    protected String user;

    protected String password;

    protected String scope;

    protected String index;

    protected String collection;
    protected Set<String> collections = new LinkedHashSet<>();


    protected void update(Settings settings) {
        this.host = getHost(settings);
        this.user = getUser(settings);
        this.password = getPassword(settings);
        this.scope = getScope(settings);
        this.collections = getCollections(settings);
        this.index = getIndex(settings);
        this.collection = getCollection(settings);
    }

    protected String getUser(Settings settings) {
        return settings.getSupplier(asList(Configurations.USER,
                        CouchbaseConfigurations.USER))
                .map(Object::toString).orElse(null);
    }

    private String getScope(Settings settings) {
        return settings.get(CouchbaseConfigurations.SCOPE)
                .map(Object::toString).orElse(null);
    }

    private String getCollection(Settings settings) {
        return settings.get(CouchbaseConfigurations.COLLECTION)
                .map(Object::toString).orElse(null);
    }

    private String getIndex(Settings settings) {
        return settings.get(CouchbaseConfigurations.INDEX)
                .map(Object::toString).orElse(null);
    }

    private Set<String> getCollections(Settings settings) {
        Set<String> collections = new LinkedHashSet<>();
        settings.get(CouchbaseConfigurations.COLLECTIONS)
                .map(Object::toString).stream()
                .flatMap(s -> Stream.of(s.split(",\\s*")))
                .forEach(collections::add);
        return collections;
    }

    protected String getPassword(Settings settings) {

        return settings.getSupplier(asList(Configurations.PASSWORD,
                        CouchbaseConfigurations.PASSWORD))
                .map(Object::toString).orElse(null);
    }

    protected String getHost(Settings settings) {
        return settings.getSupplier(asList(Configurations.HOST,
                        CouchbaseConfigurations.HOST))
                .map(Object::toString).orElse(null);
    }


    /**
     * set the host
     *
     * @param host the host
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * set the user
     *
     * @param user the user
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * set the password
     *
     * @param password the password
     */
    public void setPassword(String password) {
        this.password = password;
    }


    /**
     * Set the scope
     * @param scope the scope
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * Set the collection
     * @param collection the collection
     */
    public void setCollection(String collection) {
        this.collection = collection;
    }

    /**
     * add collection in the settings
     *
     * @param collection the collection
     * @throws NullPointerException when collection is null
     */
    public void addCollection(String collection) {
        java.util.Objects.requireNonNull(collection, "collection is required");
        this.collections.add(collection);
    }

    /**
     * Returns an immutable structure with the Couchbase settings
     * @return the {@link CouchbaseSettings}
     */
    public CouchbaseSettings toCouchbaseSettings() {
        return new CouchbaseSettings(this.host, this.user, this.password,
                this.scope, this.index, this.collection, this.collections.stream().toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CouchbaseConfiguration that = (CouchbaseConfiguration) o;
        return Objects.equals(host, that.host) && Objects.equals(user, that.user)
                && Objects.equals(password, that.password)
                && Objects.equals(scope, that.scope)
                && Objects.equals(collections, that.collections)
                && Objects.equals(index, that.index);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, user, password, scope, collections, index);
    }

    @Override
    public String toString() {
        return "CouchbaseConfiguration{" +
                "host='" + host + '\'' +
                ", user='" + user + '\'' +
                ", password='" + "***" + '\'' +
                ", scope='" + scope + '\'' +
                ", collections=" + collections +
                ", index='" + index + '\'' +
                '}';
    }
}

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
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.couchbase.communication;

import com.couchbase.client.core.error.BucketNotFoundException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.manager.bucket.BucketManager;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.couchbase.client.java.manager.collection.CollectionManager;
import com.couchbase.client.java.manager.collection.CollectionSpec;
import com.couchbase.client.java.manager.collection.ScopeSpec;
import com.couchbase.client.java.manager.query.QueryIndexManager;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.couchbase.client.java.manager.query.CreatePrimaryQueryIndexOptions.createPrimaryQueryIndexOptions;
import static com.couchbase.client.java.manager.query.GetAllQueryIndexesOptions.getAllQueryIndexesOptions;

/**
 * An immutable structure that has the Couchbase settings.
 */
public final class CouchbaseSettings {

    private static final Logger LOGGER = Logger.getLogger(CouchbaseSettings.class.getName());

    private final String host;

    private final String user;

    private final String password;

    private final String scope;

    private final String index;

    private final String collection;
    private final List<String> collections;

    CouchbaseSettings(String host, String user, String password,
                      String scope, String index, String collection,
                      List<String> collections) {
        this.host = host;
        this.user = user;
        this.password = password;
        this.scope = scope;
        this.index = index;
        this.collection = collection;
        this.collections = collections;
    }


    /**
     * Returns the host {@link org.eclipse.jnosql.communication.Configurations#HOST} or {@link CouchbaseConfigurations#HOST}
     *
     * @return the host {@link org.eclipse.jnosql.communication.Configurations#HOST} or {@link CouchbaseConfigurations#HOST}
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the user {@link org.eclipse.jnosql.communication.Configurations#USER} or {@link CouchbaseConfigurations#USER}
     *
     * @return the user {@link org.eclipse.jnosql.communication.Configurations#USER} or {@link CouchbaseConfigurations#USER}
     */
    public String getUser() {
        return user;
    }

    /**
     * Returns the password {@link org.eclipse.jnosql.communication.Configurations#PASSWORD} or {@link CouchbaseConfigurations#PASSWORD}
     *
     * @return the password {@link org.eclipse.jnosql.communication.Configurations#PASSWORD} or {@link CouchbaseConfigurations#PASSWORD}
     */
    public String getPassword() {
        return password;
    }

    /**
     * Returns the scope {@link CouchbaseConfigurations#SCOPE}
     *
     * @return the password {@link CouchbaseConfigurations#SCOPE}
     */
    public Optional<String> getScope() {
        return Optional.ofNullable(scope);
    }

    /**
     * Returns the scope {@link CouchbaseConfigurations#SCOPE}
     *
     * @return the password {@link CouchbaseConfigurations#SCOPE}
     */
    public Optional<String> getCollection() {
        return Optional.ofNullable(collection);
    }

    /**
     * Returns the scope {@link CouchbaseConfigurations#COLLECTIONS}
     *
     * @return the password {@link CouchbaseConfigurations#COLLECTIONS}
     */
    public List<String> getCollections() {
        if (collections == null || collections.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(collections);
    }

    /**
     * Returns the scope {@link CouchbaseConfigurations#INDEX}
     *
     * @return the password {@link CouchbaseConfigurations#INDEX}
     */
    public String getIndex() {
        return index;
    }


    /**
     * Create a new {@link Cluster} instance using {@link CouchbaseSettings#getHost()}
     * {@link  CouchbaseSettings#getUser()} {@link CouchbaseSettings#getPassword()}
     *
     * @return a {@link Cluster} instance
     */
    public Cluster getCluster() {
        return Cluster.connect(host, user, password);
    }

    /**
     * Given a database/bucket, it creates a basic setup to create a scope, collection, and index.
     * It will read the properties from Settings if it does exist.
     * It is for a development proposal. <b>Don't use it on production</b>.
     *
     * @param database the database
     * @throws NullPointerException when parameter is null
     */
    public void setUp(String database) {

        Objects.requireNonNull(database, "database is required");

        var collections = getCollections().stream().map(String::trim)
                .filter(index -> !index.isBlank()).toList();

        var collectionsToIndex = Arrays
                .stream(Optional.ofNullable(getIndex()).orElse("").split(","))
                .map(String::trim)
                .filter(index -> !index.isBlank()).collect(Collectors.toSet());

        var scope = getScope();

        long start = System.currentTimeMillis();
        LOGGER.log(Level.FINEST, "starting the setup with database: " + database);

        try (Cluster cluster = getCluster()) {

            BucketManager buckets = cluster.buckets();
            try {
                buckets.getBucket(database);
            } catch (BucketNotFoundException exp) {
                LOGGER.log(Level.FINEST, "The database/bucket does not exist, creating it: " + database);
                buckets.createBucket(BucketSettings.create(database));
            }

            Bucket bucket = cluster.bucket(database);

            waitUntilReady(bucket);

            CollectionManager manager = bucket.collections();

            List<ScopeSpec> scopes = manager.getAllScopes();
            String finalScope = scope.orElseGet(() -> bucket.defaultScope().name());
            ScopeSpec spec = scopes.stream().filter(s -> finalScope.equals(s.name()))
                    .findFirst().get();

            collections.forEach(collection -> {
                if (spec.collections().stream().noneMatch(c -> collectionsToIndex.contains(c.name()))) {
                    manager.createCollection(CollectionSpec.create(collection, finalScope));
                }
            });

            waitUntilReady(bucket);

            if (!collectionsToIndex.isEmpty()) {

                QueryIndexManager queryIndexManager = cluster.queryIndexes();

                Stream.concat(collectionsToIndex.stream(), collectionsToIndex.stream())
                        .distinct()
                        .forEach(collection -> {
                            var allIndexes = queryIndexManager.getAllIndexes(database, getAllQueryIndexesOptions()
                                    .scopeName(finalScope).collectionName(collection));
                            if (allIndexes.isEmpty()) {
                                LOGGER.log(Level.FINEST, "Index for " + collection + " collection does not exist, creating primary key with scope "
                                        + finalScope + " collection " + collection + " at database " + database);
                                queryIndexManager.createPrimaryIndex(database, createPrimaryQueryIndexOptions()
                                        .scopeName(finalScope).collectionName(collection));
                            }
                        });

            }

            long end = System.currentTimeMillis() - start;
            LOGGER.log(Level.FINEST, "Finished the setup with database: " + database + " end with millis "
                    + end);
        }

    }

    private void waitUntilReady(Bucket bucket) {
        bucket.waitUntilReady(Duration.of(4, ChronoUnit.SECONDS));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CouchbaseSettings that = (CouchbaseSettings) o;
        return Objects.equals(host, that.host) && Objects.equals(user, that.user)
                && Objects.equals(password, that.password) && Objects.equals(scope, that.scope)
                && Objects.equals(index, that.index) && Objects.equals(collection, that.collection)
                && Objects.equals(collections, that.collections);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, user, password, scope, index, collection, collections);
    }

    @Override
    public String toString() {
        return "CouchbaseSettings{" +
                "host='" + host + '\'' +
                ", user='" + user + '\'' +
                ", password='" + password + '\'' +
                ", scope='" + scope + '\'' +
                ", index='" + index + '\'' +
                ", collection='" + collection + '\'' +
                ", collections=" + collections +
                '}';
    }
}

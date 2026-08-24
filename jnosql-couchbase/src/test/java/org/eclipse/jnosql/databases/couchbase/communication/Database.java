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

import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.SettingsBuilder;
import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;

import java.util.Optional;

public enum Database {

    INSTANCE;

    private final CouchbaseContainer container;
    private final CouchbaseSettings settings;

    Database() {

        Settings fileSettings = CouchbaseUtil.getSettings();

        CouchbaseDocumentConfiguration configuration = new CouchbaseDocumentConfiguration();

        var bucketName = CouchbaseUtil.BUCKET_NAME;

        BucketDefinition bucketDefinition = new BucketDefinition(bucketName)
                .withFlushEnabled(true);

        container = new CouchbaseContainer("couchbase/server")
                .withBucket(bucketDefinition)
                .withCredentials(configuration.getUser(fileSettings), configuration.getPassword(fileSettings))
                .withReuse(true);

        container.start();

        configuration.update(fileSettings);

        setup(configuration, configuration.toCouchbaseSettings());

        settings = configuration.toCouchbaseSettings();

        settings.setUp(bucketName);
    }

    public CouchbaseDocumentConfiguration getDocumentConfiguration() {
        CouchbaseDocumentConfiguration configuration = setup(new CouchbaseDocumentConfiguration());
        return configuration;
    }

    public CouchbaseKeyValueConfiguration getKeyValueConfiguration() {
        CouchbaseKeyValueConfiguration configuration = setup(new CouchbaseKeyValueConfiguration());
        return configuration;
    }

    private <T extends CouchbaseConfiguration> T setup(T configuration) {
        return setup(configuration, this.settings);
    }

    private <T extends CouchbaseConfiguration> T setup(T configuration, CouchbaseSettings settings) {
        configuration.setHost(container.getConnectionString());
        configuration.setUser(container.getUsername());
        configuration.setPassword(container.getPassword());
        settings.getCollection().ifPresent(configuration::setCollection);
        settings.getCollections().stream().toList().forEach(configuration::addCollection);
        settings.getScope().ifPresent(configuration::setScope);
        return configuration;
    }

    public CouchbaseSettings getCouchbaseSettings() {
        return settings;
    }

    public Settings getSettings() {
        return getSettingsBuilder().build();
    }

    public SettingsBuilder getSettingsBuilder() {
        CouchbaseSettings couchbaseSettings = getCouchbaseSettings();
        SettingsBuilder builder = CouchbaseUtil.getSettingsBuilder();
        builder.put(CouchbaseConfigurations.HOST.get(), couchbaseSettings.getHost());
        couchbaseSettings.getScope().ifPresent(scope ->
                builder.put(CouchbaseConfigurations.SCOPE.get(), scope));
        Optional.ofNullable(couchbaseSettings.getIndex()).ifPresent(index ->
                builder.put(CouchbaseConfigurations.INDEX.get(), index));
        if (!couchbaseSettings.getCollections().isEmpty()) {
            builder.put(CouchbaseConfigurations.COLLECTIONS.get(),
                    String.join(",", couchbaseSettings.getCollections()));
        }
        Optional.ofNullable(couchbaseSettings.getUser()).ifPresent(user ->
                builder.put(CouchbaseConfigurations.USER.get(), couchbaseSettings.getUser()));
        Optional.ofNullable(couchbaseSettings.getPassword()).ifPresent(password ->
                builder.put(CouchbaseConfigurations.PASSWORD.get(), password));
        return builder;
    }

}

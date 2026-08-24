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
import org.eclipse.jnosql.communication.driver.ConfigurationReader;
import org.eclipse.jnosql.mapping.core.config.MappingConfigurations;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.eclipse.jnosql.databases.couchbase.communication.Database.INSTANCE;

public final class CouchbaseUtil {

    public static final String BUCKET_NAME = "jnosql";

    private static final String FILE_CONFIGURATION = "couchbase.properties";

    private CouchbaseUtil() {
    }

    public static Settings getSettings() {
        SettingsBuilder builder = getSettingsBuilder();
        return builder.build();
    }

    public static SettingsBuilder getSettingsBuilder() {
        Map<String, String> map = ConfigurationReader.from(CouchbaseUtil.FILE_CONFIGURATION);
        SettingsBuilder builder = Settings.builder();
        map.forEach(builder::put);
        return builder;
    }

    public static void systemPropertySetup(Consumer<CouchbaseSettings> consumer) {
        Map<String, Object> configuration = new HashMap<>(ConfigurationReader.from("couchbase.properties"));
        for (Map.Entry<String, Object> entry : configuration.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue().toString());
        }
        CouchbaseSettings couchbaseSettings = INSTANCE.getCouchbaseSettings();
        System.setProperty(CouchbaseConfigurations.HOST.get(), couchbaseSettings.getHost());
        consumer.accept(couchbaseSettings);
    }

    public static void systemPropertySetup() {
        systemPropertySetup(settings -> {
            System.setProperty(MappingConfigurations.DOCUMENT_DATABASE.get(), CouchbaseUtil.BUCKET_NAME);
            System.setProperty(MappingConfigurations.DOCUMENT_PROVIDER.get(), CouchbaseDocumentConfiguration.class.getName());
        });
    }

}

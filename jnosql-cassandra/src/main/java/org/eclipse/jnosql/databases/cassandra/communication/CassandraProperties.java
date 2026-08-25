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
package org.eclipse.jnosql.databases.cassandra.communication;


import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.SettingsBuilder;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class CassandraProperties {

    private static final int DEFAULT_PORT = 9042;

    private static final String DEFAULT_DATA_CENTER = "datacenter1";

    private final List<String> queries = new ArrayList<>();

    private final List<String> nodes = new ArrayList<>();

    private Optional<String> name;

    private Optional<String> user;

    private Optional<String> password;

    private int port;

    private String dataCenter;

    public void addQuery(String query) {
        this.queries.add(query);
    }

    public void addNodes(String node) {
        this.nodes.add(node);
    }

    public List<String> getQueries() {
        return queries;
    }

    public CqlSessionBuilder createCluster() {
        CqlSessionBuilder builder = CqlSession.builder();
        nodes.stream().map(h -> new InetSocketAddress(h, port)).forEach(builder::addContactPoint);
        name.ifPresent(builder::withApplicationName);
        builder.withLocalDatacenter(dataCenter);
        if (user.isPresent()) {
            builder.withAuthCredentials(user.orElse(""), password.orElse(""));
        }
        return builder;
    }

    public static CassandraProperties of(Map<String, String> configurations) {
        SettingsBuilder builder = Settings.builder();
        configurations.forEach(builder::put);
        Settings settings = builder.build();

        CassandraProperties cp = new CassandraProperties();
        settings.prefix(CassandraConfigurations.HOST).stream()
                .map(Object::toString).forEach(cp::addNodes);

        settings.prefix(CassandraConfigurations.QUERY)
                .stream().map(Object::toString).forEach(cp::addQuery);

        cp.port = settings.get(CassandraConfigurations.PORT)
                .map(Object::toString).map(Integer::parseInt).orElse(DEFAULT_PORT);

        cp.name = settings.get(CassandraConfigurations.NAME)
                .map(Object::toString);
        cp.dataCenter = settings.get(CassandraConfigurations.DATA_CENTER).map(Object::toString)
                .orElse(DEFAULT_DATA_CENTER);

        cp.user = settings.get(CassandraConfigurations.USER)
                .map(Object::toString);

        cp.password = settings.get(CassandraConfigurations.PASSWORD)
                .map(Object::toString);
        return cp;
    }
}

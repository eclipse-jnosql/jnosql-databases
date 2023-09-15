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
package org.eclipse.jnosql.databases.arangodb.communication;

import com.arangodb.Protocol;
import com.arangodb.entity.LoadBalancingStrategy;
import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;

final class ArangoDBBuilders {


    private ArangoDBBuilders() {
    }

    static void load(Settings settings, ArangoDBBuilder arangoDB) {

        settings.getSupplier(asList(ArangoDBConfigurations.USER, Configurations.USER))
                .map(Object::toString).ifPresent(arangoDB::user);
        settings.getSupplier(asList(ArangoDBConfigurations.PASSWORD, Configurations.PASSWORD))
                .map(Object::toString).ifPresent(arangoDB::password);
        settings.get(ArangoDBConfigurations.TIMEOUT)
                .map(Object::toString).map(Integer::valueOf).ifPresent(arangoDB::timeout);

        settings.get(ArangoDBConfigurations.CHUNK_SIZE)
                .map(Object::toString).map(Integer::valueOf)
                .ifPresent(arangoDB::chunkSize);

        settings.get(ArangoDBConfigurations.MAX_CONNECTIONS)
                .map(Object::toString).map(Integer::valueOf)
                .ifPresent(arangoDB::maxConnections);

        settings.get(ArangoDBConfigurations.USER_SSL)
                .map(Object::toString).map(Boolean::valueOf)
                .ifPresent(arangoDB::useSsl);

        settings.get(ArangoDBConfigurations.HOST_LIST)
                .map(Object::toString).map(Boolean::valueOf)
                .ifPresent(arangoDB::acquireHostList);

        settings.get(ArangoDBConfigurations.LOAD_BALANCING).map(Object::toString)
                .map(LoadBalancingStrategy::valueOf)
                .ifPresent(arangoDB::loadBalancingStrategy);

        settings.get(ArangoDBConfigurations.PROTOCOL).map(Object::toString).map(Protocol::valueOf)
                .ifPresent(arangoDB::protocol);

        settings.prefixSupplier(Arrays.asList(ArangoDBConfigurations.HOST, Configurations.HOST))
                .stream()
                .map(Object::toString)
                .map(ArangoDBHost::new)
                .flatMap(h -> h.getHost().stream())
                .forEach(h -> host(arangoDB, h));

        settings.prefix(ArangoDBConfigurations.SERIALIZER)
                .stream().map(Objects::toString)
                .map(EntrySerializer::of)
                .forEach(arangoDB::add);

        settings.prefix(ArangoDBConfigurations.DESERIALIZER)
                .stream().map(Objects::toString)
                .map(EntryDeserializer::of)
                .forEach(arangoDB::add);
    }


    private static void host(ArangoDBBuilder arangoDB, String host) {
        final String[] values = host.split(":");
        arangoDB.host(values[0], Integer.parseInt(values[1]));
    }

    private static class ArangoDBHost {
        private final String hots;

        private ArangoDBHost(String hots) {
            this.hots = hots;
        }

        public List<String> getHost() {
            return asList(this.hots.split(","));
        }
    }
}

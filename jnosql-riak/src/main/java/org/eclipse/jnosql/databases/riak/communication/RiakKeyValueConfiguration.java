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
package org.eclipse.jnosql.databases.riak.communication;


import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.RiakNode;
import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.keyvalue.KeyValueConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * The riak implementation to {@link KeyValueConfiguration} that returns {@link RiakBucketManagerFactory}.
 * riak.host-: The prefix to host. eg: riak.server.host.1= host1
 */
public class RiakKeyValueConfiguration implements KeyValueConfiguration {


    private static final String SERVER_PREFIX = "jnosql.riak.host";

    private static final RiakNode DEFAULT_NODE = new RiakNode.Builder()
            .withRemoteAddress("127.0.0.1").build();

    private final List<RiakNode> nodes = new ArrayList<>();


    /**
     * Adds new Riak node
     *
     * @param node the node
     * @throws NullPointerException when node is null
     */
    public void add(RiakNode node) throws NullPointerException {
        requireNonNull(node, "Node is required");
        this.nodes.add(node);
    }

    /**
     * Adds a new address on riak
     *
     * @param address the address to be added
     * @throws NullPointerException when address is null
     */
    public void add(String address) throws NullPointerException {
        requireNonNull(address, "Address is required");
        this.nodes.add(new RiakNode.Builder().withRemoteAddress(address).build());
    }


    @Override
    public RiakBucketManagerFactory apply(Settings settings) {
        requireNonNull(settings, "settings is required");
        List<RiakNode> nodes = new ArrayList<>();

        settings.prefix(asList(SERVER_PREFIX, Configurations.HOST.get()))
                .stream()
                .map(Object::toString)
                .map(toNode())
                .forEach(nodes::add);

        if (nodes.isEmpty()) {
            nodes.add(DEFAULT_NODE);
        }
        RiakCluster cluster = new RiakCluster.Builder(nodes)
                .build();

        return new RiakBucketManagerFactory(cluster);
    }

    private Function<String, RiakNode> toNode() {
        return h -> {
            String[] values = h.split(":");
            if (values.length == 1) {
                return new RiakNode.Builder()
                        .withRemoteAddress(values[0]).build();
            } else {
                return new RiakNode.Builder()
                        .withRemoteAddress(values[0])
                        .withRemotePort(Integer.parseInt(values[1]))
                        .build();
            }
        };
    }
}

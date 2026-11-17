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
 * The Riak implementation of {@link KeyValueConfiguration}. It creates a {@link RiakBucketManagerFactory} that
 * connects to the nodes found in the settings.
 *
 * <p>Every entry whose key starts with {@code jnosql.riak.host} or {@link Configurations#HOST} is a node given as
 * {@code host} or {@code host:port}; for example {@code jnosql.riak.host.1=host1:8087}. When no node is
 * configured, the factory connects to {@code 127.0.0.1} on the default Riak port.</p>
 *
 * <p>Example:</p>
 * {@snippet lang="java" :
 * Settings settings = Settings.builder()
 *         .put("jnosql.riak.host.1", "localhost:8087")
 *         .build();
 * RiakBucketManagerFactory factory = new RiakKeyValueConfiguration().apply(settings);
 * RiakBucketManager users = factory.apply("users");
 * }
 */
public class RiakKeyValueConfiguration implements KeyValueConfiguration {

    private static final String SERVER_PREFIX = "jnosql.riak.host";

    private static final RiakNode DEFAULT_NODE = new RiakNode.Builder()
            .withRemoteAddress("127.0.0.1").build();

    private final List<RiakNode> nodes = new ArrayList<>();


    /**
     * Adds a Riak node to this configuration instance.
     *
     * <p>The nodes added here are not read by {@link #apply(Settings)}, which builds the cluster only from the
     * settings.</p>
     *
     * @param node the node
     * @throws NullPointerException if {@code node} is {@code null}
     */
    public void add(RiakNode node) throws NullPointerException {
        requireNonNull(node, "Node is required");
        this.nodes.add(node);
    }

    /**
     * Adds a Riak node by address to this configuration instance, using the default Riak port.
     *
     * <p>The nodes added here are not read by {@link #apply(Settings)}, which builds the cluster only from the
     * settings.</p>
     *
     * @param address the node host name or IP address
     * @throws NullPointerException if {@code address} is {@code null}
     */
    public void add(String address) throws NullPointerException {
        requireNonNull(address, "Address is required");
        this.nodes.add(new RiakNode.Builder().withRemoteAddress(address).build());
    }


    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException  if {@code settings} is {@code null}
     * @throws NumberFormatException if a node port is not a number
     */
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

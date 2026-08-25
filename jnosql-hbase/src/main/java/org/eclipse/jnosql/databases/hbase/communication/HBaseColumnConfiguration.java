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
package org.eclipse.jnosql.databases.hbase.communication;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Configuration to HBase that returns {@link HBaseColumnManagerFactory}
 * hbase.family.n: as prefix to add family, eg: hbase,family.1=column-family
 */
public class HBaseColumnConfiguration implements DatabaseConfiguration {

    private final Configuration configuration;

    private final List<String> families = new ArrayList<>();

    /**
     * creates an {@link HBaseColumnConfiguration} instance with {@link HBaseConfiguration#create()}
     */
    public HBaseColumnConfiguration() {
        this.configuration = HBaseConfiguration.create();
    }

    /**
     * Creates hbase configuration
     *
     * @param configuration to be used
     * @throws NullPointerException when configuration is null
     */
    public HBaseColumnConfiguration(Configuration configuration) throws NullPointerException {
        this.configuration = requireNonNull(configuration, "configuration is required");
    }

    /**
     * Creates hbase configuration
     *
     * @param configuration to be used
     * @param families      families to be used
     * @throws NullPointerException when configuration is null
     */
    public HBaseColumnConfiguration(Configuration configuration, List<String> families) throws NullPointerException {
        this.configuration = requireNonNull(configuration, "configuration is required");
        requireNonNull(families, "families is required");
        this.families.addAll(families);
    }

    /**
     * Adds a new family
     *
     * @param family the family
     */
    public void add(String family) {
        this.families.add(requireNonNull(family, "family is required"));
    }



    @Override
    public HBaseColumnManagerFactory apply(Settings settings) throws NullPointerException {
        requireNonNull(settings, "settings is required");

        List<String> families = settings.prefix(HbaseConfigurations.FAMILY)
                .stream().map(Object::toString).collect(Collectors.toList());
        return new HBaseColumnManagerFactory(configuration, families);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HBaseColumnConfiguration that = (HBaseColumnConfiguration) o;
        return Objects.equals(configuration, that.configuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configuration);
    }

    @Override
    public String toString() {
        return "HBaseColumnConfiguration{" + "configuration=" + configuration +
                '}';
    }
}

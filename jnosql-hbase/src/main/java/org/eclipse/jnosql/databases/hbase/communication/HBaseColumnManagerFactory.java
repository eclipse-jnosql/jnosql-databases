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
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptor;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * The HBase implementation of {@link DatabaseManagerFactory} that returns {@link HBaseColumnManager}.
 * <br/>
 * Closing an {@link HBaseColumnManagerFactory} has no effect.
 */
public class HBaseColumnManagerFactory implements DatabaseManagerFactory {

    private final Configuration configuration;

    private final List<String> families;

    HBaseColumnManagerFactory(Configuration configuration, List<String> families) {
        this.configuration = configuration;
        this.families = families;
    }

    @Override
    public HBaseColumnManager apply(String database) {
        try {
            Connection connection = ConnectionFactory.createConnection(configuration);
            Admin admin = connection.getAdmin();
            TableName tableName = TableName.valueOf(database);
            if (admin.tableExists(tableName)) {
                existTable(admin, tableName);
            } else {
                createTable(admin, tableName);
            }
            Table table = connection.getTable(tableName);
            return new HBaseColumnManager(connection, table, database);
        } catch (IOException e) {
            throw new HBaseException("A error happened when try to create ColumnManager", e);
        }
    }


    private void existTable(Admin admin, TableName tableName) throws IOException {
        TableDescriptor tableDescriptor = admin.getDescriptor(tableName);
        ColumnFamilyDescriptor[] columnFamilies = tableDescriptor.getColumnFamilies();
        final TableDescriptorBuilder builder = TableDescriptorBuilder.newBuilder(tableName);
        List<String> familiesExist = Arrays.stream(columnFamilies).map(ColumnFamilyDescriptor::getName).map(String::new).toList();
        if (familiesExist.size() != families.size()) {
            families.stream().filter(s -> !familiesExist.contains(s))
                    .forEach(s -> builder.setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(s.getBytes())
                            .build()));
            final TableDescriptor descriptor = builder.build();
            admin.modifyTable(descriptor);
        }

    }

    private void createTable(Admin admin, TableName tableName) throws IOException {
        final TableDescriptorBuilder builder = TableDescriptorBuilder.newBuilder(tableName);
        families.forEach(s -> builder.setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(s.getBytes())
                .build()));
        final TableDescriptor descriptor = builder.build();
        admin.createTable(descriptor);
    }

    /**
     * Closing an {@link HBaseColumnManagerFactory} has no effect.
     */
    @Override
    public void close() {

    }


}

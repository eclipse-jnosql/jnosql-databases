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


import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.client.Result;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;

import java.util.ArrayList;
import java.util.List;

import static org.apache.hadoop.hbase.CellUtil.cloneFamily;
import static org.apache.hadoop.hbase.CellUtil.cloneQualifier;
import static org.apache.hadoop.hbase.CellUtil.cloneRow;
import static org.apache.hadoop.hbase.CellUtil.cloneValue;

class EntityUnit {

    private String rowKey;

    private String columnFamily;

    private final List<Element> columns = new ArrayList<>();

    EntityUnit(Result result) {

        for (Cell cell : result.rawCells()) {

            String name = new String(cloneQualifier(cell));
            String value = new String(cloneValue(cell));
            if (this.rowKey == null) {
                this.rowKey = new String(cloneRow(cell));
            }
            if (this.columnFamily == null) {
                this.columnFamily = new String(cloneFamily(cell));
            }
            columns.add(Element.of(name, value));
        }
    }


    public boolean isNotEmpty() {
        return !columns.isEmpty();
    }

    public CommunicationEntity toEntity() {
        var entity = CommunicationEntity.of(columnFamily);
        entity.addAll(columns);
        entity.add(HBaseUtils.getKey(rowKey));
        return entity;
    }

    @Override
    public String toString() {
        return "EntityUnit{" +
                "rowKey='" + rowKey + '\'' +
                ", columnFamily='" + columnFamily + '\'' +
                ", columns=" + columns +
                '}';
    }
}

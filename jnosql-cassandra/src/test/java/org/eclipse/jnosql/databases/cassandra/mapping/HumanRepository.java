/*
 *  Copyright (c) 2025 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.cassandra.mapping;

import jakarta.data.repository.Param;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
interface HumanRepository extends CassandraRepository<ContactCassandra, String> {

    void deleteByName(String namel);

    @CQL("select * from Person")
    List<ContactCassandra> findAllQuery();

    @CQL("select * from Person where name = ?")
    List<ContactCassandra> findByName(@Param("?")String name);

    @CQL("select * from Person where name = :name")
    List<ContactCassandra> findByName2(@Param("name") String name);
}
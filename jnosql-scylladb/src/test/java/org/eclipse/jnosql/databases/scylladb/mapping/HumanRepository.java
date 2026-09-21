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
package org.eclipse.jnosql.databases.scylladb.mapping;

import jakarta.data.repository.Param;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
interface HumanRepository extends ScyllaDBRepository<ContactScyllaDB, String> {

    void deleteByName(String namel);

    @CQL("select * from Person")
    List<ContactScyllaDB> findAllQuery();

    @CQL("select * from Person where name = ?")
    List<ContactScyllaDB> findByName(@Param("?")String name);

    @CQL("select * from Person where name = :name")
    List<ContactScyllaDB> findByName2(@Param("name") String name);
}
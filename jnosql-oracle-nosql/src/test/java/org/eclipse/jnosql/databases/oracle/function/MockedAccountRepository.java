/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.oracle.function;

import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface MockedAccountRepository extends BasicRepository<MockedAccount, String> {

    @Query("WHERE UPPER(name) = UPPER(:name)")
    List<MockedAccount> sameUpperName(@Param("name") String name);

    @Query("WHERE LEFT(name, LENGTH(:prefix)) = :prefix")
    List<MockedAccount> startingWith(@Param("prefix") String prefix);

    @Query("WHERE LEFT(name, balance) = 'x'")
    List<MockedAccount> unsupportedLength();
}
